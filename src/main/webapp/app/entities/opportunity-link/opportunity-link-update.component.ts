import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import OpportunityService from '@/entities/opportunity/opportunity.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useValidation } from '@/shared/composables';
import { LinkType } from '@/shared/model/enumerations/link-type.model';
import { type IOpportunityLink, OpportunityLink } from '@/shared/model/opportunity-link.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';

import OpportunityLinkService from './opportunity-link.service';

export default defineComponent({
  name: 'OpportunityLinkUpdate',
  setup() {
    const opportunityLinkService = inject('opportunityLinkService', () => new OpportunityLinkService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const opportunityLink: Ref<IOpportunityLink> = ref(new OpportunityLink());

    const opportunityService = inject('opportunityService', () => new OpportunityService());

    const opportunities: Ref<IOpportunity[]> = ref([]);
    const linkTypeValues: Ref<string[]> = ref(Object.keys(LinkType));
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveOpportunityLink = async opportunityLinkId => {
      try {
        const res = await opportunityLinkService().find(opportunityLinkId);
        opportunityLink.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.opportunityLinkId) {
      retrieveOpportunityLink(route.params.opportunityLinkId);
    }

    const initRelationships = () => {
      opportunityService()
        .retrieve()
        .then(res => {
          opportunities.value = res.data;
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
      opportunity: {
        required: validations.required('This field is required.'),
      },
    };
    const v$ = useVuelidate(validationRules, opportunityLink as any);
    v$.value.$validate();

    return {
      opportunityLinkService,
      alertService,
      opportunityLink,
      previousState,
      linkTypeValues,
      isSaving,
      currentLanguage,
      opportunities,
      v$,
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.opportunityLink.id) {
        this.opportunityLinkService()
          .update(this.opportunityLink)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A OpportunityLink is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.opportunityLinkService()
          .create(this.opportunityLink)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A OpportunityLink is created with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
