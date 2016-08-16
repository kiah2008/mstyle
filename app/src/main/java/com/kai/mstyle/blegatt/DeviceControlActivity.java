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
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.kai.mstyle.R;
import com.kai.mstyle.blegatt.fragment.DeviceConfigFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity implements
        DeviceConfigFragment.OnFragmentInteractionListener {
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final String EXTRAS_MONITOR_STATE = "MONITOR_STATE";
    private static final String EXTRA_SRV_CLICKED_POSITION = "EXTRA_SRV_CLICKED_POSITION";
    private static final String EXTRA_CH_CLICKED_POSITION = "EXTRA_CH_CLICKED_POSITION";

    private static final String TAG_DEVICE_CFG = "TAG_DEVICE_CFG";
    private static final int RSSI_UPDATE_TIME_INTERVAL = 2500;
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private final String LIST_PROPTERTY = "PROPERTY";
    private TextView mConnectionState;
    private TextView mDeviceRssi;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private int mSrvPosition = -1;
    private int mChPosition = -1;
    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        mSrvPosition = groupPosition;
                        mChPosition = childPosition;
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);

                        FragmentManager fragmentManager =
                                getFragmentManager();
                        DeviceConfigFragment fragment =
                                (DeviceConfigFragment) fragmentManager
                                        .findFragmentByTag
                                                (TAG_DEVICE_CFG);
                        if (fragment != null) {
                            fragmentManager.beginTransaction().remove
                                    (fragment).commit();
                        }
                        fragment = DeviceConfigFragment.newInstance
                                (mDeviceName, mDeviceAddress,
                                        characteristic.getService()
                                                .getUuid()
                                                .toString(), characteristic
                                                .getUuid().toString());
                        fragment.updateCharacteristic(characteristic);
                        fragmentManager.beginTransaction().add(R.id
                                        .device_config_layout, fragment,
                                TAG_DEVICE_CFG).addToBackStack(null)
                                .commit();
