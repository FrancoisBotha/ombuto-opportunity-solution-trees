import { type ComputedRef, defineComponent, inject } from 'vue';

import type LoginService from '@/account/login.service';

const steps = [
  { icon: 'bullseye', title: 'Outcome', text: 'Start from the measurable result your product team is accountable for.' },
  { icon: 'lightbulb', title: 'Opportunities', text: 'Map the customer needs, pains and desires uncovered in interviews.' },
  { icon: 'puzzle-piece', title: 'Solutions', text: 'Explore several ways to address each opportunity before committing.' },
  { icon: 'vial', title: 'Experiments', text: 'Test the riskiest assumptions and let the evidence pick the winner.' },
];

const links = [
  { path: '/product', icon: 'box', label: 'Products', text: 'What you are building' },
  { path: '/outcome', icon: 'bullseye', label: 'Outcomes', text: 'Where each tree starts' },
  { path: '/opportunity', icon: 'lightbulb', label: 'Opportunities', text: 'Customer needs and pains' },
  { path: '/solution', icon: 'puzzle-piece', label: 'Solutions', text: 'Ideas worth testing' },
  { path: '/assumption', icon: 'question-circle', label: 'Assumptions', text: 'What has to be true' },
  { path: '/evidence', icon: 'vial', label: 'Evidence', text: 'How you find out' },
  { path: '/interview', icon: 'comments', label: 'Interviews', text: 'What customers told you' },
  { path: '/team', icon: 'users', label: 'Teams', text: 'Who is doing the discovery' },
];

export default defineComponent({
  setup() {
    const { login } = inject<LoginService>('loginService');
    const authenticated = inject<ComputedRef<boolean>>('authenticated');
    const username = inject<ComputedRef<string>>('currentUsername');

    return {
      authenticated,
      username,
      login,
      steps,
      links,
    };
  },
});
