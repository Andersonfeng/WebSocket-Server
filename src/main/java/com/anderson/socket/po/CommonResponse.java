package com.anderson.socket.po;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author fengky
 */
@Data
@AllArgsConstructor
public class CommonResponse {
    private int code;
    private String message;
    private Object data;

    public static CommonResponse buildSuccess(Object data) {
        return new CommonResponse(200, "success", data);
    }

    public static CommonResponse buildSuccess(int code, Object data) {
        return new CommonResponse(code, "success", data);
    }

    public static CommonResponse buildFail(int code, String message) {
        return new CommonResponse(code, message, null);
    }
}
