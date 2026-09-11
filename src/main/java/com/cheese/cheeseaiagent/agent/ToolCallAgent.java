package com.cheese.cheeseaiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.cheese.cheeseaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 * <p>
 * 核心能力：
 * 1. think()：将当前会话消息交给大模型，让大模型决定是否调用工具以及调用哪些工具
 * 2. act()：根据大模型的工具调用指令，通过 ToolCallingManager 真正执行工具并收集结果
 * 3. 支持终止工具（doTerminate）：当大模型调用终止工具时，代理状态置为 FINISHED 结束任务
 * <p>
 * 注意：构造时会禁用 Spring AI 内置的工具执行机制（withInternalToolExecutionEnabled(false)），
 * 由本类自行维护工具调用流程与消息上下文，便于精细控制。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    /** 可用的工具列表（由 Spring 注入，例如文件操作、网页搜索等工具） */
    private final ToolCallback[] availableTools;

    /** 保存工具调用信息的响应结果（记录大模型本次要求调用哪些工具） */
    private ChatResponse toolCallChatResponse;

    /** 工具调用管理者：负责根据大模型返回的工具调用指令真正执行工具 */
    private final ToolCallingManager toolCallingManager;

    /** 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文 */
    private final ChatOptions chatOptions;

    /**
     * 构造方法：初始化工具列表、工具调用管理器，并关闭 Spring AI 内置工具执行
     *
     * @param availableTools 可用工具回调数组
     */
    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        // 构建默认的工具调用管理器
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        // 使用 Ollama（通义千问）专属选项，并关闭内部工具执行，改由本类手动控制
        this.chatOptions = OllamaOptions.builder().internalToolExecutionEnabled(false).build();
/*
        // 使用 DashScope（通义千问）专属选项，并关闭内部工具执行，改由本类手动控制
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
*/
    }

    /**
     * 处理当前状态并决定下一步行动（思考阶段）
     * <p>
     * 将历史消息 + 系统提示词 + 可用工具一起发给大模型，
     * 大模型会返回两类结果之一：直接给出文本回复（无需工具）或要求调用工具。
     *
     * @return 是否需要执行行动（有工具调用返回 true，否则 false）
     */
    @Override
    public boolean think() {
        // 1、校验提示词，拼接用户提示词：如果配置了下一步提示词则加入会话历史
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        // 2、调用 AI 大模型，获取工具调用结果
        List<Message> messageList = getMessageList();
        // 构造 Prompt：传入会话历史与自定义选项
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            // 发起大模型调用：携带系统提示词、可用工具列表
            // 注意：availableTools 是已封装好的 ToolCallback 实例，必须用 .toolCallbacks()
            // 传 .tools() 会走 ToolCallbacks.from() 反射查找 @Tool 注解方法而报
            // "No @Tool annotated methods found in MethodToolCallback{...}"
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应，用于等一下 Act 阶段执行工具调用
            this.toolCallChatResponse = chatResponse;
            // 3、解析工具调用结果，获取要调用的工具
            // 助手消息：大模型返回的输出（可能包含文本和工具调用列表）
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 输出提示信息：打印大模型的思考文本与选择的工具
            String result = assistantMessage.getText();
            log.info(getName() + "的思考：" + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            // 打印每个工具的名称与参数，便于观察调试
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            // 如果不需要调用工具，返回 false（本轮无需执行行动）
            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息到会话历史
                // （若调用了工具，工具执行结果会由 act() 统一追加历史）
                getMessageList().add(assistantMessage);
                return false;
            } else {
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            // 思考过程异常：记录错误日志，并把错误信息作为助手消息加入历史，本轮不行动
            log.error("{}的思考过程遇到了问题：{}", getName(), e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果（行动阶段）
     * <p>
     * 根据 think() 阶段记录的工具调用指令，通过 ToolCallingManager 真正执行工具，
     * 并把执行结果（包含助手消息与工具返回消息）写回会话历史，供下一轮思考使用。
     *
     * @return 执行结果（各工具返回结果拼接的文本）
     */
    @Override
    public String act() {
        // 没有工具调用指令则直接返回
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具需要调用";
        }
        // 调用工具：基于当前会话历史与大模型返回的工具调用响应执行
        // 注意：executeToolCalls 只从 prompt 的 options(ToolCallingChatOptions) 中取工具回调，
        // 取不到才走默认的（空）ToolCallbackResolver，所以这里必须显式带上 availableTools
        Prompt prompt = new Prompt(getMessageList(),
                ToolCallingChatOptions.builder()
                        .internalToolExecutionEnabled(false)
                        .toolCallbacks(availableTools)
                        .build());
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文：conversationHistory 已经包含了助手消息和工具调用返回的结果
        setMessageList(toolExecutionResult.conversationHistory());
        // 取最后一条消息（工具响应消息），用于判断是否调用了终止工具
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // 判断是否调用了终止工具（doTerminate）：命中则任务结束
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            // 任务结束，更改状态为 FINISHED，主循环将退出
            setState(AgentState.FINISHED);
        }
        // 拼接所有工具的执行结果文本，用于日志与返回
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 返回的结果：" + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }
}
