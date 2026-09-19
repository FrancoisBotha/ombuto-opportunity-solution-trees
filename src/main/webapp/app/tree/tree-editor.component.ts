import { computed, defineComponent, inject, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { useAlertService } from '@/shared/alert/alert.service';

import type { TreeNode, TreeNodeType } from './tree.model';
import { validChildTypes } from './tree.model';
import { layoutTree, type LayoutEdge, type LayoutNode } from './tree-layout';
import TreeNodeCard from './tree-node-card.vue';
import TreeService, { type CreateChildInput, type CreateProductInput } from './tree.service';
import { useTreeStore, type MoveToast } from './tree.store';
import { listValidTargets, type MoveTarget } from './tree-move';
import { createDragLifecycle, resolveDropTarget, type DropTarget } from './tree-drag';

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

    // Optional — the bootstrap-vue-next toast plugin may not be provided in
    // some test harnesses; fall back to a no-op so keyboard/reorder handlers
    // still run without crashing.
    let alertService: ReturnType<typeof useAlertService> | null = null;
    try {
      alertService = useAlertService();
    } catch {
      alertService = null;
    }
    const moveToast: MoveToast = {
      showError: (msg: string) => {
        if (alertService) alertService.showError(msg);
      },
    };

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
      // Never start a canvas pan when the mousedown originated on a node card —
      // that mousedown is owned by the node-drag lifecycle below.
      if ((event.target as HTMLElement).closest('.tree-node-card')) return;
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

    // --- Move-to dialog state ---
    const moveContext = ref<{ type: TreeNodeType; id: number; label: string } | null>(null);
    const moveTargets = ref<MoveTarget[]>([]);
    const selectedMoveTarget = ref<MoveTarget | null>(null);
    const isMovingNode = ref(false);
    const moveErrorMessage = ref<string | null>(null);

    const returnFocusToNode = (type: TreeNodeType, id: number) => {
      nextTick(() => {
        const selector = `[data-cy="treeNode-${type}-${id}"]`;
        const el = document.querySelector(selector) as HTMLElement | null;
        if (el) el.focus();
      });
    };

    const openMoveModal = (type: TreeNodeType, id: number) => {
      if (!canEdit.value) return;
      const node = treeStore.findNode(type, id);
      if (!node) return;
      const label = type === 'product' ? (node as any).name : (node as any).title;
      const parent = treeStore.parentOf(type, id);
      const targets = listValidTargets({
        tree: tree.value,
        nodeType: type,
        nodeId: id,
        node: node as TreeNode,
        currentParentType: parent ? parent.type : 'team',
        currentParentId: parent ? parent.id : null,
      });
      moveContext.value = { type, id, label };
      moveTargets.value = targets;
      selectedMoveTarget.value = targets.length > 0 ? targets[0] : null;
      moveErrorMessage.value = null;
      // Move focus into the dialog so keyboard users can operate it immediately
      // (Escape, Tab / arrow through targets, Enter to confirm).
      nextTick(() => {
        const dialog = document.querySelector('[data-cy="treeEditorMoveModal"]') as HTMLElement | null;
        if (!dialog) return;
        const firstTarget = dialog.querySelector('.tree-editor-move-target, [data-cy="moveCancel"]') as HTMLElement | null;
        (firstTarget ?? dialog).focus();
      });
    };
    const closeMoveModal = () => {
      const ctx = moveContext.value;
      moveContext.value = null;
      moveTargets.value = [];
      selectedMoveTarget.value = null;
      moveErrorMessage.value = null;
      if (ctx) returnFocusToNode(ctx.type, ctx.id);
    };
    const selectMoveTarget = (target: MoveTarget) => {
      selectedMoveTarget.value = target;
    };
    const confirmMove = async () => {
      const ctx = moveContext.value;
      const target = selectedMoveTarget.value;
      if (!ctx || !target) return;
      isMovingNode.value = true;
      moveErrorMessage.value = null;
      try {
        const ok = await treeStore.moveNode(ctx.type, ctx.id, target.parentType, target.parentId, {
          service: treeService(),
          toast: {
            showError: (msg: string) => {
              // Keep an inline copy in the dialog so it's visible right where the action was taken,
              // and also surface it via the app-wide toast service (fulfils the epic's "a toast explains why").
              moveErrorMessage.value = msg;
              moveToast.showError(msg);
            },
          },
        });
        if (ok) {
          const type = ctx.type;
          const id = ctx.id;
          moveContext.value = null;
          moveTargets.value = [];
          selectedMoveTarget.value = null;
          returnFocusToNode(type, id);
        }
      } finally {
        isMovingNode.value = false;
      }
    };

    const groupedMoveTargets = computed(() => {
      const groups: Array<{ productId: number; productName: string; targets: MoveTarget[] }> = [];
      for (const target of moveTargets.value) {
        let group = groups.find(g => g.productId === target.productId);
        if (!group) {
          group = { productId: target.productId, productName: target.productName, targets: [] };
          groups.push(group);
        }
        group.targets.push(target);
      }
      return groups;
    });

    // --- Reorder ---
    const canMoveUpOf = (type: TreeNodeType, id: number) => treeStore.canMoveUp(type, id);
    const canMoveDownOf = (type: TreeNodeType, id: number) => treeStore.canMoveDown(type, id);
    const canMoveToOf = (type: TreeNodeType, id: number): boolean => {
      if (!canEdit.value) return false;
      const node = treeStore.findNode(type, id);
      if (!node) return false;
      const parent = treeStore.parentOf(type, id);
      const targets = listValidTargets({
        tree: tree.value,
        nodeType: type,
        nodeId: id,
        node: node as TreeNode,
        currentParentType: parent ? parent.type : 'team',
        currentParentId: parent ? parent.id : null,
      });
      return targets.length > 0;
    };
    const reorderPrev = async (type: TreeNodeType, id: number) => {
      if (!canEdit.value) return;
      await treeStore.reorderSibling(type, id, -1, { service: treeService(), toast: moveToast });
      returnFocusToNode(type, id);
    };
    const reorderNext = async (type: TreeNodeType, id: number) => {
      if (!canEdit.value) return;
      await treeStore.reorderSibling(type, id, 1, { service: treeService(), toast: moveToast });
      returnFocusToNode(type, id);
    };

    // ---------- Drag-and-drop re-parenting / reordering ----------
    const canvasEl = ref<HTMLElement | null>(null);
    const dragActive = ref(false);
    const dragMovingType = ref<TreeNodeType | null>(null);
    const dragMovingId = ref<number | null>(null);
    const dragGhostLabel = ref<string>('');
    const dragGhostX = ref(0);
    const dragGhostY = ref(0);
    const dragTarget = ref<DropTarget | null>(null);
    /** Set to true when a drag activates so that the following click event is swallowed. */
    const swallowNextClick = ref(false);

    const clientToCanvas = (clientX: number, clientY: number) => {
      const el = canvasEl.value;
      if (!el) return { x: 0, y: 0 };
      const rect = el.getBoundingClientRect();
      // Reverse the viewport transform: translate(panX,panY) scale(zoom), then
      // account for the canvasPadding offset applied inside the viewport.
      const x = (clientX - rect.left - panX.value) / zoom.value - CANVAS_PADDING;
      const y = (clientY - rect.top - panY.value) / zoom.value - CANVAS_PADDING;
      return { x, y };
    };

    const drag = createDragLifecycle({
      canStart: () => canEdit.value,
      toCanvas: (cx, cy) => clientToCanvas(cx, cy),
      resolveTarget: pointer => {
        const type = dragMovingType.value;
        const id = dragMovingId.value;
        if (!type || id == null) return null;
        const node = treeStore.findNode(type, id);
        if (!node) return null;
        const parent = treeStore.parentOf(type, id);
        return resolveDropTarget({
          pointer,
          layoutNodes: nodes.value,
          tree: tree.value,
          movingType: type,
          movingId: id,
          movingNode: node as TreeNode,
          currentParentType: parent ? parent.type : 'team',
          currentParentId: parent ? parent.id : null,
        });
      },
      onActivate: () => {
        dragActive.value = true;
        swallowNextClick.value = true;
      },
      onMove: (_pointer, clientX, clientY, target) => {
        // Ghost tracks the cursor in client coordinates (fixed-position overlay).
        dragGhostX.value = clientX;
        dragGhostY.value = clientY;
        dragTarget.value = target;
      },
      onDrop: async (target: DropTarget) => {
        const type = dragMovingType.value;
        const id = dragMovingId.value;
        cleanupDragUI();
        if (!type || id == null) return;
        if (target.kind === 'reparent') {
          await treeStore.moveNode(type, id, target.parentType, target.parentId, {
            service: treeService(),
            toast: moveToast,
          });
        } else {
          await treeStore.moveNodeToPosition(
            type,
            id,
            target.parentType === 'team' ? null : target.parentType,
            target.parentType === 'team' ? null : target.parentId,
            target.position,
            { service: treeService(), toast: moveToast },
          );
        }
      },
      onCancel: () => {
        cleanupDragUI();
      },
    });

    const cleanupDragUI = () => {
      dragActive.value = false;
      dragMovingType.value = null;
      dragMovingId.value = null;
      dragGhostLabel.value = '';
      dragTarget.value = null;
    };

    const onNodeMouseDown = (event: MouseEvent) => {
      if (!canEdit.value) return;
      if (event.button !== 0) return;
      const cardEl = (event.target as HTMLElement).closest('.tree-node-card') as HTMLElement | null;
      if (!cardEl) return;
      // Ignore mousedown on any action button inside the card — those own the click.
      if ((event.target as HTMLElement).closest('button')) return;
      const rawType = cardEl.dataset.nodeType as TreeNodeType | undefined;
      const rawId = cardEl.dataset.nodeId ? Number(cardEl.dataset.nodeId) : NaN;
      if (!rawType || !Number.isFinite(rawId)) return;
      const node = treeStore.findNode(rawType, rawId);
      if (!node) return;
      dragMovingType.value = rawType;
      dragMovingId.value = rawId;
      dragGhostLabel.value = rawType === 'product' ? (node as any).name : (node as any).title;
      dragGhostX.value = event.clientX;
      dragGhostY.value = event.clientY;
      drag.onMouseDown(rawType, rawId, event.clientX, event.clientY);
      // Prevent the browser's native text selection / element drag on the card.
      event.preventDefault();
    };

    const onWindowMouseMove = (event: MouseEvent) => drag.onMouseMove(event.clientX, event.clientY);
    const onWindowMouseUp = () => drag.onMouseUp();
    const onWindowKeyDown = (event: KeyboardEvent) => drag.onKeyDown(event.key);

    onMounted(() => {
      window.addEventListener('mousemove', onWindowMouseMove);
      window.addEventListener('mouseup', onWindowMouseUp);
      window.addEventListener('keydown', onWindowKeyDown);
    });
    onBeforeUnmount(() => {
      window.removeEventListener('mousemove', onWindowMouseMove);
      window.removeEventListener('mouseup', onWindowMouseUp);
      window.removeEventListener('keydown', onWindowKeyDown);
    });

    /** Swallow the click that follows an activated drag so it doesn't also select the source node. */
    const onNodesClickCapture = (event: MouseEvent) => {
      if (swallowNextClick.value) {
        swallowNextClick.value = false;
        event.stopPropagation();
        event.preventDefault();
      }
    };

    const isDragValidReparentTarget = (type: TreeNodeType, id: number): boolean => {
      const t = dragTarget.value;
      return !!(t && t.kind === 'reparent' && t.parentType === type && t.parentId === id);
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
      // move-to / reorder
      moveContext,
      moveTargets,
      groupedMoveTargets,
      selectedMoveTarget,
      isMovingNode,
      moveErrorMessage,
      openMoveModal,
      closeMoveModal,
      selectMoveTarget,
      confirmMove,
      canMoveUpOf,
      canMoveDownOf,
      canMoveToOf,
      reorderPrev,
      reorderNext,
      // drag-and-drop
      canvasEl,
      dragActive,
      dragMovingType,
      dragMovingId,
      dragGhostLabel,
      dragGhostX,
      dragGhostY,
      dragTarget,
      onNodeMouseDown,
      onNodesClickCapture,
      isDragValidReparentTarget,
    };
  },
});
