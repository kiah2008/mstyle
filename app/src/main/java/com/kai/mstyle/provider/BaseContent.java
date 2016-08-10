package com.kai.mstyle.provider;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by kiah on 8/10/2016.
 */
public abstract class BaseContent
{
  protected abstract void createTables(SQLiteDatabase paramSQLiteDatabase);
}
