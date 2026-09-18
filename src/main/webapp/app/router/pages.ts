import { Authority } from '@/shared/jhipster/constants';

const MyTeams = () => import('@/teams/my-teams.vue');
const TeamDetail = () => import('@/teams/team-detail.vue');
const Trees = () => import('@/tree/trees.vue');
const TreeEditor = () => import('@/tree/tree-editor.vue');

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
  {
    path: '/trees',
    name: 'Trees',
    component: Trees,
    meta: { authorities: [Authority.USER] },
  },
  {
    path: '/trees/:teamId',
    name: 'TreeEditor',
    component: TreeEditor,
    meta: { authorities: [Authority.USER] },
  },
  // jhipster-needle-add-entity-to-router - JHipster will add entities to the router here
];
