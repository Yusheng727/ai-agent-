package com.yusheng.aiagentproject.app;

import com.yusheng.aiagentproject.advisor.MyLoggerAdvisor;
import com.yusheng.aiagentproject.chatmemory.FileBasedChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Component
public class LoveApp {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    private static final String SYSTEM_PROMPT = """
            扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。
            围绕单身、恋爱、已婚三种状态提问：
            单身状态询问社交圈拓展及追求心仪对象的困难；
            恋爱状态询问沟通、习惯差异引发的矛盾；
            已婚状态询问家庭责任与亲属关系处理的问题。
            引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。
            """;

    /**
     * AI 客户端
     *
     * @param dashscopeChatModel 注入的 ChatModel 实例，用于配置聊天模型
     */
    public LoveApp(ChatModel dashscopeChatModel) {
        String fileDir = Paths.get(System.getProperty("user.dir"), "tmp", "chat-memory").toString();
        this.chatMemory = new FileBasedChatMemory(fileDir);

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                        // new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话，支持多轮记忆
     *
     * @param message 用户输入的消息内容
     * @param chatId  聊天会话的唯一标识符，用于关联聊天历史
     * @return 聊天响应的文本内容
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

    record LoveReport(String title, List<String> suggestions) {
    }

    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱报告，标题为‘用户名’的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }
}