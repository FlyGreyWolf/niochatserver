package com.flygreywolf.msg;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.flygreywolf.bean.Chat;
import com.flygreywolf.bean.Msg;
import com.flygreywolf.bean.Room;
import com.flygreywolf.core.HandleHeartPacket;
import com.flygreywolf.core.NioServer;
import com.flygreywolf.util.Constant;
import com.flygreywolf.util.Convert;
import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author FlyGreyWolf
 * @since 2020/9/4
 */
public class HandlePayLoad {

    public static boolean isConnectHeartPacket(PayLoad payLoad) {
        if(payLoad.getContent().length == 0) { // 是连接心跳包，只有头部的4字节，无内容的空包
            return true;
        }
        return false;
    }

    public static void parse(PayLoad payLoad, SocketChannel socketChannel) {

        if(payLoad.getContent().length == 0) { // 是连接心跳包，只有头部的4字节，无内容的空包
            HandleHeartPacket.socketChanelMap.put(socketChannel, System.currentTimeMillis());
            return;
        }


        short cmd = Convert.byteArrToShort(payLoad.getContent()); // 0和1个字节代表cmd
        String msg = null;
        try {
            msg = new String(payLoad.getContent(), 2, payLoad.getContent().length-2, Constant.UTF8_Encode);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        System.out.println("cmd:" + cmd);
        System.out.println("msg:" + msg);

        if(cmd == Constant.ENTER_ROOM_CMD) { // 是在房间的心跳包
            // msg 是 roomId
            HandleHeartPacket.roomId2SocketChannel.get(Integer.parseInt(msg)).put(socketChannel, System.currentTimeMillis());

            NioServer.send( // 返回房间人数
                    Convert.shortToBytes(Constant.NUM_OF_PEOPLE_IN_ROOM_CMD),
                    HandleHeartPacket.roomId2SocketChannel.get(Integer.parseInt(msg)).size()+"",
                    socketChannel);

        } else if(cmd == Constant.SEND_MSG_CMD) {
            // msg 是 Chat对象的json字符串格式
            Msg msgObj = JSON.parseObject(msg, Msg.class);

            NioServer.send( // 本人发送消息成功了
                Convert.shortToBytes(Constant.SEND_MSG_CMD),
                    msg,
                    socketChannel);


            ConcurrentHashMap<SocketChannel, Long> socketChannel2timestamp =  HandleHeartPacket.roomId2SocketChannel.get(msgObj.getRoomId());

            Set<SocketChannel> socketChannelSet = socketChannel2timestamp.keySet();

            if (msgObj.getMsgType() == Constant.MY_TEXT_TYPE) {
                Chat chat = JSON.parseObject(msg, Chat.class);
                Msg msg2Other = new Chat(chat.getRoomId(), chat.getMsgId(), Constant.OTHER_TEXT_TYPE, chat.getContent());
                String msg2OtherStr = JSON.toJSONString(msg2Other);
                for(SocketChannel sc : socketChannelSet) {
                    if(sc != socketChannel) {

                        NioServer.send(Convert.shortToBytes(Constant.SEND_MSG_CMD), msg2OtherStr, sc);
                    }
                }
            }




        }
    }
}
