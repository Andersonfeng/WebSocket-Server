package com.anderson.socket.endpoint;

import com.anderson.socket.enums.ResponseCodeEnum;
import com.anderson.socket.po.CommonResponse;
import com.anderson.socket.po.Player;
import com.anderson.socket.po.Room;
import com.anderson.socket.po.RoomDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * @author fengky
 */
@ServerEndpoint("/webSocket/room/{playerName}")
@Component
@Slf4j
public class RoomEndpoint {
    private static final Map<String, Room> roomMap = new ConcurrentHashMap<>();
    public static final String LIST = "LIST";
    public static final String CREATE = "CREATE";
    public static final String JOIN = "JOIN";
    public static final String EXIT = "EXIT";
    public static final String READY = "READY";
    public static final String CANCEL_READY = "CANCEL_READY";
    public static final String SET_HANDCARD = "SET_HANDCARD";
    //    public static final String GET_HANDCARD = "GET_HANDCARD";
    public static final String START_GAME = "START_GAME";
    public static final String GET_TURN = "GET_TURN";
    public static final String PLAY_CARD = "PLAY_CARD";
    public static final String PASS = "PASS";
    public static final String CLEAN_DROP_ZONE = "CLEAN_DROP_ZONE";
    public static final String WIN = "WIN";

    /**
     * 大厅玩家 进房间后remove
     */
    private static Map<Session, String> playerInHall = new ConcurrentHashMap<>();
    private static List<String> messageList = new ArrayList<>();

    private ObjectMapper objectMapper = new ObjectMapper();


    @OnOpen
    public void onOpen(@PathParam("playerName") String playerName,
                       Session session) throws IOException {

        log.info("{}连入大厅", playerName);
        if (playerInHall.values().contains(playerName) ||
                roomMap.values().stream().map(room -> room.getPlayerList()).map(players -> playerName)
                        .collect(Collectors.toList()).equals(playerName)) {
            sendMessage(session, CommonResponse.buildFail(ResponseCodeEnum.已存在此用户名.code, ResponseCodeEnum.已存在此用户名.message));
//            session.close();
            return;
        }
        playerInHall.put(session, playerName);
    }

