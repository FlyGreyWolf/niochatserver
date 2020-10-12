package com.flygreywolf.core;


public class Main {

	public final static int CONNECT_LISTEN_PORT = 6666;
	public final static int IMG_LISTEN_PORT = 6667;
	public final static int GET_BIG_IMG_PORT = 6668;

	public static void main(String[] args)  {
		NioServer nioServer = new NioServer();
		nioServer.initServer(CONNECT_LISTEN_PORT);


		NioServer imgNioServer = new NioServer();
		imgNioServer.initServer(IMG_LISTEN_PORT);


		NioServer getBigImgNioServer = new NioServer();
		getBigImgNioServer.initServer(GET_BIG_IMG_PORT);
		
		new Thread(nioServer, "NioServer").start();
		new Thread(imgNioServer, "imgNioServer").start();
		new Thread(getBigImgNioServer, "getBigImgNioServer").start();
	}
}
