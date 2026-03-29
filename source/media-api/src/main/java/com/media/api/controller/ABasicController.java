package com.media.api.controller;

import com.media.api.dto.ApiMessageDto;
import com.media.api.dto.ResponseListDto;
import com.media.api.jwt.BaseJwt;
import com.media.api.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public class ABasicController {
    @Autowired
    private UserServiceImpl userService;

    public BaseJwt getSessionFromToken() {
        return userService.getAddInfoFromToken();
    }

    public <T> ApiMessageDto<T> makeResponse(Boolean result, T data, String message, String code) {
        ApiMessageDto<T> apiMessageDto = new ApiMessageDto<>();
        apiMessageDto.setResult(result);
        apiMessageDto.setData(data);
        apiMessageDto.setMessage(message);
        apiMessageDto.setCode(code);
        return apiMessageDto;
    }

    public <T> ApiMessageDto<T> makeSuccessResponse(String message) {
        return makeResponse(true, null, message, null);
    }

    public <T> ApiMessageDto<T> makeSuccessResponse(T data, String message) {
        return makeResponse(true, data, message, null);
    }

    public <T, R> ResponseListDto<R> makeResponseListDto(Page<T> page, Function<List<T>, R> mapper) {
        return new ResponseListDto<>(
                mapper.apply(page.getContent()),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
