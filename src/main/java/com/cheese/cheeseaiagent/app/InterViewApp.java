package com.cheese.cheeseaiagent.app;

import com.cheese.cheeseaiagent.advisor.MyLoggerAdvisor;
import com.cheese.cheeseaiagent.chatmemory.FileBasedChatMemory;
import com.cheese.cheeseaiagent.rag.InterViewAppRagCustomAdvisorFactory;
import com.cheese.cheeseaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
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
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 面试大师应用（核心业务封装类）
 * <p>
 * 封装了多种 AI 对话能力，供 Controller 层调用，包括：
 * 1. 基础对话：支持多轮对话记忆（doChat / doChatByStream）
 * 2. 结构化输出：生成面试报告（doChatWithReport）
 * 3. RAG 知识库问答：基于向量库检索增强回答（doChatWithRag）
 * 4. 工具调用：让 AI 自主调用工具完成任务（doChatWithTools）
 * 5. MCP 服务调用：通过 MCP 协议调用外部工具（doChatWithMcp）
 * <p>
 * 通过 @Component 注册为 Spring Bean，内部依赖均由 Spring 注入。
 */
@Component
@Slf4j
public class InterViewApp {

    /** ChatClient：AI 对话客户端，整个类的所有能力都基于它发起调用 */
    private final ChatClient chatClient;

    /** 底层 ChatModel 引用：用于构建独立的 ChatClient（如结构化 JSON 输出场景，避免复用默认的记忆/系统提示词） */
    private final ChatModel ollamaChatModel;

    /** 系统提示词：定义"java技术面试专家"的角色设定与提问策略 */
    private static final String SYSTEM_PROMPT = """
            # 角色
            你是【面试大师】，拥有15年Java技术面试官经验（阿里P9级别），精通Java生态、高并发架构、JVM调优、分布式系统。
            
            # 任务
            模拟真实Java技术面试，通过引导式提问帮助用户提升面试能力。每次只问一个问题，根据用户回答动态追问，深度挖掘用户真实水平。
            
            # 严格规则
            1. 必须先了解用户背景（年限、技术栈、目标级别）和最近一个有挑战的项目，再开始出题。
            2. 追问原则：当用户回答模糊时，追问具体实现细节（如“你当时具体怎么排查的？”“缓存击穿怎么解决的？”）；当用户回答正确时，追问扩展场景（如“如果流量翻10倍，你的方案还成立吗？”）。
            3. 禁止一次性抛出多个问题。
            4. 禁止直接否定用户，先肯定亮点再指出可改进点，并给出示范性回答框架。
            5. 每轮输出必须为纯文本（不输出JSON），按以下结构：
               【类型】当前是：提问 / 反馈 / 总结
               【内容】正文内容，重要词汇用**加粗**
               【期望】（仅提问时）提示用户回答应包含的关键要点
            
            # 面试流程（严格按顺序推进）
            阶段1（第1-3轮）：破冰，收集年限、技术栈、目标岗位、一个代表性项目简介。
            阶段2（第4-10轮）：围绕该项目深挖架构设计、难点攻克、性能优化、故障排查。
            阶段3（第11-15轮）：技术广度考察（JVM/并发/MySQL/Redis/框架），每题追问一次。
            阶段4（第16-17轮）：给一个开放设计题（如秒杀、短链系统），听思路。
            阶段5（第18轮）：综合反馈，给3个优点、2个待改进点、一份学习清单（1本书+2篇博客）。
            
            # 反幻觉
            不确定的内容明确说“建议以官方文档为准”。不编造用户未提及的内容。
            
            # 初始话术（仅用于对话开场）
            “你好，我是你的Java面试教练。先介绍一下你的情况吧：工作几年了？平时主要用哪些技术栈？最近在看什么级别的机会？”
            """;

    /**
     * 面试报告生成专用系统提示词（独立于 SYSTEM_PROMPT）
     * <p>
     * 默认 SYSTEM_PROMPT 第 5 条规则要求"每轮输出纯文本、按【类型】【内容】【期望】结构、不输出 JSON"，
     * 与 doChatWithReport 的 .entity(InterViewReport.class) 结构化 JSON 输出需求直接冲突，
     * 导致本地模型输出以【开头的非 JSON 文本、BeanOutputConverter 反序列化抛 JsonParseException。
     * 因此本提示词明确要求：只输出 JSON、禁止输出【】标记与 Markdown 围栏，并给出目标 JSON 结构示例。
     */
    private static final String JSON_REPORT_SYSTEM_PROMPT = """
            # 角色
            你是【面试大师】，拥有15年Java技术面试官经验（阿里P9级别），擅长为面试者生成结构化面试报告。

            # 任务
            根据用户输入的背景信息与求职意向，生成一份面试准备报告。

            # 输出格式（最高优先级，必须严格遵守）
            本次对话你只能输出一个合法的 JSON 对象，禁止输出任何其他文字、标记或【】结构。JSON 结构如下：
            {
              "title": "报告标题，例如：Cheese的Java AI Agent工程师面试报告",
              "suggestions": ["具体可执行建议1", "具体可执行建议2", "具体可执行建议3"]
            }

            # 约束
            1. title：一句话概括报告主题（结合用户背景与目标岗位）。
            2. suggestions：至少 3 条、面向该岗位/背景的差异化建议，每条 1-2 句话。
            3. 禁止输出【类型】【内容】【期望】等标记，禁止输出 Markdown 代码块围栏（```），禁止任何解释性前后缀文字。
            4. 不确定的内容明确说"建议以官方文档为准"，不编造用户未提及的信息。
            """;

