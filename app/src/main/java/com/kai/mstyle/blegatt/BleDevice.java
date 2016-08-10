package com.kai.mstyle.blegatt;

import android.bluetooth.BluetoothDevice;
import android.database.Cursor;
import android.text.TextUtils;

import com.kai.mstyle.provider.DeviceContent;

public class BleDevice {
    public String mAddress;
    public String mName;
    public String mUuid;

    public BleDevice(BluetoothDevice device) {
        this.mName = device.getName();
        this.mAddress = device.getAddress();
    }

    public BleDevice(Cursor cursor) {
        mName = cursor.getString(cursor.getColumnIndex(DeviceContent
                .DeviceColumns.NAME));
        mAddress = cursor.getString(cursor.getColumnIndex(DeviceContent
                .DeviceColumns.ADDRESS));
    }

    public boolean equals(Object paramObject) {
        if (paramObject == this) {
            return true;
        }
        if (paramObject == null || paramObject.getClass() != getClass()) {
            return false;
        }
        BleDevice device = (BleDevice) paramObject;
        if (!TextUtils.equals(getName(), device.getName()) || !TextUtils.equals
                (getAddress(), device.getAddress())) {
            return false;
        }

        return true;
    }

    public String getAddress() {
        return mAddress;
    }

    public String getName() {
        return mName;
    }
}