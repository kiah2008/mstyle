package com.kai.mystyle.provider;

import android.net.Uri;

/**
 * Created by kiah on 8/10/2016.
 */
public class DeviceContracts
{
  public static final String AUTHORITY = "com.kai.mstyle.provider";
  public static final Uri CONTENT_URI = Uri.parse("content://com.kai.mstyle.provider");
  protected static final String[] TABLES = { "tb_devices" };
}
