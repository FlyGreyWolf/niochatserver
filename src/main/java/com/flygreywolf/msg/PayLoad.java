package com.flygreywolf.msg;

public class PayLoad {
	
	private byte[] length; // 4字节表示content的长度
	private byte[] content;
	
	private int position; // content位置
	private int lengthSize; // length数组的长度
	
	
	public byte[] getLength() {
		return length;
	}
	public void setLength(byte[] length) {
		this.length = length;
	}
	public byte[] getContent() {
		return content;
	}
	public void setContent(byte[] content) {
		this.content = content;
	}
	 
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	public int getLengthSize() {
		return lengthSize;
	}
	public void setLengthSize(int lengthSize) {
		this.lengthSize = lengthSize;
	}  
	
	
}
