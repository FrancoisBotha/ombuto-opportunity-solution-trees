# BDD UC: Back Up and Restore Application Data

Status: TICKETS
Owner: human
Created: 2026-09-20
Last Updated: 2026-09-20

---

## Story

As an administrator
I want to take a backup of the application data and restore from it
So that I can recover from data loss or a bad change

---

## Acceptance Scenarios

### Scenario 1: Download a backup file
Given I am signed in as an administrator
And I am on the Administration > Backup page
When I click "Take Backup"
Then a backup file is downloaded to my computer
And the file name includes the date and time the backup was taken
And I see a confirmation that the backup completed

### Scenario 2: Restore from a backup file
Given I am signed in as an administrator
And I am on the Administration > Backup page
When I choose a backup file and click "Restore"
Then I am warned that restoring replaces all current data
And when I confirm, the restore runs and I see a summary of what was restored
And the trees, teams and products from the backup are the ones now shown in the application
And if I cancel the warning instead, nothing is restored and the data is unchanged

### Scenario 3: Reject a file that is not a valid backup
Given I am signed in as an administrator
And I am on the Administration > Backup page
When I choose a file that is not a valid backup and click "Restore"
Then I see an error message saying the file is not a valid backup
And no data is changed

### Scenario 4: Refuse a backup from an incompatible version
Given I am signed in as an administrator
And I have a backup file taken by an earlier, incompatible version of the application
When I choose that file and click "Restore"
Then I see a message saying the backup was taken by an incompatible version and cannot be restored
And no data is changed

### Scenario 5: Non-administrators cannot reach backup and restore
Given I am signed in as an ordinary user who is not an administrator
When I look at the Administration menu
Then there is no Backup page listed
And if I go to the Backup page address directly, I am refused access

---

## Notes

Backup covers the application's own data — teams, memberships, products and the
tree nodes beneath them. The backup file is taken and kept by the administrator;
the application does not store a list of past backups.

Restore replaces all existing application data with the contents of the file,
which is why it is guarded by an explicit confirmation. The summary shown after a
restore should say what came back (for example, how many teams, products and tree
nodes).

The Administration area and the ADMIN authority already exist in the application
shell; this capability adds a page within it.

---

## References

- prd: docs/Product Requirements Document/PRD.md
- architecture: docs/Architecture/Architecture.md
