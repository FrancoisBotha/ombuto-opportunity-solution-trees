import { computed, defineComponent, type PropType } from 'vue';

import type { IOpportunityTreeNode, IOutcomeTreeNode, IProductTreeNode, ISolutionTreeNode, TreeNode, TreeNodeType } from './tree.model';
import { validChildTypes } from './tree.model';
import { isMovableType } from './tree-move';

export default defineComponent({
  name: 'TreeNodeCard',
  props: {
    type: {
      type: String as PropType<TreeNodeType>,
      required: true,
    },
    node: {
      type: Object as PropType<TreeNode>,
      required: true,
    },
    selected: {
      type: Boolean,
      default: false,
    },
    canEdit: {
      type: Boolean,
      default: false,
    },
    canMoveUp: {
      type: Boolean,
      default: false,
    },
    canMoveDown: {
      type: Boolean,
      default: false,
    },
    canMoveTo: {
      type: Boolean,
      default: false,
    },
    x: { type: Number, default: 0 },
    y: { type: Number, default: 0 },
    width: { type: Number, default: 200 },
    height: { type: Number, default: 80 },
    hasChildren: { type: Boolean, default: false },
    collapsed: { type: Boolean, default: false },
    hiddenDescendantCount: { type: Number, default: 0 },
  },
  emits: ['select', 'add-child', 'delete', 'move-to', 'move-up', 'move-down', 'toggle-collapse'],
  setup(props, { emit }) {
    const nodeId = computed<number>(() => (props.node as { id: number }).id);
    const title = computed<string>(() => {
      if (props.type === 'product') return (props.node as IProductTreeNode).name;
      return (props.node as { title: string }).title;
    });
    const status = computed<string | null>(() => {
      if (props.type === 'product') {
        return (props.node as IProductTreeNode).archived ? 'ARCHIVED' : null;
      }
      const s = (props.node as IOutcomeTreeNode | IOpportunityTreeNode | ISolutionTreeNode).status;
      return s ? String(s) : null;
    });
    const typeLabel = computed(() => {
      switch (props.type) {
        case 'product':
          return 'Product';
        case 'outcome':
          return 'Outcome';
        case 'opportunity':
          return 'Opportunity';
        case 'solution':
          return 'Solution';
        default:
          return props.type;
      }
    });
    const cardClasses = computed(() => ({
      'tree-node-card': true,
      [`tree-node-card--${props.type}`]: true,
      'tree-node-card--selected': props.selected,
    }));
    const style = computed(() => ({
      left: `${props.x}px`,
      top: `${props.y}px`,
      width: `${props.width}px`,
      height: `${props.height}px`,
    }));
    const onClick = (event: Event) => {
      event.stopPropagation();
      emit('select');
    };
    const validChildren = computed<TreeNodeType[]>(() => validChildTypes(props.type));
    const canAddChild = computed(() => props.canEdit && validChildren.value.length > 0);
    const onAddChild = (childType: TreeNodeType, event: Event) => {
      event.stopPropagation();
      emit('add-child', { parentType: props.type, parentId: nodeId.value, childType });
    };
    const onDelete = (event: Event) => {
      event.stopPropagation();
      emit('delete', { type: props.type, id: nodeId.value });
    };
    const showMoveTo = computed(() => props.canEdit && isMovableType(props.type) && props.canMoveTo);
    const showReorder = computed(() => props.canEdit);
    const reorderPrevLabel = computed(() => (props.type === 'product' ? 'Move left' : 'Move up'));
    const reorderNextLabel = computed(() => (props.type === 'product' ? 'Move right' : 'Move down'));
    const onMoveTo = (event: Event) => {
      event.stopPropagation();
      emit('move-to', { type: props.type, id: nodeId.value });
    };
    const onMoveUp = (event: Event) => {
      event.stopPropagation();
      emit('move-up', { type: props.type, id: nodeId.value });
    };
    const onMoveDown = (event: Event) => {
      event.stopPropagation();
      emit('move-down', { type: props.type, id: nodeId.value });
    };
    const onToggleCollapse = (event: Event) => {
      event.stopPropagation();
      emit('toggle-collapse', { type: props.type, id: nodeId.value });
    };
    const collapseLabel = computed(() =>
      props.collapsed ? `Expand ${typeLabel.value.toLowerCase()}` : `Collapse ${typeLabel.value.toLowerCase()}`,
    );
    const childTypeLabel = (t: TreeNodeType) => {
      switch (t) {
        case 'product':
          return 'Product';
        case 'outcome':
          return 'Outcome';
        case 'opportunity':
          return 'Opportunity';
        case 'solution':
          return 'Solution';
      }
    };
    return {
      nodeId,
      title,
      status,
      typeLabel,
      cardClasses,
      style,
      onClick,
      validChildren,
      canAddChild,
      onAddChild,
      onDelete,
      showMoveTo,
      showReorder,
      reorderPrevLabel,
      reorderNextLabel,
      onMoveTo,
      onMoveUp,
      onMoveDown,
      onToggleCollapse,
      collapseLabel,
      childTypeLabel,
    };
  },
});
