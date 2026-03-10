package com.yusheng.aiagentproject.rag;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * 恋爱app的上下文查询增强器工厂类
 * 这个工厂类创建了一个专门用于恋爱app的ContextualQueryAugmenter实例，
 * 当用户输入与恋爱无关的问题时，增强器会返回一个预设的提示信息，告知用户只能回答恋爱相关的问题。
 */
public class LoveAppContextualQueryAugmenterFactory {
    public static ContextualQueryAugmenter createInstance() {
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，我只能回答恋爱相关的问题，别的没办法帮到您哦，
                有问题可以联系编程导航客服 https://codefather.cn
                """);
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();
    }
}
