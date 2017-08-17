package com.effects.senseme.sensemesdk.utils;

import android.util.Base64;

import java.io.UnsupportedEncodingException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
public class ApiSecurity {
	
	
	
	private static final String IV_STRING = "security.iv_string";
	private static final String KEY = "security.key";
	private static final String charset = "UTF-8";
	
	
	private static ApiSecurity instance = null;  
	
	 private ApiSecurity() {  
		  
	    } 
	 
	 public static ApiSecurity getInstance() {  
	        if (instance == null)  
	            instance = new ApiSecurity();  
	        return instance;  
	   } 
	 
	public  String encrypt(String input) {
		byte[] crypted = null;
		try {
			SecretKeySpec skey = new SecretKeySpec(KEY.getBytes(), "AES"); 
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			//cipher.init(Cipher.ENCRYPT_MODE, skey);
			byte[] initParam = IV_STRING.getBytes(charset);
		    IvParameterSpec ivParameterSpec = new IvParameterSpec(initParam);
		    cipher.init(Cipher.ENCRYPT_MODE, skey, ivParameterSpec);   
		    
			crypted = cipher.doFinal(input.getBytes());
		} catch (Exception e) {
			System.out.println(e.toString());
		}

		return new String(Base64.encode(crypted,0));
	}

	public  String decrypt(String input) {
		byte[] output = null;
		try {
			SecretKeySpec skey = new SecretKeySpec(KEY.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			byte[] initParam = IV_STRING.getBytes(charset);
		    IvParameterSpec ivParameterSpec = new IvParameterSpec(initParam);
			cipher.init(Cipher.DECRYPT_MODE, skey,ivParameterSpec);
			// output = cipher.doFinal(Base64.decodeBase64(input));
			output = cipher.doFinal(Base64.decode(input.getBytes(),0));
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return new String(output);
	}

//	public static void main(String[] args) throws UnsupportedEncodingException {
//		String key = "123456789abcdefg";
//		String data = "111111";
//
//		System.out.println(ApiSecurity.getInstance().encrypt(data));
//
//		// System.out.println(Security.decrypt("WhJkVlV/Bnohy5haaMmu9w==",
//		// key));
//		String aaa = "GiIDdeNNKOavuPk0Dq6+B+LuAMKqAnGLwVwhxFXnALU0Brjhkj806E2QZsknnuG1dNQ/mAj/yEKx9XUIBvSO6HVNIH5G6yhrWrM/rO/x75Hh8WMEEwrBwnurcKAXoBaC0pGhZF0Td+98d0MmfSXJVnkJeV/SC1U7+7rrH0dttjNy1GHxHxgu4eu0zlkTEC4nB8wo2RNrhvJKG7W5uW9KQyf3PM+a7e3TvIrnECsM6yCu4sR39FsSBT8dwWeZb0TogIrBvL6/QSlP655Vgae7T3joZhpOpby6j6KfILqYOX5fpXmkGI5P3mdvVD17NsmhkcTiOI0GrJXMLwOLtCRysreXYWEP/5jSLYJMXrl8jDISODR4FDZcRR032JKfZoHcBkYaVNUut8Mg1ZM8TWtm5LyLhDey1v1b6B70yFtHeNqY+GovEjd9E/If7BElxhX54W5R3zYIQGBMbRTQXZIidTx8b/evAqcmK9AZpw3WbQ5x3skgMdSnOAzND2BIYCi3zLb9iIrJ05DUSJizUVhmYaFbkzutPJZJSlgztqre2mM6SnmUCBK2UCfbMsHMXvcZRXoRIaH4gjAhQetknBWszwtEw+jOPxZATG/rMX6CsgONI7vFm3iRrs3PSYBjVU7BSUtpaHnvI7bO/kB5GwXiusndOAthcaKE6ZE1PT6b3DHWsVzvQsHfjh3pG2UO3PuzzDSbZrHfDZSXEy1dNnlgm4SZk8eL2yIpxtpQG1y9oFLspI7v2/hZyLXkTcrZfClyCk+CPcZIKrBCVoriTXI6Fel4j19TMe8YDZgzLS1dolVfy11glS2nOY1dqkiDuCKO";
//
//
//		String bb = ApiSecurity.getInstance().decrypt(aaa);
//
//
//		System.out.println(bb);
//
//	}
}
