/*
 * Canvas viewport maths + the Vue Flow glue for it.
 *
 * Vue Flow owns the viewport (x, y, zoom), but its own wheel zoom is switched off (zoom-on-scroll
 * false): the handoff wants a cursor-anchored ×1.08 per notch clamped to 0.35–1.6, and a Fit that
 * frames the current branch with a 0.68 floor so titles stay legible. The pure functions below are
 * the prototype's maths (Ombuto OST.dc.html bindWheel / fit) and are unit tested on their own.
 */
import { computed, onBeforeUnmount, watch, type Ref } from 'vue';

import type { VueFlowStore } from '@vue-flow/core';

import type { Placed } from '../domain/layout';

export interface Viewport {
  x: number;
  y: number;
  zoom: number;
}

export interface Point {
  x: number;
  y: number;
}

export interface Size {
  width: number;
  height: number;
}

export const ZOOM_MIN = 0.35;
export const ZOOM_MAX = 1.6;
/** Wheel step: ×1.08 per notch in, ×0.926 (≈ 1 / 1.08) out. */
export const WHEEL_IN = 1.08;
export const WHEEL_OUT = 0.926;
/** Toolbar −/+ step. */
export const BUTTON_STEP = 1.15;
/** Fit never zooms below this (titles stay legible) nor above FIT_MAX. */
export const FIT_FLOOR = 0.68;
export const FIT_MAX = 1.1;
/** Fit leaves this much air around the branch and sits it this far from the top edge. */
const FIT_MARGIN = 90;
const FIT_TOP = 28;

export const clampZoom = (zoom: number): number => Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, zoom));

/**
 * Zooms to `nextZoom` (clamped) keeping the screen point `anchor` (relative to the canvas) over the
 * same flow point: the flow point under the anchor is (anchor - t) / zoom before and after.
 */
export function zoomAt(viewport: Viewport, anchor: Point, nextZoom: number): Viewport {
  const zoom = clampZoom(nextZoom);
  const k = zoom / viewport.zoom;
  return {
    zoom,
    x: anchor.x - (anchor.x - viewport.x) * k,
    y: anchor.y - (anchor.y - viewport.y) * k,
  };
}

/** One wheel notch at the cursor: negative deltaY zooms in. */
export const wheelZoom = (viewport: Viewport, anchor: Point, deltaY: number): Viewport =>
  zoomAt(viewport, anchor, viewport.zoom * (deltaY < 0 ? WHEEL_IN : WHEEL_OUT));

/** Toolbar zoom: one step about the centre of the canvas. */
export const stepZoom = (viewport: Viewport, size: Size, direction: 1 | -1): Viewport =>
  zoomAt(viewport, { x: size.width / 2, y: size.height / 2 }, direction > 0 ? viewport.zoom * BUTTON_STEP : viewport.zoom / BUTTON_STEP);

/**
 * Fit: frame the laid-out branch. Zoom = the smaller of width/height ratios, floored at 0.68 and
 * capped at 1.1. When the branch is wider than the canvas at that zoom, centre on the roots
 * instead of the bounding box so the product(s) stay in view. Null when nothing is laid out.
 */
export function fitViewport(placed: Record<string, Placed>, rootIds: string[], size: Size): Viewport | null {
  const boxes = Object.values(placed);
  if (!boxes.length || size.width <= 0 || size.height <= 0) return null;
  let x0 = Infinity;
  let x1 = -Infinity;
  let y1 = -Infinity;
  for (const p of boxes) {
    x0 = Math.min(x0, p.x - p.w / 2);
    x1 = Math.max(x1, p.x + p.w / 2);
    y1 = Math.max(y1, p.y + p.h);
  }
  const zoom = Math.min(
    FIT_MAX,
    Math.max(FIT_FLOOR, Math.min((size.width - FIT_MARGIN) / (x1 - x0), (size.height - FIT_MARGIN) / (y1 + 40))),
  );
  const span = (x1 - x0) * zoom;
  const rootXs = rootIds.map(id => placed[id]?.x).filter((x): x is number => typeof x === 'number');
  const cx = rootXs.length ? rootXs.reduce((a, b) => a + b, 0) / rootXs.length : (x0 + x1) / 2;
  return {
    zoom,
    x: span < size.width ? (size.width - span) / 2 - x0 * zoom : size.width / 2 - cx * zoom,
    y: FIT_TOP,
  };
}

