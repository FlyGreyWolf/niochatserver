package com.flygreywolf.core;

import com.flygreywolf.msg.PayLoad;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HandleHeartPacket {

    private static Logger logger = Logger.getLogger(HandleHeartPacket.class);

    private static ConcurrentHashMap<SocketChannel, Long> socketChanelMap = new ConcurrentHashMap<>(); // 心跳包到达时间Map
    private static long MAX_TIMEOUT = 10000; // 最大超时时间，10秒


    public static boolean isHeartPacket(PayLoad payLoad) {
        if(payLoad.getContent().length == 0) { // 是心跳包，只有头部的4字节，无内容的空包
            return true;
        }
        return false;
    }

    public static void putSocketChannel(SocketChannel socketChannel) {
        socketChanelMap.put(socketChannel, System.currentTimeMillis());
    }

    public static void checkHeartPacket() {
        logger.info("Client Online Checking thread is running");
        while (true) {
            //System.out.println(socketChanelMap);
            Set<Map.Entry<SocketChannel, Long>> set = socketChanelMap.entrySet();
            try {
                Thread.sleep(10000); // 10s 检测一次

            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }

            for(Map.Entry<SocketChannel, Long> entry : set) {
                SocketChannel socketChannel = entry.getKey();
                Long lastReceiveHeartTime = entry.getValue();

                if(System.currentTimeMillis() - lastReceiveHeartTime > MAX_TIMEOUT) { // 10s以上都没有收到该客户端的心跳包了
                    try {
                        socketChannel.close(); // 释放资源
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    }
                    socketChanelMap.remove(socketChannel); // 从表中删除该通道
                }

            }

        }
    }

}
