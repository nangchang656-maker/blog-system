package cn.lzx.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES加密解密工具类
 * 与前端CryptoJS保持一致，密钥：MyBlogSecretKey2025
 *
 * 使用 AES-256-CBC 模式，与 CryptoJS 默认行为兼容
 */
public class AesUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY = "MyBlogSecretKey2025"; // 与前端保持一致
    private static final String SALTED_PREFIX = "Salted__";
    private static final int SALT_LENGTH = 8;
    private static final int KEY_LENGTH = 32; // AES-256
    private static final int IV_LENGTH = 16; // AES block size

    /**
     * AES解密（兼容CryptoJS）
     * 
     * @param encryptedData 前端CryptoJS加密的数据
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedData) {
        try {
            // 1. Base64解码得到加密数据
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);

            // 2. CryptoJS格式：前8字节是"Salted__"，接下来8字节是salt
            if (encryptedBytes.length < SALTED_PREFIX.length() + SALT_LENGTH ||
                    !new String(Arrays.copyOfRange(encryptedBytes, 0, SALTED_PREFIX.length()), StandardCharsets.UTF_8)
                            .equals(SALTED_PREFIX)) {
                throw new RuntimeException("不是有效的CryptoJS加密格式");
            }

            byte[] salt = Arrays.copyOfRange(encryptedBytes, SALTED_PREFIX.length(),
                    SALTED_PREFIX.length() + SALT_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(encryptedBytes, SALTED_PREFIX.length() + SALT_LENGTH,
                    encryptedBytes.length);

            // 3. 使用EVP_BytesToKey算法派生密钥和IV（与CryptoJS一致）
            byte[][] keyAndIv = deriveKeyAndIv(SECRET_KEY.getBytes(StandardCharsets.UTF_8), salt, KEY_LENGTH,
                    IV_LENGTH);
            byte[] key = keyAndIv[0];
            byte[] iv = keyAndIv[1];

            // 4. 解密
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(cipherText);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * AES加密（兼容CryptoJS）
     * 
     * @param data 明文数据
     * @return 加密后的字符串
     */
    public static String encrypt(String data) {
        try {
            // 1. 生成随机salt
            byte[] salt = new byte[SALT_LENGTH];
            new java.security.SecureRandom().nextBytes(salt);

            // 2. 使用EVP_BytesToKey算法派生密钥和IV
            byte[][] keyAndIv = deriveKeyAndIv(SECRET_KEY.getBytes(StandardCharsets.UTF_8), salt, KEY_LENGTH,
                    IV_LENGTH);
            byte[] key = keyAndIv[0];
            byte[] iv = keyAndIv[1];

            // 3. 加密
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // 4. 构造CryptoJS格式：Salted__ + salt + cipherText
            byte[] result = new byte[SALTED_PREFIX.length() + salt.length + encryptedBytes.length];
            System.arraycopy(SALTED_PREFIX.getBytes(StandardCharsets.UTF_8), 0, result, 0, SALTED_PREFIX.length());
            System.arraycopy(salt, 0, result, SALTED_PREFIX.length(), salt.length);
            System.arraycopy(encryptedBytes, 0, result, SALTED_PREFIX.length() + salt.length, encryptedBytes.length);

            // 5. Base64编码
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * EVP_BytesToKey算法实现（OpenSSL/CryptoJS使用的密钥派生算法）
     *
     * @param password 密码
     * @param salt     盐值
     * @param keyLen   密钥长度（字节）
     * @param ivLen    IV长度（字节）
     * @return [key, iv]
     */
    private static byte[][] deriveKeyAndIv(byte[] password, byte[] salt, int keyLen, int ivLen) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] derivedKey = new byte[keyLen + ivLen];
        byte[] hash = new byte[0];
        int offset = 0;

        while (offset < keyLen + ivLen) {
            md5.reset();
            md5.update(hash);
            md5.update(password);
            md5.update(salt);
            hash = md5.digest();

            int len = Math.min(hash.length, keyLen + ivLen - offset);
            System.arraycopy(hash, 0, derivedKey, offset, len);
            offset += len;
        }

        byte[] key = Arrays.copyOfRange(derivedKey, 0, keyLen);
        byte[] iv = Arrays.copyOfRange(derivedKey, keyLen, keyLen + ivLen);

        return new byte[][] { key, iv };
    }
}
