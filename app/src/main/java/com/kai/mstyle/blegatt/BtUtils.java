package com.kai.mstyle.blegatt;

/**
 * Created by kiah on 8/13/2016.
 */
public class BtUtils {
    /**
     * Hex chars
     */
    private static final byte[] HEX_CHAR = new byte[]
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};


    /**
     * Helper function that dump an array of bytes in hex form
     *
     * @param buffer The bytes array to dump
     * @return A string representation of the array of bytes
     */
    public static final String dumpBytes(byte[] buffer) {
        if (buffer == null) {
            return "";
        }

        StringBuffer sb = new StringBuffer();
        sb.append("0x");
        for (int i = 0; i < buffer.length; i++) {
            sb.append((char) (HEX_CHAR[(buffer[i] & 0x00F0) >> 4])).append(
                    (char) (HEX_CHAR[buffer[i] & 0x000F]));
        }

        return sb.toString();
    }
}
