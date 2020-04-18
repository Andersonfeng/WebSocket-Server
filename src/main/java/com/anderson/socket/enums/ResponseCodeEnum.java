package com.anderson.socket.enums;

/**
 * @author fengky
 */
public enum ResponseCodeEnum {
    Success(200, ""),
    RoomList(201, ""),
    ROOM(202, ""),
    HAND_CARD(203, ""),
    PLAY_CARD(204, ""),
    NEXT_TURN(205, ""),
    CLEAN_DROP_ZONE(206, ""),
    PASS(207, ""),
    PLAYER_LIST(208, ""),
    WIN(209, ""),


    满人了(401, "满人了"),
    房间中已存在该角色名(402, "房间中已存在该角色名"),
    你不在此房间中(403, "你不在此房间中"),
    已存在此用户名(404, "已存在此用户名"),
    此房间名已存在(405, "已存在此用户名"),
    没有此房间(406, "没有此房间"),
    ;
    public int code;
    public String message;

    ResponseCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }


}
