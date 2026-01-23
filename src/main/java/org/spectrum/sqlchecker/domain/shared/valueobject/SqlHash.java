package org.spectrum.sqlchecker.domain.shared.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SQL 哈希值对象（用于去重）
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Getter
@EqualsAndHashCode
public class SqlHash {

    private final String value;

    private SqlHash(String value) {
        this.value = value;
    }

    /**
     * 从抽象 SQL 生成哈希
     *
     * @param abstractSql 抽象 SQL
     * @return SQL 哈希
     */
    public static SqlHash fromAbstract(String abstractSql) {
        if (abstractSql == null || abstractSql.isBlank()) {
            throw new IllegalArgumentException("抽象 SQL 不能为空");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(abstractSql.getBytes(StandardCharsets.UTF_8));
            return new SqlHash(bytesToHex(hash));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }

    /**
     * 从原始哈希字符串创建
     *
     * @param hash 哈希字符串
     * @return SQL 哈希
     */
    public static SqlHash fromString(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("哈希值不能为空");
        }
        return new SqlHash(hash);
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 获取短哈希（前 8 位，用于显示）
     */
    public String getShortHash() {
        return value.length() >= 8 ? value.substring(0, 8) : value;
    }

    @Override
    public String toString() {
        return getShortHash();
    }
}
