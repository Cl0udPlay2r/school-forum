package com.example.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class UserTopicPreviewVO {
    private Integer id;
    private int type;
    private String title;
    private Date time;
    private Integer top;
    private Integer locked;
    private Integer invisible;
}
