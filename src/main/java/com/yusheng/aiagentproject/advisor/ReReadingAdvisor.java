package com.yusheng.aiagentproject.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

/**
 * 自定义的重读建议器，提示AI重新阅读用户输入的问题，以帮助AI更好地理解问题并提供更准确的回答。
 * 该建议器在AI生成回答之前，将用户输入的问题重新格式化并添加到AI的提示中，促使AI重新审视用户的问题，从而提高回答的相关性和准确性。
 */
public class ReReadingAdvisor implements BaseAdvisor {


	private static final String DEFAULT_RE2_ADVISE_TEMPLATE = """
			{re2_input_query}
			Read the question again: {re2_input_query}
			""";

	private final String re2AdviseTemplate;

	private int order = 0;

	public ReReadingAdvisor() {
		this(DEFAULT_RE2_ADVISE_TEMPLATE);
	}

	public ReReadingAdvisor(String re2AdviseTemplate) {
		this.re2AdviseTemplate = re2AdviseTemplate;
	}

	/**
	 * 执行请求前，改写Prompt，提示AI重新阅读用户输入的问题
	 */
	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
		String augmentedUserText = PromptTemplate.builder()
			.template(this.re2AdviseTemplate)
			.variables(Map.of("re2_input_query", chatClientRequest.prompt().getUserMessage().getText()))
			.build()
			.render();

		return chatClientRequest.mutate()
			.prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
			.build();
	}

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
		return chatClientResponse;
	}

	@Override
	public int getOrder() { 
		return this.order;
	}

	public ReReadingAdvisor withOrder(int order) {
		this.order = order;
		return this;
	}

}