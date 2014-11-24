package com.iffu.beacon.goplusbeaconconnect;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ScanActivity extends Activity implements OnClickListener {
	private static final String TAG = "MainActivity";
	private static final long SCAN_PERIOD = 5000L;
	private static final long WAIT_PERIOD = 1000L;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBtAdapter;
	private Handler mHandler, mHandlerWait;
	private Runnable mRunnable, mRunnableWait;
	private boolean isScanning = false;
	private DeviceAdapter deviceAdapter;
	private boolean mResetAdapter = false;
	List<BluetoothDevice> deviceList;
	Button button;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            finish();
        }
        mBtAdapter = mBluetoothManager.getAdapter();
        deviceList = new ArrayList<BluetoothDevice>();
        deviceAdapter = new DeviceAdapter(this, deviceList);
        
        ListView lv_device = (ListView) findViewById(R.id.lv_devlist);
        lv_device.setAdapter(deviceAdapter);
        lv_device.setOnItemClickListener(mDeviceClickListener);
        
        button = (Button) findViewById(R.id.bt_scan);
        button.setOnClickListener(this);
        mHandler = new Handler();
        mHandlerWait = new Handler();
        IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(bluetoothStatusReceiver, filter);
	}

	@Override
    public void onDestroy() {
		if(mHandler != null)
        	mHandler = null;
		unregisterReceiver(bluetoothStatusReceiver);
		super.onDestroy();
    }
	
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		if(arg0.getId() == R.id.bt_scan)
		{
			deviceList.clear();
			deviceAdapter.notifyDataSetChanged();
			scanLeDevice(true, true);
			button.setEnabled(false);
			
		}
	}

	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        	if(isScanning)
        	{
        		scanLeDevice(false, true);
        		button.setEnabled(true);
        	}
        	
            BluetoothDevice device = deviceList.get(position);
            Intent intent = new Intent(ScanActivity.this, ModifyActivity.class);
            intent.putExtra("device", device);
			startActivity(intent);
        }
    };
	
	// Scan LE Device
	private void scanLeDevice(boolean enable, boolean reset) {
        if (enable) {
        	if(reset)	//Bluetooth Adapter Reset for Android system
        	{
        		mResetAdapter = true;
        		if(mBtAdapter.isEnabled())
        			mBtAdapter.disable();
        		else
        			mBtAdapter.enable();
        	}
        	else
        	{
        		// Call from Bluetooth status receiver when adapter reset 
        		mRunnable = new Runnable() {
                	@Override
                    public void run() {
                        mBtAdapter.stopLeScan(mLeScanCallback);
                        button.setEnabled(true);
                        isScanning = false;
                    }
                };
                mHandler.postDelayed(mRunnable,SCAN_PERIOD);
                isScanning = true;
                mBtAdapter.startLeScan(mLeScanCallback);
        	}
        	
        } else {
        	mResetAdapter = false;
        	isScanning = false;
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
        
    }
	
	private class DeviceAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> devices;
        LayoutInflater inflater;

        public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.devices = devices;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) inflater.inflate(R.layout.device_item, null);
            }

            BluetoothDevice device = devices.get(position);
            TextView name = ((TextView) vg.findViewById(R.id.devname));
            TextView addr = ((TextView) vg.findViewById(R.id.devaddr));

            name.setText(device.getName());
            addr.setText(device.getAddress());

            return vg;
        }
    }
    
	// Add LE device
	private void addDevice(BluetoothDevice device) {
        boolean alreadyFounded = false;
        String deviceName = device.getName();

        if(device.getName() == null || device.getName() == "" || !deviceName.startsWith("GoPlus")) return;
        
        for (BluetoothDevice listDev : deviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
            	alreadyFounded = true;
                break;
            }
        }
        if (!alreadyFounded) {
            deviceList.add(device);
            deviceAdapter.notifyDataSetChanged();
        }
    }
	
    // Bluetooth LE scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addDevice(device);
                }
            });
        }
    };
    
    // Bluetooth Status Receiver
    private final BroadcastReceiver bluetoothStatusReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                Log.d(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED" + "state is" + state);
                if(state == BluetoothAdapter.STATE_OFF) {
                	if(mResetAdapter)
                	{
                		mBtAdapter.enable();
                	}
                }
                else if(state == BluetoothAdapter.STATE_ON) {
                	if(mResetAdapter)
                	{
                		mResetAdapter = false;
                		mRunnableWait = new Runnable() {
                        	@Override
                            public void run() {
                        		scanLeDevice(true, false);
                            }
                        };
                        mHandlerWait.postDelayed(mRunnableWait,WAIT_PERIOD);
                	}
                }
            }
        }
    };
}
