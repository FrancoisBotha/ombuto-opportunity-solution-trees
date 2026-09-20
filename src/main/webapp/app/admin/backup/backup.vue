<template>
  <div>
    <h2 id="backup-page-heading" data-cy="backupPageHeading">Backup</h2>
    <p class="text-muted">Download a complete copy of the application data, or replace the current data from a backup.</p>

    <b-alert v-if="successMessage" show variant="success" data-cy="backupSuccess">{{ successMessage }}</b-alert>
    <b-alert v-if="errorMessage" show variant="danger" data-cy="backupError">{{ errorMessage }}</b-alert>

    <section class="card mb-4" aria-labelledby="take-backup-heading">
      <div class="card-body">
        <h3 id="take-backup-heading" class="h5">Take a backup</h3>
        <p>Save the current teams, products and trees as a backup file.</p>
        <button
          type="button"
          class="btn btn-primary"
          data-cy="takeBackupButton"
          :disabled="isDownloading || isRestoring"
          @click="takeBackup"
        >
          {{ isDownloading ? 'Preparing backup…' : 'Take Backup' }}
        </button>
      </div>
    </section>

    <section class="card" aria-labelledby="restore-backup-heading">
      <div class="card-body">
        <h3 id="restore-backup-heading" class="h5">Restore a backup</h3>
        <p>Choose a backup file previously downloaded from this application.</p>
        <div class="mb-3">
          <label for="backup-file" class="form-label">Backup file</label>
          <input
            id="backup-file"
            class="form-control"
            type="file"
            accept="application/json,.json"
            data-cy="backupFileInput"
            :disabled="isRestoring"
            @change="chooseFile"
          />
        </div>
        <button
          type="button"
          class="btn btn-danger"
          data-cy="restoreButton"
          :disabled="!selectedFile || isDownloading || isRestoring"
          @click="requestRestore"
        >
          {{ isRestoring ? 'Restoring…' : 'Restore' }}
        </button>
      </div>
    </section>

    <b-alert v-if="restoreSummary" show variant="success" class="mt-4" data-cy="restoreSummary">
      <h3 class="h5 alert-heading">Restore summary</h3>
      <p class="mb-2">The backup was restored successfully.</p>
      <ul class="mb-2">
        <li>
          <strong>{{ restoreSummary.counts.teams ?? 0 }}</strong> teams
        </li>
        <li>
          <strong>{{ restoreSummary.counts.products ?? 0 }}</strong> products
        </li>
        <li>
          <strong>{{ restoreSummary.counts.treeNodes ?? 0 }}</strong> tree nodes
        </li>
      </ul>
      <p v-if="restoreSummary.exportedAt" class="mb-0" data-cy="restoreSummaryExportedAt">
        Backup taken on <time :datetime="restoreSummary.exportedAt">{{ restoreSummary.exportedAt }}</time
        >.
      </p>
    </b-alert>

    <b-modal
      v-if="showRestoreConfirmation"
      id="restore-backup-confirmation"
      v-model="showRestoreConfirmation"
      no-close-on-backdrop
      no-close-on-esc
    >
      <template #title>
        <span data-cy="restoreConfirmationHeading">Confirm restore</span>
      </template>
      <div class="modal-body" data-cy="restoreConfirmation">
        <p class="mb-0">Restoring this backup replaces all current data. This cannot be undone. Are you sure you want to continue?</p>
      </div>
      <template #footer>
        <button type="button" class="btn btn-secondary" data-cy="restoreCancelButton" @click="cancelRestore">Cancel</button>
        <button type="button" class="btn btn-danger" data-cy="restoreConfirmButton" @click="confirmRestore">Restore</button>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts" src="./backup.component.ts"></script>
