package com.example.service;


import com.alibaba.fastjson2.JSONArray;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AIChatService {
    SseEmitter chatWithAi(JSONArray content);
}
