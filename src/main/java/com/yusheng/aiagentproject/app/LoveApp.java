package com.yusheng.aiagentproject.app;

import com.yusheng.aiagentproject.advisor.ForbiddenWordsAdvisor;
import com.yusheng.aiagentproject.advisor.MyLoggerAdvisor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Slf4j
@Component
public class LoveApp {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    /**
     * 系统提示词：将模型设定为恋爱顾问，聚焦单身、恋爱、已婚三类情感问题。
     */
    private static final String SYSTEM_PROMPT = """
            扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。
            围绕单身、恋爱、已婚三种状态提问：
            单身状态询问社交圈拓展及追求心仪对象的困难；
            恋爱状态询问沟通、习惯差异引发的矛盾；
            已婚状态询问家庭责任与亲属关系处理的问题。
            引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。
            """;

    /**
     * 构造 AI 聊天应用，注入聊天模型、聊天记忆和敏感词顾问。
     */
    public LoveApp(ChatModel dashscopeChatModel,
                   ChatMemory chatMemory,
                   ForbiddenWordsAdvisor forbiddenWordsAdvisor) {
        this.chatMemory = chatMemory;

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 先做敏感词处理，再做记忆注入，最后记录常规请求日志。
                        forbiddenWordsAdvisor,
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                        // new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * 基础对话，支持多轮记忆。
     *
     * @param message 用户输入的消息内容
     * @param chatId 聊天会话 ID，用于关联历史记忆
     * @return 模型返回的文本内容
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content:{}", content);
        return content;
    }

    /**
     * 恋爱报告结构化返回对象。
     */
    record LoveReport(String title, List<String> suggestions) {
    }

    /**
     * 生成结构化恋爱报告。
     *
     * @param message 用户输入的消息内容
     * @param chatId 聊天会话 ID，用于关联历史记忆
     * @return 结构化恋爱报告
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱报告，标题为用户名的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    /**
     * RAG 使用的向量库。
     */
    @Resource
    private VectorStore loveAppVectorStore;
    @Resource
    private Advisor loveAppRagCloudAdvisor;

    /**
     * RAG 对话：先做向量检索，再把检索结果拼进提示词中参与回答。
     *
     * @param message 用户输入的消息内容
     * @param chatId 聊天会话 ID，用于关联历史记忆
     * @return 带检索增强结果的回答文本
     */
    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 先执行 RAG 检索和提示词改写。
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                // 再记录改写后的请求，便于调试观察 question_answer_context 是否已拼接。
                .advisors(MyLoggerAdvisor.advisedLogger(100))
                .advisors(loveAppRagCloudAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }



}
