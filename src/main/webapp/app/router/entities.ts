import { Authority } from '@/shared/jhipster/constants';
const Entities = () => import('@/entities/entities.vue');

const Team = () => import('@/entities/team/team.vue');
const TeamUpdate = () => import('@/entities/team/team-update.vue');
const TeamDetails = () => import('@/entities/team/team-details.vue');

const TeamMember = () => import('@/entities/team-member/team-member.vue');
const TeamMemberUpdate = () => import('@/entities/team-member/team-member-update.vue');
const TeamMemberDetails = () => import('@/entities/team-member/team-member-details.vue');

const Product = () => import('@/entities/product/product.vue');
const ProductUpdate = () => import('@/entities/product/product-update.vue');
const ProductDetails = () => import('@/entities/product/product-details.vue');

const Outcome = () => import('@/entities/outcome/outcome.vue');
const OutcomeUpdate = () => import('@/entities/outcome/outcome-update.vue');
const OutcomeDetails = () => import('@/entities/outcome/outcome-details.vue');

const Opportunity = () => import('@/entities/opportunity/opportunity.vue');
const OpportunityUpdate = () => import('@/entities/opportunity/opportunity-update.vue');
const OpportunityDetails = () => import('@/entities/opportunity/opportunity-details.vue');

const OpportunityLink = () => import('@/entities/opportunity-link/opportunity-link.vue');
const OpportunityLinkUpdate = () => import('@/entities/opportunity-link/opportunity-link-update.vue');
const OpportunityLinkDetails = () => import('@/entities/opportunity-link/opportunity-link-details.vue');

const Solution = () => import('@/entities/solution/solution.vue');
const SolutionUpdate = () => import('@/entities/solution/solution-update.vue');
const SolutionDetails = () => import('@/entities/solution/solution-details.vue');

const SolutionLink = () => import('@/entities/solution-link/solution-link.vue');
const SolutionLinkUpdate = () => import('@/entities/solution-link/solution-link-update.vue');
const SolutionLinkDetails = () => import('@/entities/solution-link/solution-link-details.vue');

const Assumption = () => import('@/entities/assumption/assumption.vue');
const AssumptionUpdate = () => import('@/entities/assumption/assumption-update.vue');
const AssumptionDetails = () => import('@/entities/assumption/assumption-details.vue');

const Experiment = () => import('@/entities/experiment/experiment.vue');
const ExperimentUpdate = () => import('@/entities/experiment/experiment-update.vue');
const ExperimentDetails = () => import('@/entities/experiment/experiment-details.vue');

const Interview = () => import('@/entities/interview/interview.vue');
const InterviewUpdate = () => import('@/entities/interview/interview-update.vue');
const InterviewDetails = () => import('@/entities/interview/interview-details.vue');

const Comment = () => import('@/entities/comment/comment.vue');
const CommentUpdate = () => import('@/entities/comment/comment-update.vue');
const CommentDetails = () => import('@/entities/comment/comment-details.vue');

const Tag = () => import('@/entities/tag/tag.vue');
const TagUpdate = () => import('@/entities/tag/tag-update.vue');
const TagDetails = () => import('@/entities/tag/tag-details.vue');

// jhipster-needle-add-entity-to-router-import - JHipster will import entities to the router here

