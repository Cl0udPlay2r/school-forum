package com.example.controller.admin;

import com.example.entity.PageRestBean;
import com.example.entity.RestBean;
import com.example.entity.dto.EmailRecord;
import com.example.service.EmailService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/email")
public class EmailAdminController {

    @Resource
    private EmailService service;

    @GetMapping("/list")
    public PageRestBean<EmailRecord> listEmailRecord(@RequestParam Integer page,
                                                     @RequestParam Integer size) {
        return PageRestBean.success(service.listEmail(page,size));
    }

    @GetMapping("/resend")
    public RestBean<Void> resendEmail(@RequestParam int emailId) {
        return service.resendEmail(emailId) ? RestBean.success() :
                RestBean.failure(400,"邮件重发失败");
    }
}