/*                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;*/
                    }
                    return false;
                }
            };
    private DeviceConfigFragment mConfFragment;
    private CONNECT_STAT mConnected = CONNECT_STAT.STAT_DISCONNECTED;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            boolean result = mBluetoothLeService.connect(mDeviceAddress);
            if (result) {
                updateConnectionState(CONNECT_STAT.STAT_CONNECTING);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            clearUI();
        }
    };
    private Handler mHandler;
    private boolean mHasMonitorRss;
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive " + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                updateConnectionState(CONNECT_STAT.STAT_CONNECTED);
                readDeviceRssi(true);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                readDeviceRssi(false);
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());

            } else if (BluetoothLeService.ACTION_RSSI_AVAILABLE.equals
                    (action)) {
                int data = intent
                        .getIntExtra
                                (BluetoothLeService.EXTRA_DATA, 0);
                displayData(R.id.peripheral_rssi, String.valueOf((data
                        != 0 ? data : "Unknown"
                )));
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals
                    (action)) {
                if (mConfFragment != null && mConfFragment.isResumed()) {
                    mConfFragment.updateData(intent);
                }
            }
        }
    };
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_RSSI_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        updateConnectionState(CONNECT_STAT.STAT_DISCONNECTED);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.kai.mstyle.R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        Log.d(TAG, "onCreate " + mDeviceName + "/" + mDeviceAddress);

        if (savedInstanceState != null) {
            mHasMonitorRss = savedInstanceState.getBoolean
                    (EXTRAS_MONITOR_STATE);
            mSrvPosition = savedInstanceState.getInt
                    (EXTRA_SRV_CLICKED_POSITION);
            mChPosition = savedInstanceState.getInt
                    (EXTRA_CH_CLICKED_POSITION);
        }
        mHandler = new Handler();
        // Sets up UI references.
        ((TextView) findViewById(com.kai.mstyle.R.id.device_address)).setText(mDeviceAddress);
        ((TextView) findViewById(com.kai.mstyle.R.id.peripheral_name)).setText(mDeviceName);
        mGattServicesList = (ExpandableListView) findViewById(com.kai.mstyle.R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mGattServicesList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                mSrvPosition = groupPosition;
                return false;
            }
        });
        mConnectionState = (TextView) findViewById(com.kai.mstyle.R.id.connection_state);
        mDeviceRssi = (TextView) findViewById(com.kai.mstyle.R.id.peripheral_rssi);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void readDeviceRssi(boolean state) {
        mHasMonitorRss = state;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mHasMonitorRss && mBluetoothLeService != null) {
                    mBluetoothLeService.readRssi();
                    readDeviceRssi(mHasMonitorRss);
                }
            }
        }, RSSI_UPDATE_TIME_INTERVAL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            if (result) {
                updateConnectionState(CONNECT_STAT.STAT_CONNECTING);
            } else {
                clearUI();
            }
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
        }
        mConnected = CONNECT_STAT.STAT_DISCONNECTED;
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRAS_MONITOR_STATE, mHasMonitorRss);
        outState.putInt(EXTRA_CH_CLICKED_POSITION, mChPosition);
        outState.putInt(EXTRA_SRV_CLICKED_POSITION, mSrvPosition);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment != null && fragment instanceof DeviceConfigFragment) {
            mConfFragment = (DeviceConfigFragment) fragment;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.kai.mstyle.R.menu.gatt_services, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mConnected == CONNECT_STAT.STAT_CONNECTED) {
            menu.findItem(com.kai.mstyle.R.id.menu_connect).setVisible(false);
            menu.findItem(com.kai.mstyle.R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_refresh).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else if (mConnected == CONNECT_STAT.STAT_CONNECTING) {
            menu.findItem(com.kai.mstyle.R.id.menu_connect).setVisible(false);
            menu.findItem(com.kai.mstyle.R.id.menu_disconnect).setVisible
                    (true);
            menu.findItem(R.id.menu_refresh).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        } else /*NONCONNECTED*/ {
            menu.findItem(com.kai.mstyle.R.id.menu_connect).setVisible(true);
            menu.findItem(com.kai.mstyle.R.id.menu_disconnect).setVisible
                    (false);
            menu.findItem(R.id.menu_refresh).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case com.kai.mstyle.R.id.menu_connect:
                if (mBluetoothLeService.connect(mDeviceAddress)) {
                    updateConnectionState(CONNECT_STAT.STAT_CONNECTING);
                }
                return true;
            case com.kai.mstyle.R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                updateConnectionState(CONNECT_STAT.STAT_DISCONNECTED);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final CONNECT_STAT eState) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String sState = null;
                String sRssi = null;
                switch (eState) {
                    case STAT_CONNECTED:
                        sState = getString(R.string.connected);
                        mConnected = CONNECT_STAT.STAT_CONNECTED;
                        break;
                    case STAT_CONNECTING:
                        sState = getString(R.string.connecting);
                        mConnected = CONNECT_STAT.STAT_CONNECTING;
                        sRssi = sState;
                        break;
                    case STAT_DISCONNECTED:
                        mConnected = CONNECT_STAT.STAT_DISCONNECTED;
                        sState = getString(R.string.disconnected);
                        sRssi = sState;
                        break;
                }
                invalidateOptionsMenu();
                if (sRssi != null) {
                    displayData(mDeviceRssi, sRssi);
                }
                mConnectionState.setText(sState);
                if (mConfFragment != null && mConfFragment.isResumed()) {
                    mConfFragment.updateConnectState(eState);
                }

            }
        });
    }

    private void displayData(int resId, String data) {
        if (data != null) {
            ((TextView) findViewById(resId)).setText(data);
        }
    }

    private void displayData(View view, String data) {
        if (data != null && view instanceof TextView) {
            mDeviceRssi.setText(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(com.kai.mstyle.R.string.unknown_service);
        String unknownCharaString = getResources().getString(com.kai.mstyle.R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, DefinedGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, DefinedGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);

                currentCharaData.put(LIST_PROPTERTY, BtUtils
                        .matchProperty(gattCharacteristic
                                .getProperties()));
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID, LIST_PROPTERTY},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                R.layout.simple_expandable_list_item2,
                new String[]{LIST_NAME, LIST_UUID, LIST_PROPTERTY},
                new int[]{R.id.ch_name, R.id.ch_uuid, R.id.ch_prop}
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
        if (mSrvPosition != -1) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mGattServicesList.expandGroup(mSrvPosition);
                    if (mChPosition != -1) {
                        DeviceConfigFragment fragment = (DeviceConfigFragment) getFragmentManager
                                ().findFragmentByTag(TAG_DEVICE_CFG);
                        final BluetoothGattCharacteristic ch = mGattCharacteristics.get(mSrvPosition)
                                .get(mChPosition);
                        if (fragment != null && TextUtils.equals(ch.getUuid
                                ().toString(), fragment.getChUuid())) {
                            fragment.updateCharacteristic
                                    (ch);
                        }
                    }
                }
            });
        }

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public BluetoothLeService getService() {
        return mBluetoothLeService;
    }

    public enum CONNECT_STAT {
        STAT_DISCONNECTED,
        STAT_CONNECTING,
        STAT_CONNECTED
    }
}
