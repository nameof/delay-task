package com.nameof.jedis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @Author: chengpan
 * @Date: 2018/11/21
 */
public class SerializeUtils {

    public static byte[] serialize(Object obj) {
        try {
            ByteArrayOutputStream bai = new ByteArrayOutputStream();
            ObjectOutputStream obi = new ObjectOutputStream(bai);
            obi.writeObject(obj);
            byte[] byt = bai.toByteArray();
            return byt;
        } catch (Exception e) {
            throw new RuntimeException("serialize error", e);
        }
    }

    public static Object deserizlize(byte[] byt) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(byt);
            ObjectInputStream oii = new ObjectInputStream(bis);
            Object obj = oii.readObject();
            return obj;
        } catch (Exception e) {
            throw new RuntimeException("deserizlize error", e);
        }
    }
}
