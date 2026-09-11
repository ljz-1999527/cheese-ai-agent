package com.cheese.cheeseaiagent.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类
 * <p>
 * 将项目中全部 AI 工具（文件操作、网页搜索、网页抓取、资源下载、
 * 终端操作、PDF 生成、终止）统一注册为 Spring Bean，
 * 供智能体（YuManus）与对话流程（LoveApp.doChatWithTools）注入使用。
 */
@Configuration
public class ToolRegistration {

    /** 搜索 API 的 Key（从配置文件 search-api.api-key 读取） */
    @Value("${search-api.api-key}")
    private String searchApiKey;

    /**
     * 注册全部工具回调
     *
     * @return 所有工具回调组成的数组
     */
    @Bean
    public ToolCallback[] allTools() {
        // 实例化各个工具
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();
        // 将工具对象统一转换为 ToolCallback 数组返回
        return ToolCallbacks.from(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool
        );
    }
}
