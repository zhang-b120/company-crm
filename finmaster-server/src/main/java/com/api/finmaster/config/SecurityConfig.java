package com.qykj.finmaster.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsUtils;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.qykj.finmaster.security.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .cors()
            .and()
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                .requestMatchers("/system/user/login").permitAll()
                .requestMatchers("/system/user/login-by-params").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/ua/**").permitAll()
                .requestMatchers("/paymentData/paymentDataDetailPage").permitAll()
                    .requestMatchers("/commonTask/getCommonTaskFileList").permitAll()
                // 大屏页面（无token访问）
                .requestMatchers("/paymentData/orderOverview").permitAll()
                .requestMatchers("/system/user/deptUsersWithChildren").permitAll()
                .requestMatchers("/dashboard/screen/data").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/admin/qcc/**").permitAll()
                // 知识库图片公开访问（img 标签直接加载，无法附带 Bearer token）
                .requestMatchers("/knowledge/article/image/**").permitAll()
                // 退款凭证 / 转账 / 取消办税截图等公开访问
                .requestMatchers("/order/refund/image/**").permitAll()
                    .requestMatchers("/workWx/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
} 