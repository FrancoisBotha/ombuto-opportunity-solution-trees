import { ref, shallowRef } from 'vue';

import { ReconnectionTimeMode, RxStomp, RxStompState, type RxStompConfig } from '@stomp/rx-stomp';
import Cookies from 'js-cookie';
import type { Observable, Subscription } from 'rxjs';
import SockJS from 'sockjs-client';

import { CSRF_TOKEN_COOKIE_NAME, CSRF_TOKEN_HEADER_NAME } from '@/shared/jhipster/constants';
import { encodeCsrfToken } from '@/shared/jhipster/encode-csrf-token';

import { defineStore } from 'pinia';

export type OstConnectionState = 'live' | 'reconnecting' | 'offline';
export type ReloadReason = 'reconnect' | 'gap' | 'epoch';

export interface OstRealtimeEvent {
  type: string;
  teamId: number;
  seq: number;
  epoch: string;
  // The server sends null when the change had no authenticated actor (TreeChangePublisher resolves
  // the login from the SecurityContext, which can be empty for a system-initiated write).
  actingUserLogin: string | null;
  at: string;
  payload?: unknown;
  [key: string]: unknown;
}

export interface OstRealtimeConsumer {
  applyEvents(events: OstRealtimeEvent[]): void;
  reloadTree(reason: ReloadReason): void | Promise<unknown>;
}

export interface SeqGapSignal {
  teamId: number;
  expected: number;
  actual: number;
}

export interface ReloadSignal {
  id: number;
  teamId: number;
  reason: ReloadReason;
}

interface StompMessage {
  body: string;
}

interface StompClient {
  connectionState$: Observable<RxStompState>;
  configure(config: RxStompConfig): void;
  activate(): void;
  deactivate(): Promise<void>;
  watch(destination: string): Observable<StompMessage>;
}

type FrameRequest = (callback: FrameRequestCallback) => number;
type FrameCancel = (handle: number) => void;

const INITIAL_RECONNECT_DELAY_MS = 1000;
const MAX_RECONNECT_DELAY_MS = 30000;
/**
 * How many events are held while a full tree read is in flight. Past this the buffer is abandoned
 * and another read is scheduled instead — cheaper than replaying an unbounded backlog, and the read
 * is authoritative anyway.
 */
const MAX_BUFFERED_DURING_RELOAD = 2000;

const websocketUrl = (): string => {
  const rawBaseHref = document.querySelector('base')?.getAttribute('href') ?? '/';
  const baseHref = rawBaseHref.endsWith('/') ? rawBaseHref : `${rawBaseHref}/`;
  return `//${window.location.host}${baseHref}websocket/tracker`;
};

const defaultFrameRequest: FrameRequest = callback => window.requestAnimationFrame(callback);
const defaultFrameCancel: FrameCancel = handle => window.cancelAnimationFrame(handle);

/**
 * Owns the single authenticated STOMP connection used while an OST team route is open.
 * Consumers receive ordered event batches, while uncertain transport state always falls back to
 * one authoritative tree read.
 */
