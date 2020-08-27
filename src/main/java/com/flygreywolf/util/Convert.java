package com.flygreywolf.util;

public class Convert {

	public static int byteArrToInteger(byte[] byteArr) {
		
		int res = 0;
		
		for(int i=0;i<4;i++) {
			res = res << 8 | byteArr[i];
		}
		
		return res;
		
	}
	
//	public static void main(String[] args) {
//		byte[] byteArr = {-128,0,0,0};
//		System.out.println(byteArrToInteger(byteArr));
//		
//	}
}
