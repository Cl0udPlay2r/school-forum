package com.example.controller;

import com.example.entity.RestBean;
import com.example.entity.vo.response.NotificationVO;
import com.example.service.NotificationService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    @Resource
    NotificationService service;

    @GetMapping("/list")
    public RestBean<List<NotificationVO>> listNotification(@RequestAttribute(Const.ATTR_USER_ID) int uid){
        return RestBean.success(service.findUserNotNotification(uid));
    }

    @GetMapping("/delete")
    public RestBean<Void> deleteNotification(@RequestParam @Min(0) int id,
                                             @RequestAttribute(Const.ATTR_USER_ID) int uid){
        service.deleteUserNotNotification(id,uid);
        return RestBean.success();
    }

    @GetMapping("/delete-all")
    public RestBean<Void> deleteAllNotification(@RequestAttribute(Const.ATTR_USER_ID) int uid){
        service.deleteUserAllNotification(uid);
        return RestBean.success();
    }
}