export const useOstRealtimeStore = defineStore('ostRealtime', () => {
  const connectionState = ref<OstConnectionState>('offline');
  const activeTeamId = ref<number | null>(null);
  const lastSeqByTeam = ref<Record<number, number>>({});
  const epochByTeam = ref<Record<number, string>>({});
  const gapSignal = shallowRef<SeqGapSignal | null>(null);
  const reloadSignal = shallowRef<ReloadSignal | null>(null);

  let clientFactory: () => StompClient = () => new RxStomp();
  let client: StompClient | null = null;
  let stateSubscription: Subscription | null = null;
  let topicSubscription: Subscription | null = null;
  let consumer: OstRealtimeConsumer | null = null;
  let activated = false;
  let transportOpen = false;
  let hasConnected = false;
  let reloadId = 0;
  let reloadPendingTeam: number | null = null;
  const lastReceivedByTeam = new Map<number, number>();
  let frameRequest = defaultFrameRequest;
  let frameCancel = defaultFrameCancel;
  let frameHandle: number | null = null;
  let queuedEvents: OstRealtimeEvent[] = [];
  /** Events that arrived while a full tree read was in flight; replayed once it resolves. */
  let bufferedDuringReload: OstRealtimeEvent[] = [];
  let bufferOverflowed = false;

  function setClientFactory(factory: () => StompClient) {
    if (client) throw new Error('The realtime client factory must be set before opening a team.');
    clientFactory = factory;
  }

  function setFrameScheduler(request: FrameRequest, cancel: FrameCancel) {
    frameRequest = request;
    frameCancel = cancel;
  }

  function configureClient(): StompClient {
    if (client) return client;
    client = clientFactory();
    client.configure({
      webSocketFactory: () => new SockJS(websocketUrl()),
      reconnectDelay: INITIAL_RECONNECT_DELAY_MS,
      maxReconnectDelay: MAX_RECONNECT_DELAY_MS,
      reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      beforeConnect: connectedClient => {
        connectedClient.configure({
          connectHeaders: {
            [CSRF_TOKEN_HEADER_NAME]: encodeCsrfToken(Cookies.get(CSRF_TOKEN_COOKIE_NAME)),
          },
        });
      },
    });
    stateSubscription = client.connectionState$.subscribe(onConnectionState);
    return client;
  }

  function onConnectionState(state: RxStompState) {
    transportOpen = state === RxStompState.OPEN;
    if (activeTeamId.value === null) {
      connectionState.value = 'offline';
      return;
    }
    if (state === RxStompState.OPEN) {
      connectionState.value = 'live';
      if (hasConnected) {
        const teamId = activeTeamId.value;
        delete lastSeqByTeam.value[teamId];
        delete epochByTeam.value[teamId];
        lastReceivedByTeam.delete(teamId);
        requestReload('reconnect');
      }
      hasConnected = true;
      return;
    }
    connectionState.value = 'reconnecting';
  }

  function openTeam(teamId: number, nextConsumer: OstRealtimeConsumer) {
    if (!Number.isSafeInteger(teamId) || teamId <= 0) throw new Error('A positive team id is required.');
    const stomp = configureClient();
    consumer = nextConsumer;
    connectionState.value = transportOpen ? 'live' : 'reconnecting';

    if (activeTeamId.value !== teamId) {
      topicSubscription?.unsubscribe();
      topicSubscription = null;
      clearQueuedEvents();
      clearReloadBuffer();
      reloadPendingTeam = null;
      activeTeamId.value = teamId;
      delete epochByTeam.value[teamId];
      lastReceivedByTeam.delete(teamId);
      topicSubscription = stomp.watch(`/topic/teams/${teamId}/tree`).subscribe(message => receive(message.body));
    }

    if (!activated) {
      activated = true;
      stomp.activate();
    }
  }

  async function closeTeam() {
    activeTeamId.value = null;
    consumer = null;
    reloadPendingTeam = null;
    transportOpen = false;
    hasConnected = false;
    connectionState.value = 'offline';
    topicSubscription?.unsubscribe();
    topicSubscription = null;
    clearQueuedEvents();
    clearReloadBuffer();
    if (client && activated) {
      activated = false;
      await client.deactivate();
    }
  }

  function receive(body: string) {
    let incoming: unknown;
    try {
      incoming = JSON.parse(body);
    } catch {
      return;
    }
    if (!isRealtimeEvent(incoming) || incoming.teamId !== activeTeamId.value) return;
    if (reloadPendingTeam === incoming.teamId) {
      // A full tree read is in flight. Dropping the event here used to lose the write outright:
      // the read's snapshot is taken on the server before the event commits, the seq baseline has
      // just been cleared (so the miss never shows up as a gap), and the session then stays
      // silently behind until something else forces another read. Hold it instead and replay it
      // once the read lands — every apply* handler is keyed by id, so replaying a change the
      // snapshot already contains is a no-op.
      if (bufferedDuringReload.length >= MAX_BUFFERED_DURING_RELOAD) bufferOverflowed = true;
      else bufferedDuringReload.push(incoming);
      return;
    }
    accept(incoming);
  }

  /** Epoch / gap checks and queueing for one event that is not waiting on a reload. */
  function accept(incoming: OstRealtimeEvent) {
    const knownEpoch = epochByTeam.value[incoming.teamId];
    if (knownEpoch !== undefined && knownEpoch !== incoming.epoch) {
      epochByTeam.value[incoming.teamId] = incoming.epoch;
      delete lastSeqByTeam.value[incoming.teamId];
      lastReceivedByTeam.delete(incoming.teamId);
      clearQueuedEvents();
      requestReload('epoch');
      return;
    }
    epochByTeam.value[incoming.teamId] = incoming.epoch;

    const previous = lastReceivedByTeam.get(incoming.teamId);
    if (previous !== undefined && incoming.seq !== previous + 1) {
      gapSignal.value = { teamId: incoming.teamId, expected: previous + 1, actual: incoming.seq };
      lastReceivedByTeam.delete(incoming.teamId);
      clearQueuedEvents();
      requestReload('gap');
      return;
    }
    lastReceivedByTeam.set(incoming.teamId, incoming.seq);
    queuedEvents.push(incoming);
    if (frameHandle === null) frameHandle = frameRequest(flushEvents);
  }

  function flushEvents() {
    frameHandle = null;
    const events = queuedEvents;
    queuedEvents = [];
    if (events.length && consumer) {
      consumer.applyEvents(events);
      for (const event of events) lastSeqByTeam.value[event.teamId] = event.seq;
    }
  }

  function clearQueuedEvents() {
    queuedEvents = [];
    if (frameHandle !== null) {
      frameCancel(frameHandle);
      frameHandle = null;
    }
  }

  function clearReloadBuffer() {
    bufferedDuringReload = [];
    bufferOverflowed = false;
  }

  /**
   * Replays what arrived while the tree was being read. Runs synchronously the moment
   * `reloadPendingTeam` is cleared, so nothing can slip in between the two.
   */
  function drainReloadBuffer(teamId: number) {
    const held = bufferedDuringReload;
    const overflowed = bufferOverflowed;
    clearReloadBuffer();
    if (overflowed) {
      requestReload('gap');
      return;
    }
    for (const event of held) {
      if (event.teamId !== activeTeamId.value || reloadPendingTeam === teamId) break;
      accept(event);
    }
  }

  function requestReload(reason: ReloadReason) {
    const teamId = activeTeamId.value;
    const target = consumer;
    if (teamId === null || !target || reloadPendingTeam === teamId) return;
    reloadPendingTeam = teamId;
    clearReloadBuffer();
    reloadSignal.value = { id: ++reloadId, teamId, reason };
    let result: void | Promise<unknown>;
    try {
      result = target.reloadTree(reason);
    } catch {
      reloadPendingTeam = null;
      clearReloadBuffer();
      return;
    }
    Promise.resolve(result)
      .catch(() => undefined)
      .finally(() => {
        if (reloadPendingTeam !== teamId) return;
        reloadPendingTeam = null;
        drainReloadBuffer(teamId);
      });
  }

  function isRealtimeEvent(value: unknown): value is OstRealtimeEvent {
    if (!value || typeof value !== 'object') return false;
    const candidate = value as Partial<OstRealtimeEvent>;
    return (
      typeof candidate.type === 'string' &&
      Number.isSafeInteger(candidate.teamId) &&
      Number.isSafeInteger(candidate.seq) &&
      typeof candidate.epoch === 'string' &&
      // actingUserLogin may legitimately be null (a write with no authenticated actor). Dropping
      // such an event would show up later as a spurious seq gap and a full reload.
      (typeof candidate.actingUserLogin === 'string' || candidate.actingUserLogin === null) &&
      typeof candidate.at === 'string'
    );
  }

  return {
    connectionState,
    activeTeamId,
    lastSeqByTeam,
    epochByTeam,
    gapSignal,
    reloadSignal,
    setClientFactory,
    setFrameScheduler,
    openTeam,
    closeTeam,
  };
});
