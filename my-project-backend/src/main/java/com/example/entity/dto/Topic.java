package com.example.entity.dto;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.entity.BaseData;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_topic")
public class Topic implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String title;
    String intro;
    String content;
    Date time;
    Integer type;
    Integer uid;
    Integer top;
    Integer locked;
    Integer invisible;

    public static String recreateIntro(JSONObject content) {
        StringBuilder builder = new StringBuilder();
        JSONArray ops = content.getJSONArray("ops");
        for (Object op : ops) {
            Object insert = JSONObject.from(op).get("insert");
            if (insert instanceof String text) {
                builder.append(text);
            }
        }

        return builder.toString();
    }

    public void createIntro() {
        intro = recreateIntro(JSONObject.from(content));
    }
}
