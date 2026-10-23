package com.example.listener;

import com.example.entity.dto.EmailRecord;
import com.example.mapper.EmailRecordMapper;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RabbitListener(queues = Const.MQ_ERROR)
public class ErrorQueueListener {

    @Resource
    EmailRecordMapper mapper;

    @RabbitHandler
    public void saveErrorToDataBase(EmailRecord emailRecord){
        log.error("有一条发送失败的邮件，邮件信息：{}",emailRecord);
        emailRecord.setStatus(2);
        mapper.updateById(emailRecord);
    }
}
