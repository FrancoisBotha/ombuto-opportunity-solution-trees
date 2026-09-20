package com.opportunity.tree.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.BackupService;
import com.opportunity.tree.service.RestoreMutex;
import com.opportunity.tree.service.dto.backup.BackupArchive;
import com.opportunity.tree.service.dto.backup.BackupRestoreSummary;
import com.opportunity.tree.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for the admin data backup export (BKRST-001).
 *
 * <p>Exposes a single {@code GET /api/admin/backup} endpoint which returns the whole
 * application data set as one JSON archive suitable for later restore. The class-level
 * {@link PreAuthorize} keeps the endpoint restricted to {@code ROLE_ADMIN} in the same
 * way as the other locked-down {@code *Resource} classes.
 */
@RestController
@RequestMapping("/api/admin/backup")
@PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
public class BackupResource {

    private static final Logger LOG = LoggerFactory.getLogger(BackupResource.class);

    private static final DateTimeFormatter FILENAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    private static final String ENTITY_NAME = "backup";
    private static final String INVALID_DETAIL = "The uploaded file is not a valid backup archive.";
    private static final String INCOMPATIBLE_VERSION_DETAIL = "The backup archive format version is not supported.";
    static final String RESTORE_FAILED_DETAIL = "The backup could not be restored. No data was changed.";
    static final String IN_PROGRESS_DETAIL = "Another restore is already running. Wait for it to finish and try again.";
    static final String UNRESOLVED_REFERENCES_DETAIL = "The backup refers to data that does not exist in this installation: ";

    private final BackupService backupService;
    private final RestoreMutex restoreMutex;
    private final ObjectMapper objectMapper;

    public BackupResource(BackupService backupService, RestoreMutex restoreMutex, ObjectMapper objectMapper) {
        this.backupService = backupService;
        this.restoreMutex = restoreMutex;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BackupArchive> exportBackup() {
        Instant exportedAt = Instant.now();
        LOG.debug("REST request to export application data backup at {}", exportedAt);
        BackupArchive archive = backupService.exportAll(exportedAt);
        String fileName = "ombuto-ost-backup-" + FILENAME_TIMESTAMP.format(exportedAt) + ".json";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(archive);
    }

    @PostMapping(value = "/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BackupRestoreSummary> restoreBackup(@RequestPart("file") MultipartFile file) {
        BackupArchive archive = readAndValidate(file);
        LOG.info("REST request to restore application data backup exported at {}", archive.exportedAt());
        // Only one restore at a time: a second one would interleave its deletes with this one's
        // inserts (BKRST fix C6). Taken before anything is validated against the database so the
        // loser never touches it at all.
        if (!restoreMutex.tryAcquire()) {
            throw new ConcurrencyFailureException(IN_PROGRESS_DETAIL);
        }
        try {
            // Everything the archive points at is checked before the restore transaction can delete
            // a single row (BKRST fix C5).
            List<String> unresolvable = backupService.unresolvableReferences(archive);
            if (!unresolvable.isEmpty()) {
                throw new BadRequestAlertException(unresolvedReferencesDetail(unresolvable), ENTITY_NAME, "backup.unresolvedReferences");
            }
            return ResponseEntity.ok(backupService.restoreAll(archive));
        } catch (BadRequestAlertException | ConcurrencyFailureException rethrown) {
            throw rethrown;
        } catch (RuntimeException failure) {
            // A row the archive carries that this build's constraints reject (a bean-validation
            // failure carries ConstraintViolationImpl{... rootBeanClass=class com.opportunity.tree
            // .domain.Team ...} in its message), or any other persistence failure. The restore
            // transaction has already rolled back, so nothing changed; the client gets one generic
            // key and the Java detail is logged, never returned (BKRST fix C4).
            LOG.error("Restore of the backup exported at {} failed and was rolled back", archive.exportedAt(), failure);
            throw new BadRequestAlertException(RESTORE_FAILED_DETAIL, ENTITY_NAME, "backup.restoreFailed");
        } finally {
            restoreMutex.release();
        }
    }

    private static String unresolvedReferencesDetail(List<String> unresolvable) {
        List<String> shown = unresolvable.stream().distinct().limit(BackupService.MAX_REPORTED_REFERENCES).toList();
        String detail = UNRESOLVED_REFERENCES_DETAIL + String.join(", ", shown);
        long hidden = unresolvable.stream().distinct().count() - shown.size();
        return hidden > 0 ? detail + " and " + hidden + " more" : detail + ".";
    }

    private BackupArchive readAndValidate(MultipartFile file) {
        final BackupArchive archive;
        try {
            archive = objectMapper.readValue(file.getInputStream(), BackupArchive.class);
        } catch (IOException | RuntimeException exception) {
            LOG.debug("Rejected an invalid backup upload", exception);
            throw invalidBackup();
        }
        if (archive == null || !archive.hasExpectedEnvelope()) {
            throw invalidBackup();
        }
        if (!BackupArchive.FORMAT_VERSION.equals(archive.formatVersion())) {
            throw new BadRequestAlertException(INCOMPATIBLE_VERSION_DETAIL, ENTITY_NAME, "backup.incompatibleVersion");
        }
        return archive;
    }

    private BadRequestAlertException invalidBackup() {
        return new BadRequestAlertException(INVALID_DETAIL, ENTITY_NAME, "backup.invalid");
    }
}
