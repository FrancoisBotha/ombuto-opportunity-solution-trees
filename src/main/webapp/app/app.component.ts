import { type ComputedRef, computed, defineComponent, inject, provide, ref } from 'vue';
import { useRoute } from 'vue-router';

import { BToastOrchestrator } from 'bootstrap-vue-next';

import JhiFooter from '@/core/jhi-footer/jhi-footer.vue';
import Navbar from '@/core/navbar/navbar.vue';
import SidebarMenu from '@/core/sidebar-menu/sidebar-menu.vue';
import { readStoredSidebarState } from '@/core/sidebar-menu/sidebar-menu.component';
import { useAlertService } from '@/shared/alert/alert.service';
import '@/shared/config/dayjs';

export default defineComponent({
  name: 'App',
  components: {
    BToastOrchestrator,
    Navbar,
    SidebarMenu,
    JhiFooter,
  },
  setup() {
    provide('alertService', useAlertService());
    const authenticated = inject<ComputedRef<boolean>>('authenticated');
    const sidebarExpanded = ref(readStoredSidebarState(true));
    const route = useRoute();
    // Routes with meta.fullBleed (the OST tree builder) render edge to edge: no card, no padding.
    const fullBleed = computed(() => !!route.meta?.fullBleed);

    return {
      authenticated,
      sidebarExpanded,
      fullBleed,
    };
  },
});
