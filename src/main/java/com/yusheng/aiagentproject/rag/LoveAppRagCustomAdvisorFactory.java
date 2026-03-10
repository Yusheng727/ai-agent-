package com.yusheng.aiagentproject.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

@Slf4j
public class LoveAppRagCustomAdvisorFactory {
    public static Advisor createLoveAppRagCustomAdvisor(VectorStore vectorStore, String status) {
        // 构建一个简单的过滤表达式，假设文档中有一个 "status" 字段，我们根据传入的 status 参数进行过滤。
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("status", status)
                .build();
        // 创建一个 VectorStoreDocumentRetriever，并设置过滤表达式、相似度阈值和返回的 topK 数量。
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression)
                .similarityThreshold(0.5)
                .topK(3)
                .build();
        // 将这个 documentRetriever 注入到 RetrievalAugmentationAdvisor 中，并返回这个顾问实例。
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(LoveAppContextualQueryAugmenterFactory.createInstance())
                .build();
    }
}
