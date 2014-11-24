package com.iffu.beacon.goplusbeaconconnect;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.iffu.beacon.sdk.connectionle.BluetoothLeService;

public class ModifyActivity extends Activity implements OnClickListener {
	private static final String TAG = "ModifyActivity";
	private BluetoothLeService mBluetoothLeService;
	private BluetoothDevice mDevice;
	EditText editUUID, editMajor, editMinor, editMeasured, editInterval, editTxPower;
	Button buttonUUID, buttonMajor, buttonMinor, buttonMeasured, buttonInterval, buttonTxPower;
	
	
	// Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.setActivityHandler(mHandler);
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final Bundle data = msg.getData();
            
            if(data == null)
            {
            	Log.e(TAG, "Message Data from BluetoothLeService is Null");
            	return;
            }
            
            switch (data.getInt("SERVICE_STATUS")) {
            	case BluetoothLeService.ACTION_GATT_CONNECTED:
            		runOnUiThread(new Runnable() {
            			public void run() {
            				final BluetoothDevice connectDev = data.getParcelable(BluetoothDevice.EXTRA_DEVICE);
            				displayCurrent("GATT Connected");
                    	}
            		});
                break;
            	case BluetoothLeService.ACTION_GATT_DISCONNECTED:
            		runOnUiThread(new Runnable() {
            			public void run() {
            				final BluetoothDevice disconnectDev = data.getParcelable(BluetoothDevice.EXTRA_DEVICE);
            				displayCurrent("GATT Disconnected");
                    	}
            		});
                break;

            	case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
            		final BluetoothDevice currentDevice = data.getParcelable(BluetoothDevice.EXTRA_DEVICE);
            		Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED Message : " + currentDevice.getAddress());
            		displayCurrent("Read Characeristic...");
            		mBluetoothLeService.readUUID();
                break;
            	case BluetoothLeService.ACTION_UUID_READ:
            		runOnUiThread(new Runnable() {
            			public void run() {
            				editUUID.setText(String.valueOf(data.getString("EXTRA_DATA")));
            				mBluetoothLeService.readMajor();
                    	}
            		});
            		break;
            	case BluetoothLeService.ACTION_MAJOR_READ:
            		runOnUiThread(new Runnable() {
            			public void run() {
            				editMajor.setText(String.valueOf(data.getInt("EXTRA_DATA")));
            				mBluetoothLeService.readMinor();
                    	}
            		});
            		break;
            	case BluetoothLeService.ACTION_MINOR_READ:
            		runOnUiThread(new Runnable() {
            			public void run() {
            				editMinor.setText(String.valueOf(data.getInt("EXTRA_DATA")));
            				mBluetoothLeService.readMeasuredPower();
                    	}
            		});
            		break;
            	case BluetoothLeService.ACTION_MEASURED_READ:
            		runOnUiThread(new Runnable() {
            			public void run() {
            				editMeasured.setText(String.valueOf(data.getInt("EXTRA_DATA")));
            				mBluetoothLeService.readAdvertisingInterval();
                    	}
            		});
            		break;
            	case BluetoothLeService.ACTION_ADV_INTERVAL_READ:
            		runOnUiThread(new Runnable() {
            			public void run() {
            				editInterval.setText(String.valueOf(data.getInt("EXTRA_DATA")));
            				mBluetoothLeService.readTxPower();
                    	}
            		});
            		break;
            	case BluetoothLeService.ACTION_TXPOWER_READ:
            		runOnUiThread(new Runnable() {
            			public void run() {
            				editTxPower.setText(String.valueOf(data.getInt("EXTRA_DATA")));
            				displayCurrent("Read completed");
                    	}
            		});
            		break;
            	case BluetoothLeService.ACTION_WRITE_VALUE:
            		runOnUiThread(new Runnable() {
            			public void run() {
            				if(data.getInt("EXTRA_DATA") == 0)
            					displayCurrent("Write completed");
            				else
            					displayCurrent("Write failed");
                    	}
            		});
            	default:
            		super.handleMessage(msg);
            }
        }
    };
    
    
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_modify);
	    
        mDevice = getIntent().getParcelableExtra("device");
        initView();
        Intent bindIntent = new Intent(ModifyActivity.this, BluetoothLeService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
    public void onDestroy() {
		unbindService(mServiceConnection);
		
	    Log.d(TAG, "onDestroy");
		super.onDestroy();
		
    }
	public void initView()
	{
		editUUID = (EditText) findViewById(R.id.edit_UUID);
		editMajor = (EditText) findViewById(R.id.edit_major);
		editMinor = (EditText) findViewById(R.id.edit_minor);
		editMeasured = (EditText) findViewById(R.id.edit_measured);
		editInterval = (EditText) findViewById(R.id.edit_interval);
		editTxPower = (EditText) findViewById(R.id.edit_txpower);
		buttonUUID = (Button) findViewById(R.id.bt_writeUUID);
		buttonMajor = (Button) findViewById(R.id.bt_writeMajor);
		buttonMinor = (Button) findViewById(R.id.bt_writeMinor);
		buttonMeasured = (Button) findViewById(R.id.bt_writeMeasured);
		buttonInterval = (Button) findViewById(R.id.bt_writeInterval);
		buttonTxPower = (Button) findViewById(R.id.bt_writeTxPower);
		
		buttonUUID.setOnClickListener(this);
		buttonMajor.setOnClickListener(this);
		buttonMinor.setOnClickListener(this);
		buttonMeasured.setOnClickListener(this);
		buttonInterval.setOnClickListener(this);
		buttonTxPower.setOnClickListener(this);
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		displayCurrent("Writing request...");
		if(arg0.getId() == R.id.bt_writeUUID)
		{
			
			if(editUUID.getText().toString().equals(""))
			{
				displayCurrent("Write an invalid value");
				return;
			}
			mBluetoothLeService.writeUUID(mDevice, editUUID.getText().toString());
		}
		else if(arg0.getId() == R.id.bt_writeMajor)
		{
			
			if(editMajor.getText().toString().equals(""))
			{
				displayCurrent("Write an invalid value");
				return;
			}
			mBluetoothLeService.writeMajor(mDevice, Integer.parseInt(editMajor.getText().toString()));
		}
		else if(arg0.getId() == R.id.bt_writeMinor)
		{
			if(editMinor.getText().toString().equals(""))
			{
				displayCurrent("Write an invalid value");
				return;
			}
			mBluetoothLeService.writeMinor(mDevice, Integer.parseInt(editMinor.getText().toString()));
		}
		else if(arg0.getId() == R.id.bt_writeMeasured)
		{
			if(editMeasured.getText().toString().equals(""))
			{
				displayCurrent("Write an invalid value");
				return;
			}
			mBluetoothLeService.writeMeasuredPower(mDevice, Integer.parseInt(editMeasured.getText().toString()));
		}
		else if(arg0.getId() == R.id.bt_writeInterval)
		{
			if(editInterval.getText().toString().equals(""))
			{
				displayCurrent("Write an invalid value");
				return;
			}
			mBluetoothLeService.writeAdvertisingInterval(mDevice, Integer.parseInt(editInterval.getText().toString()));
		}
		else if(arg0.getId() == R.id.bt_writeTxPower)
		{
			if(editTxPower.getText().toString().equals(""))
			{
				displayCurrent("Write an invalid value");
				return;
			}
			mBluetoothLeService.writeTxPower(mDevice, Integer.parseInt(editTxPower.getText().toString()));
		}
	}

	public void displayCurrent(String status)
	{
		TextView statusTxt = (TextView) findViewById(R.id.txt_staus);
		statusTxt.setText(status);
	}
}
