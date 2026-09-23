# Backup schema coverage (BKRST-005)

`BackupArchiveCoverageTest` runs in the ordinary Maven unit-test phase, without a
database or Spring application context. It scans the domain package for JPA
entities and builds Hibernate mapping metadata. Adding a persisted entity,
property (including inherited and implicitly mapped properties), foreign key or
owning collection without its archive representation fails the build.

The guard compares that independently discovered model with the row records
actually reachable through `BackupArchive` collections. Entity `X` must have a
reachable `XRow`; basic properties retain their names and to-one references use
`<property>Id`. An owning many-to-many property uses the archive collection
`<lowerCamelOwner><CapitalizedProperty>` containing `JoinRow.leftId` and
`JoinRow.rightId`. Inverse collections duplicate the owning side and are not
separate stored data. Unsupported composite ids, embedded values or collection
layouts fail explicitly, requiring an extension of the guard rather than silently
skipping data.

The sole table-exclusion policy, including reasons, is
[`coverage-exclusions.properties`](../../src/test/resources/backup/coverage-exclusions.properties).
The guard reads this file; do not add a second exclusion list to Java code or
documentation. Non-JPA framework tables are documented there too. This guard
checks JPA-persisted data, not arbitrary SQL tables outside the domain model.

## Format version policy

**Every archive coverage change requires a `BackupArchive.FORMAT_VERSION` bump**,
including adding, removing or renaming an entity collection, join collection or
row field, and changing a row field's Java type. Even additions are treated as
incompatible: restoring an older archive cannot recover a newly introduced field
or collection, and the current restore contract requires a matching version.

The guard compares the actual archive shape with the frozen
`src/test/resources/backup/archive-v<FORMAT_VERSION>.txt` snapshot. The version 1
snapshot records today's format; introducing the guard does not change that format.
An unchanged version with a changed shape fails. A new version without its own
snapshot also fails. Existing version snapshots are historical contracts and must
not be rewritten to make a failure pass.

When changing coverage:

1. Update `BackupArchive`, its export/restore mapping and envelope validation.
2. Increment `FORMAT_VERSION` and add a new snapshot, leaving older snapshots intact.
   The version assertion prints the actual shape for the new file: collection keys
   and row fields are sorted, with fully qualified Java types.
3. Run the targeted guard and the relevant export/restore tests in their appropriate
   test phase.

The guard proves structural coverage, not that export/restore assigns the correct
values. The existing backup resource tests still provide that behavioral coverage.

## Targeted command and omission proof

From PowerShell or another shell:

```sh
node mvnw.cjs -ntp --batch-mode '-Dskip.installnodenpm' '-Dskip.npm' test '-Dtest=BackupArchiveCoverageTest'
```

Mutation tests remove an entity collection, a scalar field, a foreign key, every
join collection and each join endpoint from the reflected archive shape, then
require the same checker to reject the omission with an actionable message.
They also verify that changing the archive without changing its format version
fails. These mutations operate on fresh copies and cannot weaken the real-model
coverage assertion.

The source-level omission experiment was also run during implementation: remove
`ProductRow.vision`, temporarily adapt the export constructor and restore setter
so compilation still succeeds, then run
`-Dtest=BackupArchiveCoverageTest#everyPersistedEntityFieldAndJoinIsInBackupArchive`.
The test failed with `Product.vision: add vision to BackupArchive.ProductRow`.
Both production files were restored byte-for-byte before the final gate run.
