package com.flygreywolf.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.flygreywolf.bean.Room;
import com.flygreywolf.msg.HandlePayLoad;
import com.flygreywolf.msg.PayLoad;
import com.flygreywolf.util.ByteArrPrint;
import com.flygreywolf.util.Constant;
import com.flygreywolf.util.Convert;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * java里面的new IO， 对应到linux系统下的nonblockingIO
 * 
 * @author FlyGreyWolf
 * @since 2020-08-21
 *
 */
public class NioServer implements Runnable {

	private static Logger logger= Logger.getLogger(NioServer.class);

	private ServerSocketChannel serverSocketChannel = null;
	private Selector selector = null;
	private SelectionKey selectionKey = null;


	private static List<Room> roomList = new ArrayList<>();
	private HashMap<SocketChannel, PayLoad> cache = new HashMap<SocketChannel, PayLoad>(); // 解决拆包、粘包的cache


	/**
	 * initServer
	 * 
	 * @throws IOException
	 */
	public void initServer(int port)  {
		try {
			selector = Selector.open(); // epoll create 相当于在内核开辟空间fdxx，使用红黑树存放所有的fd
			serverSocketChannel = ServerSocketChannel.open(); // socket，在linux底层其实就返回一个fd1
			serverSocketChannel.configureBlocking(false); // socket 设为 非阻塞
			serverSocketChannel.socket().bind(new InetSocketAddress(port)); // socket bind 端口

			// 将serverSocket的fd注册到内核开辟的空间中，epoll_ctl(fd1,ADD,fdxx,accept)
			selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			logger.info("initServer is finished and success at " + port );

		} catch (IOException e) {
			logger.error(e.getMessage());
		}


		if(port == Main.CONNECT_LISTEN_PORT) {
			initRoomList();

			new Thread(new Runnable() { // 检测客户端连接状态的线程
				@Override
				public void run() {
					HandleHeartPacket.checkConnectHeartPacket();
				}
			}).start();

			new Thread(new Runnable() { // 检测客户端在房间的状态
				@Override
				public void run() {
					HandleHeartPacket.checkRoomHeartPacket();
				}
			}).start();
		}

		
	}

	/**
	 * 初始化房间列表
	 */
	public void initRoomList() {

		roomList.add(new Room(1,"test1", "我是test1"));
		roomList.add(new Room(2,"test2", "我是test2"));
		roomList.add(new Room(3,"test3", "我是test3"));
		roomList.add(new Room(4,"test4", "我是test4"));

		HandleHeartPacket.roomId2SocketChannel.put(1, new ConcurrentHashMap<SocketChannel, Long>());
		HandleHeartPacket.roomId2SocketChannel.put(2, new ConcurrentHashMap<SocketChannel, Long>());
		HandleHeartPacket.roomId2SocketChannel.put(3, new ConcurrentHashMap<SocketChannel, Long>());
		HandleHeartPacket.roomId2SocketChannel.put(4, new ConcurrentHashMap<SocketChannel, Long>());



	}

	public void run() {
		logger.info("Server is running");
		while (true) {
			try {
				int selectKey = selector.select(); // 返回有事件的fd个数

				if (selectKey > 0) {
					Set<SelectionKey> keySet = selector.selectedKeys(); // 相当于epoll wait，得到有事件的fds
					Iterator<SelectionKey> iter = keySet.iterator();
					while (iter.hasNext()) {
						SelectionKey selectionKey = iter.next();
						iter.remove();
						if (selectionKey.isAcceptable()) { // 接收连接
							accept(selectionKey);
						} else if (selectionKey.isReadable()) { // 读取数据
							read(selectionKey);
						} 
					}
				}
			} catch (IOException e) {
				logger.error(e.getMessage());
				try {
					serverSocketChannel.close(); // 关闭serversocket
				} catch (IOException e1) {
					logger.error(e.getMessage());
				}
			}
		}
	}
	
    public void accept(SelectionKey key) {
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = serverSocketChannel.accept();
            logger.info("accept a connect from clien:"+socketChannel.getRemoteAddress());
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);