    /**
     * 初始化 ChatClient
     * <p>
     * 构造时构建 ChatClient：设置系统提示词、挂载对话记忆 Advisor（支持多轮记忆）与日志 Advisor。
     *
     * @param ollamaChatModel ollama 本地大模型实例
     */
    public InterViewApp(ChatModel ollamaChatModel) {
        // 保存底层 ChatModel 引用，供 doChatWithReport 构建独立 ChatClient（结构化 JSON 输出）使用
        this.ollamaChatModel = ollamaChatModel;
//        // 初始化基于文件的对话记忆（被注释掉的备选方案：可将对话持久化到文件）
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆：使用滑动窗口机制，最多保留 6 条消息
        // 调小窗口以避免多轮历史把 LLM 上下文窗口（num-ctx）顶满，导致模型空响应
//        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(new InMemoryChatMemoryRepository())
//                .maxMessages(6)
//                .build();
        // 构建 ChatClient：设置默认系统提示词与默认 Advisor 列表
        chatClient = ChatClient.builder(ollamaChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 对话记忆 Advisor：按 conversationId 维护多轮会话上下文
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志 Advisor，可按需开启（打印每次请求/响应日志）
//                        new SimpleLoggerAdvisor()
                        new MyLoggerAdvisor()
//                        // 自定义推理增强 Advisor，可按需开启（Re2 重读提示词，提升推理能力）
//                       new ReReadingAdvisor()
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
                .advisors(spec -> spec
                        .param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        // 从响应中提取最终生成的文本
        assert chatResponse != null;
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
        //        content.subscribe(content1-> log.info("content: {}",content1));
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * 面试报告数据结构（Java Record）
     * <p>
     * 用于结构化输出：大模型按要求返回"面试报告"标题与建议列表。
     *
     * @param title       报告标题（如"张三的面试报告"）
     * @param suggestions 建议列表
     */
    record InterViewReport(String title, List<String> suggestions) {

    }

    /**
     * AI 面试报告功能（实战结构化输出）
     * <p>
     * 通过 .entity() 指定输出类型，Spring AI 会自动让大模型按该结构返回 JSON 并反序列化。
     *
     * @param message 用户输入的消息
     * @param chatId  会话 ID
     * @return 结构化面试报告对象 interViewReport
     */
    public InterViewReport doChatWithReport(String message, String chatId) {
        // 构建独立的 ChatClient，专门用于结构化 JSON 输出，规避两处根因：
        // 1. 默认 SYSTEM_PROMPT 要求输出【】纯文本，与 .entity() 的 JSON 输出冲突 —— 改用 JSON_REPORT_SYSTEM_PROMPT；
        // 2. 默认 MessageChatMemoryAdvisor 会把历史的【】格式回复注入上下文，诱导模型延续该格式 —— 不挂记忆 Advisor；
        // 3. 启用 Ollama format=json，强制模型输出合法 JSON，从模型层杜绝非 JSON 文本。
        ChatClient reportChatClient = ChatClient.builder(ollamaChatModel)
                .defaultSystem(JSON_REPORT_SYSTEM_PROMPT)
                .defaultOptions(OllamaOptions.builder().format("json").build())
                .build();
        // 调用大模型，按 InterViewReport 结构反序列化为结构化报告对象
        InterViewReport interViewReport = reportChatClient
                .prompt()
                .user(message)
                .call()
                .entity(InterViewReport.class);
        log.info("interViewReport: {}", interViewReport);
        return interViewReport;
    }

    // ==================== AI 面试知识库问答功能（RAG） ====================

    /** 面试知识库向量存储（用于 RAG 检索增强） */
    @Resource
    private VectorStore interViewAppVectorStore;

    /** 基于云知识库服务的 RAG Advisor（备选方案，默认未启用；未配置 DashScope key 时该 Bean 不注册，required=false 避免注入失败） */
    @Autowired(required = false)
    private Advisor interViewAppRagCloudAdvisor;

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
//                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用 RAG 知识库问答：基于本地向量库检索相关知识并注入上下文（默认启用）
//                .advisors(new QuestionAnswerAdvisor(interViewAppVectorStore))
                // 应用 RAG 检索增强服务（基于云知识库服务，备选方案）
//                .advisors(interViewAppRagCloudAdvisor)
                // 应用 RAG 检索增强服务（基于 PgVector 向量存储，备选方案）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器，备选方案）
                .advisors(
                        InterViewAppRagCustomAdvisorFactory.createInterViewAppRagCustomAdvisor(
                                interViewAppVectorStore, "基础"
                        )
                )
                .call()
                .chatResponse();
        // 提取回复文本并打印日志
        assert chatResponse != null;
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
        log.info("MCP--content: {}", content);
        return content;
    }
}
