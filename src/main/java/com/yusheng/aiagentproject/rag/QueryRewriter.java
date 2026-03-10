package com.yusheng.aiagentproject.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

@Component
public class QueryRewriter {
    private final QueryTransformer queryTransformer;
    // 构造函数注入 ChatModel，创建 QueryTransformer 实例
    public QueryRewriter(ChatModel dashscopeChatModel) {
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);
        // 可以在这里添加一些默认的系统提示或者顾问，以帮助模型更好地理解查询改写的上下文。
        queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }
    public String doQueryRewrite(String prompt) {
        Query query = new Query(prompt);
        // 执行查询改写
        Query transformedQuery = queryTransformer.transform(query);
        // 返回改写后的查询文本
        return transformedQuery.text();
    }
}
