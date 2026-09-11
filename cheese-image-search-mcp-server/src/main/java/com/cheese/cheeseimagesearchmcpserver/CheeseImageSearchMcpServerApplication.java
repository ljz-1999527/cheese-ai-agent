package com.cheese.cheeseimagesearchmcpserver;

import com.cheese.cheeseimagesearchmcpserver.tools.ImageSearchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * 图片搜索 MCP 服务启动类
 * <p>
 * 这是一个独立的 MCP（Model Context Protocol）服务模块，
 * 将图片搜索能力（Pexels）封装为 MCP 工具，供主应用
 * （cheese-ai-agent）通过 MCP 协议远程调用。
 */
@SpringBootApplication
public class CheeseImageSearchMcpServerApplication {

    /**
     * 程序入口：启动 Spring Boot 应用
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CheeseImageSearchMcpServerApplication.class, args);
    }

    /**
     * 注册图片搜索工具为 MCP 工具回调
     * <p>
     * 将 ImageSearchTool 中带有 @Tool 注解的方法
     * 暴露为可供 MCP 客户端调用的工具。
     *
     * @param imageSearchTool 图片搜索工具实例（由 Spring 注入）
     * @return 工具回调提供者
     */
    @Bean
    public ToolCallbackProvider imageSearchTools(ImageSearchTool imageSearchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(imageSearchTool)
                .build();
    }

}
