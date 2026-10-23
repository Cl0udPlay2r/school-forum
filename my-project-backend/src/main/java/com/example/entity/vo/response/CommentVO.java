package com.example.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class CommentVO {
    int id;
    String content;
    Date time;
    String quote;
    User user;

    @Data
    public static class User{
        int id;
        String name;
        String avatar;
    }
}
