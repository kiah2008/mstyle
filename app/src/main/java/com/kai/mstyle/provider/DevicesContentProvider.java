package com.kai.mstyle.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * Created by kiah on 8/10/2016.
 */
public class DevicesContentProvider extends ContentProvider {
    private static final int BASE_SHIFT = 3;
    private static final int DEVICES = 0;
    private static final int DEVICES_BASE = 0;
    private static final int DEVICES_ID = 1;
    private static final UriMatcher sURIMatcher = new UriMatcher(-1);
    private Context mContext = getContext();
    private SQLiteDatabase mDatabase;

    public DevicesContentProvider() {
        init();
    }

    private static Uri getBaseNotificationUri(int match) {
        switch (match) {
            case DEVICES:
            case DEVICES_ID:
                return DeviceContent.CONTENT_URI;
            default:
                return null;
        }
    }

    private static String whereWithId(String id, String where) {
        StringBuilder localStringBuilder = new StringBuilder(128);
        localStringBuilder.append("_id=");
        localStringBuilder.append(id);
        if (where != null) {
            localStringBuilder.append(" AND (");
            localStringBuilder.append(where);
            localStringBuilder.append(')');
        }
        return localStringBuilder.toString();
    }

    private int findMatch(Uri uri) {
        int i = sURIMatcher.match(uri);
        if (i < 0)
            throw new IllegalArgumentException("Unknown uri: " + uri);
        return i;
    }

    private void init() {
        synchronized (sURIMatcher) {
            sURIMatcher.addURI(DeviceContracts.AUTHORITY, "devices", 0);
            sURIMatcher.addURI(DeviceContracts.AUTHORITY, "devices/#", 1);
            return;
        }
    }

    @Override
    public int delete(Uri paramUri, String selection, String[] selectionArgs) {
        int match = findMatch(paramUri);
        String tableName = null;
        int result = 0;
        try {
            tableName = findTableName(match);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        switch (match) {
            case DEVICES:
                result = mDatabase.delete(tableName, selection, selectionArgs);
                break;
            case DEVICES_ID:
                String id = paramUri.getPathSegments().get(1);
                result = mDatabase.delete(tableName, whereWithId(id,
                        selection), selectionArgs);
                break;
            default:
                break;
        }
        mContext.getContentResolver().notifyChange(getBaseNotificationUri
                (match), null);
        return result;
    }

    String findTableName(int match)
            throws Exception {
        int i = match >> BASE_SHIFT;
        if (i >= DeviceContracts.TABLES.length)
            throw new IllegalArgumentException("failed to get table name " + match);
        return DeviceContracts.TABLES[i];
    }

    SQLiteDatabase getDatabase(Context context) {
        return DevicesDatabaseHelper.getInstance(context).getWritableDatabase();
    }

    public String getType(Uri uri) {
        switch (findMatch(uri)) {
            case DEVICES:
                return "vnd.android.cursor.dir/devices";
            case DEVICES_ID:
                return "vnd.android.cursor.item/devices";
            default:
                return null;
        }
    }

    public Uri insert(Uri uri, ContentValues values) {
        int i = findMatch(uri);
        String tableName = null;
        try {
            tableName = findTableName(i);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        uri = ContentUris.withAppendedId(uri, mDatabase.insert(tableName, "foo", values));
        mContext.getContentResolver().notifyChange(getBaseNotificationUri(i), null, false);
        return uri;
    }

    public boolean onCreate() {
        mContext = getContext();
        mDatabase = getDatabase(mContext);
        return false;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        int match = findMatch(uri);
        String tableName = null;
        final SQLiteDatabase db = mDatabase;
        String limit = null;
        limit = uri.getQueryParameter("limit");
        try {
            tableName = findTableName(match);
        } catch (Exception localException) {
            localException.printStackTrace();
        }
        switch (match) {
            case DEVICES:
                cursor = db.query(tableName, projection, selection,
                        selectionArgs, null,
                        null, null, limit);
                break;
            case DEVICES_ID:
                String id = uri.getPathSegments().get(1);
                cursor = db.query(tableName, projection, whereWithId(id,
                        selection), selectionArgs, null, null, null, limit);
                break;
            default:
                throw new IllegalArgumentException("failed to query " + uri);
        }
        if (cursor != null) {
            cursor.setNotificationUri(mContext.getContentResolver(), uri);
        }
        return cursor;
    }

    public int update(Uri paramUri, ContentValues paramContentValues, String paramString, String[] paramArrayOfString) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}