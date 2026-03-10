package com.yusheng.aiagentproject.app;

import com.yusheng.aiagentproject.advisor.ForbiddenWordsAdvisor;
import com.yusheng.aiagentproject.advisor.MyLoggerAdvisor;
import com.yusheng.aiagentproject.rag.LoveAppRagCustomAdvisorFactory;
import com.yusheng.aiagentproject.rag.QueryRewriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * LoveApp 是项目里的情感咨询入口类。
 *
 * <p>可以把它理解成一个统一的“聊天前台”：
 * 普通聊天、结构化输出、RAG 检索增强聊天，最终都从这里进入。
 *
 * <p>这个类本身不负责向量入库，也不负责记忆存储的底层实现，
 * 它只负责把这些能力组织起来，然后交给 ChatClient 去发起一次完整调用。
 */
@Slf4j
@Component
public class LoveApp {

    /**
     * 所有聊天场景共用的基础系统提示词。
     *
     * <p>它定义了这个 AI 助手的角色、说话方式和追问方向。
     * 即使后面进入 RAG 场景，也是在这个提示词基础上追加知识库上下文，
     * 而不是完全换掉一套新人格。
     */
    private static final String SYSTEM_PROMPT = """
            你是一名专注恋爱与婚姻咨询的情感顾问。
            开场要先说明自己的身份，并告诉用户可以放心倾诉情感困扰。

            你需要围绕用户当前的感情阶段继续追问：
            1. 单身阶段：重点了解社交圈、择偶标准、主动表达和追求中的困难。
            2. 恋爱阶段：重点了解沟通方式、相处习惯、冲突触发点和安全感问题。
            3. 已婚阶段：重点了解家庭责任分配、亲密关系变化以及与双方家人的边界问题。

            回答时不要急着下结论，要先帮助用户把事情讲清楚，再给出温和、具体、可执行的建议。
            """;

    /**
     * ChatClient 是 Spring AI 提供的统一调用入口。
     *
     * <p>它相当于一个已经预装好默认系统提示词、默认顾问链的“聊天客户端”。
     * 后续每次 doChat / doChatWithReport / doChatWithRag，
     * 都是在这个客户端上继续补充 user、system 或 advisors 参数。
     */
    private final ChatClient chatClient;

    /**
     * ChatMemory 负责保存多轮对话记忆。
     *
     * <p>这里虽然没有直接手工操作它，
     * 但会通过 MessageChatMemoryAdvisor 把历史消息带入后续提问中。
     */
    private final ChatMemory chatMemory;

    /**
     * PGVector 版向量库。
     *
     * <p>这是当前 RAG 场景真正使用的向量检索实现，
     * 适合对接 PostgreSQL + PGVector 存储。
     */
    private final VectorStore pgVectorVectorStore;

    /**
     * 本地内存版向量库。
     *
     * <p>它基于 SimpleVectorStore，适合本地调试或和 PGVector 做效果对比。
     */
    private final VectorStore loveAppVectorStore;

    /**
     * 基于云端知识库服务的 RAG 顾问。
     *
     * <p>启用这个 Bean 时，检索增强逻辑由云端知识库服务完成。
     */
    private final Advisor loveAppRagCloudAdvisor;

    /**
     * 查询重写器。
     *
     * <p>它的作用不是替用户发言，而是把原始问题改写成更适合“检索知识库”的表达，
     * 让相似度搜索更容易命中相关文档。
     */
    private final QueryRewriter queryRewriter;

