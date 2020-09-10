package com.libaolu.nmon.utils;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2019/4/24 10:44
 **/
public class Base64Util {

    /**
     * BASE64字符串解码为二进制数据
     * @param base64
     * @return
     * @throws Exception
     */
    public static byte[] decode(String base64) throws Exception {
        return new BASE64Decoder().decodeBuffer(base64);
    }

    /**
     * 二进制数据编码为BASE64字符串
     * @param bytes
     * @return
     * @throws Exception
     */
    public static String encode(byte[] bytes) throws Exception {
        return new BASE64Encoder().encode(bytes);
    }


}