    @OnMessage
    public void onMessage(@PathParam("playerName") String playerName,
                          String message,
                          Session session) throws JsonProcessingException {

        log.info("onMessage:{}   plyaerName:{}", message, playerName);
        CommonResponse commonResponse;

        if (LIST.equals(message.toUpperCase())) {
            commonResponse = CommonResponse.buildSuccess(listRoom());
            sendMessage(session, commonResponse);
            return;
        }

        if (message.toUpperCase().contains(CREATE)) {
            String roomName = message.split(":")[1];

            if (StringUtils.isEmpty(roomName))
                return;
            createRoom(roomName, playerName, session);
            sendMessageToAll(CommonResponse.buildSuccess(listRoom()));
            return;
        }

        if (message.toUpperCase().contains(JOIN)) {
            String roomName = message.split(":")[1];
            if (joinRoom(roomName, playerName, session)) {
                //通知大厅玩家房间信息有变化
                sendMessageToAll(CommonResponse.buildSuccess(listRoom()));
                sendMessageToRoom(roomName, CommonResponse.buildSuccess(ResponseCodeEnum.ROOM.code, getRoom(roomName)));
            }
            return;
        }

        if (message.toUpperCase().contains(EXIT)) {
            String roomName = message.split(":")[1];
            if (exitRoom(roomName, playerName, session)) {
                //通知大厅玩家房间信息有变化
                sendMessageToAll(CommonResponse.buildSuccess(listRoom()));
                sendMessageToRoom(roomName, CommonResponse.buildSuccess(ResponseCodeEnum.ROOM.code, getRoom(roomName)));
                removeEmptyRoom(roomName);
            }
            return;
        }

        if (message.toUpperCase().contains(READY)) {
            String roomName = message.split(":")[1];
            Room room = roomMap.get(roomName);
            List<Player> playerList = room.getPlayerList();
            for (Player player : playerList) {
                player.setReady(Boolean.TRUE);
            }
            sendMessageToRoom(roomName, CommonResponse.buildSuccess(ResponseCodeEnum.ROOM.code, getRoom(roomName)));
            return;
        }

        if (message.toUpperCase().contains(CANCEL_READY)) {
            String roomName = message.split(":")[1];
            Room room = roomMap.get(roomName);
            List<Player> playerList = room.getPlayerList();
            for (Player player : playerList) {
                player.setReady(Boolean.FALSE);
            }
            sendMessageToRoom(roomName, CommonResponse.buildSuccess(ResponseCodeEnum.ROOM.code, getRoom(roomName)));
            return;
        }

        if (message.toUpperCase().contains(SET_HANDCARD)) {
            String roomName = message.split(":")[1];
            int index = Integer.parseInt(message.split(":")[2]);
            String pokerListJson = message.substring(message.indexOf("["));
            var pokersList = objectMapper.readValue(pokerListJson, new TypeReference<List<Player.Poker>>() {
            });
            Room room = roomMap.get(roomName);
            List<Player> playerList = room.getPlayerList();
            if (playerList.size() < index + 1)
                return;
            playerList.get(index).setPoker(pokersList);
            log.info("index:{}  handCardJson:{}", index, pokersList);
            return;
        }

        if (message.toUpperCase().contains(START_GAME)) {
            String roomName = message.split(":")[1];
            Room room = roomMap.get(roomName);
            List<Player> playerList = room.getPlayerList();
            for (int i = 0; i < playerList.size(); i++) {
                sendMessage(playerList.get(i).getSession(), CommonResponse.buildSuccess(ResponseCodeEnum.HAND_CARD.code, playerList.get(i).getPoker()));
            }
            sendMessageToRoom(roomName, CommonResponse.buildSuccess(ResponseCodeEnum.PLAYER_LIST.code, playerList));
        }

        if (message.toUpperCase().contains(PASS)) {
            String roomName = message.split(":")[1];
            int index = Integer.parseInt(message.split(":")[2]);
            sendMessageToRoom(roomName, CommonResponse.buildSuccess(ResponseCodeEnum.PASS.code, index));
        }


        if (message.toUpperCase().contains(CLEAN_DROP_ZONE)) {//通知玩家清空出牌区
            String roomName = message.split(":")[1];
            sendMessageToRoom(roomName, CommonResponse.buildSuccess(ResponseCodeEnum.CLEAN_DROP_ZONE.code, ""));
        }

        if (message.toUpperCase().contains(PLAY_CARD)) {
            String roomName = message.split(":")[1];
            int index = Integer.parseInt(message.split(":")[2]);
            Room room = roomMap.get(roomName);
            List<Player> playerList = room.getPlayerList();
            String pokerListJson = message.substring(message.indexOf("["));
            var pokersList = objectMapper.readValue(pokerListJson, new TypeReference<List<Player.Poker>>() {
            });

            //出掉玩家手中的牌
            playerList.get(index).getPoker().removeIf(poker -> pokersList.contains(poker));
            //告知房间玩家 打出了什么牌
            Player player = new Player();
            player.setIndex(index);
            player.setPoker(pokersList);
            sendMessageToRoom(roomName, CommonResponse.buildSuccess(ResponseCodeEnum.PLAY_CARD.code, player));

            //告知房间玩家 轮到谁出牌
            int nextIndex = (index + 1) % playerList.size();
            sendMessageToRoom(roomName, CommonResponse.buildSuccess(ResponseCodeEnum.NEXT_TURN.code, nextIndex));

            //刷新房间玩家手牌等信息
            sendMessageToRoom(roomName, CommonResponse.buildSuccess(ResponseCodeEnum.PLAYER_LIST.code, playerList));

            //玩家打出牌
            //更新所有玩家出牌堆
            //轮到下一玩家出牌

            return;
        }

        if (message.toUpperCase().contains(WIN)) {
            String roomName = message.split(":")[1];
            int username = Integer.parseInt(message.split(":")[2]);
            sendMessageToRoom(roomName, CommonResponse.buildSuccess(ResponseCodeEnum.WIN.code, "WINNER IS " + username));
        }
    }

    /**
     * 创建房间并加入
     *
     * @param roomName
     * @param playerName
     * @param session
     */
    private boolean createRoom(String roomName, String playerName, Session session) throws JsonProcessingException {
        if (roomMap.containsKey(roomName)) {
            sendMessage(session, CommonResponse.buildFail(ResponseCodeEnum.此房间名已存在.code, ResponseCodeEnum.此房间名已存在.message));
            return Boolean.FALSE;
        }

        Room room = new Room(roomName, roomMap.size(), new ArrayList<Player>() {
            {
                add(new Player(playerName, session));
            }
        });

        roomMap.put(roomName, room);
        playerInHall.remove(session);
        sendMessage(session, CommonResponse.buildSuccess(ResponseCodeEnum.ROOM.code, getRoom(roomName)));

        return Boolean.TRUE;
    }


    /**
     * 玩家离开房间
     *
     * @param roomName
     * @param playerName
     * @param session
     * @return
     */
    private boolean exitRoom(String roomName, String playerName, Session session) throws JsonProcessingException {
        Room room = roomMap.get(roomName);
        if (Objects.isNull(room)) {
            sendMessage(session, CommonResponse.buildFail(406, "没有此房间"));
            return Boolean.FALSE;
        }
        List<Player> playerList = room.getPlayerList();
        boolean flag;
        //玩家离开房间
        flag = playerList.removeIf(player -> player.getName().equals(playerName));
        //玩家加入大厅
        if (flag)
            playerInHall.put(session, playerName);
        else
            sendMessage(session, CommonResponse.buildFail(ResponseCodeEnum.你不在此房间中.code, ResponseCodeEnum.你不在此房间中.message));
        return flag;
    }