    /**
     * 构造函数中完成 LoveApp 的核心依赖装配。
     *
     * <p>这里做了三件事：
     * 1. 注入底层聊天模型 ChatModel。
     * 2. 注入对话记忆和敏感词顾问。
     * 3. 注入不同的 RAG 方案依赖，方便在同一个入口里切换测试。
     */
    public LoveApp(ChatModel dashscopeChatModel,
                   ChatMemory chatMemory,
                   ForbiddenWordsAdvisor forbiddenWordsAdvisor,
                   @Lazy @Qualifier("loveAppVectorStore") VectorStore loveAppVectorStore,
                   @Lazy @Qualifier("pgVectorVectorStore") VectorStore pgVectorVectorStore,
                   @Lazy @Qualifier("loveAppRagCloudAdvisor") Advisor loveAppRagCloudAdvisor,
                   QueryRewriter queryRewriter) {
        this.chatMemory = chatMemory;
        this.loveAppVectorStore = loveAppVectorStore;
        this.pgVectorVectorStore = pgVectorVectorStore;
        this.loveAppRagCloudAdvisor = loveAppRagCloudAdvisor;
        this.queryRewriter = queryRewriter;
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        forbiddenWordsAdvisor,
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    /**
     * 最基础的聊天方法。
     *
     * <p>特点：
     * 1. 使用默认系统提示词。
     * 2. 使用默认 advisor，包括敏感词过滤和聊天记忆。
     * 3. 不做结构化输出，也不走知识库检索。
     *
     * @param message 用户本次输入
     * @param chatId  对话 ID，用于把多轮消息串起来
     * @return 模型生成的自然语言回答
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 结构化输出对象。
     *
     * <p>这里用 Java record 接收模型返回的结构化 JSON，
     * 相比普通 String，更适合后续页面展示或接口返回。
     */
    record LoveReport(String title, List<String> suggestions) {
    }

    /**
     * 让模型按结构化报告形式返回结果。
     *
     * <p>和 doChat 相比，这里额外加了一段 system 提示，
     * 明确要求模型输出“标题 + 建议列表”，
     * 最终再通过 entity(LoveReport.class) 映射成 Java 对象。
     *
     * @param message 用户问题
     * @param chatId  对话 ID
     * @return 结构化恋爱建议报告
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + System.lineSeparator()
                        + "请将最终回答整理为结构化报告，包含标题和建议列表。")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    /**
     * 带 RAG 的聊天总入口。
     *
     * <p>这里故意保留多种 RAG 方案，方便你通过注释切换当前要测试的方法。
     * 但同一时刻只建议启用一种方案，避免出现“手动检索一次，advisor 又检索一次”的重复增强问题。
     *
     * @param message 用户原始问题
     * @param chatId  对话 ID
     * @return 引入知识库后的最终回答
     */
    public String doChatWithRag(String message, String chatId) {
        // 方案一：手动 RAG（默认启用，最适合观察查询改写、检索结果和上下文拼接过程）
        //return doChatWithManualPgVectorRag(message, chatId);

        // 方案二：Advisor RAG（本地 SimpleVectorStore）
        // return doChatWithAdvisorRag(message, chatId, new QuestionAnswerAdvisor(loveAppVectorStore));

        // 方案三：Advisor RAG（云端知识库服务）
        // return doChatWithAdvisorRag(message, chatId, loveAppRagCloudAdvisor);

        // 方案四：Advisor RAG（PGVector 向量库存储）
        // return doChatWithAdvisorRag(message, chatId, new QuestionAnswerAdvisor(pgVectorVectorStore));

        // 方案五：Advisor RAG（PGVector + 自定义元数据过滤）
         return doChatWithAdvisorRag(message, chatId,
                 LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(loveAppVectorStore, "已婚"));
    }

    /**
     * 手动 RAG 方案。
     *
     * <p>这个方法显式执行“查询重写 -> 向量检索 -> 上下文拼接 -> 最终回答”。
     * 它最适合学习和排查，因为每一步都能看到中间结果。
     */
    private String doChatWithManualPgVectorRag(String message, String chatId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        log.info("Original question: {}", message);
        log.info("Rewritten retrieval question: {}", rewrittenMessage);

        List<Document> retrievedDocuments = pgVectorVectorStore.similaritySearch(
                SearchRequest.builder().query(rewrittenMessage).topK(3).build()
        );

        String context = retrievedDocuments.stream()
                .map(Document::getText)
                .reduce((left, right) -> left + System.lineSeparator() + "-----" + System.lineSeparator() + right)
                .orElse("");

        log.info("RAG retrieved documents count: {}", retrievedDocuments.size());
        if (!context.isBlank()) {
            String preview = context.length() > 500 ? context.substring(0, 500) + "..." : context;
            log.info("RAG retrieved document preview: {}", preview);
        }

        String ragSystemPrompt = SYSTEM_PROMPT + System.lineSeparator()
                + "请优先参考下面的知识库内容来回答用户问题。"
                + "如果知识库里已经给出了明确事实，不要否认它。"
                + System.lineSeparator()
                + "知识库内容："
                + System.lineSeparator()
                + context;

        ChatResponse chatResponse = chatClient
                .prompt()
                .system(ragSystemPrompt)
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(MyLoggerAdvisor.advisedLogger(100))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * Advisor RAG 通用方案。
     *
     * <p>只要传入不同的 Advisor，就能复用同一套调用模板。
     * 这样 LoveApp 里既能保留多种 RAG 方案，又不会把各套写法散落得到处都是。
     */
    private String doChatWithAdvisorRag(String message, String chatId, Advisor ragAdvisor) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(MyLoggerAdvisor.advisedLogger(100))
                .advisors(ragAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}