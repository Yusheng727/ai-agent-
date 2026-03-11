package com.yusheng.aiagentproject.app;

import com.yusheng.aiagentproject.advisor.ForbiddenWordsAdvisor;
import com.yusheng.aiagentproject.advisor.MyLoggerAdvisor;
import com.yusheng.aiagentproject.rag.LoveAppRagCustomAdvisorFactory;
import com.yusheng.aiagentproject.rag.QueryRewriter;
import com.yusheng.aiagentproject.tool.WebSearchTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
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
 */
@Slf4j
@Component
public class LoveApp {

    /**
     * 所有聊天场景共用的基础系统提示词。
     */
    private static final String SYSTEM_PROMPT = """
            你是一名专注恋爱与婚姻咨询的情感顾问。
            开场要先说明自己的身份，并告诉用户可以放心倾诉情感困扰。

            你需要围绕用户当前的感情阶段继续追问：
            1. 单身阶段：重点了解社交圈、择偶标准、主动表达和追求中的困难。
            2. 恋爱阶段：重点了解沟通方式、相处习惯、冲突触发点和安全感问题。
            3. 已婚阶段：重点了解家庭责任分配、亲密关系变化以及与双方家人的边界问题。

            回答时不要急着下结论，要先帮助用户把事情讲清楚，再给出温和、具体、可执行的建议。
            当用户明确需要最新、实时、站外信息时，可以调用联网搜索工具补充依据。
            """;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final VectorStore pgVectorVectorStore;
    private final VectorStore loveAppVectorStore;
    private final Advisor loveAppRagCloudAdvisor;
    private final QueryRewriter queryRewriter;
    private final WebSearchTool webSearchTool;

    public LoveApp(ChatModel dashscopeChatModel,
                   ChatMemory chatMemory,
                   ForbiddenWordsAdvisor forbiddenWordsAdvisor,
                   @Lazy @Qualifier("loveAppVectorStore") VectorStore loveAppVectorStore,
                   @Lazy @Qualifier("pgVectorVectorStore") VectorStore pgVectorVectorStore,
                   @Lazy @Qualifier("loveAppRagCloudAdvisor") Advisor loveAppRagCloudAdvisor,
                   QueryRewriter queryRewriter,
                   WebSearchTool webSearchTool) {
        this.chatMemory = chatMemory;
        this.loveAppVectorStore = loveAppVectorStore;
        this.pgVectorVectorStore = pgVectorVectorStore;
        this.loveAppRagCloudAdvisor = loveAppRagCloudAdvisor;
        this.queryRewriter = queryRewriter;
        this.webSearchTool = webSearchTool;
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
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .tools(webSearchTool)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 结构化输出对象。
     */
    public record LoveReport(String title, List<String> suggestions) {
    }

    /**
     * 结构化报告输出。
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + System.lineSeparator()
                        + "请将最终回答整理为结构化报告，包含标题和建议列表。")
                .user(message)
                .tools(webSearchTool)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    /**
     * 带 RAG 的聊天总入口。
     *
     * <p>这里保留多种 RAG 方案，便于手动切换验证。
     */
    public String doChatWithRag(String message, String chatId) {
        // 方案一：手动 RAG（显示执行“查询改写 -> 检索 -> 拼接上下文 -> 回答”）
        // return doChatWithManualPgVectorRag(message, chatId);

        // 方案二：Advisor RAG（本地向量库）
        // return doChatWithAdvisorRag(message, chatId,
        //        new org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor(loveAppVectorStore));

        // 方案三：Advisor RAG（云端知识库服务）
        // return doChatWithAdvisorRag(message, chatId, loveAppRagCloudAdvisor);

        // 方案四：Advisor RAG（PGVector 向量库）
        // return doChatWithAdvisorRag(message, chatId,
        //        new org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor(pgVectorVectorStore));

        // 方案五：Advisor RAG（自定义元数据过滤）
        return doChatWithAdvisorRag(message, chatId,
                LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(loveAppVectorStore, "已婚"));
    }

    /**
     * 手动 RAG 方案。
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
                .tools(webSearchTool)
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
     */
    private String doChatWithAdvisorRag(String message, String chatId, Advisor ragAdvisor) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .tools(webSearchTool)
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

    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))

                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
