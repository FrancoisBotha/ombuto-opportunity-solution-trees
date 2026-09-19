import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useVuelidate } from '@vuelidate/core';

import AssumptionService from '@/entities/assumption/assumption.service';
import EvidenceService from '@/entities/evidence/evidence.service';
import OpportunityService from '@/entities/opportunity/opportunity.service';
import OutcomeService from '@/entities/outcome/outcome.service';
import SolutionService from '@/entities/solution/solution.service';
import UserService from '@/entities/user/user.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';
import { type IAssumption } from '@/shared/model/assumption.model';
import { Comment, type IComment } from '@/shared/model/comment.model';
import { type IEvidence } from '@/shared/model/evidence.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';
import { type IOutcome } from '@/shared/model/outcome.model';
import { type ISolution } from '@/shared/model/solution.model';

import CommentService from './comment.service';

export default defineComponent({
  name: 'CommentUpdate',
  setup() {
    const commentService = inject('commentService', () => new CommentService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const comment: Ref<IComment> = ref(new Comment());
    const userService = inject('userService', () => new UserService());
    const users: Ref<Array<any>> = ref([]);

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

    const retrieveComment = async commentId => {
      try {
        const res = await commentService().find(commentId);
        res.createdDate = new Date(res.createdDate);
        res.editedDate = new Date(res.editedDate);
        comment.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.commentId) {
      retrieveComment(route.params.commentId);
    }

    const initRelationships = () => {
      userService()
        .retrieve()
        .then(res => {
          users.value = res.data;
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

    const dataUtils = useDataUtils();

    const validations = useValidation();
    const validationRules = {
      body: {
        required: validations.required('This field is required.'),
      },
      createdDate: {
        required: validations.required('This field is required.'),
      },
      editedDate: {},
      author: {
        required: validations.required('This field is required.'),
      },
      outcome: {},
      opportunity: {},
      solution: {},
      assumption: {},
      evidence: {},
    };
    const v$ = useVuelidate(validationRules, comment as any);
    v$.value.$validate();

    return {
      commentService,
      alertService,
      comment,
      previousState,
      isSaving,
      currentLanguage,
      users,
      outcomes,
      opportunities,
      solutions,
      assumptions,
      evidences,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: comment }),
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.comment.id) {
        this.commentService()
          .update(this.comment)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(`A Comment is updated with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.commentService()
          .create(this.comment)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(`A Comment is created with identifier ${param.id}`);
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