			// 将roomList的信息以json字符串格式返回前端
			JSONArray roomListJsonArray= JSONArray.parseArray(JSON.toJSONString(roomList));
			send(Convert.shortToBytes(Constant.ROOM_LIST_CMD), roomListJsonArray.toJSONString(), socketChannel);
		} catch (IOException e) {
        	logger.error(e.getMessage());
        }
    }

	/**
	 * 发送数据
	 *
	 * @param cmd
	 * @param msg
	 * @param channel
	 * @return
	 */
	public static boolean send(byte[] cmd, String msg, SocketChannel channel) {
		ByteBuffer byteBuffer = null;
		try {
			byte[] msgByteArr = msg.getBytes(Constant.UTF8_Encode);
			byte[] combine = new byte[cmd.length + msgByteArr.length];
			System.arraycopy(cmd, 0, combine, 0, cmd.length);
			System.arraycopy(msgByteArr, 0, combine, cmd.length, msgByteArr.length);


			int contentLen = combine.length;
			byteBuffer = ByteBuffer.allocate(4 + contentLen);
			System.out.println(contentLen);
			byteBuffer.put(Convert.intToBytes(contentLen));
			byteBuffer.put(combine);
			byteBuffer.flip();
			//System.out.println("[server] send " + contentLen + "bytes--->" + ByteArrPrint.printByteArr(combine));
		} catch (UnsupportedEncodingException e) { // 不支持该编码
			logger.error(e.getMessage());
			try {
				channel.close();
			} catch (IOException e1) {
				logger.error(e1.getMessage());
			}
			return false;
		}

		while (byteBuffer.hasRemaining()) {
			try {

				channel.write(byteBuffer);

			} catch (Exception e) {
				logger.error(e.getMessage());
				try {
					channel.close();
				} catch (Exception e1) {
					logger.error(e1.getMessage());
				}
				return false;
			}
		}
		return true;
	}

     
    public void handleByteArr(byte[] byteArr, int pos, int len, SocketChannel channel) {
    	while(len - pos >= 4) {
			byte[] length = new byte[4];
			System.arraycopy(byteArr, pos, length, 0, 4);
			
			int contentLen = Convert.byteArrToInteger(length);


			if(contentLen > Constant.MAX_CONTENT_LEN) {
				logger.info("contentLen > " + Constant.MAX_CONTENT_LEN + "，有可能是恶意攻击");
				return;
			}
			
			PayLoad payLoad = new PayLoad();
			payLoad.setLengthSize(4);
			payLoad.setLength(length);
			
			pos = pos + 4;
			
			if(len - pos >= contentLen) { // 可以读完
				byte[] content = new byte[contentLen];
				System.arraycopy(byteArr, pos, content, 0, contentLen);
				payLoad.setContent(content);
				pos = pos + contentLen;
				//System.out.println(new String(content));
				HandlePayLoad.parse(payLoad, channel);

			} else { // 读不完，发生拆包问题
				byte[] content = new byte[contentLen];
				System.arraycopy(byteArr, pos, content, 0, len-pos);
				payLoad.setContent(content);
				payLoad.setPosition(len-pos);
				pos = len;
//				System.out.println("发生拆包B ，只读取到一部分"+new String(content));
				cache.put(channel, payLoad);
			}
		}
    	
    	// 头部不全
    	if(len - pos > 0 && len -pos < 4) {
    		byte[] length = new byte[4];
			PayLoad payLoad = new PayLoad();
			System.arraycopy(byteArr, pos, length, 0, len-pos);
			payLoad.setLengthSize(len-pos);
			payLoad.setLength(length);
			pos = len;
			cache.put(channel, payLoad);
    	}
    }
    
    public void read(SelectionKey selectionKey) {
		SocketChannel channel = (SocketChannel) selectionKey.channel();
        try {

            ByteBuffer byteBuffer = ByteBuffer.allocate(128);

            int len = channel.read(byteBuffer); // 读到的长度


			int pos = 0;

            if (len > 0) {

            	byte[] byteArr = byteBuffer.array();
            	if(cache.containsKey(channel)) {

            		PayLoad payLoad = cache.get(channel);
					System.out.println("len:" + len);
					System.out.println("total:" + payLoad.getContent().length);


            		if(payLoad.getLengthSize() == 4) { // 头部完整
            			int remainLen = Convert.byteArrToInteger(payLoad.getLength()) - payLoad.getPosition();

                		if(len >= remainLen) { // 可以读完
                			cache.remove(channel);
                			System.arraycopy(byteArr, pos, payLoad.getContent(), payLoad.getPosition(), remainLen);
                			pos = pos + remainLen;
                			//System.out.println(new String(payLoad.getContent()));

							HandlePayLoad.parse(payLoad, channel);

                			// 还要把剩下的字节处理完
                			handleByteArr(byteArr, pos, len, channel);

                		} else { // 读不完，发生拆包问题
                			System.arraycopy(byteArr, pos, payLoad.getContent(), payLoad.getPosition(), len-pos);
                			payLoad.setPosition(payLoad.getPosition()+len-pos);
                		}
            		} else { // 头部不完整
            			int headRemainBytes = 4 - payLoad.getLengthSize();

            			if(len >= headRemainBytes) { // 可以组装成完整的头部了
            				System.arraycopy(byteArr, pos, payLoad.getLength(), payLoad.getLengthSize(), headRemainBytes);
            				payLoad.setLengthSize(4);
            				pos = pos + headRemainBytes;
            				int contentLen = Convert.byteArrToInteger(payLoad.getLength());
            				if(contentLen > Constant.MAX_CONTENT_LEN) {
                                logger.info("contentLen > " + Constant.MAX_CONTENT_LEN + "，有可能是恶意攻击");
            					return;
            				}
            				if(len - pos >= contentLen) { // 可以读完
            					cache.remove(channel);
            					byte[] content = new byte[contentLen];
            					System.arraycopy(byteArr, pos, content, 0, contentLen);
            					payLoad.setContent(content);
            					pos = pos + contentLen;
            					//System.out.println(new String(content));

            					HandlePayLoad.parse(payLoad, channel);

            					// 还要把剩下的字节处理完
                    			handleByteArr(byteArr, pos, len, channel);
            				} else { // 读不完，发生拆包问题
            					byte[] content = new byte[contentLen];
            					System.arraycopy(byteArr, pos, content, 0, len-pos);
            					payLoad.setContent(content);
            					payLoad.setPosition(len-pos);
            					pos = len;
            					//System.out.println("发生拆包，只读取到一部分"+new String(content));
            				}

            			} else { // 还是没能组装成完整头部
            				System.arraycopy(byteArr, pos, payLoad.getLength(), payLoad.getLengthSize(), len-pos);
            				payLoad.setLengthSize(payLoad.getLengthSize() + len  -pos);
            				pos = len;
            			}
            		}

            	} else { // 无缓存，代表是新的数据包
            		handleByteArr(byteArr, pos, len, channel);
            	}
            } else { // 如果客户端断开连接了，也会不停地产生OP_READ事件，但是read的返回值是-1
				channel.close();
				cache.remove(channel);
				selectionKey.cancel();
				logger.debug("客户端断开连接");
			}
        } catch (Exception e) {
			cache.remove(channel);
			e.printStackTrace();
			logger.error(e.getMessage());
		}
    }
}
