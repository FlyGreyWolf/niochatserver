package com.flygreywolf.core;

import java.io.IOException;

public class Main {
	public static void main(String[] args) throws IOException {
		NioServer nioServer = new NioServer();
		nioServer.initServer();
		
		new Thread(nioServer, "NioServer").start();
//		
//		NioSocketClient client = new NioSocketClient();
//        client.initClient();
//        client.start();
	}
}
