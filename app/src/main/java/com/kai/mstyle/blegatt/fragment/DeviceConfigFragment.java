package com.kai.mstyle.blegatt.fragment;

import android.app.Fragment;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.kai.mstyle.R;
import com.kai.mstyle.blegatt.BluetoothLeService;
import com.kai.mstyle.blegatt.DefinedGattAttributes;
import com.kai.mstyle.blegatt.DeviceControlActivity;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeviceConfigFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DeviceConfigFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DeviceConfigFragment extends Fragment implements View.OnClickListener {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_device_config, container, false);
    }

    public void updateCharacteristic(BluetoothGattCharacteristic ch) {
        mCharacteristic = ch;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        mDataType = (TextView) view.findViewById(R.id.char_details_type);
        mProp = (TextView) view.findViewById(R.id.char_details_properties);
        mReadButton = (Button)view.findViewById(R.id.char_details_read_btn);
        mReadButton.setOnClickListener(this);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
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
            return;
        }
        int dataType = intent.getIntExtra(BluetoothLeService
                .EXTRA_DATA_TYPE, 0);
        String prop = intent.getStringExtra(BluetoothLeService
                .EXTRA_PROPERTY);
        mDataType.setText(DefinedGattAttributes.lookupTypeDescription(dataType));
        mProp.setText(prop);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.char_details_read_btn:
                if(mListener!= null) {
                    mListener.getService().readCharacteristic(mCharacteristic);
                }
                break;
        }
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
