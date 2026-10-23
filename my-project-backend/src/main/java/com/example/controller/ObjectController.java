package com.example.controller;

import com.example.entity.RestBean;
import com.example.service.ImageService;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ObjectController {
    @Resource
    ImageService service;

    @GetMapping("/images/**")
    public void imageFetch(HttpServletRequest request,
                           HttpServletResponse response) throws Exception {
        this.fetchImage(request, response);
    }

    private void fetchImage(HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        String imagePath = request.getServletPath().substring(7);
        ServletOutputStream outputStream = response.getOutputStream();
        if(imagePath.length() <= 13){
            response.setStatus(404);
            outputStream.println(RestBean.failure(404,"NOT FOUND").toString());
        }else{
            try{
                service.fetchImageFromMinio(outputStream,imagePath);
                response.setHeader("Cache-Control","max-age=2592000");
                response.setHeader("Content-Type","image/jpg");
            }catch (ErrorResponseException e){
                if(e.errorResponse().code().equals("404")){
                    log.error("Minion桶里没此图片的错误码：{}", e.errorResponse().toString());
                    response.setStatus(404);
                }else{
                    log.error("从Minion中获取图片异常:{}", e.getMessage(), e);
                }
            }

        }
    }
}
