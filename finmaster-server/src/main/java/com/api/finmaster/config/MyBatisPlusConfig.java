package com.qykj.finmaster.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
//import com.qykj.finmaster.common.utils.TenantContext;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Configuration
@MapperScan("com.qykj.finmaster.**.mapper")
public class MyBatisPlusConfig {

//    @Autowired
//    private TenantLineInnerInterceptor tenantLineInnerInterceptor;

    /**
     * 分页插件和租户插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
//        // 添加租户插件
//        interceptor.addInnerInterceptor(tenantLineInnerInterceptor);
        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 自动填充处理器
     */
    @Component
    public static class MetaHandler implements MetaObjectHandler {

        @Override
        public void insertFill(MetaObject metaObject) {
            this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
            this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            // 设置默认值
            this.strictInsertFill(metaObject, "enabled", Boolean.class, true);
            this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
//            // 设置租户ID
//            Long tenantId = TenantContext.getCurrentTenantId();
//            if (tenantId != null) {
//                this.strictInsertFill(metaObject, "tenantId", Long.class, tenantId);
//            }
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        }
    }
} 