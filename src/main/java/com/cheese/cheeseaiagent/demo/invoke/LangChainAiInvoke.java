package com.cheese.cheeseaiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * LangChain4j 框架调用 AI 大模型（演示类）
 * <p>
 * 演示使用 LangChain4j 社区版提供的 DashScope（通义千问）集成，
 * 通过 builder 模式快速构建一个聊天模型并直接对话。
 * <p>
 * 运行方式：直接执行 main 方法。
 */
public class LangChainAiInvoke {

    /**
     * 程序入口：演示 LangChain4j 调用大模型
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        // 构建通义千问聊天模型：配置 API Key 与模型名称（qwen-max）
        ChatLanguageModel qwenChatModel = QwenChatModel.builder()
                .apiKey(TestApiKey.API_KEY)
                .modelName("qwen-max")
                .build();
        // 发起单轮对话并打印回复
        String answer = qwenChatModel.chat("我是程序员鱼皮，这是编程导航 codefather.cn 的 AI 超级智能体原创项目");
        System.out.println(answer);
    }
}
