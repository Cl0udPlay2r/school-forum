package com.example.controller.admin;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.entity.PageRestBean;
import com.example.entity.RestBean;
import com.example.entity.vo.response.TopicPreviewVO;
import com.example.service.TopicService;
import com.example.utils.ProhibitedUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/forum")
public class ForumAdminController {
    @Resource
    TopicService service;

    @Resource
    ProhibitedUtils prohibitedUtils;

    @GetMapping("/list")
    public PageRestBean<TopicPreviewVO> listTopic(@RequestParam int page,
                                                  @RequestParam int size,
                                                  @RequestParam(required = false) String keyword) {
        JSONObject object = service.listTopicAdmin(page, size, keyword);
        return PageRestBean.success(object.getJSONArray("list").toList(TopicPreviewVO.class),
                object.getIntValue("total"),
                page);
    }

    @GetMapping("/delete")
    public RestBean<Void> deleteTopic(@RequestParam int topicId) {
        service.deleteTopic(topicId);
        return RestBean.success();
    }

    @PostMapping("/top")
    public RestBean<Void> getTopTopic(@RequestBody JSONObject object) {
        service.setTop(object.getIntValue("tid"),
                object.getBooleanValue("status"));
        return RestBean.success();
    }

    @PostMapping("/lock")
    public RestBean<Void> lockTopic(@RequestBody JSONObject object) {
        service.lockTopic(object.getIntValue("tid"),
                object.getBooleanValue("status"));
        return RestBean.success();
    }

    @PostMapping("/invisible")
    public RestBean<Void> blockTopic(@RequestBody JSONObject object) {
        service.blockTopic(object.getIntValue("tid"),
                object.getBooleanValue("status"));
        return RestBean.success();
    }

    @GetMapping("/prohibited-list")
    public RestBean<List<String>> getProhibitedWords() {
        return RestBean.success(prohibitedUtils.getProhibitedWords());
    }

    @PostMapping("/prohibited-save")
    public RestBean<Void> saveProhibitedWords(@RequestBody JSONArray object) {
        prohibitedUtils.saveProhibitedWords(object.toList(String.class));
        return RestBean.success();
    }


}
