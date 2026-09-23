import { defineComponent, provide } from 'vue';

import UserService from '@/entities/user/user.service';

import AssumptionService from './assumption/assumption.service';
import CommentService from './comment/comment.service';
import EvidenceService from './evidence/evidence.service';
import InterviewService from './interview/interview.service';
import MeetingTranscriptService from './meeting-transcript/meeting-transcript.service';
import NodeHistoryService from './node-history/node-history.service';
import NodeLinkService from './node-link/node-link.service';
import OpenQuestionService from './open-question/open-question.service';
import OpportunityService from './opportunity/opportunity.service';
import OutcomeService from './outcome/outcome.service';
import ProductService from './product/product.service';
import SolutionService from './solution/solution.service';
import TagService from './tag/tag.service';
import TeamService from './team/team.service';
import TeamMemberService from './team-member/team-member.service';
// jhipster-needle-add-entity-service-to-entities-component-import - JHipster will import entities services here

export default defineComponent({
  name: 'Entities',
  setup() {
    provide('userService', () => new UserService());
    provide('teamService', () => new TeamService());
    provide('teamMemberService', () => new TeamMemberService());
    provide('productService', () => new ProductService());
    provide('outcomeService', () => new OutcomeService());
    provide('opportunityService', () => new OpportunityService());
    provide('solutionService', () => new SolutionService());
    provide('assumptionService', () => new AssumptionService());
    provide('interviewService', () => new InterviewService());
    provide('commentService', () => new CommentService());
    provide('tagService', () => new TagService());
    provide('evidenceService', () => new EvidenceService());
    provide('nodeLinkService', () => new NodeLinkService());
    provide('openQuestionService', () => new OpenQuestionService());
    provide('nodeHistoryService', () => new NodeHistoryService());
    provide('meetingTranscriptService', () => new MeetingTranscriptService());
    // jhipster-needle-add-entity-service-to-entities-component - JHipster will import entities services here
  },
});
