package com.example.Sistema_de_Evaluacion_de_Proyectos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Removes the obsolete matter ownership from section tables created by older schema versions. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SeccionSchemaMigration implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(SeccionSchemaMigration.class);
    private final DataSource dataSource;

    public SeccionSchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String product = metadata.getDatabaseProductName().toLowerCase(Locale.ROOT);
            boolean mysql = product.contains("mysql") || product.contains("mariadb");
            boolean postgres = product.contains("postgresql");
            if (!mysql && !postgres) return;

            String table = findSectionTable(metadata, connection);
            if (table == null) return;
            String schema = connection.getSchema();
            String catalog = connection.getCatalog();
            String oldColumn = findColumn(metadata, catalog, schema, table, "materia_id");
            if (oldColumn == null) return;

            String tableSql = quote(table, mysql);
            Set<String> foreignKeys = foreignKeys(metadata, catalog, schema, table, oldColumn);
            Set<String> uniqueIndexes = uniqueIndexes(metadata, catalog, schema, table, oldColumn);
            try (Statement statement = connection.createStatement()) {
                for (String key : foreignKeys) {
                    statement.execute("ALTER TABLE " + tableSql + (mysql ? " DROP FOREIGN KEY " : " DROP CONSTRAINT IF EXISTS ") + quote(key, mysql));
                }
                for (String index : uniqueIndexes) {
                    statement.execute("ALTER TABLE " + tableSql + (mysql ? " DROP INDEX " : " DROP CONSTRAINT IF EXISTS ") + quote(index, mysql));
                }
                statement.execute("ALTER TABLE " + tableSql + " DROP COLUMN " + quote(oldColumn, mysql));
            }
            log.info("Migrated legacy secciones schema: removed obsolete materia_id column and related constraints");
        }
    }

    private String findSectionTable(DatabaseMetaData metadata, Connection connection) throws SQLException {
        try (ResultSet tables = metadata.getTables(connection.getCatalog(), connection.getSchema(), null, new String[]{"TABLE"})) {
            while (tables.next()) {
                String name = tables.getString("TABLE_NAME");
                if ("secciones".equalsIgnoreCase(name)) return name;
            }
        }
        return null;
    }

    private String findColumn(DatabaseMetaData metadata, String catalog, String schema, String table, String column) throws SQLException {
        try (ResultSet columns = metadata.getColumns(catalog, schema, table, null)) {
            while (columns.next()) {
                String name = columns.getString("COLUMN_NAME");
                if (column.equalsIgnoreCase(name)) return name;
            }
        }
        return null;
    }

    private Set<String> foreignKeys(DatabaseMetaData metadata, String catalog, String schema, String table, String column) throws SQLException {
        Set<String> keys = new LinkedHashSet<>();
        try (ResultSet rows = metadata.getImportedKeys(catalog, schema, table)) {
            while (rows.next()) {
                if (column.equalsIgnoreCase(rows.getString("FKCOLUMN_NAME"))) {
                    String name = rows.getString("FK_NAME");
                    if (name != null) keys.add(name);
                }
            }
        }
        return keys;
    }

    private Set<String> uniqueIndexes(DatabaseMetaData metadata, String catalog, String schema, String table, String column) throws SQLException {
        Set<String> indexes = new LinkedHashSet<>();
        try (ResultSet rows = metadata.getIndexInfo(catalog, schema, table, true, false)) {
            while (rows.next()) {
                String name = rows.getString("INDEX_NAME");
                String indexedColumn = rows.getString("COLUMN_NAME");
                if (name != null && indexedColumn != null && column.equalsIgnoreCase(indexedColumn)) indexes.add(name);
            }
        }
        return indexes;
    }

    private String quote(String identifier, boolean mysql) {
        String escaped = identifier.replace(mysql ? "`" : "\"", mysql ? "``" : "\"\"");
        return mysql ? "`" + escaped + "`" : "\"" + escaped + "\"";
    }
}
