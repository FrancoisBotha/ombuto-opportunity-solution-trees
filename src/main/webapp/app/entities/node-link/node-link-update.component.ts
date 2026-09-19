import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import AssumptionService from '@/entities/assumption/assumption.service';
import { type IAssumption } from '@/shared/model/assumption.model';
import EvidenceService from '@/entities/evidence/evidence.service';
import OpportunityService from '@/entities/opportunity/opportunity.service';
import OutcomeService from '@/entities/outcome/outcome.service';
import ProductService from '@/entities/product/product.service';
import SolutionService from '@/entities/solution/solution.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import { type IEvidence } from '@/shared/model/evidence.model';
import { type INodeLink, NodeLink } from '@/shared/model/node-link.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';
import { type IOutcome } from '@/shared/model/outcome.model';
import { type IProduct } from '@/shared/model/product.model';
import { type ISolution } from '@/shared/model/solution.model';

import NodeLinkService from './node-link.service';

export default defineComponent({
  name: 'NodeLinkUpdate',
  setup() {
    const nodeLinkService = inject('nodeLinkService', () => new NodeLinkService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const nodeLink: Ref<INodeLink> = ref(new NodeLink());

    const productService = inject('productService', () => new ProductService());

    const products: Ref<IProduct[]> = ref([]);

    const outcomeService = inject('outcomeService', () => new OutcomeService());

    const outcomes: Ref<IOutcome[]> = ref([]);

    const opportunityService = inject('opportunityService', () => new OpportunityService());

    const opportunities: Ref<IOpportunity[]> = ref([]);

    const solutionService = inject('solutionService', () => new SolutionService());

    const solutions: Ref<ISolution[]> = ref([]);

    const assumptionService = inject('assumptionService', () => new AssumptionService());

    const assumptions: Ref<IAssumption[]> = ref([]);

    const evidenceService = inject('evidenceService', () => new EvidenceService());

    const evidences: Ref<IEvidence[]> = ref([]);
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'en'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveNodeLink = async nodeLinkId => {
      try {
        const res = await nodeLinkService().find(nodeLinkId);
        res.createdDate = new Date(res.createdDate);
        nodeLink.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.nodeLinkId) {
      retrieveNodeLink(route.params.nodeLinkId);
    }

    const initRelationships = () => {
      productService()
        .retrieve()
        .then(res => {
          products.value = res.data;
        });
      outcomeService()
        .retrieve()
        .then(res => {
          outcomes.value = res.data;
        });
      opportunityService()
        .retrieve()
        .then(res => {
          opportunities.value = res.data;
        });
      solutionService()
        .retrieve()
        .then(res => {
          solutions.value = res.data;
        });
      assumptionService()
        .retrieve()
        .then(res => {
          assumptions.value = res.data;
        });
      evidenceService()
        .retrieve()
        .then(res => {
          evidences.value = res.data;
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
      sortOrder: {
        required: validations.required('This field is required.'),
        integer: validations.integer('This field should be a number.'),
      },
      createdDate: {
        required: validations.required('This field is required.'),
      },
      product: {},
      outcome: {},
      opportunity: {},
      solution: {},
      assumption: {},
      evidence: {},
    };
    const v$ = useVuelidate(validationRules, nodeLink as any);
    v$.value.$validate();

    return {
      nodeLinkService,
      alertService,
      nodeLink,
      previousState,
      isSaving,
      currentLanguage,
      products,
      outcomes,
      opportunities,
      solutions,
      assumptions,
      evidences,
      v$,
      ...useDateFormat({ entityRef: nodeLink }),
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.nodeLink.id) {
        this.nodeLinkService()
          .update(this.nodeLink)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A NodeLink is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.nodeLinkService()
          .create(this.nodeLink)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A NodeLink is created with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
