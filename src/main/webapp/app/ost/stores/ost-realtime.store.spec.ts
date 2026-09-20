import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { RxStompState } from '@stomp/rx-stomp';
import { ReconnectionTimeMode } from '@stomp/stompjs';
import { createPinia, setActivePinia } from 'pinia';
import { Observable, Subject } from 'rxjs';

import { type OstRealtimeEvent, type ReloadReason, useOstRealtimeStore } from './ost-realtime.store';

class FakeStompClient {
  readonly connectionState$ = new Subject<RxStompState>();
  readonly messages = new Subject<{ body: string }>();
  readonly configure = vi.fn();
  readonly activate = vi.fn();
  readonly deactivate = vi.fn(async () => undefined);
  readonly watched: string[] = [];
  readonly unsubscribed: string[] = [];

  watch(destination: string) {
    this.watched.push(destination);
    return new Observable<{ body: string }>(subscriber => {
      const subscription = this.messages.subscribe(subscriber);
      return () => {
        this.unsubscribed.push(destination);
        subscription.unsubscribe();
      };
    });
  }

  emit(event: OstRealtimeEvent) {
    this.messages.next({ body: JSON.stringify(event) });
  }
}

const event = (teamId: number, seq: number, epoch = 'epoch-a'): OstRealtimeEvent => ({
  type: 'NODE_UPDATED',
  teamId,
  seq,
  epoch,
  actingUserLogin: 'alice',
  at: '2026-09-20T00:00:00Z',
  payload: { key: 'opportunity-1' },
});

