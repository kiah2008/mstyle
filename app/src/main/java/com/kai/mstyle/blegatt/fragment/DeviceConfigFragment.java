package com.kai.mstyle.blegatt.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.kai.mstyle.R;
import com.kai.mstyle.blegatt.BluetoothLeService;
import com.kai.mstyle.blegatt.BtUtils;
import com.kai.mstyle.blegatt.DefinedGattAttributes;
import com.kai.mstyle.blegatt.DeviceControlActivity;

import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeviceConfigFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DeviceConfigFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DeviceConfigFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = DeviceConfigFragment.class
            .getSimpleName();
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_DEV_NAME = "param1";
    private static final String ARG_UUID = "param2";
    private static final String ARG_SRV_UUID = "param3";
    private static final String ARG_DEV_ADDRESS = "param4";

    private String mDevName;
    private String mDevAddress;
    private String mUUID;
    private String mSrvUUID;
    private DeviceControlActivity.CONNECT_STAT mConState = DeviceControlActivity.CONNECT_STAT.STAT_DISCONNECTED;
    private TextView mDataType;
    private TextView mProp;
    private Button mReadButton;
    private BluetoothGattCharacteristic mCharacteristic;
    private OnFragmentInteractionListener mListener;
    private ProgressBar mProgress;
    private TextView mDataString;
    private TextView mDataHex;
    private TextView mDataDecimal;
    private Button mDataWriteBt;
    private EditText mDataInput;
    private ToggleButton mNotification;

    public DeviceConfigFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param devName Parameter 1.
     * @param uuid    Parameter 2.
     * @return A new instance of fragment DeviceConfigFragment.
     */
    public static DeviceConfigFragment newInstance(String devName, String
            devAddress, String srvUuid, String uuid) {
        DeviceConfigFragment fragment = new DeviceConfigFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEV_NAME, devName);
        args.putString(ARG_SRV_UUID, srvUuid);
        args.putString(ARG_UUID, uuid);
        args.putString(ARG_DEV_ADDRESS, devAddress);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Bundle args = getArguments();
            mDevName = args.getString(ARG_DEV_NAME);
            mDevAddress = args.getString(ARG_DEV_ADDRESS);
            mSrvUUID = getArguments().getString(ARG_SRV_UUID);
            mUUID = getArguments().getString(ARG_UUID);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_device_config, container, false);
    }

    public void updateCharacteristic(BluetoothGattCharacteristic ch) {
        mCharacteristic = ch;
        readCharacters();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mNotification = (ToggleButton) view.findViewById(R.id
                .char_details_notification_switcher);
        mDataInput = (EditText) view.findViewById(R.id.char_details_hex_value);
        ((TextView) view.findViewById(R.id.char_details_peripheral_name))
                .setText(mDevName);
        ((TextView) view.findViewById(R.id.char_details_peripheral_address))
                .setText(mDevAddress);

        ((TextView) view.findViewById(R.id.char_details_service))
                .setText(DefinedGattAttributes.lookup(mSrvUUID));
        ((TextView) view.findViewById(R.id.char_details_service_uuid))
                .setText(mSrvUUID);

        ((TextView) view.findViewById(R.id.char_details_name))
                .setText(DefinedGattAttributes.lookup(mUUID));
        ((TextView) view.findViewById(R.id.char_details_uuid))
                .setText(mUUID);
        mDataString = (TextView) view.findViewById(R.id.char_details_ascii_value);
        mDataHex = (TextView) view.findViewById(R.id.details_hex_value);
        mDataDecimal = (TextView) view.findViewById(R.id.char_details_decimal_value);
        mDataType = (TextView) view.findViewById(R.id.char_details_type);
        mProp = (TextView) view.findViewById(R.id.char_details_properties);
        mReadButton = (Button) view.findViewById(R.id.char_details_read_btn);
        mDataWriteBt = (Button) view.findViewById(R.id.char_details_write_btn);
        mDataWriteBt.setOnClickListener(this);
        ;
        mReadButton.setOnClickListener(this);
        mProgress = (ProgressBar) getActivity().findViewById(R.id
                .cfg_waiting);
        if (mCharacteristic == null) {
            mProgress.setVisibility(View.VISIBLE);
        } else {
            //read characteristics
            readCharacters();

        }
    }

    private void readCharacters() {
        BluetoothLeService service = null;
        if (mListener != null && mCharacteristic != null) {
            service = mListener.getService();
            if (service != null) {
                service.readCharacteristic(mCharacteristic);
            }
            if (mProgress != null) {
                mProgress.setVisibility(View.GONE);
            }
        }
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void updateConnectState(DeviceControlActivity.CONNECT_STAT state) {
        if (mConState == state) {
            return;
        }
        mConState = state;
        switch (mConState) {
            case STAT_CONNECTED:
                break;
            case STAT_CONNECTING:
                break;
            case STAT_DISCONNECTED:
                break;
        }
    }

    public void updateData(Intent intent) {
        String uuid = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
        if (!TextUtils.equals(uuid, mUUID)) {
            Log.d(TAG, "updateData not right UUID " + uuid + "/" + mUUID);
            return;
        }
        int dataType = intent.getIntExtra(BluetoothLeService
                .EXTRA_DATA_TYPE, 0);
        int prop = intent.getIntExtra(BluetoothLeService
                .EXTRA_PROPERTY, -1);
        mReadButton.setEnabled((prop & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
        mDataWriteBt.setEnabled((prop & BluetoothGattCharacteristic
                .PROPERTY_WRITE) != 0);
        mNotification.setEnabled((prop & BluetoothGattCharacteristic
                .PROPERTY_NOTIFY) != 0);
        mDataType.setText(DefinedGattAttributes.lookupTypeDescription(dataType));
        mProp.setText(BtUtils.getPropertyDescription(prop));
        byte[] datas = intent.getByteArrayExtra(BluetoothLeService
                .EXTRA_DATA);
        StringBuilder hexBuilder = new StringBuilder(datas.length);
        StringBuilder strBuilder = new StringBuilder(datas.length);
        for (byte data : datas) {
            hexBuilder.append(String.format("%02X", data));
            if (data > 0 && data < 128)
                strBuilder.append(String.format("%c", data));
        }
        mDataHex.setText("0x" + hexBuilder.toString());
        mDataInput.setText("0x" + hexBuilder.toString());

        mDataString.setText(strBuilder.toString());
        //TODO: use format to get the real value
        int intValue = 0;
        if (datas.length > 0) intValue = (int) datas[0];
        if (datas.length > 1) intValue = intValue + ((int) datas[1] << 8);
        if (datas.length > 2) intValue = intValue + ((int) datas[2] << 8);
        if (datas.length > 3) intValue = intValue + ((int) datas[3] << 8);
        mDataDecimal.setText(String.valueOf(intValue));
    }

    @Override
    public void onClick(View v) {
        boolean result = false;
        switch (v.getId()) {
            case R.id.char_details_read_btn:
                if (mListener != null && mCharacteristic != null) {
                    Log.d(TAG, "readCharacteristic " + mUUID);
                    BluetoothLeService service = mListener.getService();
                    if (service != null) {
                        result = service.
                                readCharacteristic
                                        (mCharacteristic);
                    }
                }
                break;
            case R.id.char_details_write_btn:
                String newValue = mDataInput.getText().toString().toLowerCase
                        (Locale
                                .ENGLISH);
                byte[] dataToWrite = BtUtils.parseHexStringToBytes(newValue);
                BluetoothLeService service = mListener.getService();
                if (service != null) {
                    service.writeDataToCharacteristic(mCharacteristic, dataToWrite);
                }
                break;
        }
    }

    public String getChUuid() {
        return mUUID;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);

        BluetoothLeService getService();
    }

}
