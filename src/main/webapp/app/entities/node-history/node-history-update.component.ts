import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import UserService from '@/entities/user/user.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import { HistoryEventType } from '@/shared/model/enumerations/history-event-type.model';
import { TreeNodeType } from '@/shared/model/enumerations/tree-node-type.model';
import { type INodeHistory, NodeHistory } from '@/shared/model/node-history.model';

import NodeHistoryService from './node-history.service';

export default defineComponent({
  name: 'NodeHistoryUpdate',
  setup() {
    const nodeHistoryService = inject('nodeHistoryService', () => new NodeHistoryService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const nodeHistory: Ref<INodeHistory> = ref(new NodeHistory());
    const userService = inject('userService', () => new UserService());
    const users: Ref<Array<any>> = ref([]);
    const treeNodeTypeValues: Ref<string[]> = ref(Object.keys(TreeNodeType));
    const historyEventTypeValues: Ref<string[]> = ref(Object.keys(HistoryEventType));
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveNodeHistory = async nodeHistoryId => {
      try {
        const res = await nodeHistoryService().find(nodeHistoryId);
        res.createdDate = new Date(res.createdDate);
        nodeHistory.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.nodeHistoryId) {
      retrieveNodeHistory(route.params.nodeHistoryId);
    }

    const initRelationships = () => {
      userService()
        .retrieve()
        .then(res => {
          users.value = res.data;
        });
    };

    initRelationships();

    const validations = useValidation();
    const validationRules = {
      nodeType: {
        required: validations.required('This field is required.'),
      },
      nodeId: {
        required: validations.required('This field is required.'),
        integer: validations.integer('This field should be a number.'),
      },
      eventType: {
        required: validations.required('This field is required.'),
      },
      summary: {
        required: validations.required('This field is required.'),
        maxLength: validations.maxLength('This field cannot be longer than 500 characters.', 500),
      },
      createdDate: {
        required: validations.required('This field is required.'),
      },
      author: {},
    };
    const v$ = useVuelidate(validationRules, nodeHistory as any);
    v$.value.$validate();

    return {
      nodeHistoryService,
      alertService,
      nodeHistory,
      previousState,
      treeNodeTypeValues,
      historyEventTypeValues,
      isSaving,
      currentLanguage,
      users,
      v$,
      ...useDateFormat({ entityRef: nodeHistory }),
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.nodeHistory.id) {
        this.nodeHistoryService()
          .update(this.nodeHistory)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A NodeHistory is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.nodeHistoryService()
          .create(this.nodeHistory)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A NodeHistory is created with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
