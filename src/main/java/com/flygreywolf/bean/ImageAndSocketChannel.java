package com.flygreywolf.bean;

import java.nio.channels.SocketChannel;

public class ImageAndSocketChannel {
    private Image image;
    private SocketChannel socketChannel;


    public ImageAndSocketChannel(Image image, SocketChannel socketChannel) {
        this.image = image;
        this.socketChannel = socketChannel;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }
}
