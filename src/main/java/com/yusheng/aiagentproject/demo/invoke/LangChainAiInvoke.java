package com.yusheng.aiagentproject.demo.invoke;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenChatModel;

public class LangChainAiInvoke {
    public static void main(String[] args) {

        QwenChatModel model = QwenChatModel.builder()
                .apiKey(TestApiKey.API_KEY)
                .modelName("qwen-max")
                .build();

        String answer = model.chat("请介绍一下Java语言");

        System.out.println(answer);
    }
}
