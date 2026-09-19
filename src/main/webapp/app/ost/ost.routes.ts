import type { RouteRecordRaw } from 'vue-router';

import { Authority } from '@/shared/jhipster/constants';

// Lazy: the whole OST module (and its fonts / Vue Flow CSS) loads only when a tree route is visited.
const TreesLanding = () => import('@/ost/shell/TreesLanding.vue');
const OstShell = () => import('@/ost/shell/OstShell.vue');
const TreesDashboardPage = () => import('@/ost/pages/TreesDashboardPage.vue');
const CanvasPage = () => import('@/ost/pages/CanvasPage.vue');
const ExperimentsPage = () => import('@/ost/pages/ExperimentsPage.vue');
const NodeDetailPage = () => import('@/ost/pages/NodeDetailPage.vue');

/** Routes of the OST tree builder; spread into router/pages.ts. `fullBleed` drops app.vue's card chrome. */
const ostRoutes: RouteRecordRaw[] = [
  {
    path: '/trees',
    name: 'Trees',
    component: TreesLanding,
    meta: { authorities: [Authority.USER], fullBleed: true },
  },
  {
    path: '/trees/:teamId',
    component: OstShell,
    meta: { authorities: [Authority.USER], fullBleed: true },
    children: [
      { path: '', name: 'OstDashboard', component: TreesDashboardPage },
      { path: 'canvas', name: 'OstCanvas', component: CanvasPage },
      { path: 'experiments', name: 'OstExperiments', component: ExperimentsPage },
      { path: 'nodes/:nodeKey', name: 'OstNodeDetail', component: NodeDetailPage },
    ],
  },
];

export default ostRoutes;
