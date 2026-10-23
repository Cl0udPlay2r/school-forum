package com.example.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ProhibitedUtils {

    private static final String filePath = "./prohibited.json";
    private static List<String> prohibitedWords;


    @PostConstruct
    public void init() {
        try (FileInputStream stream = new FileInputStream(filePath)) {
            JSONArray parse = JSONArray.parse(new String(stream.readAllBytes()));
            prohibitedWords = parse.toList(String.class);
        } catch (IOException e) {
            log.error("违禁词读取失败");
            prohibitedWords = new ArrayList<>();
        }
    }

    /**
     * 检测字符串中是否包含违禁词（用于评论检测
     *
     * @param word 字符串
     * @return 包含返回正，反之
     */
    public boolean containProhibitedWord(String word) {
        for (String pro : prohibitedWords) {
            if (word.contains(pro)) return true;
        }
        return false;
    }

    /***
     *  检测JSON数组中是否含有违禁词（用于帖子内容检测）
     * @param object 帖子对象
     * @return 包含返回正，反之
     */
    public boolean containProhibitedWord(JSONObject object) {
        for (String pro : prohibitedWords) {
            for (Object op : object.getJSONArray("ops")) {
                String text = JSONObject.from(op).getString("insert");
                if (text.contains(pro)) return true;
            }
        }
        return false;
    }

    /**
     * 获取违禁词列表
     *
     * @return 违禁词列表
     */
    public List<String> getProhibitedWords() {
        return prohibitedWords;
    }

    public void saveProhibitedWords(List<String> pro) {
        prohibitedWords = pro;
        try (FileOutputStream stream = new FileOutputStream(filePath)) {
            stream.write(JSONObject.toJSONString(pro).getBytes());
        } catch (IOException e) {
            log.error("保存违禁词失败:{}", e.getMessage());
        }
    }
}
