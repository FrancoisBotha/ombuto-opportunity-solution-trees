package com.opportunity.tree.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.opportunity.tree.service.dto.backup.BackupArchive;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BackupArchiveCoverageTest {

    @Test
    void everyPersistedEntityFieldAndJoinIsInBackupArchive() throws Exception {
        BackupCoverage.verify(BackupCoverage.archiveShape(), BackupCoverage.exclusions());
    }

    @Test
    void missingEntityNamesTheEntityAndBackupArchive() throws Exception {
        var archive = BackupCoverage.archiveShape();
        archive.remove("teams:TeamRow");
        assertThatThrownBy(() -> BackupCoverage.verify(archive, BackupCoverage.exclusions()))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Team")
            .hasMessageContaining("BackupArchive");
    }

    @Test
    void missingScalarFieldNamesTheFieldAndBackupArchive() throws Exception {
        var archive = BackupCoverage.archiveShape();
        archive.get("products:ProductRow").remove("vision");
        assertThatThrownBy(() -> BackupCoverage.verify(archive, BackupCoverage.exclusions()))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Product.vision")
            .hasMessageContaining("BackupArchive");
    }

    @Test
    void missingForeignKeyIsAlsoAnOmission() throws Exception {
        var archive = BackupCoverage.archiveShape();
        archive.get("teamMembers:TeamMemberRow").remove("userId");
        assertThatThrownBy(() -> BackupCoverage.verify(archive, BackupCoverage.exclusions()))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("TeamMember.user")
            .hasMessageContaining("userId");
    }

    @Test
    void everyOwningJoinTableAndBothJoinColumnsAreChecked() throws Exception {
        for (String join : new String[] { "opportunityInterviews", "opportunityTags", "solutionTags" }) {
            var archive = BackupCoverage.archiveShape();
            archive.remove(join + ":JoinRow");
            assertThatThrownBy(() -> BackupCoverage.verify(archive, BackupCoverage.exclusions()))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(join)
                .hasMessageContaining("BackupArchive");
            for (String column : new String[] { "leftId", "rightId" }) {
                var missingColumn = BackupCoverage.archiveShape();
                missingColumn.get(join + ":JoinRow").remove(column);
                assertThatThrownBy(() -> BackupCoverage.verify(missingColumn, BackupCoverage.exclusions()))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining(join)
                    .hasMessageContaining(column);
            }
        }
    }

    @Test
    void exclusionsAreReadFromTheDocumentedPolicyAndRequireReasons() throws Exception {
        var exclusions = BackupCoverage.exclusions();
        assertThat(exclusions).isNotEmpty();
        assertThat(exclusions.values()).allSatisfy(reason -> assertThat(reason).isNotBlank());
        assertThatThrownBy(() -> BackupCoverage.verify(BackupCoverage.archiveShape(), Map.of()))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("BackupArchive");
        assertThatThrownBy(() -> BackupCoverage.verify(BackupCoverage.archiveShape(), Map.of("some_table", "")))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("reason");
    }

    @Test
    void coverageMatchesTheFrozenFormatVersion() throws Exception {
        BackupCoverage.verifyVersion(BackupArchive.FORMAT_VERSION, BackupCoverage.archiveShape());
    }

    @Test
    void changingCoverageWithoutBumpingFormatVersionFails() throws Exception {
        var archive = BackupCoverage.archiveShape();
        archive.get("products:ProductRow").put("newPersistedField", "java.lang.String");
        assertThatThrownBy(() -> BackupCoverage.verifyVersion(BackupArchive.FORMAT_VERSION, archive))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("BackupArchive.FORMAT_VERSION");
    }

    @Test
    void newFormatVersionRequiresItsOwnSnapshot() {
        assertThatThrownBy(() -> BackupCoverage.verifyVersion("unregistered", BackupCoverage.archiveShape()))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("BackupArchive.FORMAT_VERSION");
    }
}
