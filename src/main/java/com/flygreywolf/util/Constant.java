package com.flygreywolf.util;

import com.flygreywolf.bean.Image;
import com.flygreywolf.bean.Msg;
import com.flygreywolf.bean.RedPacket;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 常量
 *
 * @author FlyGreyWolf
 * @since 2020/8/30
 */
public class Constant {

    public static String UTF8_Encode = "UTF-8";
    public static int MAX_CONTENT_LEN = Integer.MAX_VALUE; // 消息内容最大长度是1500个字节


    /**
     * 指令
     */
    public static Short ROOM_LIST_CMD = 0X0001; // 表示携带的数据是房间数据
    public static Short ENTER_ROOM_CMD = 0X0002; // 表示在房间当中的心跳包
    public static Short NUM_OF_PEOPLE_IN_ROOM_CMD = 0X0003; // 表示在房间内的人数指令
    public static Short SEND_CHAT_CMD = 0X0004; // 发送消息的指令
    public static Short SNED_RED_PACKET_CMD = 0X0005; // 发送红包的指令
    public static Short GET_RED_PACKET_CMD = 0X0006; // 抢红包的指令
    public static Short SEND_IMG_CMD = 0X0007;
    public static Short GET_BIG_IMG_CMD = 0X0008; // 看大图


    /**
     * 消息类型
     */
    public final static int MY_TEXT_TYPE = 1;
    public final static int OTHER_TEXT_TYPE = 2;
    public final static int MY_PACKET_TYPE = 3;
    public final static int OTHER_PACKET_TYPE = 4;
    public final static int MY_IMG_TYPE = 5;
    public final static int OTHER_IMG_TYPE = 6;



    /**
     * 数据结构
     */
    public static ConcurrentHashMap<Integer, Msg> id2Msg = new ConcurrentHashMap<>(); // msgId --> msgObj
    public static HashSet<String> imgNameSet = new HashSet<>();


}
