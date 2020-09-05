package com.flygreywolf.bean;

/**
 * @author FlyGreyWolf
 * @since 2020/9/1
 */
public class Room {
    private Integer roomId;
    private String roomName;
    private String roomMsg;


    public Room(Integer roomId, String roomName, String roomMsg) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomMsg = roomMsg;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomMsg() {
        return roomMsg;
    }

    public void setRoomMsg(String roomMsg) {
        this.roomMsg = roomMsg;
    }


}
