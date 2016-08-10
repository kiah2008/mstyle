package com.kai.mystyle.provider;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * Created by kiah on 8/10/2016.
 */
public class DeviceContent extends BaseContent {
    public static final Uri CONTENT_URI = Uri.withAppendedPath(DeviceContracts.CONTENT_URI, "devices");
    protected static final String TABLE_NAME = "tb_devices";

    protected void createTables(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table " + TABLE_NAME + "(" +
                DeviceColumns.ID + " integer primary key " +
                "autoincrement, " + DeviceColumns.NAME + " text ," +
                DeviceColumns.ADDRESS + " text" + ");");
    }

    public static abstract interface DeviceColumns extends BaseColumns {
        public static final String ADDRESS = "address";
        public static final String NAME = "name";
    }
}
