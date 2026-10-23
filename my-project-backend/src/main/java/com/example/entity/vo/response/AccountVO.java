package com.example.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AccountVO {
    private int id;
    private String username;
    private String email;
    private String role;
    private String avatar;
    private Date registerTime;
    private boolean mute;
    private boolean banned;
}
