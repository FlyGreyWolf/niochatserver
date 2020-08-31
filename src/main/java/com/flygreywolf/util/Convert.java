package com.flygreywolf.util;

public class Convert {

	/**
	 * byte[]到int
	 *
	 * @param byteArr
	 * @return
	 */
	public static int byteArrToInteger(byte[] byteArr) {
		
		int res = 0;
		
		for(int i=0;i<4;i++) {
			res = res << 8 | byteArr[i];
		}
		
		return res;
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

	/**
	 * short到byte[]
	 *
	 * @param value
	 * @return
	 */
	public static byte[] shortToBytes(short value) {
		byte[] result = new byte[2];
		result[0] = (byte) ((value >> 8) & 0xFF);
		result[1] = (byte) (value & 0xFF);
		return result;
	}
}
