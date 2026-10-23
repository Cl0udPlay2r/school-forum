package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.service.AIChatService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class AIChatServiceImpl implements AIChatService {

    @Resource
    ChatModel chatModel;

    @Override
    public SseEmitter chatWithAi(JSONArray content) {

        SseEmitter emitter = new SseEmitter(180_000L);

        List<Object> list = content.stream().map(item -> {
            JSONObject ob = JSONObject.from(item);
            return switch (ob.getString("type")) {
                case "user" -> new UserMessage(ob.getString("text"));
                case "assistant" -> new AssistantMessage(ob.getString("text"));
                default -> new RuntimeException();
            };
        }).toList();

        Prompt prompt = new Prompt(list.toArray(new Message[0]));
        Flux<ChatResponse> flux = chatModel.stream(prompt);

        flux.subscribe(response -> {
            String text = response.getResult().getOutput().getText();
            try {
                emitter.send(text);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }, emitter::completeWithError, emitter::complete);

        return emitter;
    }
}
