import { defineComponent, provide } from 'vue';

import UserService from '@/entities/user/user.service';

import AssumptionService from './assumption/assumption.service';
import CommentService from './comment/comment.service';
import ExperimentService from './experiment/experiment.service';
import InterviewService from './interview/interview.service';
import OpportunityService from './opportunity/opportunity.service';
import OpportunityLinkService from './opportunity-link/opportunity-link.service';
import OutcomeService from './outcome/outcome.service';
import ProductService from './product/product.service';
import SolutionService from './solution/solution.service';
import SolutionLinkService from './solution-link/solution-link.service';
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
    provide('opportunityLinkService', () => new OpportunityLinkService());
    provide('solutionService', () => new SolutionService());
    provide('solutionLinkService', () => new SolutionLinkService());
    provide('assumptionService', () => new AssumptionService());
    provide('experimentService', () => new ExperimentService());
    provide('interviewService', () => new InterviewService());
    provide('commentService', () => new CommentService());
    provide('tagService', () => new TagService());
    // jhipster-needle-add-entity-service-to-entities-component - JHipster will import entities services here
  },
});
