package com.flygreywolf.msg;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.flygreywolf.bean.*;
import com.flygreywolf.core.HandleHeartPacket;
import com.flygreywolf.core.NioServer;
import com.flygreywolf.util.Constant;
import com.flygreywolf.util.Convert;
import com.flygreywolf.util.ImgUtil;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author FlyGreyWolf
 * @since 2020/9/4
 */
public class HandlePayLoad {

    private static Logger logger= Logger.getLogger(HandlePayLoad.class);

    private static volatile AtomicInteger msgId = new AtomicInteger(0);

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
        //System.out.println("msg:" + msg);

        if(cmd == Constant.ENTER_ROOM_CMD) { // 是在房间的心跳包
            // msg 是 roomId
            HandleHeartPacket.roomId2SocketChannel.get(Integer.parseInt(msg)).put(socketChannel, System.currentTimeMillis());


            NioServer.send( // 返回房间人数
                    Convert.shortToBytes(Constant.NUM_OF_PEOPLE_IN_ROOM_CMD),
                    HandleHeartPacket.roomId2SocketChannel.get(Integer.parseInt(msg)).size()+"",
                    socketChannel);

        } else if(cmd == Constant.SEND_CHAT_CMD) { // 用户发送文字消息
            // msg 是 Chat对象的json字符串格式
            Msg msgObj = JSON.parseObject(msg, Msg.class);

            ConcurrentHashMap<SocketChannel, Long> socketChannel2timestamp =  HandleHeartPacket.roomId2SocketChannel.get(msgObj.getRoomId());

            Set<SocketChannel> socketChannelSet = socketChannel2timestamp.keySet();

            if (msgObj.getMsgType() == Constant.MY_TEXT_TYPE) {
                Chat chat = JSON.parseObject(msg, Chat.class);
                Integer nowMsgId = msgId.addAndGet(1);
                Msg msg2Other = new Chat(chat.getRoomId(), nowMsgId, Constant.OTHER_TEXT_TYPE, chat.getContent());


                // 返回给其他人
                String msg2OtherStr = JSON.toJSONString(msg2Other);

                // 返回给发送者
                msg2Other.setMsgType(Constant.MY_TEXT_TYPE);
                String msg2MyStr = JSON.toJSONString(msg2Other);



                logger.info("msgId:" + nowMsgId);

                for(SocketChannel sc : socketChannelSet) {
                    if(sc != socketChannel) {
                        NioServer.send(Convert.shortToBytes(Constant.SEND_CHAT_CMD), msg2OtherStr, sc);
                    } else {
                        NioServer.send( // 本人发送文字消息成功了
                                Convert.shortToBytes(Constant.SEND_CHAT_CMD),
                                msg2MyStr,
                                socketChannel);
                    }
                }
            }
        } else if (cmd == Constant.SNED_RED_PACKET_CMD) { // 用户发送红包消息
            // msg 是 RedPacket对象的json字符串格式
            Msg msgObj = JSON.parseObject(msg, Msg.class);

            ConcurrentHashMap<SocketChannel, Long> socketChannel2timestamp =  HandleHeartPacket.roomId2SocketChannel.get(msgObj.getRoomId());

            Set<SocketChannel> socketChannelSet = socketChannel2timestamp.keySet();

            if (msgObj.getMsgType() == Constant.MY_PACKET_TYPE) {
                RedPacket redPacket = JSON.parseObject(msg, RedPacket.class);
                Integer nowMsgId = msgId.addAndGet(1);
                Msg msg2Other = new RedPacket(
                        redPacket.getRoomId(),
                        nowMsgId,
                        Constant.OTHER_PACKET_TYPE,
                        redPacket.getTotalMoney(),
                        redPacket.getTotalNum(),
                        redPacket.getContent(),
                        redPacket.getTotalMoney(),
                        redPacket.getRemainNum());

                Constant.id2Msg.put(nowMsgId, msg2Other);
                // 返回给其他人
                String msg2OtherStr = JSON.toJSONString(msg2Other);

                // 返回给发送者
                msg2Other.setMsgType(Constant.MY_PACKET_TYPE);
                String msg2MyStr = JSON.toJSONString(msg2Other);

                logger.info("msgId:" + nowMsgId);


                for(SocketChannel sc : socketChannelSet) {
                    if(sc != socketChannel) {
                        NioServer.send(Convert.shortToBytes(Constant.SNED_RED_PACKET_CMD), msg2OtherStr, sc);
                    } else {
                        NioServer.send( // 本人发送红包成功了
                                Convert.shortToBytes(Constant.SNED_RED_PACKET_CMD),
                                msg2MyStr,
                                socketChannel);
                    }
                }
            }
        } else if (cmd == Constant.GET_RED_PACKET_CMD) { // 抢红包

            Integer msgId = Integer.parseInt(msg);

            System.out.println("fuck : " + msgId);


            Msg msgObj = Constant.id2Msg.get(msgId);


            if(msgObj instanceof RedPacket) {
                RedPacket redPacket = (RedPacket) msgObj;

                if(redPacket.getRemainNum().equals("0")) { // 剩余红包数为0
                    NioServer.send( // 本人抢红包行为
                            Convert.shortToBytes(Constant.GET_RED_PACKET_CMD),
                            JSON.toJSONString(redPacket),
                            socketChannel);
                    return;
                }

                synchronized (redPacket) {

                    if(redPacket.getRemainNum().equals("0")) { // 剩余红包数为0

                    } else if (redPacket.getRemainNum().equals("1")) { // 剩余红包数为1

                        redPacket.getMoneyGet().add(redPacket.getRemainMoney().setScale(2));
                        redPacket.setRemainMoney(new BigDecimal(0));
                        redPacket.setRemainNum("0");

                        try {
                            redPacket.getUsersGet().add(new User(socketChannel.getRemoteAddress().toString()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    } else {

                        String remainNum =(Integer.parseInt(redPacket.getRemainNum()) - 1) + "";


                        BigDecimal minGet = new BigDecimal("0.01");
                        BigDecimal maxGet = redPacket.getRemainMoney().subtract(new BigDecimal(remainNum).multiply(minGet).setScale(2)).setScale(2);

                        Random random = new Random();
                        int randomInt = random.nextInt(101);


                        BigDecimal get = maxGet.multiply(new BigDecimal(randomInt)).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_DOWN);

                        if (get.compareTo(minGet) < 0) {
                            get = minGet;
                        }


                        redPacket.setRemainNum(remainNum);
                        redPacket.setRemainMoney(redPacket.getRemainMoney().subtract(get).setScale(2));


                        redPacket.getMoneyGet().add(get);
                        try {
                            redPacket.getUsersGet().add(new User(socketChannel.getRemoteAddress().toString()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                logger.info(redPacket);
                NioServer.send( // 本人抢红包行为
                        Convert.shortToBytes(Constant.GET_RED_PACKET_CMD),
                        JSON.toJSONString(redPacket),
                        socketChannel);
            }
        } else if(cmd == Constant.SEND_IMG_CMD) {

            Image img = JSON.parseObject(msg, Image.class);

            System.out.println(socketChannel.socket().getPort());

            Integer nowMsgId = msgId.addAndGet(1);
            File file = ImgUtil.getFile(img.getContent(), "./", "origin_image_" + nowMsgId + ".png");
            int roomId = img.getRoomId();
            img.setMsgId(nowMsgId);

            //比例小于1缩小，宽和高同时缩小为原来的0.3倍
            try {
                String handleFilePath ="./handle_image_"+ nowMsgId + ".png";

                Thumbnails.of(file)
                        .scale(0.3f)
                        .outputQuality(1) // 图片质量压缩100%
                        .toFile(handleFilePath);

                file = new File(handleFilePath);
                img.setContent(ImgUtil.getBytes(handleFilePath, (int)file.length()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            NioServer.send( // 返回图片
                    Convert.shortToBytes(Constant.SEND_IMG_CMD),
                    JSON.toJSONString(img),
                    socketChannel); // 通过6667端口的socketchannel返回去

            Constant.id2ImageAndSocketChannel.put(nowMsgId, new ImageAndSocketChannel(img, socketChannel));

        } else if(cmd == Constant.IS_SEND_IMG_YOU_CMD) {
            JSONArray  params =   JSON.parseArray(msg); // 0:msgId, 1:roomId

            String msgIdStr = (String) params.get(0);
            String roomIdStr = (String) params.get(1);

            Integer msgId = Integer.valueOf(msgIdStr);
            Integer roomId = Integer.valueOf(roomIdStr);

            System.out.println("sb"+msgId + "____________" + roomId );

            ConcurrentHashMap<SocketChannel, Long> socketChannel2timestamp =  HandleHeartPacket.roomId2SocketChannel.get(roomId);
            Set<SocketChannel> socketChannelSet = socketChannel2timestamp.keySet();

            ImageAndSocketChannel isc = Constant.id2ImageAndSocketChannel.get(msgId);

            Image img = isc.getImage();


            img.setMsgType(Constant.OTHER_IMG_TYPE);

            String imgStr = JSON.toJSONString(img);
            SocketChannel sendSC = isc.getSocketChannel();

            for(SocketChannel s : socketChannelSet) {

                if(s != socketChannel) {
                    NioServer.send( // 返回图片
                            Convert.shortToBytes(Constant.SEND_IMG_CMD),
                            imgStr,
                            socketChannel); // 通过6666端口的socketchannel返回去
                }

            }


        }
    }
}
