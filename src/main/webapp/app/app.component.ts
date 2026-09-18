import { type ComputedRef, defineComponent, inject, provide, ref } from 'vue';

import { BToastOrchestrator } from 'bootstrap-vue-next';

import JhiFooter from '@/core/jhi-footer/jhi-footer.vue';
import Navbar from '@/core/navbar/navbar.vue';
import Ribbon from '@/core/ribbon/ribbon.vue';
import SidebarMenu from '@/core/sidebar-menu/sidebar-menu.vue';
import { readStoredSidebarState } from '@/core/sidebar-menu/sidebar-menu.component';
import { useAlertService } from '@/shared/alert/alert.service';
import '@/shared/config/dayjs';

export default defineComponent({
  name: 'App',
  components: {
    BToastOrchestrator,
    Ribbon,
    Navbar,
    SidebarMenu,
    JhiFooter,
  },
  setup() {
    provide('alertService', useAlertService());
    const authenticated = inject<ComputedRef<boolean>>('authenticated');
    const sidebarExpanded = ref(readStoredSidebarState(true));

    return {
      authenticated,
      sidebarExpanded,
    };
  },
});
