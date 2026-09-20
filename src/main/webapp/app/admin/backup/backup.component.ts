import { type Ref, defineComponent, inject, ref } from 'vue';

import { useAlertService } from '@/shared/alert/alert.service';

import BackupService, { type BackupRestoreSummary } from './backup.service';

const INVALID_BACKUP_MESSAGE = 'That file is not a valid backup.';
const INCOMPATIBLE_BACKUP_MESSAGE = 'That backup was created by an incompatible version and cannot be restored.';
const GENERIC_RESTORE_FAILURE_MESSAGE = 'The backup could not be restored. No data was changed.';

/**
 * One sentence per message key the restore endpoint can answer with. The server never returns the
 * underlying Java or SQL failure, so the page has to do the wording.
 */
const RESTORE_ERROR_MESSAGES: Record<string, string> = {
  'error.backup.invalid': INVALID_BACKUP_MESSAGE,
  'error.backup.incompatibleVersion': INCOMPATIBLE_BACKUP_MESSAGE,
  'error.backup.restoreFailed': GENERIC_RESTORE_FAILURE_MESSAGE,
  'error.upload.tooLarge': 'That backup file is larger than this server accepts. No data was changed.',
  'error.concurrencyFailure': 'Another restore is already running. Wait for it to finish and try again.',
};

function datedBackupFilename(): string {
  const timestamp = new Date()
    .toISOString()
    .replace(/[-:]/g, '')
    .replace(/\.\d{3}Z$/, 'Z')
    .replace('T', '-');
  return `ombuto-ost-backup-${timestamp}.json`;
}

function responseFilename(contentDisposition?: string): string {
  const match = contentDisposition?.match(/filename\*?=(?:UTF-8''|["']?)([^"';\s]+)/i);
  return match?.[1] ? decodeURIComponent(match[1]) : datedBackupFilename();
}

export default defineComponent({
  name: 'JhiBackup',
  setup() {
    const backupService = inject('backupService', () => new BackupService());
    const alertService = inject('alertService', () => useAlertService(), true);
    const selectedFile: Ref<File | null> = ref(null);
    const showRestoreConfirmation = ref(false);
    const isDownloading = ref(false);
    const isRestoring = ref(false);
    const successMessage: Ref<string | null> = ref(null);
    const errorMessage: Ref<string | null> = ref(null);
    const restoreSummary: Ref<BackupRestoreSummary | null> = ref(null);

    return {
      alertService,
      backupService,
      errorMessage,
      isDownloading,
      isRestoring,
      restoreSummary,
      selectedFile,
      showRestoreConfirmation,
      successMessage,
    };
  },
  methods: {
    chooseFile(event: Event): void {
      const input = event.target as HTMLInputElement;
      this.selectedFile = input.files?.[0] ?? null;
      this.errorMessage = null;
      this.restoreSummary = null;
    },
    async takeBackup(): Promise<void> {
      this.isDownloading = true;
      this.errorMessage = null;
      this.successMessage = null;
      try {
        const response = await this.backupService().download();
        const url = URL.createObjectURL(response.data);
        const link = document.createElement('a');
        link.href = url;
        link.download = responseFilename(response.headers?.['content-disposition']);
        link.click();
        URL.revokeObjectURL(url);
        this.successMessage = 'Backup completed.';
        this.alertService.showSuccess(this.successMessage);
      } catch {
        this.errorMessage = 'The backup could not be downloaded. Please try again.';
        this.alertService.showError(this.errorMessage);
      } finally {
        this.isDownloading = false;
      }
    },
    requestRestore(): void {
      if (!this.selectedFile) return;
      this.errorMessage = null;
      this.restoreSummary = null;
      this.showRestoreConfirmation = true;
    },
    cancelRestore(): void {
      this.showRestoreConfirmation = false;
    },
    async confirmRestore(): Promise<void> {
      if (!this.selectedFile) return;
      this.showRestoreConfirmation = false;
      this.isRestoring = true;
      this.errorMessage = null;
      this.successMessage = null;
      try {
        const response = await this.backupService().restore(this.selectedFile);
        this.restoreSummary = response.data;
        this.successMessage = 'Restore completed.';
        this.alertService.showSuccess(this.successMessage);
      } catch (error: any) {
        const errorKey = error?.response?.data?.message;
        if (errorKey === 'error.backup.unresolvedReferences') {
          // The server lists which users or rows it could not resolve; that list is the message.
          this.errorMessage = error?.response?.data?.detail ?? GENERIC_RESTORE_FAILURE_MESSAGE;
        } else {
          this.errorMessage = RESTORE_ERROR_MESSAGES[errorKey] ?? GENERIC_RESTORE_FAILURE_MESSAGE;
        }
        this.alertService.showError(this.errorMessage);
      } finally {
        this.isRestoring = false;
      }
    },
  },
});
