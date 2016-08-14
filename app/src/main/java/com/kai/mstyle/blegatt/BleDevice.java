package com.kai.mstyle.blegatt;

import android.bluetooth.BluetoothDevice;
import android.database.Cursor;
import android.text.TextUtils;

import com.kai.mstyle.provider.DeviceContent;

public class BleDevice {
    public static final int RSSI_INVALID = -1;
    public String mAddress;
    public String mName;
    public long mId;
    private int mRssi = RSSI_INVALID;
    private SignalStrength.SIGNAL_LEVEL mRssiLevel = SignalStrength
            .SIGNAL_LEVEL.SIGNAL_NONE;
    private long mAddedTime;

    public BleDevice(BluetoothDevice device) {
        this.mName = device.getName();
        this.mAddress = device.getAddress();
    }

    public BleDevice(Cursor cursor) {
        mName = cursor.getString(cursor.getColumnIndex(DeviceContent
                .DeviceColumns.NAME));
        mAddress = cursor.getString(cursor.getColumnIndex(DeviceContent
                .DeviceColumns.ADDRESS));
        mId = cursor.getLong(cursor.getColumnIndex(DeviceContent.DeviceColumns
                .ID));
        mAddedTime = cursor.getLong(cursor.getColumnIndex(DeviceContent
                .DeviceColumns.ADD_TIME));
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

    public long getId() {
        return mId;
    }

    public int getRssi() {
        return mRssi;
    }

    public void setRssi(int rssi) {
        mRssi = rssi;
    }

    public SignalStrength.SIGNAL_LEVEL getRssiLevel() {
        return mRssiLevel;
    }

    public void setRssiLevel(SignalStrength.SIGNAL_LEVEL level) {
        mRssiLevel = level;
    }
    public long getAddedTime() {
        return mAddedTime;
    }
}