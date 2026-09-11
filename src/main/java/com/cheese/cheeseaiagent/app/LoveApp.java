package com.cheese.cheeseaiagent.app;

import com.cheese.cheeseaiagent.advisor.MyLoggerAdvisor;
import com.cheese.cheeseaiagent.advisor.ReReadingAdvisor;
import com.cheese.cheeseaiagent.chatmemory.FileBasedChatMemory;
import com.cheese.cheeseaiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.cheese.cheeseaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 恋爱大师应用（核心业务封装类）
 * <p>
 * 封装了多种 AI 对话能力，供 Controller 层调用，包括：
 * 1. 基础对话：支持多轮对话记忆（doChat / doChatByStream）
 * 2. 结构化输出：生成恋爱报告（doChatWithReport）
 * 3. RAG 知识库问答：基于向量库检索增强回答（doChatWithRag）
 * 4. 工具调用：让 AI 自主调用工具完成任务（doChatWithTools）
 * 5. MCP 服务调用：通过 MCP 协议调用外部工具（doChatWithMcp）
 * <p>
 * 通过 @Component 注册为 Spring Bean，内部依赖均由 Spring 注入。
 */
@Component
@Slf4j
public class LoveApp {

    /** ChatClient：AI 对话客户端，整个类的所有能力都基于它发起调用 */
    private final ChatClient chatClient;

    /** 系统提示词：定义"恋爱心理专家"的角色设定与提问策略 */
    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    /**
     * 初始化 ChatClient
     * <p>
     * 构造时构建 ChatClient：设置系统提示词、挂载对话记忆 Advisor（支持多轮记忆）与日志 Advisor。
     *
     * @param dashscopeChatModel 通义千问（DashScope）大模型实例
     */
    public LoveApp(ChatModel dashscopeChatModel) {
//        // 初始化基于文件的对话记忆（被注释掉的备选方案：可将对话持久化到文件）
//        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆：使用滑动窗口机制，最多保留 20 条消息
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        // 构建 ChatClient：设置默认系统提示词与默认 Advisor 列表
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 对话记忆 Advisor：按 conversationId 维护多轮会话上下文
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志 Advisor，可按需开启（打印每次请求/响应日志）
                        new MyLoggerAdvisor()
//                        // 自定义推理增强 Advisor，可按需开启（Re2 重读提示词，提升推理能力）
//                       ,new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message 用户输入的消息
     * @param chatId  会话 ID（用于区分不同用户/会话的记忆）
     * @return AI 回复的文本内容
     */
    public String doChat(String message, String chatId) {
        // 发起同步对话调用，并传入 conversationId 以关联会话记忆
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        // 从响应中提取最终生成的文本
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message 用户输入的消息
     * @param chatId  会话 ID
     * @return 流式响应 Flux，逐个返回 AI 生成的文本片段
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        // 发起流式对话调用，通过 .stream().content() 获取文本片段流
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * 恋爱报告数据结构（Java Record）
     * <p>
     * 用于结构化输出：大模型按要求返回"恋爱报告"标题与建议列表。
     *
     * @param title       报告标题（如"张三的恋爱报告"）
     * @param suggestions 建议列表
     */
    record LoveReport(String title, List<String> suggestions) {

    }

    /**
     * AI 恋爱报告功能（实战结构化输出）
     * <p>
     * 通过 .entity() 指定输出类型，Spring AI 会自动让大模型按该结构返回 JSON 并反序列化。
     *
     * @param message 用户输入的消息
     * @param chatId  会话 ID
     * @return 结构化恋爱报告对象 LoveReport
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        // 在系统提示词中追加生成报告的要求，然后调用大模型并自动转换为 LoveReport 对象
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    // ==================== AI 恋爱知识库问答功能（RAG） ====================

    /** 恋爱知识库向量存储（用于 RAG 检索增强） */
    @Resource
    private VectorStore loveAppVectorStore;

    /** 基于云知识库服务的 RAG Advisor（备选方案，默认未启用；未配置 DashScope key 时该 Bean 不注册，required=false 避免注入失败） */
    @Autowired(required = false)
    private Advisor loveAppRagCloudAdvisor;

    /** PgVector 向量存储（备选方案，默认未启用；PgVectorVectorStoreConfig 的 @Configuration 已注释时该 Bean 不注册，required=false 避免注入失败） */
    @Autowired(required = false)
    @Qualifier("pgVectorVectorStore")
    private VectorStore pgVectorVectorStore;

    /** 查询重写器：对用户问题先做改写，提升检索质量 */
    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 和 RAG 知识库进行对话
     * <p>
     * 流程：查询重写 → 大模型调用（携带 RAG Advisor 进行知识库检索增强）。
     * 代码中提供了多种 RAG 实现方案（本地向量库 / 云知识库 / PgVector / 自定义增强器），
     * 可按需注释切换。
     *
     * @param message 用户输入的消息
     * @param chatId  会话 ID
     * @return AI 回复的文本内容
     */
    public String doChatWithRag(String message, String chatId) {
        // 查询重写：先对用户问题做改写，使检索结果更精准
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                // 使用改写后的查询
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用 RAG 知识库问答：基于本地向量库检索相关知识并注入上下文（默认启用）
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 应用 RAG 检索增强服务（基于云知识库服务，备选方案）
//                .advisors(loveAppRagCloudAdvisor)
                // 应用 RAG 检索增强服务（基于 PgVector 向量存储，备选方案）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器，备选方案）
//                .advisors(
//                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
//                                loveAppVectorStore, "单身"
//                        )
//                )
                .call()
                .chatResponse();
        // 提取回复文本并打印日志
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // ==================== AI 调用工具能力 ====================

    /** 项目注册的全部工具回调（由 Spring 自动注入，如文件操作、网页搜索等） */
    @Resource
    private ToolCallback[] allTools;

    /**
     * AI 对话功能（支持调用工具）
     * <p>
     * 让 AI 在对话过程中按需调用注册的工具（如查资料、操作文件），
     * 适合需要外部能力辅助回答的场景。
     *
     * @param message 用户输入的消息
     * @param chatId  会话 ID
     * @return AI 回复的文本内容
     */
    public String doChatWithTools(String message, String chatId) {
        // 发起对话调用并挂载全部工具回调，AI 可自主决定是否调用工具
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // ==================== AI 调用 MCP 服务 ====================

    /** MCP 工具回调提供者（通过 MCP 协议接入外部工具服务，由 Spring 自动注入） */
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 对话功能（调用 MCP 服务）
     * <p>
     * 与 doChatWithTools 类似，区别在于工具来源是 MCP 服务（如本项目自带的
     * 图片搜索 MCP Server），通过 ToolCallbackProvider 批量注册。
     *
     * @param message 用户输入的消息
     * @param chatId  会话 ID
     * @return AI 回复的文本内容
     */
    public String doChatWithMcp(String message, String chatId) {
        // 发起对话调用并挂载 MCP 提供的全部工具回调
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
