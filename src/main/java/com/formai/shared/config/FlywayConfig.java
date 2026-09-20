package com.formai.shared.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationInitializer iamFlywayMigrationInitializer(DataSource dataSource) {
        var flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas("iam")
                .table("flyway_iam_users")
                .locations("classpath:db/migration/iam")
                .load();
        return new FlywayMigrationInitializer(flyway, null);
    }
}
