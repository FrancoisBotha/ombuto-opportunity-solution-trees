import { computed, defineComponent, inject, ref, watch } from 'vue';

import { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import { OutcomeStatus } from '@/shared/model/enumerations/outcome-status.model';
import { SolutionStatus } from '@/shared/model/enumerations/solution-status.model';

import type { IOpportunityTreeNode, IOutcomeTreeNode, IProductTreeNode, ISolutionTreeNode, TreeNode, TreeNodeType } from './tree.model';
import TreeService from './tree.service';
import { useTreeStore, type UpdateNodePatch } from './tree.store';

interface ProductForm {
  name: string;
  description: string;
  vision: string;
  archived: boolean;
}
interface TitledForm {
  title: string;
  description: string;
  status: string;
  valuerating: number;
  complexity: number;
  effort: number;
}

const emptyTitled = (): TitledForm => ({ title: '', description: '', status: '', valuerating: 3, complexity: 3, effort: 3 });
const emptyProduct = (): ProductForm => ({ name: '', description: '', vision: '', archived: false });

export default defineComponent({
  name: 'TreeDetailPanel',
  emits: ['delete'],
  setup(_, { emit }) {
    const store = useTreeStore();
    const treeService = inject('treeService', () => new TreeService(), true);

    const productForm = ref<ProductForm>(emptyProduct());
    const titledForm = ref<TitledForm>(emptyTitled());
    const validationError = ref<string | null>(null);
    const serverError = ref<string | null>(null);
    const isSaving = ref(false);

    // Which selection the panel is *currently editing*. May lag behind the store's
    // selection while an unsaved-changes prompt is open.
    const editingType = ref<TreeNodeType | null>(null);
    const editingId = ref<number | null>(null);

    // Pending selection change waiting on the unsaved-changes prompt.
    interface Pending {
      type: TreeNodeType | null;
      id: number | null;
    }
    const pendingSwitch = ref<Pending | null>(null);
    const showUnsavedPrompt = computed(() => pendingSwitch.value !== null);
    // Set when keepEditing reverts the store's selection back to what we're editing:
    // the resulting selection watcher must NOT reset the form or the user's edits vanish.
    let suppressNextReset = false;

    const currentNode = computed<TreeNode | null>(() => {
      if (editingType.value == null || editingId.value == null) return null;
      return store.findNode(editingType.value, editingId.value);
    });

    const canEdit = computed(() => store.canEdit);
    const readOnly = computed(() => !canEdit.value);

    const nodeLabel = computed(() => {
      const n = currentNode.value;
      if (!n) return '';
      if (editingType.value === 'product') return (n as IProductTreeNode).name ?? '';
      return (n as { title: string }).title ?? '';
    });

    const outcomeStatusValues = Object.values(OutcomeStatus) as string[];
    const opportunityStatusValues = Object.values(OpportunityStatus) as string[];
    const solutionStatusValues = Object.values(SolutionStatus) as string[];
    const statusOptions = computed<string[]>(() => {
      if (editingType.value === 'outcome') return outcomeStatusValues;
      if (editingType.value === 'opportunity') return opportunityStatusValues;
      if (editingType.value === 'solution') return solutionStatusValues;
      return [];
    });

    const resetForm = (type: TreeNodeType | null, node: TreeNode | null) => {
      validationError.value = null;
      serverError.value = null;
      if (!type || !node) {
        productForm.value = emptyProduct();
        titledForm.value = emptyTitled();
        return;
      }
      if (type === 'product') {
        const p = node as IProductTreeNode;
        productForm.value = {
          name: p.name ?? '',
          description: p.description ?? '',
          vision: p.vision ?? '',
          archived: p.archived ?? false,
        };
      } else if (type === 'outcome') {
        const o = node as IOutcomeTreeNode;
        titledForm.value = {
          title: o.title ?? '',
          description: o.description ?? '',
          status: (o.status as unknown as string) ?? '',
          valuerating: 3,
          complexity: 3,
          effort: 3,
        };
      } else if (type === 'opportunity') {
        const o = node as IOpportunityTreeNode;
        titledForm.value = {
          title: o.title ?? '',
          description: o.description ?? '',
          status: (o.status as unknown as string) ?? '',
          valuerating: o.valuerating ?? 3,
          complexity: o.complexity ?? 3,
          effort: 3,
        };
      } else if (type === 'solution') {
        const s = node as ISolutionTreeNode;
        titledForm.value = {
          title: s.title ?? '',
          description: s.description ?? '',
          status: (s.status as unknown as string) ?? '',
          valuerating: 3,
          complexity: 3,
          effort: s.effort ?? 3,
        };
      }
    };

    const isDirty = (): boolean => {
      const type = editingType.value;
      const node = currentNode.value;
      if (!type || !node) return false;
      if (type === 'product') {
        const p = node as IProductTreeNode;
        return (
          productForm.value.name !== (p.name ?? '') ||
          productForm.value.description !== (p.description ?? '') ||
          productForm.value.vision !== (p.vision ?? '') ||
          productForm.value.archived !== (p.archived ?? false)
        );
      }
      const orig = node as any;
      if (titledForm.value.title !== (orig.title ?? '')) return true;
      if (titledForm.value.description !== (orig.description ?? '')) return true;
      if (titledForm.value.status !== ((orig.status as string) ?? '')) return true;
      if (type === 'opportunity') {
        if (titledForm.value.valuerating !== (orig.valuerating ?? 3)) return true;
        if (titledForm.value.complexity !== (orig.complexity ?? 3)) return true;
      }
      if (type === 'solution') {
        if (titledForm.value.effort !== (orig.effort ?? 3)) return true;
      }
      return false;
    };

    // TREE-007 unsaved-changes decision: PROMPT (not silent discard). Switching selection
    // while the form is dirty shows an in-panel prompt: 'Keep editing' reverts the selection
    // and keeps the edits; 'Discard changes' drops them and switches to the new node.
    // Sync editing state with the store's selection unless there are unsaved changes.
    const adoptStoreSelection = () => {
      editingType.value = store.selectedNodeType;
      editingId.value = store.selectedNodeId;
      resetForm(editingType.value, currentNode.value);
    };

    watch(
      () => [store.selectedNodeType, store.selectedNodeId] as const,
      newValue => {
        const type = newValue[0];
        const id = newValue[1];
        // Nothing being edited yet — adopt immediately.
        if (editingType.value == null || editingId.value == null) {
          adoptStoreSelection();
          return;
        }
        // Same node — reset form to fresh values (e.g. after a save), unless we're
        // just bouncing back from a "keep editing" choice.
        if (type === editingType.value && id === editingId.value) {
          if (suppressNextReset) {
            suppressNextReset = false;
            return;
          }
          resetForm(editingType.value, currentNode.value);
          return;
        }
        if (isDirty()) {
          pendingSwitch.value = { type, id };
        } else {
          adoptStoreSelection();
        }
      },
      { immediate: true },
    );

    const discardAndSwitch = () => {
      if (!pendingSwitch.value) return;
      // Commit the switch to the store's newest selection (which may have moved on).
      pendingSwitch.value = null;
      adoptStoreSelection();
    };
    const keepEditing = () => {
      if (!pendingSwitch.value) return;
      // Revert the store's selection to whatever we're still editing.
      const t = editingType.value;
      const i = editingId.value;
      pendingSwitch.value = null;
      if (t && i != null && (store.selectedNodeType !== t || store.selectedNodeId !== i)) {
        suppressNextReset = true;
        store.selectNode(t, i);
      }
    };

    const validate = (): string | null => {
      const type = editingType.value;
      if (!type) return null;
      if (type === 'product') {
        const name = productForm.value.name?.trim() ?? '';
        if (name.length < 2) return 'Name must be at least 2 characters.';
        if (name.length > 100) return 'Name must be at most 100 characters.';
        return null;
      }
      const title = titledForm.value.title?.trim() ?? '';
      if (title.length < 2) return 'Title must be at least 2 characters.';
      if (title.length > 200) return 'Title must be at most 200 characters.';
      if (type === 'opportunity') {
        const vr = Number(titledForm.value.valuerating);
        const cx = Number(titledForm.value.complexity);
        if (!Number.isFinite(vr) || vr < 1 || vr > 5) return 'Value rating must be between 1 and 5.';
        if (!Number.isFinite(cx) || cx < 1 || cx > 5) return 'Complexity must be between 1 and 5.';
      }
      if (type === 'solution') {
        const ef = Number(titledForm.value.effort);
        if (!Number.isFinite(ef) || ef < 1 || ef > 5) return 'Effort must be between 1 and 5.';
      }
      return null;
    };

    const buildPatch = (): UpdateNodePatch => {
      const type = editingType.value!;
      if (type === 'product') {
        return {
          name: productForm.value.name,
          description: productForm.value.description || null,
          vision: productForm.value.vision || null,
          archived: productForm.value.archived,
        };
      }
      if (type === 'outcome') {
        return {
          title: titledForm.value.title,
          description: titledForm.value.description || null,
          status: titledForm.value.status as any,
        };
      }
      if (type === 'opportunity') {
        return {
          title: titledForm.value.title,
          description: titledForm.value.description || null,
          status: titledForm.value.status as any,
          valuerating: Number(titledForm.value.valuerating),
          complexity: Number(titledForm.value.complexity),
        };
      }
      // solution
      return {
        title: titledForm.value.title,
        description: titledForm.value.description || null,
        status: titledForm.value.status as any,
        effort: Number(titledForm.value.effort),
      };
    };

    const save = async () => {
      serverError.value = null;
      const problem = validate();
      if (problem) {
        validationError.value = problem;
        return;
      }
      validationError.value = null;
      if (editingType.value == null || editingId.value == null) return;
      isSaving.value = true;
      try {
        const updated = await store.updateNode(editingType.value, editingId.value, buildPatch(), treeService());
        if (!updated) {
          serverError.value = store.writeErrorMessage ?? 'The change could not be saved.';
          return;
        }
        // Refresh the form baseline against the updated node.
        resetForm(editingType.value, currentNode.value);
      } finally {
        isSaving.value = false;
      }
    };

    const onDelete = () => {
      if (editingType.value == null || editingId.value == null) return;
      emit('delete', { type: editingType.value, id: editingId.value });
    };

    return {
      // state
      editingType,
      editingId,
      currentNode,
      canEdit,
      readOnly,
      productForm,
      titledForm,
      statusOptions,
      validationError,
      serverError,
      isSaving,
      showUnsavedPrompt,
      nodeLabel,
      // actions
      save,
      onDelete,
      discardAndSwitch,
      keepEditing,
    };
  },
});
