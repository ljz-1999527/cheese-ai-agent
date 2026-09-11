package com.cheese.cheeseaiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 项目启动类（入口类）
 * <p>
 * 负责启动 Spring Boot 应用，是整个 cheese-ai-agent 项目的唯一入口。
 * 通过 @SpringBootApplication 开启自动配置与组件扫描，
 * 扫描 com.cheese.cheeseaiagent 包及其子包下的所有 Spring 组件（Controller、Service、Component、Config 等）。
 */
@SpringBootApplication(exclude = {
        // 为了便于大家开发调试和部署，取消数据库自动配置，需要使用 PgVector 时把 DataSourceAutoConfiguration.class 删除
        // exclude 属性表示排除指定的自动配置类：这里排除了数据源自动配置，
        // 即项目启动时不会强制要求配置 DataSource，方便无数据库环境下直接运行
        DataSourceAutoConfiguration.class
})
public class CheeseAiAgentApplication {

    /**
     * 应用启动主方法
     *
     * @param args 命令行启动参数（一般不需要传）
     */
    public static void main(String[] args) {
        // 调用 SpringApplication.run 启动整个 Spring 容器，
        // 传入当前类作为主配置源，args 为命令行参数
        SpringApplication.run(CheeseAiAgentApplication.class, args);
    }

}
