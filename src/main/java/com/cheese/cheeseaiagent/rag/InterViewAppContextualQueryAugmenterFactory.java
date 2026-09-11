package com.cheese.cheeseaiagent.rag;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * 创建上下文查询增强器的工厂
 * <p>
 * 在 RAG 生成回答前，将检索到的知识上下文与原始问题一起交给大模型。
 * 该类负责构建自定义的 ContextualQueryAugmenter：
 * - 当检索结果为空（allowEmptyContext=false）时，不进行回答，
 *   而是返回预设的"只能回答恋爱相关问题"提示模板。
 */
public class InterViewAppContextualQueryAugmenterFactory {

    /**
     * 创建上下文查询增强器实例
     *
     * @return 配置好的 ContextualQueryAugmenter 对象
     */
    public static ContextualQueryAugmenter createInstance() {
        // 定义空上下文时的回复模板：当知识库未检索到相关内容时，输出该提示
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，我只能回答面试相关的问题，别的没办法帮到您哦，
                有问题可以联系系统客服程序员cheese
                """);
        // 构建增强器：禁止空上下文回答，并指定空上下文时的提示模板
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();
    }
}
