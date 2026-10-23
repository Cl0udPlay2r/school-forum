package com.example.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Interact;
import com.example.entity.dto.Topic;
import com.example.entity.dto.TopicType;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.*;

import java.util.List;

public interface TopicService extends IService<Topic> {
    List<TopicType> listTypes();
    String createTopic(int uid, TopicCreateVO vo);
    List<TopicPreviewVO> listTopicByPage(int page,int type);
    JSONObject listTopicAdmin(int page, int size, String keyword);
    List<TopicTopVO> listTopTopics();

    List<UserTopicPreviewVO> getUserTopic(int uid);
    TopicDetailVO getTopic(int tid,int uid);
    void interact(Interact interact,boolean status);
    List<TopicPreviewVO> listTopicCollects(int uid);
    String updateTopic(TopicUpdateVO vo, int uid);
    String createComment(AddCommentVO vo,int uid);
    List<CommentVO> listComments(int tid,int pageNumber);
    void deleteComment(int id,int uid);
    void deleteTopic(int tid);

    void deleteUserTopic(int tid, int uid);
    void setTop(int tid, boolean status);
    void lockTopic(int tid, boolean status);
    void blockTopic(int tid, boolean status);
}
