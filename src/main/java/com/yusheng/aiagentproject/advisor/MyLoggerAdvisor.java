package com.yusheng.aiagentproject.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor, CallAroundAdvisor, StreamAroundAdvisor {

    /**
     * advisor 执行顺序，值越小越早执行。
     */
    private final int order;

    /**
     * 是否记录经过 around advisor 改写后的请求内容。
     */
    private final boolean logAdvisedRequest;

    /**
     * 默认日志顾问：保留原有行为，只记录普通 ChatClientRequest。
     */
    public MyLoggerAdvisor() {
        this(0, false);
    }

    public MyLoggerAdvisor(int order) {
        this(order, false);
    }

    /**
     * 创建一个专门用于 RAG 调试的日志顾问。
     * 这个实例会输出 QuestionAnswerAdvisor 改写后的 userText 和检索上下文。
     */
    public static MyLoggerAdvisor advisedLogger(int order) {
        return new MyLoggerAdvisor(order, true);
    }

    private MyLoggerAdvisor(int order, boolean logAdvisedRequest) {
        this.order = order;
        this.logAdvisedRequest = logAdvisedRequest;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    /**
     * 记录普通请求链路里的 prompt 内容。
     */
    private ChatClientRequest before(ChatClientRequest request) {
        log.info("AI Request: {}", request.prompt().getContents());
        return request;
    }

    /**
     * 记录 around advisor 链路中的改写结果。
     * 在 RAG 场景下，这里可以看到 question_answer_context 是否已经注入。
     */
    private AdvisedRequest before(AdvisedRequest request) {
        if (!logAdvisedRequest) {
            return request;
        }
        log.info("AI Advised User Text: {}", request.userText());

        Object questionAnswerContext = request.userParams().get("question_answer_context");
        if (questionAnswerContext != null) {
            log.info("AI Question Answer Context: {}", abbreviate(questionAnswerContext.toString()));
        }

        Object retrievedDocuments = request.adviseContext().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        if (retrievedDocuments instanceof List<?> documents) {
            log.info("AI Retrieved Documents Count: {}", documents.size());
            String documentPreview = documents.stream()
                    .filter(Document.class::isInstance)
                    .map(Document.class::cast)
                    .map(Document::getText)
                    .filter(StringUtils::hasText)
                    .limit(3)
                    .reduce((left, right) -> left + System.lineSeparator() + "-----" + System.lineSeparator() + right)
                    .orElse("");
            if (StringUtils.hasText(documentPreview)) {
                log.info("AI Retrieved Document Preview: {}", abbreviate(documentPreview));
            }
        }
        return request;
    }

    /**
     * 避免日志过长，把长文本裁剪到固定长度。
     */
    private String abbreviate(String text) {
        int maxLength = 500;
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private void observeAfter(ChatClientResponse response) {
        log.info("AI Response: {}", response.chatResponse().getResult().getOutput().getText());
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        request = this.before(request);
        ChatClientResponse response = chain.nextCall(request);
        this.observeAfter(response);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        request = this.before(request);
        Flux<ChatClientResponse> responses = chain.nextStream(request);
        return new ChatClientMessageAggregator().aggregateChatClientResponse(responses, this::observeAfter);
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        return chain.nextAroundCall(this.before(advisedRequest));
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        return chain.nextAroundStream(this.before(advisedRequest));
    }
}
