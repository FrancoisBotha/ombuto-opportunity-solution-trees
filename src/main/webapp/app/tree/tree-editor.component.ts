import { computed, defineComponent, inject, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import type { TreeNodeType } from './tree.model';
import { layoutTree, type LayoutEdge, type LayoutNode } from './tree-layout';
import TreeNodeCard from './tree-node-card.vue';
import TreeService from './tree.service';
import { useTreeStore } from './tree.store';

const CANVAS_PADDING = 40;
const MIN_ZOOM = 0.25;
const MAX_ZOOM = 2.5;

export default defineComponent({
  name: 'TreeEditor',
  components: { TreeNodeCard },
  setup() {
    const route = useRoute();
    const treeService = inject('treeService', () => new TreeService(), true);
    const treeStore = useTreeStore();

    const teamId = computed(() => {
      const raw = route.params.teamId;
      const value = Array.isArray(raw) ? raw[0] : raw;
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : null;
    });

    const isLoading = computed(() => treeStore.loading);
    const hasError = computed(() => treeStore.error !== null);
    const errorKind = computed(() => treeStore.error);
    const errorMessage = computed(() => treeStore.errorMessage);
    const tree = computed(() => treeStore.tree);
    const canEdit = computed(() => treeStore.canEdit);
    const products = computed(() => treeStore.products);
    const focusedProductId = computed(() => treeStore.focusedProductId);

    const layout = computed(() =>
      layoutTree(tree.value, {
        focusedProductId: focusedProductId.value,
      }),
    );

    const nodes = computed<LayoutNode[]>(() => layout.value.nodes);
    const edges = computed<LayoutEdge[]>(() => layout.value.edges);
    const canvasWidth = computed(() => Math.max(200, layout.value.width + CANVAS_PADDING * 2));
    const canvasHeight = computed(() => Math.max(200, layout.value.height + CANVAS_PADDING * 2));

    const zoom = ref(1);
    const panX = ref(0);
    const panY = ref(0);

    const isDragging = ref(false);
    const dragStart = ref<{ x: number; y: number; panX: number; panY: number } | null>(null);

    const onCanvasMouseDown = (event: MouseEvent) => {
      isDragging.value = true;
      dragStart.value = { x: event.clientX, y: event.clientY, panX: panX.value, panY: panY.value };
    };
    const onCanvasMouseMove = (event: MouseEvent) => {
      if (!isDragging.value || !dragStart.value) return;
      panX.value = dragStart.value.panX + (event.clientX - dragStart.value.x);
      panY.value = dragStart.value.panY + (event.clientY - dragStart.value.y);
    };
    const onCanvasMouseUp = () => {
      isDragging.value = false;
      dragStart.value = null;
    };

    const onWheel = (event: WheelEvent) => {
      event.preventDefault();
      const delta = -event.deltaY * 0.001;
      const next = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, zoom.value + delta));
      zoom.value = next;
    };

    const zoomIn = () => {
      zoom.value = Math.min(MAX_ZOOM, zoom.value + 0.1);
    };
    const zoomOut = () => {
      zoom.value = Math.max(MIN_ZOOM, zoom.value - 0.1);
    };
    const resetView = () => {
      zoom.value = 1;
      panX.value = 0;
      panY.value = 0;
    };

    const isSelected = (type: TreeNodeType, id: number) => treeStore.selectedNodeType === type && treeStore.selectedNodeId === id;

    const onNodeSelect = (type: TreeNodeType, id: number) => {
      treeStore.selectNode(type, id);
    };
    const onCanvasClick = (event: MouseEvent) => {
      // Only clear selection when the empty canvas itself was clicked, not a node.
      if ((event.target as HTMLElement).closest('.tree-node-card')) return;
      treeStore.clearSelection();
    };

    const onFocusProduct = (event: Event) => {
      const value = (event.target as HTMLSelectElement).value;
      if (!value) {
        treeStore.clearFocus();
      } else {
        const id = Number(value);
        if (Number.isFinite(id)) treeStore.focusProduct(id);
      }
    };
    const clearFocus = () => treeStore.clearFocus();

    const load = async () => {
      if (teamId.value == null) return;
      await treeStore.load(teamId.value, treeService());
    };

    onMounted(load);
    watch(teamId, () => {
      resetView();
      load();
    });

    return {
      teamId,
      isLoading,
      hasError,
      errorKind,
      errorMessage,
      tree,
      canEdit,
      products,
      focusedProductId,
      nodes,
      edges,
      canvasWidth,
      canvasHeight,
      zoom,
      panX,
      panY,
      isDragging,
      onCanvasMouseDown,
      onCanvasMouseMove,
      onCanvasMouseUp,
      onWheel,
      zoomIn,
      zoomOut,
      resetView,
      isSelected,
      onNodeSelect,
      onCanvasClick,
      onFocusProduct,
      clearFocus,
      load,
      canvasPadding: CANVAS_PADDING,
    };
  },
});
