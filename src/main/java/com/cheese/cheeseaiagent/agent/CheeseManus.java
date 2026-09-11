package com.cheese.cheeseaiagent.agent;

import com.cheese.cheeseaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import static org.springframework.ai.model.tool.ToolCallingChatOptions.mergeToolCallbacks;

/**
 * cheese的 AI 超级智能体（拥有自主规划能力，可以直接使用）
 * <p>
 * 继承 ToolCallAgent，是一个可直接使用的成品智能体：
 * 1. 拥有"全知全能"的系统提示词，可应对任意任务
 * 2. 拥有"自主拆解任务、按步骤调用工具"的下一步行动提示词
 * 3. 自动注册所有可用工具（allTools），并启用自定义日志 Advisor
 * 4. 将最大执行步数提升到 20，支持更复杂的多步任务
 * <p>
 * 通过 @Component 注册为 Spring Bean，可直接注入使用。
 */
@Component
public class CheeseManus extends ToolCallAgent {

    /**
     * 构造方法：注入所有工具与通义千问大模型，完成智能体初始化
     *
     * @param allTools          项目注册的全部工具回调（文件操作、网页搜索、PDF 生成等）
     * @param ollamaChatModel Ollama（本地）大模型实例
     */
    public CheeseManus(ToolCallback[] allTools,
                       ToolCallbackProvider mcpToolCallbackProvider,
                       ChatModel ollamaChatModel) {
        // 合并本地工具 + MCP 工具（图片搜索等），传给父类
        super(mergeToolCallbacks(allTools, mcpToolCallbackProvider.getToolCallbacks()));
        // 调用父类构造，传入全部工具
//        super(allTools);
        // 设置代理名称
        this.setName("cheeseManus");
        // 定义系统提示词：声明"全能型 AI 助手"的身份与能力范围
        String SYSTEM_PROMPT = """
                You are CheeseManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.

                FILE OUTPUT RULES (follow strictly):
                1. When the user asks to output/export/save a text-based file (.txt, .md, .html, .csv, .json, .xml, .yaml, etc.), use the writeFile tool with the correct file extension in the fileName.
                2. When the user asks for a PDF output (以 PDF 格式输出 / 生成 PDF / PDF 文件), you MUST use the generatePDF tool and never writeFile. You may pass network image URLs (e.g. obtained from the searchImage tool) via the imageUrls parameter so images are rendered into the PDF.
                3. After writing or generating a file, report the returned file path to the user.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        // 定义下一步行动提示词：指导大模型如何主动规划与拆解任务
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                When the task is finished, summarize the result and clearly tell the user the path of any output file.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        // 提高最大执行步数到 20，支持更复杂的多步任务
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端：绑定大模型，并挂载自定义日志 Advisor（打印请求/响应日志）
        ChatClient chatClient = ChatClient.builder(ollamaChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
    private static ToolCallback[] mergeToolCallbacks(ToolCallback[] local, ToolCallback[] mcp) {
        ToolCallback[] merged = new ToolCallback[local.length + mcp.length];
        System.arraycopy(local, 0, merged, 0, local.length);
        System.arraycopy(mcp, 0, merged, local.length, mcp.length);
        return merged;
    }
}
