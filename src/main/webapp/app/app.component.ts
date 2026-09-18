import { defineComponent, provide } from 'vue';

import { BToastOrchestrator } from 'bootstrap-vue-next';

import JhiFooter from '@/core/jhi-footer/jhi-footer.vue';
import JhiNavbar from '@/core/jhi-navbar/jhi-navbar.vue';
import Ribbon from '@/core/ribbon/ribbon.vue';
import { useAlertService } from '@/shared/alert/alert.service';
import '@/shared/config/dayjs';

export default defineComponent({
  name: 'App',
  components: {
    BToastOrchestrator,
    Ribbon,
    JhiNavbar,
    JhiFooter,
  },
  setup() {
    provide('alertService', useAlertService());

    return {};
  },
});
