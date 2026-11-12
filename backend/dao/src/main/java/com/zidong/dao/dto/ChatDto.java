package com.zidong.dao.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatDto {

    @NotBlank(message = "Message cannot be blank")
    private String message;


    private String taskId;
}
