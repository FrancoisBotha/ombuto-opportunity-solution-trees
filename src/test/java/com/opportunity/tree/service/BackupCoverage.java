package com.opportunity.tree.service;

import com.opportunity.tree.service.dto.backup.BackupArchive;
import jakarta.persistence.Entity;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/** Database-free build guard. Expectations come from JPA mappings, never from an entity allowlist. */
final class BackupCoverage {

    private static final Metadata METADATA = metadata();

    private BackupCoverage() {}

    private static Metadata metadata() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        var registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .build();
        try {
            var sources = new MetadataSources(registry);
            var entities = scanner.findCandidateComponents("com.opportunity.tree.domain");
            require(!entities.isEmpty(), "No JPA entities discovered; BackupArchive coverage cannot be checked");
            for (var entity : entities) {
                sources.addAnnotatedClass(Class.forName(entity.getBeanClassName()));
            }
            return sources.buildMetadata();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    static Map<String, String> exclusions() throws IOException {
        var properties = new Properties();
        try (var stream = BackupCoverage.class.getResourceAsStream("/backup/coverage-exclusions.properties")) {
            require(stream != null, "BackupArchive exclusion policy is missing");
            properties.load(stream);
        }
        var result = new TreeMap<String, String>();
        properties.forEach((key, value) -> result.put(key.toString(), value.toString()));
        return result;
    }

    /** Only records actually reachable through archive collections count as covered rows. */
    static Map<String, Map<String, String>> archiveShape() {
        var result = new TreeMap<String, Map<String, String>>();
        for (var component : BackupArchive.class.getRecordComponents()) {
            if (component.getType() != List.class) continue;
            var row = (Class<?>) ((ParameterizedType) component.getGenericType()).getActualTypeArguments()[0];
            require(row.isRecord(), "BackupArchive." + component.getName() + " must contain row records");
            var fields = new TreeMap<String, String>();
            for (var field : row.getRecordComponents()) {
                fields.put(field.getName(), field.getGenericType().getTypeName());
            }
            result.put(component.getName() + ":" + row.getSimpleName(), fields);
        }
        return result;
    }

    static void verify(Map<String, Map<String, String>> archive, Map<String, String> exclusions) {
        exclusions.forEach((table, reason) -> require(!reason.isBlank(), "BackupArchive exclusion " + table + " needs a reason"));
        var missing = new ArrayList<String>();
        for (var entity : METADATA.getEntityBindings()) {
            if (exclusions.containsKey(entity.getTable().getName())) continue;
            String name = simpleName(entity.getClassName());
            var row = archive
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().endsWith(":" + name + "Row"))
                .findFirst();
            if (row.isEmpty()) {
                missing.add(name + " (" + entity.getTable().getName() + "): add a reachable " + name + "Row to BackupArchive");
                continue;
            }
            var properties = new ArrayList<>(entity.getPropertyClosure());
            require(entity.getIdentifierProperty() != null, "BackupArchive guard needs an explicit mapping for composite id on " + name);
            properties.add(entity.getIdentifierProperty());
            for (Property property : properties) {
                var value = property.getValue();
                if (value instanceof Collection collection) {
                    // The owning side holds the stored relationship. The inverse side duplicates it.
                    if (!collection.isInverse()) checkJoin(archive, exclusions, missing, name, property.getName(), collection);
                } else {
                    require(
                        !(value instanceof Component),
                        "BackupArchive guard needs an explicit mapping for embedded " + name + "." + property.getName()
                    );
                    String field = property.getName() + (value instanceof ToOne ? "Id" : "");
                    if (!row.get().getValue().containsKey(field)) {
                        missing.add(name + "." + property.getName() + ": add " + field + " to BackupArchive." + name + "Row");
                    }
                }
            }
        }
        // Inspect even joins owned by an excluded entity: their exclusion must be explicit too.
        for (var collection : METADATA.getCollectionBindings()) {
            if (!collection.isInverse() && exclusions.containsKey(collection.getOwner().getTable().getName())) {
                String role = collection.getRole();
                checkJoin(
                    archive,
                    exclusions,
                    missing,
                    simpleName(collection.getOwner().getClassName()),
                    role.substring(role.lastIndexOf('.') + 1),
                    collection
                );
            }
        }
        require(missing.isEmpty(), "BackupArchive is missing persisted data:\n" + String.join("\n", missing));
    }

    private static void checkJoin(
        Map<String, Map<String, String>> archive,
        Map<String, String> exclusions,
        List<String> missing,
        String owner,
        String property,
        Collection collection
    ) {
        String table = collection.getCollectionTable().getName();
        if (exclusions.containsKey(table)) return;
        String slot =
            Character.toLowerCase(owner.charAt(0)) + owner.substring(1) + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        require(
            !collection.isOneToMany() && collection.getKey().getColumnSpan() == 1 && collection.getElement().getColumnSpan() == 1,
            "BackupArchive guard needs an explicit row mapping for " + table
        );
        var row = archive.get(slot + ":JoinRow");
        if (row == null) {
            missing.add(table + " (" + owner + "." + property + "): add " + slot + " to BackupArchive");
            return;
        }
        for (String column : List.of("leftId", "rightId")) {
            if (!row.containsKey(column)) missing.add(table + " (" + slot + "): add " + column + " to BackupArchive.JoinRow");
        }
    }

    static void verifyVersion(String version, Map<String, Map<String, String>> archive) throws IOException {
        String snapshot = "/backup/archive-v" + version + ".txt";
        try (var stream = BackupCoverage.class.getResourceAsStream(snapshot)) {
            require(stream != null, "BackupArchive.FORMAT_VERSION " + version + " requires a frozen snapshot at " + snapshot);
            String expected = new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n").strip();
            var lines = new ArrayList<String>();
            new TreeMap<>(archive).forEach((slot, fields) -> {
                lines.add(slot);
                new TreeMap<>(fields).forEach((name, type) -> lines.add("  " + name + ":" + type));
            });
            String actual = String.join("\n", lines);
            require(
                expected.equals(actual),
                "BackupArchive coverage changed. Bump BackupArchive.FORMAT_VERSION and add a new snapshot; do not overwrite " +
                    snapshot +
                    ".\nActual shape:\n" +
                    actual
            );
        }
    }

    private static String simpleName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
