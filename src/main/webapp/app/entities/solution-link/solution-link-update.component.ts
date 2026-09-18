import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import SolutionService from '@/entities/solution/solution.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useValidation } from '@/shared/composables';
import { LinkType } from '@/shared/model/enumerations/link-type.model';
import { type ISolutionLink, SolutionLink } from '@/shared/model/solution-link.model';
import { type ISolution } from '@/shared/model/solution.model';

import SolutionLinkService from './solution-link.service';

export default defineComponent({
  name: 'SolutionLinkUpdate',
  setup() {
    const solutionLinkService = inject('solutionLinkService', () => new SolutionLinkService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const solutionLink: Ref<ISolutionLink> = ref(new SolutionLink());

    const solutionService = inject('solutionService', () => new SolutionService());

    const solutions: Ref<ISolution[]> = ref([]);
    const linkTypeValues: Ref<string[]> = ref(Object.keys(LinkType));
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveSolutionLink = async solutionLinkId => {
      try {
        const res = await solutionLinkService().find(solutionLinkId);
        solutionLink.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.solutionLinkId) {
      retrieveSolutionLink(route.params.solutionLinkId);
    }

    const initRelationships = () => {
      solutionService()
        .retrieve()
        .then(res => {
          solutions.value = res.data;
        });
    };

    initRelationships();

    const validations = useValidation();
    const validationRules = {
      name: {
        required: validations.required('This field is required.'),
        minLength: validations.minLength('This field is required to be at least 1 characters.', 1),
        maxLength: validations.maxLength('This field cannot be longer than 100 characters.', 100),
      },
      url: {
        required: validations.required('This field is required.'),
        maxLength: validations.maxLength('This field cannot be longer than 2000 characters.', 2000),
      },
      type: {
        required: validations.required('This field is required.'),
      },
      sortOrder: {
        required: validations.required('This field is required.'),
        integer: validations.integer('This field should be a number.'),
      },
      solution: {
        required: validations.required('This field is required.'),
      },
    };
    const v$ = useVuelidate(validationRules, solutionLink as any);
    v$.value.$validate();

    return {
      solutionLinkService,
      alertService,
      solutionLink,
      previousState,
      linkTypeValues,
      isSaving,
      currentLanguage,
      solutions,
      v$,
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.solutionLink.id) {
        this.solutionLinkService()
          .update(this.solutionLink)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A SolutionLink is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.solutionLinkService()
          .create(this.solutionLink)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A SolutionLink is created with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
