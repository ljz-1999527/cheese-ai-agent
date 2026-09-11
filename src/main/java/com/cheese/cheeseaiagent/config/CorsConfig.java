package com.cheese.cheeseaiagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置
 * <p>
 * 实现 WebMvcConfigurer 接口来定制 Spring MVC 的跨域（CORS）策略。
 * 作用是允许前端（如 Vue 开发服务器 http://localhost:5173）跨域访问本后端接口，
 * 避免浏览器因同源策略拦截请求。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 配置全局跨域规则
     *
     * @param registry CORS 注册器，用于添加跨域映射规则
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求
        registry.addMapping("/**")
                // 允许发送 Cookie（需要配合 allowedOriginPatterns 使用）
                .allowCredentials(true)
                // 放行哪些域名（必须用 patterns，否则 * 会和 allowCredentials 冲突）
                .allowedOriginPatterns("*")
                // 允许的 HTTP 方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许携带任意请求头
                .allowedHeaders("*")
                // 允许前端读取任意响应头
                .exposedHeaders("*");
    }
}
