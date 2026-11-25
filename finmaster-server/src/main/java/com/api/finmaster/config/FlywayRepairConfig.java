package com.qykj.finmaster.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自动修复 Flyway 失败记录：启动时先 repair() 清理 schema_history 里 success=0 的记录，再 migrate()。
 * 适合幂等改造阶段，避免每次失败后都要手动 DELETE FROM flyway_schema_history。
 */
@Configuration
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
