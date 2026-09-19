<template>
  <div class="ost-tab" data-cy="ostTab-detail">
    <!-- STUB (step 6): filled in by step 10. -->
    <p class="ost-tab__stub">{{ node ? node.title : '' }}</p>
  </div>
</template>

<script setup lang="ts">
/** Detail-panel "detail" tab. Owned by step 10. */
import { computed } from 'vue';

import { useOstTreeStore } from '../../stores/ost-tree.store';

const props = defineProps<{ nodeKey: string }>();
const tree = useOstTreeStore();
const node = computed(() => tree.byId(props.nodeKey));
</script>
