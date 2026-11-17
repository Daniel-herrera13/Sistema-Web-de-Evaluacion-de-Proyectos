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
        String seleccionado = env.getProperty("app.db.active", "primary");
        seleccionado = seleccionado == null || seleccionado.isBlank()
                ? "primary" : seleccionado.trim().toLowerCase(Locale.ROOT);
        String prefijo;
        String nombre;
        switch (seleccionado) {
            case "primary" -> {
                prefijo = "app.db.primary";
                nombre = "MySQL (principal)";
            }
            case "fallback" -> {
                prefijo = "app.db.fallback";
                nombre = "PostgreSQL (respaldo)";
            }
            default -> throw new IllegalStateException(
                    "Selección de base inválida: " + seleccionado + ". Usa app.db.active=primary o fallback en application.properties.");
        }

        log.info("Motor de base de datos seleccionado manualmente: {}", nombre);
        try {
            return crear(env, prefijo, nombre);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo conectar a la base seleccionada (" + nombre
                    + "). Revisa app.db.active y las propiedades de conexión en application.properties. No hay cambio automático.", e);
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
