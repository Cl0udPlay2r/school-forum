package com.example.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class Interact {
    Integer tid;
    Integer uid;
    Date time;
    String type;

    public String toKey(){
        return tid + ":" + uid;
    }

    public static Interact parseInteract(String str,String type){
        String[] split = str.split(":");
        return new Interact(Integer.parseInt(split[0]),Integer.parseInt( split[1]),new Date(),type);
    }
}
