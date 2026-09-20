import { flushPromises, mount } from '@vue/test-utils';
import sinon from 'sinon';

import adminRoutes from '@/router/admin';
import { Authority } from '@/shared/jhipster/constants';

import Backup from './backup.vue';

describe('Backup administration page', () => {
  const backupService = {
    download: sinon.stub(),
    restore: sinon.stub(),
  };
  const alertService = {
    showSuccess: sinon.stub(),
    showError: sinon.stub(),
  };

  const mountPage = () =>
    mount(Backup, {
      global: {
        provide: {
          alertService,
          backupService: () => backupService,
        },
        stubs: {
          BAlert: { template: '<div><slot /></div>' },
          BModal: { template: '<div><slot name="title" /><slot /><slot name="footer" /></div>' },
        },
      },
    });

  beforeEach(() => {
    backupService.download.reset();
    backupService.restore.reset();
    alertService.showSuccess.reset();
    alertService.showError.reset();
  });

  it('is registered as an admin-only route', () => {
    const route = adminRoutes.find(candidate => candidate.path === '/admin/backup');

    expect(route?.meta?.authorities).toEqual([Authority.ADMIN]);
  });

  it('downloads the dated server filename and confirms completion', async () => {
    const blob = new Blob(['backup'], { type: 'application/json' });
    backupService.download.resolves({
      data: blob,
      headers: { 'content-disposition': 'attachment; filename="ombuto-ost-backup-20260920-081530.json"' },
    });
    const objectUrl = sinon.stub(URL, 'createObjectURL').returns('blob:backup');
    const revokeUrl = sinon.stub(URL, 'revokeObjectURL');
    const click = sinon.stub(HTMLAnchorElement.prototype, 'click');
    const wrapper = mountPage();
    const createElement = sinon.spy(document, 'createElement');

    await wrapper.get('[data-cy="takeBackupButton"]').trigger('click');
    await flushPromises();

    const downloadLink = createElement.getCalls().find(call => call.firstArg === 'a')?.returnValue as HTMLAnchorElement;
    expect(backupService.download.calledOnce).toBe(true);
    expect(click.calledOnce).toBe(true);
    expect(downloadLink.download).toBe('ombuto-ost-backup-20260920-081530.json');
    expect(alertService.showSuccess.calledWith('Backup completed.')).toBe(true);
    expect(wrapper.get('[data-cy="backupSuccess"]').text()).toContain('Backup completed');
    objectUrl.restore();
    revokeUrl.restore();
    click.restore();
    createElement.restore();
  });

  it('guards restore with an explicit warning and cancelling sends no request', async () => {
    const wrapper = mountPage();
    const file = new File(['{}'], 'backup.json', { type: 'application/json' });
    const input = wrapper.get('[data-cy="backupFileInput"]');
    Object.defineProperty(input.element, 'files', { value: [file] });
    await input.trigger('change');

    await wrapper.get('[data-cy="restoreButton"]').trigger('click');

    expect(wrapper.get('[data-cy="restoreConfirmation"]').text()).toContain('replaces all current data');
    expect(backupService.restore.called).toBe(false);

    await wrapper.get('[data-cy="restoreCancelButton"]').trigger('click');

    expect(backupService.restore.called).toBe(false);
    expect(wrapper.find('[data-cy="restoreConfirmation"]').exists()).toBe(false);
  });

  it('shows teams, products and tree node counts after a confirmed restore', async () => {
    backupService.restore.resolves({
      data: { exportedAt: '2026-09-20T08:15:30Z', counts: { teams: 3, products: 5, treeNodes: 21 } },
    });
    const wrapper = mountPage();
    const file = new File(['{}'], 'backup.json', { type: 'application/json' });
    const input = wrapper.get('[data-cy="backupFileInput"]');
    Object.defineProperty(input.element, 'files', { value: [file] });
    await input.trigger('change');
    await wrapper.get('[data-cy="restoreButton"]').trigger('click');
    await wrapper.get('[data-cy="restoreConfirmButton"]').trigger('click');
    await flushPromises();

    expect(backupService.restore.calledOnceWith(file)).toBe(true);
    const summary = wrapper.get('[data-cy="restoreSummary"]').text();
    expect(summary).toContain('3');
    expect(summary).toContain('teams');
    expect(summary).toContain('5');
    expect(summary).toContain('products');
    expect(summary).toContain('21');
    expect(summary).toContain('tree nodes');
    expect(wrapper.get('[data-cy="restoreSummaryExportedAt"]').text()).toContain('2026-09-20T08:15:30Z');
  });

  it.each([
    ['error.backup.invalid', 'not a valid backup'],
    ['error.backup.incompatibleVersion', 'incompatible version'],
  ])('renders a friendly message for %s', async (message, expected) => {
    backupService.restore.rejects({ response: { status: 400, data: { message, detail: 'raw server error' } } });
    const wrapper = mountPage();
    const file = new File(['bad'], 'backup.json', { type: 'application/json' });
    const input = wrapper.get('[data-cy="backupFileInput"]');
    Object.defineProperty(input.element, 'files', { value: [file] });
    await input.trigger('change');
    await wrapper.get('[data-cy="restoreButton"]').trigger('click');
    await wrapper.get('[data-cy="restoreConfirmButton"]').trigger('click');
    await flushPromises();

    const error = wrapper.get('[data-cy="backupError"]').text();
    expect(error).toContain(expected);
    expect(error).not.toContain('raw server error');
  });
});
