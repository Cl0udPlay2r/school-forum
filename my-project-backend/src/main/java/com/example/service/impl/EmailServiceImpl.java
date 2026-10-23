package com.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.dto.EmailRecord;
import com.example.mapper.EmailRecordMapper;
import com.example.service.EmailService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Resource
    RabbitTemplate rabbitTemplate;

    @Resource
    EmailRecordMapper mapper;

    @Override
    public void sendVerifyEmail(String type, String email, int code) {
        EmailRecord emailRecord = switch (type) {
            case "register" -> new EmailRecord("欢迎注册我们的网站",
                    "您的邮件注册验证码为: "+code+"，有效时间3分钟，为了保障您的账户安全，请勿向他人泄露验证码信息。",
                    email);

            case "reset" -> new EmailRecord("您的密码重置邮件",
                    "你好，您正在执行重置密码操作，验证码: "+code+"，有效时间3分钟，如非本人操作，请无视。",
                    email);

            case "modify" -> new EmailRecord("您的邮件修改验证邮件",
                    "您好，您正在执行修改邮件操作，验证码："+ code + "，有效时间3分钟，如非本人操作，请无视。",
                    email);

            default -> throw new IllegalArgumentException(type);
        };
        mapper.insert(emailRecord);
        rabbitTemplate.convertAndSend(Const.MQ_MAIL, emailRecord);
    }

    @Override
    public Page<EmailRecord> listEmail(int page,int size) {
        return mapper.selectPage(Page.of(page,size),Wrappers.emptyWrapper());
    }

    @Override
    public boolean resendEmail(int emailId) {
        EmailRecord emailRecord = mapper.selectById(emailId);
        if(emailRecord==null)return false;
        emailRecord.setStatus(0);
        rabbitTemplate.convertAndSend(Const.MQ_MAIL, emailRecord);
        mapper.updateById(emailRecord);
        return true;
    }
}
