import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import ProductService from '@/entities/product/product.service';
import UserService from '@/entities/user/user.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { OutcomeStatus } from '@/shared/model/enumerations/outcome-status.model';
import { type IOutcome, Outcome } from '@/shared/model/outcome.model';
import { type IProduct } from '@/shared/model/product.model';

import OutcomeService from './outcome.service';

export default defineComponent({
  name: 'OutcomeUpdate',
  setup() {
    const outcomeService = inject('outcomeService', () => new OutcomeService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const outcome: Ref<IOutcome> = ref(new Outcome());

    const productService = inject('productService', () => new ProductService());

    const products: Ref<IProduct[]> = ref([]);
    const userService = inject('userService', () => new UserService());
    const users: Ref<Array<any>> = ref([]);
    const outcomeStatusValues: Ref<string[]> = ref(Object.keys(OutcomeStatus));
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveOutcome = async outcomeId => {
      try {
        const res = await outcomeService().find(outcomeId);
        res.createdDate = new Date(res.createdDate);
        res.lastModifiedDate = new Date(res.lastModifiedDate);
        outcome.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.outcomeId) {
      retrieveOutcome(route.params.outcomeId);
    }

    const initRelationships = () => {
      productService()
        .retrieve()
        .then(res => {
          products.value = res.data;
        });
      userService()
        .retrieve()
        .then(res => {
          users.value = res.data;
        });
    };

    initRelationships();

    const dataUtils = useDataUtils();

    const validations = useValidation();
    const validationRules = {
      title: {
        required: validations.required('This field is required.'),
        minLength: validations.minLength('This field is required to be at least 2 characters.', 2),
        maxLength: validations.maxLength('This field cannot be longer than 200 characters.', 200),
      },
      description: {},
      metric: {
        maxLength: validations.maxLength('This field cannot be longer than 200 characters.', 200),
      },
      targetValue: {
        maxLength: validations.maxLength('This field cannot be longer than 100 characters.', 100),
      },
      currentValue: {
        maxLength: validations.maxLength('This field cannot be longer than 100 characters.', 100),
      },
      status: {
        required: validations.required('This field is required.'),
      },
      startDate: {},
      targetDate: {},
      sortOrder: {
        required: validations.required('This field is required.'),
        integer: validations.integer('This field should be a number.'),
      },
      createdDate: {
        required: validations.required('This field is required.'),
      },
      lastModifiedDate: {},
      product: {
        required: validations.required('This field is required.'),
      },
      owner: {},
    };
    const v$ = useVuelidate(validationRules, outcome as any);
    v$.value.$validate();

    return {
      outcomeService,
      alertService,
      outcome,
      previousState,
      outcomeStatusValues,
      isSaving,
      currentLanguage,
      products,
      users,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: outcome }),
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.outcome.id) {
        this.outcomeService()
          .update(this.outcome)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A Outcome is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.outcomeService()
          .create(this.outcome)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A Outcome is created with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