export default {
  path: '/',
  component: Entities,
  children: [
    {
      path: 'team',
      name: 'Team',
      component: Team,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'team/new',
      name: 'TeamCreate',
      component: TeamUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'team/:teamId/edit',
      name: 'TeamEdit',
      component: TeamUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'team/:teamId/view',
      name: 'TeamView',
      component: TeamDetails,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'team-member',
      name: 'TeamMember',
      component: TeamMember,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'team-member/new',
      name: 'TeamMemberCreate',
      component: TeamMemberUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'team-member/:teamMemberId/edit',
      name: 'TeamMemberEdit',
      component: TeamMemberUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'team-member/:teamMemberId/view',
      name: 'TeamMemberView',
      component: TeamMemberDetails,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'product',
      name: 'Product',
      component: Product,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'product/new',
      name: 'ProductCreate',
      component: ProductUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'product/:productId/edit',
      name: 'ProductEdit',
      component: ProductUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'product/:productId/view',
      name: 'ProductView',
      component: ProductDetails,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'outcome',
      name: 'Outcome',
      component: Outcome,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'outcome/new',
      name: 'OutcomeCreate',
      component: OutcomeUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'outcome/:outcomeId/edit',
      name: 'OutcomeEdit',
      component: OutcomeUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'outcome/:outcomeId/view',
      name: 'OutcomeView',
      component: OutcomeDetails,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'opportunity',
      name: 'Opportunity',
      component: Opportunity,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'opportunity/new',
      name: 'OpportunityCreate',
      component: OpportunityUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'opportunity/:opportunityId/edit',
      name: 'OpportunityEdit',
      component: OpportunityUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'opportunity/:opportunityId/view',
      name: 'OpportunityView',
      component: OpportunityDetails,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'opportunity-link',
      name: 'OpportunityLink',
      component: OpportunityLink,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'opportunity-link/new',
      name: 'OpportunityLinkCreate',
      component: OpportunityLinkUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'opportunity-link/:opportunityLinkId/edit',
      name: 'OpportunityLinkEdit',
      component: OpportunityLinkUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'opportunity-link/:opportunityLinkId/view',
      name: 'OpportunityLinkView',
      component: OpportunityLinkDetails,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'solution',
      name: 'Solution',
      component: Solution,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'solution/new',
      name: 'SolutionCreate',
      component: SolutionUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'solution/:solutionId/edit',
      name: 'SolutionEdit',
      component: SolutionUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'solution/:solutionId/view',
      name: 'SolutionView',
      component: SolutionDetails,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'solution-link',
      name: 'SolutionLink',
      component: SolutionLink,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'solution-link/new',
      name: 'SolutionLinkCreate',
      component: SolutionLinkUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'solution-link/:solutionLinkId/edit',
      name: 'SolutionLinkEdit',
      component: SolutionLinkUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'solution-link/:solutionLinkId/view',
      name: 'SolutionLinkView',
      component: SolutionLinkDetails,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'assumption',
      name: 'Assumption',
      component: Assumption,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'assumption/new',
      name: 'AssumptionCreate',
      component: AssumptionUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'assumption/:assumptionId/edit',
      name: 'AssumptionEdit',
      component: AssumptionUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'assumption/:assumptionId/view',
      name: 'AssumptionView',
      component: AssumptionDetails,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'experiment',
      name: 'Experiment',
      component: Experiment,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'experiment/new',
      name: 'ExperimentCreate',
      component: ExperimentUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'experiment/:experimentId/edit',
      name: 'ExperimentEdit',
      component: ExperimentUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'experiment/:experimentId/view',
      name: 'ExperimentView',
      component: ExperimentDetails,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'interview',
      name: 'Interview',
      component: Interview,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'interview/new',
      name: 'InterviewCreate',
      component: InterviewUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'interview/:interviewId/edit',
      name: 'InterviewEdit',
      component: InterviewUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'interview/:interviewId/view',
      name: 'InterviewView',
      component: InterviewDetails,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'comment',
      name: 'Comment',
      component: Comment,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'comment/new',
      name: 'CommentCreate',
      component: CommentUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'comment/:commentId/edit',
      name: 'CommentEdit',
      component: CommentUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'comment/:commentId/view',
      name: 'CommentView',
      component: CommentDetails,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'tag',
      name: 'Tag',
      component: Tag,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'tag/new',
      name: 'TagCreate',
      component: TagUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'tag/:tagId/edit',
      name: 'TagEdit',
      component: TagUpdate,
      meta: { authorities: [Authority.USER] },
    },
    {
      path: 'tag/:tagId/view',
      name: 'TagView',
      component: TagDetails,
      meta: { authorities: [Authority.USER] },
    },
    // jhipster-needle-add-entity-to-router - JHipster will add entities to the router here
  ],
};
