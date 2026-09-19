import { computed, defineComponent, inject, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import type { TreeNodeType } from './tree.model';
import { validChildTypes } from './tree.model';
import { layoutTree, type LayoutEdge, type LayoutNode } from './tree-layout';
import TreeNodeCard from './tree-node-card.vue';
import TreeService, { type CreateChildInput, type CreateProductInput } from './tree.service';
import { useTreeStore } from './tree.store';

const CANVAS_PADDING = 40;
const MIN_ZOOM = 0.25;
const MAX_ZOOM = 2.5;

interface AddChildContext {
  parentType: TreeNodeType;
  parentId: number;
  childType: TreeNodeType;
}

interface DeleteContext {
  type: TreeNodeType;
  id: number;
  label: string;
  descendantCount: number;
}

export default defineComponent({
  name: 'TreeEditor',
  components: { TreeNodeCard },
  setup() {
    const route = useRoute();
    const treeService = inject('treeService', () => new TreeService());
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
    const writeError = computed(() => treeStore.writeError);
    const writeErrorMessage = computed(() => treeStore.writeErrorMessage);

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
      if ((event.target as HTMLElement).closest('.tree-node-card')) return;
      if ((event.target as HTMLElement).closest('.tree-editor-modal')) return;
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

    // --- Add product modal state ---
    const showAddProductModal = ref(false);
    const productForm = ref<CreateProductInput>({ name: '', description: '', vision: '' });
    const isSavingProduct = ref(false);

    const openAddProductModal = () => {
      if (!canEdit.value) return;
      treeStore.clearWriteError();
      productForm.value = { name: '', description: '', vision: '' };
      showAddProductModal.value = true;
    };
    const closeAddProductModal = () => {
      showAddProductModal.value = false;
    };
    const submitAddProduct = async () => {
      if (!productForm.value.name || productForm.value.name.trim().length < 2) {
        treeStore.writeError = 'validation';
        treeStore.writeErrorMessage = 'Please enter a product name (at least 2 characters).';
        return;
      }
      isSavingProduct.value = true;
      try {
        const created = await treeStore.addProduct({ ...productForm.value }, treeService());
        if (created) closeAddProductModal();
      } finally {
        isSavingProduct.value = false;
      }
    };

    // --- Add child modal state ---
    const addChildContext = ref<AddChildContext | null>(null);
    const childForm = ref<CreateChildInput>({ title: '', description: '' });
    const isSavingChild = ref(false);

    const openAddChildModal = (parentType: TreeNodeType, parentId: number, childType: TreeNodeType) => {
      if (!canEdit.value) return;
      const allowed = validChildTypes(parentType);
      if (!allowed.includes(childType)) return;
      treeStore.clearWriteError();
      addChildContext.value = { parentType, parentId, childType };
      childForm.value = { title: '', description: '' };
    };
    const closeAddChildModal = () => {
      addChildContext.value = null;
    };
    const submitAddChild = async () => {
      const ctx = addChildContext.value;
      if (!ctx) return;
      if (!childForm.value.title || childForm.value.title.trim().length < 2) {
        treeStore.writeError = 'validation';
        treeStore.writeErrorMessage = 'Please enter a title (at least 2 characters).';
        return;
      }
      isSavingChild.value = true;
      try {
        const created = await treeStore.addChild(ctx.parentType, ctx.parentId, ctx.childType, { ...childForm.value }, treeService());
        if (created) closeAddChildModal();
      } finally {
        isSavingChild.value = false;
      }
    };

    // --- Delete confirmation modal state ---
    const deleteContext = ref<DeleteContext | null>(null);
    const isDeleting = ref(false);

    const openDeleteModal = (type: TreeNodeType, id: number) => {
      if (!canEdit.value) return;
      const node = treeStore.findNode(type, id);
      if (!node) return;
      const label = type === 'product' ? (node as any).name : (node as any).title;
      const descendantCount = treeStore.descendantCountOf(type, id);
      treeStore.clearWriteError();
      deleteContext.value = { type, id, label, descendantCount };
    };
    const closeDeleteModal = () => {
      deleteContext.value = null;
    };
    const confirmDelete = async () => {
      const ctx = deleteContext.value;
      if (!ctx) return;
      isDeleting.value = true;
      try {
        const ok = await treeStore.deleteNode(ctx.type, ctx.id, treeService());
        if (ok) closeDeleteModal();
      } finally {
        isDeleting.value = false;
      }
    };

    const dismissWriteError = () => treeStore.clearWriteError();

    // --- Detail panel state ---
    const OUTCOME_STATUSES = ['DRAFT', 'ACTIVE', 'ACHIEVED', 'ABANDONED'] as const;
    const OPPORTUNITY_STATUSES = ['IDENTIFIED', 'EXPLORING', 'PRIORITISED', 'IN_PROGRESS', 'ADDRESSED', 'PARKED', 'DISCARDED'] as const;
    const SOLUTION_STATUSES = ['IDEA', 'ASSUMPTION_MAPPING', 'TESTING', 'VALIDATED', 'INVALIDATED', 'SHIPPED', 'DROPPED'] as const;

    const selectedNode = computed(() => {
      if (!treeStore.selectedNodeType || treeStore.selectedNodeId == null) return null;
      return treeStore.findNode(treeStore.selectedNodeType, treeStore.selectedNodeId);
    });
    const selectedNodeType = computed(() => treeStore.selectedNodeType);
    const detailStatusOptions = computed<readonly string[]>(() => {
      switch (treeStore.selectedNodeType) {
        case 'outcome':
          return OUTCOME_STATUSES;
        case 'opportunity':
          return OPPORTUNITY_STATUSES;
        case 'solution':
          return SOLUTION_STATUSES;
        default:
          return [];
      }
    });
    const detailForm = ref<{ title: string; status: string }>({ title: '', status: '' });
    const isSavingDetail = ref(false);

    watch(
      [selectedNode, selectedNodeType],
      ([node, type]) => {
        if (!node || !type || type === 'product') {
          detailForm.value = { title: '', status: '' };
          return;
        }
        const anyNode = node as { title?: string; status?: string | null };
        detailForm.value = {
          title: anyNode.title ?? '',
          status: (anyNode.status as string | null | undefined) ?? '',
        };
      },
      { immediate: true },
    );

    const saveDetail = async () => {
      const type = treeStore.selectedNodeType;
      const id = treeStore.selectedNodeId;
      if (!type || id == null || type === 'product') return;
      const patch: Record<string, unknown> = {
        title: detailForm.value.title,
      };
      if (detailForm.value.status) patch.status = detailForm.value.status;
      isSavingDetail.value = true;
      try {
        await treeStore.updateNode(type, id, patch, treeService());
      } finally {
        isSavingDetail.value = false;
      }
    };

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
      writeError,
      writeErrorMessage,
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
      validChildTypes,
      // add product
      showAddProductModal,
      productForm,
      isSavingProduct,
      openAddProductModal,
      closeAddProductModal,
      submitAddProduct,
      // add child
      addChildContext,
      childForm,
      isSavingChild,
      openAddChildModal,
      closeAddChildModal,
      submitAddChild,
      // delete
      deleteContext,
      isDeleting,
      openDeleteModal,
      closeDeleteModal,
      confirmDelete,
      dismissWriteError,
      // detail panel
      selectedNode,
      selectedNodeType,
      detailStatusOptions,
      detailForm,
      isSavingDetail,
      saveDetail,
    };
  },
});
