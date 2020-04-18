package com.anderson.socket;

import com.anderson.socket.endpoint.WebSocketServer;
import com.anderson.socket.enums.ResponseCodeEnum;
import com.anderson.socket.po.CommonResponse;
import com.anderson.socket.po.Player;
import com.anderson.socket.po.Room;
import com.anderson.socket.po.RoomDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author fengky
 */


public class WebSocketTEst {

    @Autowired
    WebSocketServer webSocketServer;

    ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, Room> roomMap = new ConcurrentHashMap<>();

    @Test
    public void testRemoveCardFromPlayer() throws JsonProcessingException {
        List<Player.Poker> pokerList1 = new ArrayList<>() {{
            add(new Player.Poker(7,2));
            add(new Player.Poker(3,4));
            add(new Player.Poker(3,2));
        }};
        String pokerListJson = "[{\"point\":7,\"color\":2},{\"point\":3,\"color\":4}]";
        var pokerList2 = objectMapper.readValue(pokerListJson, new TypeReference<List<Player.Poker>>() {
        });
        System.out.println(pokerList1);
        pokerList1.removeIf(poker -> pokerList2.contains(poker));
        System.out.println(pokerList1);
    }

    @Test
    public void test() throws JsonProcessingException {

        String message = "SET_HANDCARD:Hall:0:[{\"point\":15,\"color\":1},{\"point\":14,\"color\":3},{\"point\":4,\"color\":3},{\"point\":14,\"color\":4},{\"point\":5,\"color\":2},{\"point\":11,\"color\":2},{\"point\":12,\"color\":2},{\"point\":14,\"color\":2},{\"point\":10,\"color\":2},{\"point\":7,\"color\":2},{\"point\":6,\"color\":2},{\"point\":6,\"color\":1},{\"point\":7,\"color\":4}]";
        String roomName = message.split(":")[1];
        int index = Integer.parseInt(message.split(":")[2]);
        String pokerListJson = message.substring(message.indexOf("["));
        var pokerList = objectMapper.readValue(pokerListJson, new TypeReference<List<Player.Poker>>() {
        });

        String s = objectMapper.writeValueAsString(CommonResponse.buildSuccess(ResponseCodeEnum.ROOM.code, pokerList));
        System.out.println(s);
    }

    @Test
    public void testRoomJson() throws JsonProcessingException {
        Room room1 = new Room("roomName", 1, new ArrayList<>());
        Room room2 = new Room("roomName", 2, new ArrayList<>());
        roomMap.put("12431234", room1);
        roomMap.put("12431123234", room2);
        List<Room> collect = roomMap.values().stream().collect(Collectors.toList());

        String s = objectMapper.writeValueAsString(collect);
        List<RoomDTO> roomDTOList = roomMap.values().stream()
                .map(room -> {
                    RoomDTO roomDTO = new RoomDTO();
                    BeanUtils.copyProperties(room, roomDTO);
                    roomDTO.setPlayerCount(room.getPlayerList().size());
                    return roomDTO;
                }).collect(Collectors.toList());


        System.out.println(objectMapper.writeValueAsString(CommonResponse.buildSuccess(roomDTOList)));
    }

    @Test
    public void testRoomName() {
        var varString = "varString";

        System.out.println(varString);
    }

    @Test
    public void jdk9_Feature() {

//        IntStream.iterate(2, i -> i < 1000000, i -> i + 1)
//                .dropWhile(i -> i % 2 == 0)
//                .forEach(System.out::println);
//
        IntStream.iterate(2, i -> i < 1000000, i -> i + 1)
                .takeWhile(i -> i % 2 == 0)
                .forEach(System.out::println);

        List<People> list = new ArrayList<>() {{
            add(new People());
            add(null);
        }};
        long count = Stream.ofNullable(null)
                .count();

        System.out.println(count);

        Map.of("小明", "男", "韩梅梅", "女");

//        IntStream.iterate(2, i -> i < 1000000, i -> i + 1)
//                .filter(i -> i % 2 == 0)
//                .forEach(System.out::println);//1s823ms


    }

    @Data
    class People {
        private String name;
    }
}
