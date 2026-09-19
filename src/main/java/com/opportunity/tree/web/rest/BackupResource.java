package com.opportunity.tree.web.rest;

import com.opportunity.tree.security.AuthoritiesConstants;
import com.opportunity.tree.service.BackupService;
import com.opportunity.tree.service.dto.backup.BackupArchive;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private final BackupService backupService;

    public BackupResource(BackupService backupService) {
        this.backupService = backupService;
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
}
