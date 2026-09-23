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

const Solution = () => import('@/entities/solution/solution.vue');
const SolutionUpdate = () => import('@/entities/solution/solution-update.vue');
const SolutionDetails = () => import('@/entities/solution/solution-details.vue');

const Assumption = () => import('@/entities/assumption/assumption.vue');
const AssumptionUpdate = () => import('@/entities/assumption/assumption-update.vue');
const AssumptionDetails = () => import('@/entities/assumption/assumption-details.vue');

const Interview = () => import('@/entities/interview/interview.vue');
const InterviewUpdate = () => import('@/entities/interview/interview-update.vue');
const InterviewDetails = () => import('@/entities/interview/interview-details.vue');

const Comment = () => import('@/entities/comment/comment.vue');
const CommentUpdate = () => import('@/entities/comment/comment-update.vue');
const CommentDetails = () => import('@/entities/comment/comment-details.vue');

const Tag = () => import('@/entities/tag/tag.vue');
const TagUpdate = () => import('@/entities/tag/tag-update.vue');
const TagDetails = () => import('@/entities/tag/tag-details.vue');

const Evidence = () => import('@/entities/evidence/evidence.vue');
const EvidenceUpdate = () => import('@/entities/evidence/evidence-update.vue');
const EvidenceDetails = () => import('@/entities/evidence/evidence-details.vue');

const NodeLink = () => import('@/entities/node-link/node-link.vue');
const NodeLinkUpdate = () => import('@/entities/node-link/node-link-update.vue');
const NodeLinkDetails = () => import('@/entities/node-link/node-link-details.vue');

const OpenQuestion = () => import('@/entities/open-question/open-question.vue');
const OpenQuestionUpdate = () => import('@/entities/open-question/open-question-update.vue');
const OpenQuestionDetails = () => import('@/entities/open-question/open-question-details.vue');

const NodeHistory = () => import('@/entities/node-history/node-history.vue');
const NodeHistoryUpdate = () => import('@/entities/node-history/node-history-update.vue');
const NodeHistoryDetails = () => import('@/entities/node-history/node-history-details.vue');

const MeetingTranscript = () => import('@/entities/meeting-transcript/meeting-transcript.vue');
const MeetingTranscriptUpdate = () => import('@/entities/meeting-transcript/meeting-transcript-update.vue');
const MeetingTranscriptDetails = () => import('@/entities/meeting-transcript/meeting-transcript-details.vue');

// jhipster-needle-add-entity-to-router-import - JHipster will import entities to the router here

