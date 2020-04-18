package com.anderson.socket.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.websocket.Session;
import java.util.List;

/**
 * @author fengky
 */
@Data
@NoArgsConstructor
public class Player {
    private int index;
    private String name;
    @JsonIgnore
    private Session session;
    private List<Poker> poker;
    @JsonIgnore
    private boolean ready;

    public Player(String name, Session session) {
        this.name = name;
        this.session = session;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Poker {
        private int point;
        private int color;
    }
}
