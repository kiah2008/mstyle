package com.kai.mstyle.blegatt;

import android.bluetooth.BluetoothGattCharacteristic;

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

    public static String matchProperty(int property) {
        String prop = "";
        if ((property & BluetoothGattCharacteristic.PERMISSION_READ) != 0) {
            prop += "R";
        }
        if ((property & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
            prop += "W";
        }
        if ((property & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            prop += "N";
        }

        return prop;
    }

    public static int getValueFormat(int properties) {
        if ((BluetoothGattCharacteristic.FORMAT_FLOAT & properties) != 0)
            return BluetoothGattCharacteristic.FORMAT_FLOAT;
        if ((BluetoothGattCharacteristic.FORMAT_SFLOAT & properties) != 0)
            return BluetoothGattCharacteristic.FORMAT_SFLOAT;
        if ((BluetoothGattCharacteristic.FORMAT_SINT16 & properties) != 0)
            return BluetoothGattCharacteristic.FORMAT_SINT16;
        if ((BluetoothGattCharacteristic.FORMAT_SINT32 & properties) != 0)
            return BluetoothGattCharacteristic.FORMAT_SINT32;
        if ((BluetoothGattCharacteristic.FORMAT_SINT8 & properties) != 0)
            return BluetoothGattCharacteristic.FORMAT_SINT8;
        if ((BluetoothGattCharacteristic.FORMAT_UINT16 & properties) != 0)
            return BluetoothGattCharacteristic.FORMAT_UINT16;
        if ((BluetoothGattCharacteristic.FORMAT_UINT32 & properties) != 0)
            return BluetoothGattCharacteristic.FORMAT_UINT32;
        if ((BluetoothGattCharacteristic.FORMAT_UINT8 & properties) != 0)
            return BluetoothGattCharacteristic.FORMAT_UINT8;
        return 0;
    }

    public static String getPropertyDescription(int props) {
        String propertiesString = String.format("0x%02X [", props);
        if ((props & BluetoothGattCharacteristic.PROPERTY_READ) != 0)
            propertiesString += "read ";
        if ((props & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)
            propertiesString += "write ";
        if ((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0)
            propertiesString += "notify ";
        if ((props & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0)
            propertiesString += "indicate ";
        if ((props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0)
            propertiesString += "write_no_response ";
        propertiesString += "]";
        return propertiesString;
    }

    public static byte[] parseHexStringToBytes(final String hex) {
        String tmp = null;
        if (hex.indexOf('c') != -1) {
            hex.substring(2).replaceAll
                    ("[^[0-9][a-f]]", "");
        } else {
            hex.replaceAll
                    ("[^[0-9][a-f]]", "");
        }
        byte[] bytes = new byte[tmp.length() / 2]; // every two letters in the string are one byte finally
        String part = "";

        for (int i = 0; i < bytes.length; ++i) {
            part = "0x" + tmp.substring(i * 2, i * 2 + 2);
            bytes[i] = Long.decode(part).byteValue();
        }

        return bytes;
    }
}
