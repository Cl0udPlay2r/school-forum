package com.example.controller;

import com.example.entity.RestBean;
import com.example.entity.dto.Interact;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.*;
import com.example.service.AccountService;
import com.example.service.TopicService;
import com.example.service.WeatherService;
import com.example.utils.Const;
import com.example.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/forum")
public class ForumController {
    @Resource
    WeatherService weatherService;

    @Resource
    TopicService topicService;

    @Resource
    ControllerUtils utils;

    @Resource
    AccountService accountService;

    @GetMapping("/weather")
    public RestBean<WeatherVO> weather(double longitude, double latitude ){
        WeatherVO vo = weatherService.fetchWeather(longitude, latitude);
        return vo == null ?
                RestBean.failure(400,"获取地理位置与天气信息失败,请联系管理员!") : RestBean.success(vo);
    }

    @GetMapping("/types")
    public RestBean<List<TopicTypeVO>> types(){
        return RestBean.success(topicService
                .listTypes()
                .stream()
                .map(type -> type.asViewObject(TopicTypeVO.class))
                .toList());
    }

    @PostMapping("/create-topic")
    public RestBean<Void> createTopic(@Valid @RequestBody TopicCreateVO vo,
                                      @RequestAttribute(Const.ATTR_USER_ID) int id){
        if(accountService.findAccountById(id).isMute()){
            return RestBean.forbidden("帖子发表失败，该帐号已被禁用发言...");
        }
        return utils.messageHandle(()-> topicService.createTopic(id,vo));
    }

    @GetMapping("/list-topic")
    public RestBean<List<TopicPreviewVO>> listTopic(@RequestParam @Min(0) int page,
                                                     @RequestParam @Min(0) int type){
        return RestBean.success(topicService.listTopicByPage(page + 1,type));
    }

    @GetMapping("/top-topic")
    public RestBean<List<TopicTopVO>> topTopic(){
        return RestBean.success(topicService.listTopTopics());
    }

    @GetMapping("/topic")
    public RestBean<TopicDetailVO> topic(@RequestParam @Min(0) int tid,
                                         @RequestAttribute(Const.ATTR_USER_ID) int id){
        TopicDetailVO vo = topicService.getTopic(tid, id);
        if (vo != null) {
            return RestBean.success(vo);
        }
        return RestBean.failure(404, "帖子不存在或被屏蔽");
    }

    @GetMapping("/interact")
    public RestBean<Void>  interact(@RequestParam @Min(0) int tid,
                                    @RequestParam @Pattern(regexp = ("like|collect")) String type,
                                    @RequestParam boolean status,
                                    @RequestAttribute(Const.ATTR_USER_ID) int id){
        topicService.interact(new Interact(tid, id, new Date(), type), status);
        return RestBean.success();
    }

    @GetMapping("/collects")
    public RestBean<List<TopicPreviewVO>> collects(@RequestAttribute(Const.ATTR_USER_ID) int id){
        return RestBean.success(topicService.listTopicCollects(id));
    }

    @PostMapping("/update-topic")
    public RestBean<Void> updateTopic(@Valid @RequestBody TopicUpdateVO vo,
                                      @RequestAttribute(Const.ATTR_USER_ID) int id){
        return utils.messageHandle(() -> topicService.updateTopic(vo,id));
    }

    @PostMapping("/add-comment")
    public RestBean<Void> commentTopic(@Valid @RequestBody AddCommentVO vo,
                                       @RequestAttribute(Const.ATTR_USER_ID) int id){
        if(accountService.findAccountById(id).isMute()){
            return RestBean.forbidden("帖子发表失败，该帐号已被禁用发言...");
        }
        return utils.messageHandle(() -> topicService.createComment(vo,id));
    }

    @GetMapping("/comments")
    public RestBean<List<CommentVO>> comments(@RequestParam @Min(0) int tid,
                                              @RequestParam @Min(1) int page){
        return RestBean.success(topicService.listComments(tid,page));
    }

    @GetMapping("/delete-comment")
    public RestBean<Void> deleteComment(@RequestParam @Min(0) int id,
                                        @RequestAttribute(Const.ATTR_USER_ID) int uid){
        topicService.deleteComment(id,uid);
        return RestBean.success();
    }

    @GetMapping("/self-topics")
    public RestBean<List<UserTopicPreviewVO>> selfTopics(@RequestAttribute(Const.ATTR_USER_ID) int uid) {
        return RestBean.success(topicService.getUserTopic(uid));
    }

    @GetMapping("/delete-topic")
    public RestBean<Void> deleteTopic(@RequestParam int tid,
                                      @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        topicService.deleteUserTopic(tid, uid);
        return RestBean.success();
    }
}
