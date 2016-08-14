/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kai.mstyle.blegatt;

import android.app.Activity;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.kai.mstyle.R;
import com.kai.mstyle.provider.DeviceContent;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemLongClickListener, ActionMode.Callback {

    private static final String TAG = DeviceScanActivity.class.getSimpleName();
    private static final int LOADER_DEVICERS = 0;
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private SimpleCursorAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private ListView mListView;
    private HashMap<String, SignalStrength.SIGNAL_LEVEL> mDeviceRssiLevels = new
            HashMap<>(10);
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("MM/dd HH:mm");
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Device found " + device.getAddress
                                    () + "/" + device.getName() + "/rssi: "
                                    + rssi);
                            SignalStrength.SIGNAL_LEVEL level =
                                    SignalStrength.getLevel(rssi);
                            if (!mDeviceRssiLevels.containsKey(device.getAddress
                                    ()) || (mDeviceRssiLevels.get(device.getAddress())
                                    != level)) {
                                mDeviceRssiLevels.put(device.getAddress(),
                                        level);
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            } else {
                                return;
                            }
                            if (scanRecord != null && scanRecord.length > 0) {
                                Log.d(TAG, "Device found records" +
                                        BtUtils.dumpBytes(scanRecord));
                            }
                            checkContent(device);
                        }
                    });
                }
            };
    private ActionMode mCabMode;

    boolean checkContent(BluetoothDevice device) {
        Cursor c = mLeDeviceListAdapter.getCursor();
        String address = device.getAddress();
        if (TextUtils.isEmpty(address)) {
            return false;
        }
        if (c != null) {
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                if (device.getAddress().equals(c.getString(c.getColumnIndex(DeviceContent
                        .DeviceColumns.ADDRESS)))) {
                    Log.d(TAG, "checkContent with dup " + device.getName());
                    return false;
                }
            }
        } else {
            Log.d(TAG, "checkContent witch empty content, insert!");
        }
        ContentValues cv = new ContentValues();
        cv.put(DeviceContent.DeviceColumns.NAME, device.getName());
        cv.put(DeviceContent.DeviceColumns.ADDRESS, device.getAddress());
        cv.put(DeviceContent.DeviceColumns.ADD_TIME, System
                .currentTimeMillis());
        getContentResolver().insert(DeviceContent.CONTENT_URI, cv);
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mLeDeviceListAdapter = new SimpleCursorAdapter
                (DeviceScanActivity.this, R.layout.listitem_device, null,
                        new String[]{DeviceContent.DeviceColumns.NAME,
                                DeviceContent.DeviceColumns.ADDRESS},
                        new int[]{R.id.device_name, R.id.device_address}) {
            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                super.bindView(view, context, cursor);
                BleDevice device = new BleDevice(cursor);
                view.setTag(device);
                if (device.getAddedTime() > 0) {
                    Calendar clendar = Calendar.getInstance();
                    clendar.setTimeInMillis(device.getAddedTime());
                    ((TextView) view.findViewById(R.id.add_date))
                            .setText(mDateFormat.format(clendar
                                    .getTime()).toString());
                }
                SignalStrength.SIGNAL_LEVEL level = mDeviceRssiLevels.get
                        (device.getAddress());
                if (level == null) {
                    return;
                }
                ImageView image = (ImageView) view.findViewById(R.id
                        .signal_level);
                switch (level) {
                    case SIGNAL_GREAT:
                        image.setImageResource(R.drawable.ic_qs_signal_4);
                        break;
                    case SIGNAL_GOOD:
                        image.setImageResource(R.drawable.ic_qs_signal_3);
                        break;
                    case SIGNAL_POOR:
                        image.setImageResource(R.drawable.ic_qs_signal_2);
                        break;
//                    case SIGNAL_GREAT:
//                        image.setImageResource(R.drawable.ic_qs_signal_1);
//                        break;
                    case SIGNAL_NONE:
                    default:
                        image.setImageResource(R.drawable.ic_qs_signal_0);
                        break;
                }
                device.setRssiLevel(level);
            }
        };
        setListAdapter(mLeDeviceListAdapter);
        getLoaderManager().initLoader(LOADER_DEVICERS, Bundle.EMPTY, this);
        mListView = getListView();
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mListView.setOnItemLongClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
//        mLeDeviceListAdapter = new LeDeviceListAdapter();
//        setListAdapter(mLeDeviceListAdapter);
//        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (mCabMode != null) {
            return;
        }
        BleDevice device = (BleDevice) v.getTag();
        if (device == null) {
            Log.e(TAG, "itemClicked empty");
            return;
        }
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "onCreateLoader " + id);
        return new CursorLoader(this, DeviceContent.CONTENT_URI,
                DeviceContent.COLUMNS,
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null) {
            Log.e(TAG, "Empty content");
            return;
        }
        Log.d(TAG, "onLoadFinished " + loader.getId() + "/" + data.getCount());
        switch (loader.getId()) {
            case LOADER_DEVICERS:
                mLeDeviceListAdapter.swapCursor(data);
                break;
            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mLeDeviceListAdapter != null) {
            mLeDeviceListAdapter.swapCursor(null);
        }

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mCabMode != null)
            return false;
        mListView.setItemChecked(position, true);
        mCabMode = startActionMode(this);
        return true;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        boolean handled = true;
        switch (item.getItemId()) {
            case R.id.menu_delete:
                int selPosition = mListView.getCheckedItemPosition();
                View view = mListView.getChildAt(selPosition);
                BleDevice device = (BleDevice) view.getTag();
                Log.d(TAG, "action Delete " + selPosition + "/" + device
                        .getId());
                int result = getContentResolver().delete(ContentUris
                        .withAppendedId(DeviceContent
                                .CONTENT_URI, device
                                .getId()), null, null);
                if (result > 0) {
                    mDeviceRssiLevels.remove(device.getAddress());
                    Toast.makeText(this, device.getName() + (" ") + getString
                                    (R.string.del_success),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.menu_update:
                break;
        }
        if (handled) {
            mode.finish();
        }
        return handled;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        Log.d(TAG, "destroy actionMode ");
        mCabMode = null;

    }
    /*
    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    // Adapter for holding devices found through scanning.

    private class LeDeviceListAdapter extends CursorAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }
         public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return null;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

        }
    }*/

}