export default {
  path: '/',
  component: Entities,
  children: [
    {
      path: 'team',
      name: 'Team',
      component: Team,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'team/new',
      name: 'TeamCreate',
      component: TeamUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'team/:teamId/edit',
      name: 'TeamEdit',
      component: TeamUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'team/:teamId/view',
      name: 'TeamView',
      component: TeamDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'team-member',
      name: 'TeamMember',
      component: TeamMember,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'team-member/new',
      name: 'TeamMemberCreate',
      component: TeamMemberUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'team-member/:teamMemberId/edit',
      name: 'TeamMemberEdit',
      component: TeamMemberUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'team-member/:teamMemberId/view',
      name: 'TeamMemberView',
      component: TeamMemberDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'product',
      name: 'Product',
      component: Product,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'product/new',
      name: 'ProductCreate',
      component: ProductUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'product/:productId/edit',
      name: 'ProductEdit',
      component: ProductUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'product/:productId/view',
      name: 'ProductView',
      component: ProductDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'outcome',
      name: 'Outcome',
      component: Outcome,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'outcome/new',
      name: 'OutcomeCreate',
      component: OutcomeUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'outcome/:outcomeId/edit',
      name: 'OutcomeEdit',
      component: OutcomeUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'outcome/:outcomeId/view',
      name: 'OutcomeView',
      component: OutcomeDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'opportunity',
      name: 'Opportunity',
      component: Opportunity,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'opportunity/new',
      name: 'OpportunityCreate',
      component: OpportunityUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'opportunity/:opportunityId/edit',
      name: 'OpportunityEdit',
      component: OpportunityUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'opportunity/:opportunityId/view',
      name: 'OpportunityView',
      component: OpportunityDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'solution',
      name: 'Solution',
      component: Solution,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'solution/new',
      name: 'SolutionCreate',
      component: SolutionUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'solution/:solutionId/edit',
      name: 'SolutionEdit',
      component: SolutionUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'solution/:solutionId/view',
      name: 'SolutionView',
      component: SolutionDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'assumption',
      name: 'Assumption',
      component: Assumption,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'assumption/new',
      name: 'AssumptionCreate',
      component: AssumptionUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'assumption/:assumptionId/edit',
      name: 'AssumptionEdit',
      component: AssumptionUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'assumption/:assumptionId/view',
      name: 'AssumptionView',
      component: AssumptionDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'interview',
      name: 'Interview',
      component: Interview,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'interview/new',
      name: 'InterviewCreate',
      component: InterviewUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'interview/:interviewId/edit',
      name: 'InterviewEdit',
      component: InterviewUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'interview/:interviewId/view',
      name: 'InterviewView',
      component: InterviewDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'comment',
      name: 'Comment',
      component: Comment,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'comment/new',
      name: 'CommentCreate',
      component: CommentUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'comment/:commentId/edit',
      name: 'CommentEdit',
      component: CommentUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'comment/:commentId/view',
      name: 'CommentView',
      component: CommentDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'tag',
      name: 'Tag',
      component: Tag,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'tag/new',
      name: 'TagCreate',
      component: TagUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'tag/:tagId/edit',
      name: 'TagEdit',
      component: TagUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'tag/:tagId/view',
      name: 'TagView',
      component: TagDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'evidence',
      name: 'Evidence',
      component: Evidence,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'evidence/new',
      name: 'EvidenceCreate',
      component: EvidenceUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'evidence/:evidenceId/edit',
      name: 'EvidenceEdit',
      component: EvidenceUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'evidence/:evidenceId/view',
      name: 'EvidenceView',
      component: EvidenceDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'node-link',
      name: 'NodeLink',
      component: NodeLink,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'node-link/new',
      name: 'NodeLinkCreate',
      component: NodeLinkUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'node-link/:nodeLinkId/edit',
      name: 'NodeLinkEdit',
      component: NodeLinkUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'node-link/:nodeLinkId/view',
      name: 'NodeLinkView',
      component: NodeLinkDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'open-question',
      name: 'OpenQuestion',
      component: OpenQuestion,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'open-question/new',
      name: 'OpenQuestionCreate',
      component: OpenQuestionUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'open-question/:openQuestionId/edit',
      name: 'OpenQuestionEdit',
      component: OpenQuestionUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'open-question/:openQuestionId/view',
      name: 'OpenQuestionView',
      component: OpenQuestionDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'node-history',
      name: 'NodeHistory',
      component: NodeHistory,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'node-history/new',
      name: 'NodeHistoryCreate',
      component: NodeHistoryUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'node-history/:nodeHistoryId/edit',
      name: 'NodeHistoryEdit',
      component: NodeHistoryUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'node-history/:nodeHistoryId/view',
      name: 'NodeHistoryView',
      component: NodeHistoryDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'meeting-transcript',
      name: 'MeetingTranscript',
      component: MeetingTranscript,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'meeting-transcript/new',
      name: 'MeetingTranscriptCreate',
      component: MeetingTranscriptUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'meeting-transcript/:meetingTranscriptId/edit',
      name: 'MeetingTranscriptEdit',
      component: MeetingTranscriptUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'meeting-transcript/:meetingTranscriptId/view',
      name: 'MeetingTranscriptView',
      component: MeetingTranscriptDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    // jhipster-needle-add-entity-to-router - JHipster will add entities to the router here
  ],
};