/** Viewport that puts the flow point `point` in the middle of the canvas at `zoom` (clamped). */
export function centreOn(point: Point, size: Size, zoom: number): Viewport {
  const z = clampZoom(zoom);
  return { zoom: z, x: size.width / 2 - point.x * z, y: size.height / 2 - point.y * z };
}

/** Room kept between a node brought into view (revealDelta) and the canvas edge, in screen px. */
export const REVEAL_MARGIN = 24;

/**
 * Screen delta to pan by so the laid-out box `p` is fully inside a canvas of `size` at `viewport`
 * (its zoom included): {0, 0} when it already is; otherwise the smallest move that leaves it
 * REVEAL_MARGIN inside the edge it crossed (a box larger than the canvas keeps its top-left in view).
 */
export function revealDelta(p: Placed, viewport: Viewport, size: Size, margin = REVEAL_MARGIN): Point {
  const z = viewport.zoom;
  const axis = (lo: number, hi: number, extent: number) => {
    if (lo >= 0 && hi <= extent) return 0;
    if (lo < 0 || hi - lo + 2 * margin > extent) return margin - lo;
    return extent - margin - hi;
  };
  const left = (p.x - p.w / 2) * z + viewport.x;
  const top = p.y * z + viewport.y;
  return { x: axis(left, left + p.w * z, size.width), y: axis(top, top + p.h * z, size.height) };
}

/**
 * Binds the maths to a Vue Flow store and the canvas element: a non-passive wheel listener
 * (Vue Flow's zoom-on-scroll must be off), toolbar steps, fit and centring.
 *
 * Automatic fits stay live: after a fit, until the user moves the view (wheel, pointer press in
 * the canvas, zoom buttons, centring, panning), every canvas resize fits again instead of just
 * shifting — the first fit often runs while the app sidebar is still settling, and a fit computed
 * for a canvas that then shrinks leaves the outer branches clipped.
 */
