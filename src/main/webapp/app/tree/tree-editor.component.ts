import { computed, defineComponent, inject, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';

import TreeService from './tree.service';
import { useTreeStore } from './tree.store';

export default defineComponent({
  name: 'TreeEditor',
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

    const load = async () => {
      if (teamId.value == null) return;
      await treeStore.load(teamId.value, treeService());
    };

    onMounted(load);
    watch(teamId, load);

    return {
      teamId,
      isLoading,
      hasError,
      errorKind,
      errorMessage,
      tree,
      canEdit,
      products,
      load,
    };
  },
});
