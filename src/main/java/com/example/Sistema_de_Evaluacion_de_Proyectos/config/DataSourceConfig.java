package com.example.Sistema_de_Evaluacion_de_Proyectos.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Locale;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    public DataSource dataSource(Environment env) {
        String motor = env.getProperty("app.db.active", "mysql");
        motor = motor == null || motor.isBlank() ? "mysql" : motor.trim().toLowerCase(Locale.ROOT);
        String prefijo;
        String nombre;
        switch (motor) {
            case "mysql", "mariadb" -> {
                prefijo = "app.db.mysql";
                nombre = "MySQL/MariaDB";
            }
            case "postgres", "postgresql" -> {
                prefijo = "app.db.postgres";
                nombre = "PostgreSQL";
            }
            default -> throw new IllegalStateException(
                    "Motor de base de datos inválido: " + motor + ". Usa DB_TYPE=mysql o DB_TYPE=postgres.");
        }

        log.info("Motor de base de datos seleccionado manualmente: {}", nombre);
        try {
            return crear(env, prefijo, nombre);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo conectar al motor seleccionado (" + nombre
                    + "). El backend no cambiará automáticamente a otra base de datos.", e);
        }
    }

    private DataSource crear(Environment env, String prefijo, String nombre) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(env.getRequiredProperty(prefijo + ".url"));
        cfg.setUsername(env.getRequiredProperty(prefijo + ".username"));
        cfg.setPassword(env.getRequiredProperty(prefijo + ".password"));
        cfg.setConnectionTimeout(3000);
        cfg.setInitializationFailTimeout(-1);

        HikariDataSource ds = new HikariDataSource(cfg);
        try (Connection c = ds.getConnection()) {
            log.info("Conectado a {}", nombre);
            return ds;
        } catch (Exception e) {
            ds.close();
            throw new IllegalStateException(e);
        }
    }
}