export function useViewport(flow: VueFlowStore, el: Ref<HTMLElement | null>) {
  const rect = () => {
    const r = el.value?.getBoundingClientRect();
    return { left: r?.left ?? 0, top: r?.top ?? 0, width: r?.width ?? 0, height: r?.height ?? 0 };
  };
  const size = (): Size => {
    const { width, height } = rect();
    return { width, height };
  };
  const current = (): Viewport => ({ ...flow.viewport.value });
  /** Canvas box the current viewport was computed for (see the resize observer below). */
  let last: ReturnType<typeof rect> | null = null;
  const apply = (next: Viewport, duration?: number) => {
    const r = rect();
    if (r.width && r.height) last = r;
    return flow.setViewport(next, duration ? { duration } : undefined);
  };

  /** Re-runs the last fit on resize while the view is still the automatic one; null once the user moved it. */
  let refit: (() => boolean) | null = null;
  /** The user took over the view: resizes no longer re-fit. */
  const userMoved = () => {
    refit = null;
  };

  function onWheel(event: WheelEvent) {
    event.preventDefault();
    userMoved();
    const r = el.value!.getBoundingClientRect();
    apply(wheelZoom(current(), { x: event.clientX - r.left, y: event.clientY - r.top }, event.deltaY));
  }

  // While a fit is live, a resize fits again. Otherwise: when the canvas's left/top edge moves
  // (app sidebar animating) keep the point in the middle of the canvas in the middle; when only the
  // right/bottom edge moves (detail panel opening or closing, window resize) the content stays
  // where it is on screen, as in the prototype — so the first click on a node (which opens the
  // panel) never slides the node from under the pointer, and a double-click to rename still lands
  // on the same title. Only a node the opening panel would clip is then eased back into view
  // (reveal, called by the canvas).
  const resizer =
    typeof ResizeObserver === 'undefined'
      ? null
      : new ResizeObserver(() => {
          const now = rect();
          if (last && now.width && now.height && (now.width !== last.width || now.height !== last.height)) {
            const anchored = Math.abs(now.left - last.left) < 0.5 && Math.abs(now.top - last.top) < 0.5;
            if (refit) refit();
            else if (!anchored) {
              const v = current();
              apply({ zoom: v.zoom, x: v.x + (now.width - last.width) / 2, y: v.y + (now.height - last.height) / 2 });
            }
          }
          last = now.width && now.height ? now : last;
        });

  watch(
    el,
    (next, prev) => {
      prev?.removeEventListener('wheel', onWheel);
      prev?.removeEventListener('pointerdown', userMoved, true);
      prev?.removeEventListener('keydown', userMoved, true);
      next?.addEventListener('wheel', onWheel, { passive: false });
      // Any press or key in the canvas (pan, node click, minimap, keyboard) is the user taking over.
      next?.addEventListener('pointerdown', userMoved, true);
      next?.addEventListener('keydown', userMoved, true);
      if (prev) resizer?.unobserve(prev);
      if (next) resizer?.observe(next);
    },
    { immediate: true },
  );
  onBeforeUnmount(() => {
    el.value?.removeEventListener('wheel', onWheel);
    el.value?.removeEventListener('pointerdown', userMoved, true);
    el.value?.removeEventListener('keydown', userMoved, true);
    resizer?.disconnect();
  });

  return {
    zoom: computed(() => flow.viewport.value.zoom),
    size,
    zoomIn: () => {
      userMoved();
      return apply(stepZoom(current(), size(), 1));
    },
    zoomOut: () => {
      userMoved();
      return apply(stepZoom(current(), size(), -1));
    },
    /**
     * Frames the laid-out branch (`placed()` / `rootIds()` are read again on every re-fit); returns
     * false when the canvas has no size yet or nothing is laid out. Stays live until the user moves.
     */
    fit(placed: () => Record<string, Placed>, rootIds: () => string[]): boolean {
      const run = () => {
        const next = fitViewport(placed(), rootIds(), size());
        if (next) apply(next);
        return !!next;
      };
      refit = run;
      return run();
    },
    /** Centres a laid-out box, never zooming out below the fit floor. */
    centreOnBox(p: Placed): boolean {
      const s = size();
      if (!s.width) return false;
      userMoved();
      apply(centreOn({ x: p.x, y: p.y + p.h / 2 }, s, Math.max(FIT_FLOOR, flow.viewport.value.zoom)));
      return true;
    },
    centreOnPoint(point: Point) {
      userMoved();
      apply(centreOn(point, size(), flow.viewport.value.zoom));
    },
    /** Moves the view by a screen delta (keeps a node anchored across a re-layout), eased like nodes. */
    panBy(dx: number, dy: number, duration = 180) {
      userMoved();
      const v = current();
      apply({ zoom: v.zoom, x: v.x + dx, y: v.y + dy }, duration);
    },
    /**
     * Eases the view just far enough that a laid-out box is fully visible (e.g. a node the opening
     * detail panel would clip); false when it already was or the canvas has no size.
     */
    reveal(p: Placed, duration = 220): boolean {
      const s = size();
      if (!s.width || !s.height) return false;
      const d = revealDelta(p, current(), s);
      if (Math.abs(d.x) < 0.5 && Math.abs(d.y) < 0.5) return false;
      userMoved();
      const v = current();
      apply({ zoom: v.zoom, x: v.x + d.x, y: v.y + d.y }, duration);
      return true;
    },
    userMoved,
  };
}