describe('OST realtime store', () => {
  let client: FakeStompClient;
  let store: ReturnType<typeof useOstRealtimeStore>;
  let applyEvents: Mock<(events: OstRealtimeEvent[]) => void>;
  let reloadTree: Mock<(reason: ReloadReason) => void>;
  let frames: FrameRequestCallback[];

  beforeEach(() => {
    setActivePinia(createPinia());
    client = new FakeStompClient();
    store = useOstRealtimeStore();
    store.setClientFactory(() => client);
    frames = [];
    store.setFrameScheduler(callback => {
      frames.push(callback);
      return frames.length;
    }, vi.fn());
    applyEvents = vi.fn();
    reloadTree = vi.fn();
  });

  const open = (teamId = 7) => store.openTeam(teamId, { applyEvents, reloadTree });
  const flushFrame = () => frames.shift()?.(performance.now());

  it('connects with the authenticated SockJS client and subscribes to the open team topic', () => {
    document.cookie = 'XSRF-TOKEN=session-csrf';
    open();

    const config = client.configure.mock.calls[0][0];
    expect(config).toEqual(
      expect.objectContaining({
        reconnectDelay: 1000,
        maxReconnectDelay: 30000,
        reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
        webSocketFactory: expect.any(Function),
        beforeConnect: expect.any(Function),
      }),
    );
    config.beforeConnect(client);
    expect(client.configure).toHaveBeenLastCalledWith({
      connectHeaders: { 'X-XSRF-TOKEN': expect.stringMatching(/^[\w-]+$/) },
    });
    expect(client.activate).toHaveBeenCalledOnce();
    expect(client.watched).toEqual(['/topic/teams/7/tree']);
  });

  it('unsubscribes the previous team and disconnects cleanly when the OST route closes', async () => {
    open(7);
    store.openTeam(8, { applyEvents, reloadTree });

    expect(client.unsubscribed).toEqual(['/topic/teams/7/tree']);
    expect(client.watched).toEqual(['/topic/teams/7/tree', '/topic/teams/8/tree']);

    await store.closeTeam();
    expect(client.unsubscribed).toEqual(['/topic/teams/7/tree', '/topic/teams/8/tree']);
    expect(client.deactivate).toHaveBeenCalledOnce();
    expect(store.connectionState).toBe('offline');
  });

  it('stays live when switching teams over an already-open connection', () => {
    open(7);
    client.connectionState$.next(RxStompState.OPEN);

    store.openTeam(8, { applyEvents, reloadTree });

    expect(store.connectionState).toBe('live');
    expect(client.unsubscribed).toEqual(['/topic/teams/7/tree']);
    expect(client.watched.at(-1)).toBe('/topic/teams/8/tree');
  });

  it('exposes reconnecting, live and offline states and requests one reload after reconnect', async () => {
    open();
    expect(store.connectionState).toBe('reconnecting');

    client.connectionState$.next(RxStompState.OPEN);
    expect(store.connectionState).toBe('live');
    expect(reloadTree).not.toHaveBeenCalled();

    client.connectionState$.next(RxStompState.CLOSED);
    expect(store.connectionState).toBe('reconnecting');
    client.connectionState$.next(RxStompState.OPEN);
    await Promise.resolve();

    expect(store.connectionState).toBe('live');
    expect(reloadTree).toHaveBeenCalledExactlyOnceWith('reconnect');
    expect(store.reloadSignal).toMatchObject({ teamId: 7, reason: 'reconnect' });
  });

  it('uses capped exponential reconnect backoff so failed connections do not busy-loop', () => {
    open();
    const config = client.configure.mock.calls[0][0];

    expect(config.reconnectDelay).toBeGreaterThanOrEqual(1000);
    expect(config.maxReconnectDelay).toBe(30000);
    expect(config.reconnectTimeMode).toBe(ReconnectionTimeMode.EXPONENTIAL);
  });

  it('tracks sequence per team, raises a gap signal and asks for one full reload', async () => {
    open();
    client.emit(event(7, 40));
    flushFrame();
    client.emit(event(7, 42));
    client.emit(event(7, 43));
    await Promise.resolve();

    expect(store.lastSeqByTeam[7]).toBe(40);
    expect(store.gapSignal).toEqual({ teamId: 7, expected: 41, actual: 42 });
    expect(reloadTree).toHaveBeenCalledExactlyOnceWith('gap');
    expect(store.reloadSignal).toMatchObject({ teamId: 7, reason: 'gap' });
  });

  it('raises one reload signal when the server epoch changes', async () => {
    open();
    client.emit(event(7, 1, 'epoch-a'));
    client.emit(event(7, 2, 'epoch-b'));
    client.emit(event(7, 3, 'epoch-b'));
    await Promise.resolve();

    expect(store.epochByTeam[7]).toBe('epoch-b');
    expect(reloadTree).toHaveBeenCalledExactlyOnceWith('epoch');
    expect(store.reloadSignal).toMatchObject({ teamId: 7, reason: 'epoch' });
  });

  it('hands a burst of valid events to its consumer once per animation frame', () => {
    open();
    client.emit(event(7, 10));
    client.emit(event(7, 11));
    client.emit(event(7, 12));

    expect(applyEvents).not.toHaveBeenCalled();
    expect(frames).toHaveLength(1);
    flushFrame();

    expect(applyEvents).toHaveBeenCalledOnce();
    expect(applyEvents.mock.calls[0][0].map((item: OstRealtimeEvent) => item.seq)).toEqual([10, 11, 12]);
  });

  it('accepts an event whose actingUserLogin is null instead of dropping it as a gap', () => {
    open();
    client.emit({ ...event(7, 10), actingUserLogin: null });
    client.emit(event(7, 11));
    flushFrame();

    expect(applyEvents).toHaveBeenCalledOnce();
    expect(applyEvents.mock.calls[0][0].map((item: OstRealtimeEvent) => item.seq)).toEqual([10, 11]);
    expect(applyEvents.mock.calls[0][0][0].actingUserLogin).toBeNull();
    // The null-actor event was counted, so no gap is reported for seq 11.
    expect(store.gapSignal).toBeNull();
    expect(reloadTree).not.toHaveBeenCalled();
    expect(store.lastSeqByTeam[7]).toBe(11);
  });

  it('still drops an event whose actingUserLogin is neither a string nor null', () => {
    open();
    client.emit({ ...event(7, 10), actingUserLogin: 42 } as unknown as OstRealtimeEvent);

    expect(frames).toHaveLength(0);
    expect(applyEvents).not.toHaveBeenCalled();
  });
});
