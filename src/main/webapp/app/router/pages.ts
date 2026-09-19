import ostRoutes from '@/ost/ost.routes';
import { Authority } from '@/shared/jhipster/constants';

const MyTeams = () => import('@/teams/my-teams.vue');
const TeamDetail = () => import('@/teams/team-detail.vue');

// jhipster-needle-add-entity-to-router-import - JHipster will import entities to the router here

export default [
  {
    path: '/teams',
    name: 'MyTeams',
    component: MyTeams,
    meta: { authorities: [Authority.USER] },
  },
  {
    path: '/teams/:id',
    name: 'MyTeamDetail',
    component: TeamDetail,
    meta: { authorities: [Authority.USER] },
  },
  ...ostRoutes,
  // jhipster-needle-add-entity-to-router - JHipster will add entities to the router here
];
