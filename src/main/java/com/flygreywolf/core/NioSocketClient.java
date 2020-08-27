package com.flygreywolf.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
 
public class NioSocketClient extends Thread {
    private SocketChannel socketChannel;
    private Selector selector = null;
    private int clientId;
    private InetSocketAddress inetSocketAddress = new InetSocketAddress("192.168.43.65",8888);
    static {
        System.out.println("sssssss");
    }
 
    public static void main(String args[]) throws IOException {
        NioSocketClient client = new NioSocketClient();
        NioSocketClient client1 = new NioSocketClient();
        client.initClient();
        client.start();
    }
 
    public NioSocketClient() {
    }
 
    public NioSocketClient(int clientId) {
        this.clientId = clientId;
    }
 
    public void initClient() throws IOException {

        selector = Selector.open();

        while(true){     // 死循环直到连接成功
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(inetSocketAddress);
            System.out.println("fff");
            try{
                while(!socketChannel.finishConnect()){
                }
                synchronized (selector) {
                    socketChannel.register(selector, SelectionKey.OP_WRITE);
                }
                send(socketChannel);
                break;
            }catch(Exception e) {
                socketChannel.close();
                continue;
            }

        }
    }
 
    public void run() {
        while (true) {
            try {
                int key = selector.select();
                if (key > 0) {
                    Set<SelectionKey> keySet = selector.selectedKeys();
                    Iterator<SelectionKey> iter = keySet.iterator();
                    while (iter.hasNext()) {
                        SelectionKey selectionKey = null;
                        synchronized (iter) {
                            selectionKey = iter.next();
                            iter.remove();
                        }

                        if (selectionKey.isReadable()) {
                            read(selectionKey);
                        }
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

 
    public void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        int len = channel.read(byteBuffer);
        if (len > 0) {
            byteBuffer.flip();
            byte[] byteArray = new byte[byteBuffer.limit()];
            byteBuffer.get(byteArray);
            System.out.println("client[" + clientId + "]" + "receive from server:");
            System.out.println(new String(byteArray));
            len = channel.read(byteBuffer);
            byteBuffer.clear();
 
        }
        key.interestOps(SelectionKey.OP_READ);
    }
 
    public void send(SocketChannel channel) {

        for (int i =1; i < 10; i++) {
            String ss = i + "Server ,how are you? this is package message from NioSocketClient!";
            int head = (ss).getBytes().length;
            ByteBuffer byteBuffer = ByteBuffer.allocate(4 + head);
            byteBuffer.put(intToBytes(head));
            byteBuffer.put(ss.getBytes());
            byteBuffer.flip();
            System.out.println("[client] send:" + i + "-- " + head + ss);
            while (byteBuffer.hasRemaining()) {
                try {
                    channel.write(byteBuffer);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }
 
    /**
     * int到byte[]
     * 
     * @param 
     * @return
     */
    public static byte[] intToBytes(int value) {
        byte[] result = new byte[4];
        // 由高位到低位
        result[0] = (byte) ((value >> 24) & 0xFF);
        result[1] = (byte) ((value >> 16) & 0xFF);
        result[2] = (byte) ((value >> 8) & 0xFF);
        result[3] = (byte) (value & 0xFF);
        return result;
    }
}