package com.prashant.ai_chat_bot.controller;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatclient")
public class BasicChatController {

    //Spring Fluent API for talking to AI Models
    private final ChatClient chatClient;

    //auto configuration
    public BasicChatController(ChatModel chatModel){
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @PostMapping("/chat")
    public String chat(
            @RequestBody String messageInput) {

        return chatClient
                .prompt()
                .user(messageInput)
                .call()
                .content();
    }
}
