package com.cheese.cheeseaiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.cheese.cheeseaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 * <p>
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现 step 方法。
 * <p>
 * 核心设计：
 * 1. 状态机：IDLE（空闲）→ RUNNING（运行中）→ FINISHED（完成）/ ERROR（错误）
 * 2. 步进循环：在 maxSteps 限制内反复调用子类实现的 step() 单步逻辑
 * 3. 上下文记忆：通过 messageList 维护多轮对话消息，供大模型参考
 * 4. 支持同步（run）与 SSE 流式（runStream）两种执行方式
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // ==================== 核心属性 ====================

    /**
     * 代理名称（如 "cheeseManus"），用于日志与调试
     */
    private String name;

    // ==================== 提示词 ====================

    /**
     * 系统提示词：定义代理的角色、能力与行为准则，发给大模型作为系统消息
     */
    private String systemPrompt;
    /**
     * 下一步行动提示词：指导大模型在每轮思考时如何规划下一步
     */
    private String nextStepPrompt;

    // ==================== 代理状态 ====================

    /**
     * 代理当前所处的执行状态，初始为空闲状态
     */
    private AgentState state = AgentState.IDLE;

    // ==================== 执行步骤控制 ====================

    /**
     * 当前已执行的步数
     */
    private int currentStep = 0;
    /**
     * 最大执行步数限制，防止代理无限循环，默认 10 步
     */
    private int maxSteps = 10;

    // ==================== LLM 大模型 ====================

    /**
     * 大模型对话客户端，负责与底层大模型（如通义千问）通信
     */
    private ChatClient chatClient;

    // ==================== Memory 记忆 ====================

    /**
     * 会话消息列表（需要自主维护会话上下文），按顺序保存每一轮的用户消息、助手消息与工具调用消息
     */
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行代理（同步方式）
     * <p>
     * 整个执行过程：校验状态 → 状态置为 RUNNING → 循环执行 step() 直到完成或达到最大步数 → 清理资源。
     *
     * @param userPrompt 用户提示词
     * @return 执行结果（每步结果按换行拼接）
     */
    public String run(String userPrompt) {
        // 1、基础校验：确保代理处于空闲状态且用户提示词非空
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 2、执行，更改状态为运行中
        this.state = AgentState.RUNNING;
        // 记录消息上下文：把用户输入加入会话历史
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表：收集每一步的执行结果，最终拼接返回
        List<String> results = new ArrayList<>();
        try {
            // 执行循环：在未达到最大步数且代理未完成时反复执行单步逻辑
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxSteps);
                // 单步执行：调用子类实现的具体步骤逻辑
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤限制：如果循环结束仍处于运行中，说明达到最大步数，强制结束
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            // 将所有步骤结果拼接为完整字符串返回
            return String.join("\n", results);
        } catch (Exception e) {
            // 执行过程中出现异常：状态置为错误，记录日志并返回错误信息
            state = AgentState.ERROR;
            log.error("error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            // 3、清理资源：无论成功或失败都会执行，子类可重写 cleanup 释放资源
            this.cleanup();
        }
    }

    /**
     * 运行代理（流式输出，基于 SSE）
     * <p>
     * 通过 SseEmitter 将每一步的执行结果实时推送给前端，适合长耗时任务；
     * 内部使用 CompletableFuture 异步执行，不阻塞主线程。
     *
     * @param userPrompt 用户提示词
     * @return SseEmitter 对象，前端可通过 Server-Sent Events 接收逐步结果
     */
    public SseEmitter runStream(String userPrompt) {
        Long duringTime = 10*60*1000L; //超时时间
        // 创建一个超时时间较长的 SseEmitter（5 分钟超时）
        SseEmitter sseEmitter = new SseEmitter(duringTime); // 5 分钟超时
        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            // 1、基础校验：状态与提示词校验，不通过则直接推送错误并结束
            try {
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send("错误：无法从状态运行代理：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误：不能使用空提示词运行代理");
                    sseEmitter.complete();
                    return;
                }
            } catch (Exception e) {
                // 校验过程本身出错（如推送失败），以异常方式结束 SSE
                sseEmitter.completeWithError(e);
            }
            // 2、执行，更改状态为运行中
            this.state = AgentState.RUNNING;
            // 记录消息上下文：把用户输入加入会话历史
            messageList.add(new UserMessage(userPrompt));
            // 保存结果列表
            List<String> results = new ArrayList<>();
            try {
                // 执行循环：逻辑与同步版一致，区别在于每步结果会实时推送给前端
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{}", stepNumber, maxSteps);
                    // 单步执行
                    String stepResult = step();
                    String result = "Step " + stepNumber + ": " + stepResult;
                    results.add(result);
                    // 输出当前每一步的结果到 SSE，前端可实时展示
                    sseEmitter.send(result);
                }
                // 检查是否超出步骤限制
                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    results.add("Terminated: Reached max steps (" + maxSteps + ")");
                    sseEmitter.send("执行结束：达到最大步骤（" + maxSteps + "）");
                }
                // 正常完成，关闭 SSE 连接
                sseEmitter.complete();
            } catch (Exception e) {
                // 执行异常：状态置为错误，推送错误信息
                state = AgentState.ERROR;
                log.error("error executing agent", e);
                try {
                    sseEmitter.send("执行错误：" + e.getMessage());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                // 3、清理资源
                this.cleanup();
            }
        });

        // 设置超时回调：SSE 连接超时时将状态置为错误并清理资源
        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });
        // 设置完成回调：正常结束后若仍处于运行中则标记为完成，并清理资源
        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });
        return sseEmitter;
    }

    /**
     * 定义单个步骤（抽象方法，由子类实现）
     * <p>
     * 子类需实现具体的单步执行逻辑，例如"思考-行动"（ReAct）或"工具调用"。
     *
     * @return 单步执行结果字符串
     */
    public abstract String step();

    /**
     * 清理资源（钩子方法）
     * <p>
     * 默认为空实现，子类可以重写此方法来清理资源（如关闭连接、释放文件句柄等）。
     */
    protected void cleanup() {
        // 子类可以重写此方法来清理资源
    }
}
