package com.example.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;
@TableName("db_email_record")
@Accessors(chain = true)
@NoArgsConstructor
@Data
public class EmailRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String email;
    private String title;
    private String content;
    private Date time;
    //表示该邮件的当前状态，0表示正在发送，1表示已发送，2表示发送失败
    private Integer status;

    public EmailRecord(String title,String content,String email){
        this.title=title;
        this.content=content;
        this.email=email;
        this.status=0;
        this.time=new Date();
    }
}
