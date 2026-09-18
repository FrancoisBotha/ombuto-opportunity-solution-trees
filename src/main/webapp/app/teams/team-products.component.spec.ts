import { beforeEach, describe, expect, it, vitest } from 'vitest';

import { createTestingPinia } from '@pinia/testing';
import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import AlertService from '@/shared/alert/alert.service';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { IMyTeam, ITeamProduct } from './my-team.model';
import TeamProducts from './team-products.vue';
import TeamsService from './teams.service';
import { useTeamsStore } from './teams.store';

type TeamProductsComponentType = InstanceType<typeof TeamProducts>;

const sampleTeam = (id: number, role: TeamRole, productCount = 0): IMyTeam => ({
  id,
  name: `Team ${id}`,
  description: null,
  createdDate: null,
  role,
  memberCount: 3,
  productCount,
});

const product = (id: number, name: string, archived = false, description: string | null = 'desc'): ITeamProduct => ({
  id,
  name,
  description,
  archived,
  createdDate: '2026-09-01T00:00:00Z',
  teamId: 7,
});

describe('TeamProducts Component', () => {
  let teamsServiceStub: SinonStubbedInstance<TeamsService>;
  let alertService: AlertService;
  let mountOptions: MountingOptions<TeamProductsComponentType>['global'];

  beforeEach(() => {
    teamsServiceStub = sinon.createStubInstance<TeamsService>(TeamsService);

    alertService = new AlertService({
      toast: { show: vitest.fn() } as any,
    });

    mountOptions = {
      plugins: [createTestingPinia({ stubActions: false })],
      stubs: {
        'font-awesome-icon': true,
      },
      provide: {
        alertService,
        teamsService: () => teamsServiceStub,
      },
    };
  });

  const mountAs = async (role: TeamRole, products: ITeamProduct[]) => {
    teamsServiceStub.listTeamProducts.resolves(products);
    const wrapper = shallowMount(TeamProducts, {
      global: mountOptions,
      props: { teamId: 7 },
    });
    const store = useTeamsStore();
    store.upsertTeam(sampleTeam(7, role, products.length));
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    return wrapper;
  };

  it('lists products with name, description and archived state from the team-scoped API', async () => {
    const wrapper = await mountAs(TeamRole.VIEWER, [product(1, 'Alpha', false, 'first'), product(2, 'Beta', true, null)]);
    expect(teamsServiceStub.listTeamProducts.calledOnceWith(7)).toBe(true);
    const rows = wrapper.findAll('[data-cy-shared="productRow"]');
    expect(rows).toHaveLength(2);
    expect(wrapper.find('[data-cy="productRow-1"]').text()).toContain('Alpha');
    expect(wrapper.find('[data-cy="productRow-1"]').text()).toContain('first');
    // Archived product has a badge / visual marker
    expect(wrapper.find('[data-cy="productArchivedBadge-2"]').exists()).toBe(true);
    // Non-archived does not
    expect(wrapper.find('[data-cy="productArchivedBadge-1"]').exists()).toBe(false);
  });

  it('shows create / edit / archive controls to owners', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, [product(1, 'Alpha')]);
    expect(wrapper.find('[data-cy="newProductButton"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="editProductButton-1"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="archiveProductButton-1"]').exists()).toBe(true);
  });

  it('shows create / edit / archive controls to editors', async () => {
    const wrapper = await mountAs(TeamRole.EDITOR, [product(1, 'Alpha')]);
    expect(wrapper.find('[data-cy="newProductButton"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="editProductButton-1"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="archiveProductButton-1"]').exists()).toBe(true);
  });

  it('hides create / edit / archive controls from viewers', async () => {
    const wrapper = await mountAs(TeamRole.VIEWER, [product(1, 'Alpha'), product(2, 'Beta', true)]);
    expect(wrapper.find('[data-cy="newProductButton"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="editProductButton-1"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="archiveProductButton-1"]').exists()).toBe(false);
    expect(wrapper.find('[data-cy="unarchiveProductButton-2"]').exists()).toBe(false);
  });

  it('lets owners and editors un-archive a product', async () => {
    const wrapper = await mountAs(TeamRole.EDITOR, [product(1, 'Alpha', true)]);
    // The archive button on an archived product is labelled as un-archive
    expect(wrapper.find('[data-cy="unarchiveProductButton-1"]').exists()).toBe(true);
  });

  it('creates a product (name required, description optional) and reloads', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, []);
    const comp = wrapper.vm as any;
    const created = product(11, 'New product');
    teamsServiceStub.createTeamProduct.resolves(created);
    teamsServiceStub.listTeamProducts.resolves([created]);

    comp.openCreateForm();
    comp.newProductName = 'New product';
    comp.newProductDescription = '';
    await comp.submitCreate();

    expect(teamsServiceStub.createTeamProduct.calledOnce).toBe(true);
    const [teamIdArg, payload] = teamsServiceStub.createTeamProduct.firstCall.args;
    expect(teamIdArg).toBe(7);
    expect(payload.name).toBe('New product');
    // description empty → null
    expect(payload.description).toBeNull();
    expect(comp.showCreateForm).toBe(false);
  });

  it('rejects an empty name at the client with a validation message', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, []);
    const comp = wrapper.vm as any;
    comp.openCreateForm();
    comp.newProductName = '   ';
    await comp.submitCreate();
    expect(teamsServiceStub.createTeamProduct.called).toBe(false);
    expect(comp.createError).toBeTruthy();
  });

  it('archives a product and refreshes the list', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, [product(1, 'Alpha')]);
    const comp = wrapper.vm as any;
    const archived = product(1, 'Alpha', true);
    teamsServiceStub.setProductArchived.resolves(archived);
    teamsServiceStub.listTeamProducts.resolves([archived]);
    await comp.setArchived(product(1, 'Alpha'), true);
    expect(teamsServiceStub.setProductArchived.calledOnceWith(1, true)).toBe(true);
  });

  it('surfaces a 403 from the API as a user-visible message', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, [product(1, 'Alpha')]);
    const comp = wrapper.vm as any;
    teamsServiceStub.setProductArchived.rejects({ response: { status: 403, data: {} } });
    await comp.setArchived(product(1, 'Alpha'), true);
    await wrapper.vm.$nextTick();
    expect(comp.actionError).toContain('permission');
  });

  it('surfaces a validation error from the API on create', async () => {
    const wrapper = await mountAs(TeamRole.OWNER, []);
    const comp = wrapper.vm as any;
    teamsServiceStub.createTeamProduct.rejects({
      response: { status: 400, data: { detail: 'Name already used' } },
    });
    comp.openCreateForm();
    comp.newProductName = 'Alpha';
    await comp.submitCreate();
    expect(comp.createError).toContain('Name already used');
  });
});
