package com.jasonchen.microlang.utils;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * jasonchen
 * 2015/04/10
 */
public class StreamUtility {
	/**
	 * @param is
	 *            读取转换inputstream流数据
	 * @return String 转换结果
	 * @throws java.io.IOException
	 */
	public static String readFromStream(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = 0;
		while ((len = is.read(buffer)) != -1) {
			baos.write(buffer, 0, len);
		}
		is.close();
		String result = baos.toString();
		baos.close();
		return result;
	}
}
