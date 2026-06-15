package com.shinhan.eclipse.worker.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("당신은 IPO 뉴스 요약 전문가입니다. 반드시 JSON 형식으로만 응답하세요.")
                .build();
    }
}
