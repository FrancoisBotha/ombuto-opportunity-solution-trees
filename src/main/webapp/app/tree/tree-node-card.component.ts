import { computed, defineComponent, type PropType } from 'vue';

import type { IOpportunityTreeNode, IOutcomeTreeNode, IProductTreeNode, ISolutionTreeNode, TreeNode, TreeNodeType } from './tree.model';

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
    x: { type: Number, default: 0 },
    y: { type: Number, default: 0 },
    width: { type: Number, default: 200 },
    height: { type: Number, default: 80 },
  },
  emits: ['select'],
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
    return { nodeId, title, status, typeLabel, cardClasses, style, onClick };
  },
});
