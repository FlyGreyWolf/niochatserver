package com.flygreywolf.util;

/**
 * 常量
 *
 * @author FlyGreyWolf
 * @since 2020/8/30
 */
public class Constant {

    public static String UTF8_Encode = "UTF-8";
    public static int MAX_CONTENT_LEN = 1500; // 消息内容最大长度是1500个字节


    /**
     * 指令
     */
    public static Short ROOM_LIST_CMD = 0X0001; // 表示携带的数据是房间数据
    public static Short ENTER_ROOM_CMD = 0X0002; // 表示在房间当中的心跳包
    public static Short NUM_OF_PEOPLE_IN_ROOM_CMD = 0X0003; // 表示在房间内的人数指令
    public static Short SEND_MSG_CMD = 0X0004; // 发送消息的指令

}
