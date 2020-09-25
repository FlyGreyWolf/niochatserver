package com.flygreywolf.core;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HandleHeartPacket {

    private static Logger logger = Logger.getLogger(HandleHeartPacket.class);

    public static ConcurrentHashMap<SocketChannel, Long> socketChanelMap = new ConcurrentHashMap<>(); // 连接心跳包到达时间Map
    public static ConcurrentHashMap<Integer, ConcurrentHashMap<SocketChannel, Long>> roomId2SocketChannel = new ConcurrentHashMap<>(); // 在房间的心跳包，0x0002


    private static long MAX_TIMEOUT = 10000; // 最大超时时间，10秒



    /**
     * 测试连接的心跳包
     */
    public static void checkConnectHeartPacket() {
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

    /**
     * 测试房间内的心跳包
     */
    public static void checkRoomHeartPacket() {
        logger.info("Client In Room Checking thread is running");

        while (true) {
            System.out.println(roomId2SocketChannel);
            Set<Map.Entry<Integer, ConcurrentHashMap<SocketChannel, Long>>> set = roomId2SocketChannel.entrySet();
            try {
                Thread.sleep(10000); // 10s 检测一次

            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }

            for(Map.Entry<Integer, ConcurrentHashMap<SocketChannel, Long>> entry : set) {

                Integer roomId = entry.getKey();
                ConcurrentHashMap<SocketChannel, Long> socketChannelTimestamp = entry.getValue();

                for(SocketChannel socketChannel : socketChannelTimestamp.keySet()) {
                    if(System.currentTimeMillis() - socketChannelTimestamp.get(socketChannel) > MAX_TIMEOUT) { // 10s以上都没有收到该客户端在房间内的的心跳包了
                        socketChannelTimestamp.remove(socketChannel);
                    }
                }
            }


        }

    }

}
