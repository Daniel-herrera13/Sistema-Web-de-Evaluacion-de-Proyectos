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

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    public DataSource dataSource(Environment env) {
        try {
            return crear(env, "app.db.primary", "MySQL");
        } catch (Exception e) {
            log.warn("MySQL no disponible ({}). Usando PostgreSQL.", e.getMessage());
            return crear(env, "app.db.fallback", "PostgreSQL");
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