    /**
     * 加入房间
     * session 从clientInHall中remove
     *
     * @param roomName
     * @param playerName
     * @param session
     * @return
     */
    private boolean joinRoom(String roomName,
                             String playerName,
                             Session session) throws JsonProcessingException {
        if (!roomMap.containsKey(roomName)) {
            sendMessage(session, CommonResponse.buildFail(ResponseCodeEnum.没有此房间.code, ResponseCodeEnum.没有此房间.message));
            return Boolean.FALSE;
        }

        Room room = roomMap.get(roomName);
        List<Player> playerList = room.getPlayerList();

        if (playerList.size() >= 4) {
            sendMessage(session, CommonResponse.buildFail(ResponseCodeEnum.满人了.code, ResponseCodeEnum.满人了.message));
            return Boolean.FALSE;
        }


        if (playerList.stream().map(Player::getName).collect(Collectors.toList()).contains(playerName)) {
            sendMessage(session, CommonResponse.buildFail(ResponseCodeEnum.房间中已存在该角色名.code, ResponseCodeEnum.房间中已存在该角色名.message));
            return Boolean.FALSE;
        }

        playerList.add(new Player(playerName, session));
        room.setPlayerList(playerList);

        //玩家从大厅进入房间
        playerInHall.remove(session);

        //通知房间其他人 有人加入了房间
        return Boolean.TRUE;
    }

    /**
     * 获取房间列表
     *
     * @return
     * @throws JsonProcessingException
     */
    private List listRoom() {
        List<RoomDTO> roomDTOList = roomMap.values().stream()
                .map(this::coverRoomToRoomDTO)
                .collect(Collectors.toList());
        return roomDTOList;
    }

    /**
     * ROOM -> RoomDTO
     *
     * @param room
     * @return
     */
    private RoomDTO coverRoomToRoomDTO(Room room) {
        RoomDTO roomDTO = new RoomDTO();
        BeanUtils.copyProperties(room, roomDTO);
        List<Player> playerList = room.getPlayerList();
        roomDTO.setPlayerCount(playerList.size());
        roomDTO.setPlayerNameList(playerList.stream().map(Player::getName).collect(Collectors.toList()));
        return roomDTO;
    }

    /**
     * 获取房间信息
     *
     * @param roomName
     * @return
     */
    private RoomDTO getRoom(String roomName) {
        Room room = roomMap.get(roomName);
        return coverRoomToRoomDTO(room);
    }

    @OnClose
    public void onClose(@PathParam("playerName") String playerName,
                        Session session) throws JsonProcessingException {
//        RemovePlayer(roomName, playerName);
        log.info("onClose {}断开了连接", playerName);

        if (playerInHall.containsKey(session)) {
            playerInHall.remove(session);
            return;
        }

        Collection<Room> roomList = roomMap.values();
        String roomName = null;
        for (Room room : roomList) {
            for (Player player : room.getPlayerList()) {
                if (playerName.equals(player.getName()))
                    roomName = room.getName();
            }
        }

        if (!StringUtils.isEmpty(roomName)) {
            Room room = roomMap.get(roomName);
            List<Player> playerList = room.getPlayerList();
            playerList.removeIf(player -> player.getName().equals(playerName));
            sendMessageToRoom(roomName, CommonResponse.buildSuccess(ResponseCodeEnum.ROOM.code, getRoom(roomName)));
            sendMessageToAll(CommonResponse.buildSuccess(ResponseCodeEnum.ROOM.code, listRoom()));
        }


        removeEmptyRoom(roomName);

    }

    /**
     * 将玩家移除房间
     *
     * @param roomName
     * @param playerName
     */
    private void RemovePlayer(String roomName, String playerName) {
        List<Player> playerList = roomMap.get(roomName).getPlayerList();
        if (!playerList.isEmpty())
            playerList.removeIf(player -> player.getName().equals(playerName));
    }

    @OnError
    public void onError(Throwable error, Session session) {
        log.error("onError {}", error.toString());
        playerInHall.remove(session);
//        error.printStackTrace();
    }

    /**
     * 向所有大厅用户发送消息
     *
     * @param commonResponse
     */
    private void sendMessageToAll(CommonResponse commonResponse) throws JsonProcessingException {

        String json = objectMapper.writeValueAsString(commonResponse);
        log.info("sendMessageToAll:{}", json);
        for (Session session : playerInHall.keySet()) {
            try {
                session.getBasicRemote().sendText(json);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 向单位用户发送消息
     *
     * @param session
     * @param commonResponse
     */
    private void sendMessage(Session session, CommonResponse commonResponse) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(commonResponse);
        log.info("sendMessage:{}", json);
        session.getAsyncRemote().sendText(json);
    }


    /**
     * 向房间内玩家发送消息
     *
     * @param roomName
     * @param commonResponse
     */
    private void sendMessageToRoom(String roomName, CommonResponse commonResponse) throws JsonProcessingException {
        Room room = roomMap.get(roomName);
        String message = objectMapper.writeValueAsString(commonResponse);
        log.info("sendMessageToRoom:{}", message);
        for (Player player : room.getPlayerList()) {
            Session session = player.getSession();
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }


    /**
     * 检查空房间并关闭
     */
    private void removeEmptyRoom(String roomName) {
        Room room = roomMap.get(roomName);
        List<Player> playerList = room.getPlayerList();
        if (playerList.isEmpty())
            roomMap.remove(roomName);
    }
}
