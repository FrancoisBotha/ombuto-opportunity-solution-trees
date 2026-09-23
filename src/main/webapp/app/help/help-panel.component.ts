import { computed, defineComponent, nextTick, onBeforeUnmount, ref, watch } from 'vue';

import { type HelpTopic, helpTopics } from './help-topics';

export default defineComponent({
  name: 'HelpPanel',
  setup() {
    const open = ref(false);
    const query = ref('');
    const selectedId = ref<string | null>(null);
    const linkRef = ref<HTMLElement | null>(null);
    const panelRef = ref<HTMLElement | null>(null);
    const searchRef = ref<HTMLInputElement | null>(null);

    const selectedTopic = computed<HelpTopic | null>(() =>
      selectedId.value ? (helpTopics.find(t => t.id === selectedId.value) ?? null) : null,
    );

    const filteredTopics = computed<HelpTopic[]>(() => {
      const q = query.value.trim().toLowerCase();
      if (!q) {
        return [...helpTopics];
      }
      return helpTopics.filter(t => {
        const haystack = [t.title, t.summary, ...t.steps].join(' ').toLowerCase();
        return haystack.includes(q);
      });
    });

    const openPanel = () => {
      open.value = true;
      selectedId.value = null;
      query.value = '';
      nextTick(() => {
        const el = searchRef.value ?? panelRef.value;
        if (el && typeof (el as HTMLElement).focus === 'function') {
          (el as HTMLElement).focus();
        }
      });
    };

    const closePanel = () => {
      if (!open.value) {
        return;
      }
      open.value = false;
      selectedId.value = null;
      nextTick(() => {
        linkRef.value?.focus();
      });
    };

    const selectTopic = (id: string) => {
      selectedId.value = id;
    };

    const backToList = () => {
      selectedId.value = null;
    };

    const onKeydown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && open.value) {
        event.preventDefault();
        closePanel();
      }
    };

    watch(
      open,
      isOpen => {
        if (isOpen) {
          document.addEventListener('keydown', onKeydown);
        } else {
          document.removeEventListener('keydown', onKeydown);
        }
      },
      { immediate: false },
    );

    onBeforeUnmount(() => {
      document.removeEventListener('keydown', onKeydown);
    });

    return {
      open,
      query,
      selectedTopic,
      filteredTopics,
      linkRef,
      panelRef,
      searchRef,
      openPanel,
      closePanel,
      selectTopic,
      backToList,
    };
  },
});
