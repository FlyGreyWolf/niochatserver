package com.flygreywolf.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.flygreywolf.msg.PayLoad;
import com.flygreywolf.util.Convert;

/**
 * java里面的new IO， 对应到linux系统下的nonblockingIO
 * 
 * @author FlyGreyWolf
 * @since 2020-08-21
 *
 */
public class NioServer implements Runnable {

	private ServerSocketChannel serverSocketChannel = null;
	private Selector selector = null;
	private SelectionKey selectionKey = null;
	private final static int LISTEN_PORT = 8888;
	private static HashMap<SocketChannel, PayLoad> cache = new HashMap<SocketChannel, PayLoad>();

	/**
	 * initServer
	 * 
	 * @throws IOException
	 */
	public void initServer() throws IOException {
		selector = Selector.open(); // epoll create 相当于在内核开辟空间fdxx，使用红黑树存放所有的fd
		serverSocketChannel = ServerSocketChannel.open(); // socket，在linux底层其实就返回一个fd1
		serverSocketChannel.configureBlocking(false); // socket 设为 非阻塞
		serverSocketChannel.socket().bind(new InetSocketAddress(LISTEN_PORT)); // socket bind 端口
		
		// 将serverSocket的fd注册到内核开辟的空间中，epoll_ctl(fd1,ADD,fdxx,accept)
		selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); 
		System.out.println("initServer is finished and success");
	}

	public void run() {
		System.out.println("Server is running");
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
				e.printStackTrace();
				try {
					serverSocketChannel.close(); // 关闭serversocket
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
    public void accept(SelectionKey key) {
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = serverSocketChannel.accept();
            System.out.println("accept a connect from clien:"+socketChannel.getRemoteAddress());
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
     
    public void handleByteArr(byte[] byteArr, int pos, int len, SocketChannel channel) {
    	while(len - pos >= 4) {
			byte[] length = new byte[4];
			System.arraycopy(byteArr, pos, length, 0, 4);
			
			int contentLen = Convert.byteArrToInteger(length);
			if(contentLen > 1500) {
				System.out.println("contentLen > 1500，有可能是恶意攻击");
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
				System.out.println(new String(content));
			} else { // 读不完，发生拆包问题
				byte[] content = new byte[contentLen];
				System.arraycopy(byteArr, pos, content, 0, len-pos);
				payLoad.setContent(content);
				payLoad.setPosition(len-pos);
				pos = len;
//				System.out.println("发生拆包，只读取到一部分"+new String(content));
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
        try {
            SocketChannel channel = (SocketChannel) selectionKey.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(128);
            
            int len = channel.read(byteBuffer); // 读到的长度
            int pos = 0;
            
            if (len > 0) {
 
            	byte[] byteArr = byteBuffer.array();
            	if(cache.containsKey(channel)) {
            		
            		PayLoad payLoad = cache.get(channel);
            		
            		if(payLoad.getLengthSize() == 4) { // 头部完整
            			int remainLen = Convert.byteArrToInteger(payLoad.getLength()) - payLoad.getPosition();
                  		 
                		if(len >= remainLen) { // 可以读完
                			cache.remove(channel);
                			System.arraycopy(byteArr, pos, payLoad.getContent(), payLoad.getPosition(), remainLen);
                			pos = pos + remainLen;
                			System.out.println(new String(payLoad.getContent()));
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
            				if(contentLen > 1500) {
            					System.out.println("contentLen > 1500，有可能是恶意攻击");
            					return;
            				}
            				if(len - pos >= contentLen) { // 可以读完
            					cache.remove(channel);
            					byte[] content = new byte[contentLen];
            					System.arraycopy(byteArr, pos, content, 0, contentLen);
            					payLoad.setContent(content);
            					pos = pos + contentLen;
            					System.out.println(new String(content));
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
            }
        } catch (IOException e) {
            try {
                serverSocketChannel.close();
                selectionKey.cancel();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }
}
