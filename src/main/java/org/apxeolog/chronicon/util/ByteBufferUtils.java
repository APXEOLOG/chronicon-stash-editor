package org.apxeolog.chronicon.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author APXEOLOG (Artyom Melnikov), at 01.03.2018
 */
public class ByteBufferUtils {

    public static byte[] getByteArray(ByteBuffer byteBuffer, int size) {
        byte[] result = new byte[size];
        byteBuffer.get(result);
        return result;
    }

    public static String getString(ByteBuffer byteBuffer) {
        int length = byteBuffer.getInt();
        return new String(getByteArray(byteBuffer, length));
    }

    public static void putString(ByteBuffer byteBuffer, String string) {
        byte[] data = string.getBytes(StandardCharsets.UTF_8);
        byteBuffer.putInt(data.length);
        byteBuffer.put(data);
    }
}
