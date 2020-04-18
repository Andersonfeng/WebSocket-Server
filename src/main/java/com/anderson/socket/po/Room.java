package com.anderson.socket.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author fengky
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Room {
    private String name;
    private int roomNumber;
    @JsonIgnore
    private List<Player> playerList;
}
