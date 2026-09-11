package com.cheese.cheeseaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Spring AI 框架调用 AI 大模型（Ollama 演示类）
 * <p>
 * 演示通过 Spring AI 的统一接口调用本地部署的 Ollama 大模型。
 * 实现 CommandLineRunner 接口，取消 @Component 注释后，项目启动时会自动执行 run 方法。
 */
// 取消注释后，项目启动时会执行
//@Component
public class OllamaAiInvoke implements CommandLineRunner {

    /** 注入 Ollama 大模型实例（需在 application.yml 中配置 Ollama 相关参数） */
    @Resource
    private ChatModel ollamaChatModel;

    /**
     * 启动时自动执行：向 Ollama 模型发起一次对话
     *
     * @param args 命令行参数
     * @throws Exception 调用大模型可能抛出的异常
     */
    @Override
    public void run(String... args) throws Exception {
        // 构造 Prompt 并调用大模型，取出助手消息
        AssistantMessage assistantMessage = ollamaChatModel.call(new Prompt("你好，我是鱼皮"))
                .getResult()
                .getOutput();
        // 打印模型回复的文本
        System.out.println(assistantMessage.getText());
    }
}
