package com.example.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
public class TopicDetailVO {
    Integer id;
    String title;
    String content;
    Integer type;
    Date time;
    User user;
    InteractTopic interactTopic;
    Long comments;
    Integer locked;
    Integer invisible;


    @Data
    @AllArgsConstructor
    public static class InteractTopic{
        Boolean like;
        Boolean collect;
    }

    @Data
    public static class User{
        Integer id;
        String username;
        String avatar;
        String desc;
        Integer gender;
        String phone;
        String qq;
        String wx;
        String email;
    }
}
