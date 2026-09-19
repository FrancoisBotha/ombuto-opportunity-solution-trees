import { type ComputedRef, computed, defineComponent, inject } from 'vue';

import type LoginService from '@/account/login.service';
import { Authority } from '@/shared/jhipster/constants';
import { useStore } from '@/store';

export interface HomeLink {
  path: string;
  icon: string;
  label: string;
  text: string;
}

const steps = [
  { icon: 'bullseye', title: 'Outcome', text: 'Start from the measurable result your product team is accountable for.' },
  { icon: 'lightbulb', title: 'Opportunities', text: 'Map the customer needs, pains and desires uncovered in interviews.' },
  { icon: 'puzzle-piece', title: 'Solutions', text: 'Explore several ways to address each opportunity before committing.' },
  {
    icon: 'vial',
    title: 'Assumptions & evidence',
    text: 'Name what has to be true for each solution, then gather the evidence that supports or refutes it.',
  },
];

/** Where every signed-in user works: their teams' trees and the teams themselves. */
export const userLinks: HomeLink[] = [
  { path: '/trees', icon: 'sitemap', label: 'Trees', text: 'Your teams’ opportunity solution trees' },
  { path: '/teams', icon: 'users', label: 'Teams', text: 'Who is doing the discovery' },
];

/** Raw CRUD screens (Static Data) — administrators only; everyone else gets a 403 there. */
export const adminLinks: HomeLink[] = [
  { path: '/product', icon: 'box', label: 'Products', text: 'What you are building' },
  { path: '/outcome', icon: 'bullseye', label: 'Outcomes', text: 'Where each tree starts' },
  { path: '/opportunity', icon: 'lightbulb', label: 'Opportunities', text: 'Customer needs and pains' },
  { path: '/solution', icon: 'puzzle-piece', label: 'Solutions', text: 'Ideas worth testing' },
  { path: '/assumption', icon: 'question-circle', label: 'Assumptions', text: 'What has to be true' },
  { path: '/evidence', icon: 'vial', label: 'Evidence', text: 'What you found out' },
  { path: '/interview', icon: 'comments', label: 'Interviews', text: 'What customers told you' },
  { path: '/team', icon: 'users', label: 'All teams', text: 'Every team and its members' },
];

export default defineComponent({
  setup() {
    const { login } = inject<LoginService>('loginService');
    const authenticated = inject<ComputedRef<boolean>>('authenticated');
    const username = inject<ComputedRef<string>>('currentUsername');
    const store = useStore();
    const isAdmin = computed<boolean>(() => Boolean(store.account?.authorities?.includes(Authority.ADMIN)));

    return {
      authenticated,
      username,
      login,
      steps,
      userLinks,
      adminLinks,
      isAdmin,
    };
  },
});
