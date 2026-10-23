package com.example.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.dto.EmailRecord;

public interface EmailService {
    void sendVerifyEmail(String type, String email,int code);
    Page<EmailRecord> listEmail(int page,int size);
    boolean resendEmail(int emailId);
}
