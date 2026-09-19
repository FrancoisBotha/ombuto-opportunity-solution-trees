package com.opportunity.tree.service.dto.backup;

import java.time.Instant;
import java.util.Map;

/** Summary returned after a backup archive has been restored successfully. */
public record BackupRestoreSummary(Instant exportedAt, Map<String, Integer> counts) {}
