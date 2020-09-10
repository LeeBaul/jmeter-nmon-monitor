package com.libaolu.nmon.utils;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;

/**
 * <p/>
 *  RSA加解密工具类
 * @author libaolu
 * @version 1.0
 * @dateTime 2019/4/24 10:35
 **/
public class RsaUtil {

    /**
     *  加密算法RSA.
     */
    public static final String KEY_ALGORITHM = "RSA";
    /** *//**
     * RSA最大解密密文大小
     */
    private static final int MAX_DECRYPT_BLOCK = 128;

    /**
     *  公钥解密
     * @param encryptedData 已加密数据
     * @param publicKey  公钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] decryptByPublicKey(byte[] encryptedData, String publicKey) throws Exception {
        byte[] keyBytes = Base64Util.decode(publicKey);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        Key publicK = keyFactory.generatePublic(x509KeySpec);
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, publicK);
        int inputLen = encryptedData.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
        }
        byte[] decryptedData = out.toByteArray();
        out.close();
        return decryptedData;
    }

    /**
     * 获取固定的PublicKey
     * @return base64码的公钥
     */
    public static String getPublicKey(){
        StringBuilder sb = new StringBuilder();
        sb.append("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDgRcPPXSgdqZ4U2gIx7v+2MGzn\n");
        sb.append("afGgqm2ive/SMXOJ91TL7g8eZOUufaXqw3q7BKx1F6rwjk8r7xkENsbpoCVFPgD2\n");
        sb.append("M7guMu3Yo4dNlEz5S2OJbJQ0M5uI/uDHhs+pgdNXFNFQCDkZlJ+ZqPf0CeY4LBP2\n");
        sb.append("QkxrKBFN61x2uMjtLwIDAQAB\n");
        return sb.toString();
    }

}