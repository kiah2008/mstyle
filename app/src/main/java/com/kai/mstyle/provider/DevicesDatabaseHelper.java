package com.kai.mstyle.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by kiah on 8/10/2016.
 */
public class DevicesDatabaseHelper extends SQLiteOpenHelper
{
  private static final String DATABASE_NAME = "devices.db";
  private static final int DATABASE_VERSION = 1;
  private static DevicesDatabaseHelper mInstance;

  private DevicesDatabaseHelper(Context paramContext, String paramString, int paramInt)
  {
    super(paramContext, paramString, null, paramInt);
  }

  public static DevicesDatabaseHelper getInstance(Context paramContext)
  {
    if (mInstance == null)
      mInstance = new DevicesDatabaseHelper(paramContext, "devices.db", 1);
    return mInstance;
  }

  private void initTables(SQLiteDatabase paramSQLiteDatabase)
  {
    new DeviceContent().createTables(paramSQLiteDatabase);
  }

  public void onCreate(SQLiteDatabase paramSQLiteDatabase)
  {
    initTables(paramSQLiteDatabase);
  }

  public void onUpgrade(SQLiteDatabase paramSQLiteDatabase, int paramInt1, int paramInt2)
  {
  }
}