package com.yusheng.aiagentproject.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author yusheng
 * @date 2024/6/11 17:01
 * SpringAI框架调用示例
 */
@Component
public class SpringAiAiInvoke implements CommandLineRunner {

    @Resource
    private ChatModel dashscopeChatModel;
    // 通过实现CommandLineRunner接口，在应用启动时执行调用示例
    @Override
    // 调用ChatModel的call方法，传入Prompt对象，获取AssistantMessage结果并打印输出
    public void run(String... args) throws Exception {
        AssistantMessage output = dashscopeChatModel.call(new Prompt("你好，我是yusheng"))
                .getResult()
                .getOutput();
        System.out.println(output.getText());
    }
}
