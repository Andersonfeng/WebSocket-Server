package com.anderson.socket.po;

import lombok.Data;

import java.util.List;

/**
 * @author fengky
 */
@Data
public class RoomDTO extends Room {
    private int playerCount;
    private List<String> playerNameList;
}
