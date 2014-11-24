package com.iffu.beacon.sdk.connectionle;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service
{
	private static final String TAG = BluetoothLeService.class.getSimpleName();
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;
	private BluetoothGattServer mGattServer;
	private int mConnectionState = 0;
	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;
	public static final int ACTION_GATT_CONNECTED = 21;
	public static final int ACTION_GATT_DISCONNECTED = 22;
	public static final int ACTION_GATT_SERVICES_DISCOVERED = 23;
	public static final int ACTION_UUID_READ = 24;
	public static final int ACTION_MAJOR_READ = 25;
	public static final int ACTION_MINOR_READ = 26;
	public static final int ACTION_MEASURED_READ = 27;
	public static final int ACTION_ADV_INTERVAL_READ = 28;
	public static final int ACTION_TXPOWER_READ = 29;
	public static final int ACTION_BATT_READ_VALUE = 30;
	public static final int ACTION_RSSI_VALUE = 31;
	public static final int ACTION_WRITE_VALUE = 32;
	public static final int ACTION_READ_FAIL = -1;
	public static final String EXTRA_DATA = "EXTRA_DATA";
	public static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
	public static final String SERVICE_STATUS = "SERVICE_STATUS";
	public static final UUID GOPLUS_SERVICE = UUID.fromString("2A588020-4FB2-40F5-8204-85315DEF11C5");
	public static final UUID BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
	public static final UUID DIS_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
	public static final UUID GOPLUS_MAJOR = UUID.fromString("2A588021-4FB2-40F5-8204-85315DEF11C5");
	public static final UUID GOPLUS_MINOR = UUID.fromString("2A588022-4FB2-40F5-8204-85315DEF11C5");
	public static final UUID GOPLUS_MEASUERED_POWER = UUID.fromString("2A588023-4FB2-40F5-8204-85315DEF11C5");
	public static final UUID GOPLUS_ADV_INTERVAL = UUID.fromString("2A588024-4FB2-40F5-8204-85315DEF11C5");
	public static final UUID GOPLUS_TXPOWER = UUID.fromString("2A588025-4FB2-40F5-8204-85315DEF11C5");
	public static final UUID GOPLUS_UUID = UUID.fromString("2A588026-4FB2-40F5-8204-85315DEF11C5");
	public static final UUID BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
	public static final UUID SYSTEM_ID = UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb");
	public static final UUID MODEL_NUMBER = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
	public static final UUID SERIAL_NUMBER = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
	public static final UUID HARDWARE_REVISION = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
	public static final UUID FIRMWARE_REVISION = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
	public static final UUID SOFTWARE_REVISION = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");
	public static final UUID MANUFACTURER_NAME = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");

	private Handler mActivityHandler = null;

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
    	@Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
            	mConnectionState = STATE_CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                
                final Intent intent = new Intent("Proximity Service Noti");
                intent.putExtra(SERVICE_STATUS,ACTION_GATT_CONNECTED);
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
                sendBroadcast(intent);
                // Attempts to discover services after successful connection.
               	Log.i(TAG, "Attempting to start service discovery!!" + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                final Intent intent = new Intent("Proximity Service Noti");
                intent.putExtra(SERVICE_STATUS,ACTION_GATT_DISCONNECTED);
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
                sendBroadcast(intent);
            } 
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Bundle mBundle = new Bundle();
            	Message msg = Message.obtain(mActivityHandler);
            	mBundle.putInt(SERVICE_STATUS, ACTION_GATT_SERVICES_DISCOVERED);
                mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
                msg.setData(mBundle);
                msg.sendToTarget();
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        	if (status == BluetoothGatt.GATT_SUCCESS) {
		        if (BluetoothLeService.GOPLUS_MAJOR.equals(characteristic.getUuid()))
		        {
		        	Bundle mBundle = new Bundle();
		        	Message msg = Message.obtain(mActivityHandler);
		        	int intLevel =  byteToShort(characteristic.getValue(), ByteOrder.BIG_ENDIAN);
		        	Log.d(TAG, String.format("Received Major Id: %d", intLevel));
		        	mBundle.putInt(SERVICE_STATUS, ACTION_MAJOR_READ);
		        	mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
		        	mBundle.putInt(EXTRA_DATA, intLevel);
		        	msg.setData(mBundle);
		        	msg.sendToTarget();
		        } 
		        else if (BluetoothLeService.GOPLUS_MINOR.equals(characteristic.getUuid()))
		        {
		        	Bundle mBundle = new Bundle();
		        	Message msg = Message.obtain(mActivityHandler);
		        	int intLevel =  byteToShort(characteristic.getValue(), ByteOrder.BIG_ENDIAN);
		        	Log.d(TAG, String.format("Received Minor Id: %d", intLevel));
		        	mBundle.putInt(SERVICE_STATUS, ACTION_MINOR_READ);
		        	mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
		        	mBundle.putInt(EXTRA_DATA, intLevel);
		        	msg.setData(mBundle);
		        	msg.sendToTarget();
		        } 
		        else if (BluetoothLeService.GOPLUS_MEASUERED_POWER.equals(characteristic.getUuid()))
		        {
		        	Bundle mBundle = new Bundle();
		        	Message msg = Message.obtain(mActivityHandler);
		        	int intLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8 , 0);
		        	Log.d(TAG, String.format("Received Meaused Power: %d", intLevel));
		        	mBundle.putInt(SERVICE_STATUS, ACTION_MEASURED_READ);
		        	mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
		        	mBundle.putInt(EXTRA_DATA, intLevel);
		        	msg.setData(mBundle);
		        	msg.sendToTarget();
		        } 
		        else if (BluetoothLeService.GOPLUS_ADV_INTERVAL.equals(characteristic.getUuid()))
		        {
		        	Bundle mBundle = new Bundle();
		        	Message msg = Message.obtain(mActivityHandler);
		        	int intLevel =  byteToShort(characteristic.getValue(), ByteOrder.BIG_ENDIAN);
		        	Log.d(TAG, String.format("Received Advertising Interval: %d", intLevel));
		        	mBundle.putInt(SERVICE_STATUS, ACTION_ADV_INTERVAL_READ);
		        	mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
		        	mBundle.putInt(EXTRA_DATA, intLevel);
		        	msg.setData(mBundle);
		        	msg.sendToTarget();
		        } 
		        else if (BluetoothLeService.GOPLUS_TXPOWER.equals(characteristic.getUuid()))
		        {
		        	Bundle mBundle = new Bundle();
		        	Message msg = Message.obtain(mActivityHandler);
		        	int intLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8 , 0);
		        	Log.d(TAG, String.format("Received Tx Power: %d", intLevel));
		        	mBundle.putInt(SERVICE_STATUS, ACTION_TXPOWER_READ);
		        	mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
		        	mBundle.putInt(EXTRA_DATA, intLevel);
		        	msg.setData(mBundle);
		        	msg.sendToTarget();
		        } 
		        else if (BluetoothLeService.BATTERY_LEVEL.equals(characteristic.getUuid()))
		        {
		        	Bundle mBundle = new Bundle();
		        	Message msg = Message.obtain(BluetoothLeService.this.mActivityHandler);
		        	int intLevel =  byteToShort(characteristic.getValue(), ByteOrder.BIG_ENDIAN);
		        	Log.d(TAG, String.format("Received Battery Level: %d", intLevel));
		        	mBundle.putInt(SERVICE_STATUS, ACTION_BATT_READ_VALUE);
		        	mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
		        	mBundle.putInt(EXTRA_DATA, intLevel);
		        	msg.setData(mBundle);
		        	msg.sendToTarget();
		        }
		        else if (BluetoothLeService.GOPLUS_UUID.equals(characteristic.getUuid()))
		        {
		        	Bundle mBundle = new Bundle();
		        	Message msg = Message.obtain(mActivityHandler);
		        	String strUUID = byteArrayToHex(characteristic.getValue());
		        	Log.d(TAG, String.format("Received Tx Power: %s", strUUID));
		        	mBundle.putInt(SERVICE_STATUS, ACTION_UUID_READ);
		        	mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
		        	mBundle.putString(EXTRA_DATA, strUUID);
		        	msg.setData(mBundle);
		        	msg.sendToTarget();
		        } 
		        
        	}
        	else
        	{
        		if (BATTERY_LEVEL.equals(characteristic.getUuid())) {
	            	Bundle mBundle = new Bundle();
	            	Message msg = Message.obtain(mActivityHandler);
	            	mBundle.putInt(SERVICE_STATUS, ACTION_BATT_READ_VALUE);
	                mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
	                mBundle.putInt(EXTRA_DATA, -1);
	                msg.setData(mBundle);
	                msg.sendToTarget();
            	}
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
           	if (BATTERY_LEVEL.equals(characteristic.getUuid())) {
        		byte byteLevel[];
            	int intLevel;

                Bundle mBundle = new Bundle();
                Message msg = Message.obtain(mActivityHandler);
                byteLevel = characteristic.getValue();
                intLevel = byteLevel[0] & 0xFF;
                Log.d(TAG, String.format("Received Battery Level: %d", intLevel));
                mBundle.putInt(SERVICE_STATUS, ACTION_BATT_READ_VALUE);
                mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
                mBundle.putInt(EXTRA_DATA, intLevel);
                msg.setData(mBundle);
                msg.sendToTarget();
           	} 
            else
            {
            	if (BATTERY_LEVEL.equals(characteristic.getUuid())) {
	            	Bundle mBundle = new Bundle();
	            	Message msg = Message.obtain(mActivityHandler);
	            	mBundle.putInt(SERVICE_STATUS, ACTION_BATT_READ_VALUE);
	                mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
	                mBundle.putInt(EXTRA_DATA, -1);
	                msg.setData(mBundle);
	                msg.sendToTarget();
            	}
            }
        }
        
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
        	Log.i(TAG, "onCharacteristicWrite:" + characteristic.getUuid());
        	Bundle mBundle = new Bundle();
        	Message msg = Message.obtain(mActivityHandler);
        	mBundle.putInt(SERVICE_STATUS, ACTION_WRITE_VALUE);
        	mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
        	if (status == 0)
        		mBundle.putInt(EXTRA_DATA, 0);
        	else
        		mBundle.putInt(EXTRA_DATA, -1);
        	msg.setData(mBundle);
        	msg.sendToTarget();
        }

        @Override
        public void  onReadRemoteRssi (BluetoothGatt gatt, int rssi, int status)
        {
        	Bundle mBundle = new Bundle();
        	if(status == BluetoothGatt.GATT_SUCCESS)
        	{
                Message msg = Message.obtain(mActivityHandler);
                mBundle.putInt(SERVICE_STATUS, ACTION_RSSI_VALUE);
                mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, gatt.getDevice());
                mBundle.putInt(EXTRA_DATA, rssi);
                msg.setData(mBundle);
                msg.sendToTarget();
        	}
        }
    };

    private BluetoothGattServerCallback mGattServerCallbacks = new BluetoothGattServerCallback() {
		@Override
		public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
			Log.i(TAG, "GATT_SERVER : onConnectionStateChange - " + newState);
		}

		@Override
		public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
				BluetoothGattDescriptor descriptor) {
			byte[] value = descriptor.getValue();
			if (mGattServer != null) {
				// Notify the Handler that there was a descriptor read request.
				// The Handler should call handleRead() after receiving this
				// message.
				Log.i(TAG, "GATT_SERVER : onDescriptorReadRequest");
			}
		}

		@Override
		public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
				boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
			if (mGattServer != null) {
				// Notify the Handler that there was a descriptor write request.
				// The Handler should call handleWrite() after receiving this
				// message.
				Log.i(TAG, "GATT_SERVER : onDescriptorWriteRequest");
			}
		}

		@Override
		public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
				BluetoothGattCharacteristic characteristic) {
			if (mGattServer != null) {
				// Notify the Handler that there was a characteristic read
				// request. The Handler should call handleRead() after receiving
				// this message.
				Log.i(TAG, "GATT_SERVER : onCharacteristicReadRequest");
			}
		}

		@Override
		public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value)
		{
			if (BluetoothLeService.this.mGattServer != null)
			{
				Log.i(TAG, "GATT_SERVER : onCharacteristicWriteRequest");
			}
		}
    };


	public void setActivityHandler(Handler mHandler) {
        Log.d(TAG, "Activity Handler set");
        mActivityHandler = mHandler;
    }
	
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        
        openServer();
        return true;
    }

    private void openServer()
    {
    	mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallbacks);
       	if(mGattServer != null)
       		registerService();
       	return;
    }
    
    public void registerService() {
        Log.d(TAG, "registerService()");

	    BluetoothGattCharacteristic major = new BluetoothGattCharacteristic(
		      GOPLUS_MAJOR,  
		      BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ, 
		      BluetoothGattCharacteristic.PERMISSION_WRITE
		      );
	    BluetoothGattCharacteristic minor = new BluetoothGattCharacteristic(
		      GOPLUS_MINOR, 
		      BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ, 
		      BluetoothGattCharacteristic.PERMISSION_WRITE
		      );
	    BluetoothGattCharacteristic measuredPower = new BluetoothGattCharacteristic(
		      GOPLUS_MEASUERED_POWER,
		      BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ, 
		      BluetoothGattCharacteristic.PERMISSION_WRITE
		      );
	    BluetoothGattCharacteristic advInterval = new BluetoothGattCharacteristic(
		      GOPLUS_ADV_INTERVAL, 
		      BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ, 
		      BluetoothGattCharacteristic.PERMISSION_WRITE
		      );
	    BluetoothGattCharacteristic txPower = new BluetoothGattCharacteristic(
		      GOPLUS_TXPOWER, 
		      BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ, 
		      BluetoothGattCharacteristic.PERMISSION_WRITE
		      );
	    BluetoothGattCharacteristic uuid = new BluetoothGattCharacteristic(
			      GOPLUS_TXPOWER, 
			      BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ, 
			      BluetoothGattCharacteristic.PERMISSION_WRITE
			      );
	
	    BluetoothGattService goplusService = new BluetoothGattService(GOPLUS_SERVICE, 
	    		BluetoothGattService.SERVICE_TYPE_PRIMARY);
	    goplusService.addCharacteristic(major);
	    goplusService.addCharacteristic(minor);
	    goplusService.addCharacteristic(measuredPower);
	    goplusService.addCharacteristic(advInterval);
	    goplusService.addCharacteristic(txPower);
	    goplusService.addCharacteristic(uuid);
	
	    BluetoothGattCharacteristic batteryLevel = new BluetoothGattCharacteristic(
	      BATTERY_LEVEL, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
	
	    BluetoothGattService battery = new BluetoothGattService(BATTERY_SERVICE, 
	    		BluetoothGattService.SERVICE_TYPE_PRIMARY);
	    battery.addCharacteristic(batteryLevel);
	
	    this.mGattServer.addService(goplusService);
	    this.mGattServer.addService(battery);
    }
    
    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
		
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        
        if(mGattServer == null ) {
        	return;
        }
        mGattServer.clearServices();
        mGattServer.close();
        mGattServer = null;

        	
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
    
    public void writeUUID(BluetoothDevice iDevice, String uuid) {
    	BluetoothGattService andgoService = this.mBluetoothGatt.getService(GOPLUS_SERVICE);
    	if (andgoService == null) {
    		Log.e(TAG, "GoPLUS Service not found!");
    		return;
    	}
	    BluetoothGattCharacteristic serviceUUID = andgoService.getCharacteristic(GOPLUS_UUID);
	    if (serviceUUID == null) {
	    	Log.e(TAG, "GoPLUS UUID charateristic not found!");
	    	return;
	    }
	    byte[] byteUUID = hexToByteArray(uuid);
	    serviceUUID.setValue(byteUUID);
	    
	    boolean status = false;
	    status = this.mBluetoothGatt.writeCharacteristic(serviceUUID);
	    Log.d(TAG, "writeUUID() - status=" + status);
    }
    
    public void writeMajor(BluetoothDevice iDevice, int major) {
    	BluetoothGattService andgoService = this.mBluetoothGatt.getService(GOPLUS_SERVICE);
    	if (andgoService == null) {
    		Log.e(TAG, "GoPLUS Service not found!");
    		return;
    	}
	    BluetoothGattCharacteristic majorId = andgoService.getCharacteristic(GOPLUS_MAJOR);
	    if (majorId == null) {
	    	Log.e(TAG, "GoPLUS MAJOR charateristic not found!");
	    	return;
	    }
    
	    majorId.setValue(shortTobyte(major, ByteOrder.BIG_ENDIAN));
	    boolean status = false;
	    status = this.mBluetoothGatt.writeCharacteristic(majorId);
	    Log.d(TAG, "writeMajor() - status=" + status);
    }

    public void writeMinor(BluetoothDevice iDevice, int minor)
    {
    	BluetoothGattService andgoService = this.mBluetoothGatt.getService(GOPLUS_SERVICE);
	    if (andgoService == null) {
	    	Log.e(TAG, "GOPLUS_SERVICE not found!");
	    	return;
	    }
	    
	    BluetoothGattCharacteristic minorId = andgoService.getCharacteristic(GOPLUS_MINOR);
	    if (minorId == null) {
	    	Log.e(TAG, "GOPLUS_MINOR charateristic not found!");
	    	return;
	    }
	
	    minorId.setValue(shortTobyte(minor, ByteOrder.BIG_ENDIAN));
	    boolean status = false;
	    status = this.mBluetoothGatt.writeCharacteristic(minorId);
	    Log.d(TAG, "writeMinor() - status=" + status);
    }

    public void writeMeasuredPower(BluetoothDevice iDevice, int measuredPower) {
	    BluetoothGattService andgoService = this.mBluetoothGatt.getService(GOPLUS_SERVICE);
	    if (andgoService == null) {
	    	Log.e(TAG, "GOPLUS_SERVICE not found!");
	    	return;
	    }
	    BluetoothGattCharacteristic measured = andgoService.getCharacteristic(GOPLUS_MEASUERED_POWER);
	    if (measured == null) {
	    	Log.e(TAG, "GoPLUS_ MEASUERED_POWER charateristic not found!");
	    	return;
	    }
	    measured.setValue(measuredPower, BluetoothGattCharacteristic.FORMAT_SINT8, 0);
	    boolean status = false;
	    status = this.mBluetoothGatt.writeCharacteristic(measured);
	    Log.d(TAG, "writeMeasuredPower() - status=" + status);
    }

    public void writeAdvertisingInterval(BluetoothDevice iDevice, int interval) {
    	BluetoothGattService andgoService = this.mBluetoothGatt.getService(GOPLUS_SERVICE);
	    if (andgoService == null) {
	    	Log.e(TAG, "GOPLUS_SERVICE not found!");
	    	return;
	    }
	    BluetoothGattCharacteristic advInterval = andgoService.getCharacteristic(GOPLUS_ADV_INTERVAL);
	    if (advInterval == null) {
	    	Log.e(TAG, "GOPLUS_ADV_INTERVAL charateristic not found!");
	    	return;
	    }
	
	    advInterval.setValue(shortTobyte(interval, ByteOrder.BIG_ENDIAN));
	    boolean status = false;
	    status = this.mBluetoothGatt.writeCharacteristic(advInterval);
	    Log.d(TAG, "writeAdvertisingInterval() - status=" + status);
    }

    public void writeTxPower(BluetoothDevice iDevice, int txVal) {
	    BluetoothGattService andgoService = this.mBluetoothGatt.getService(GOPLUS_SERVICE);
	    if (andgoService == null) {
	    	Log.e(TAG, "GOPLUS_SERVICE not found!");
	    	return;
	    }
	    BluetoothGattCharacteristic txPower = andgoService.getCharacteristic(GOPLUS_TXPOWER);
	    if (txPower == null) {
	    	Log.e(TAG, "GOPLUS_TXPOWER charateristic not found!");
	    	return;
	    }
	
	    txPower.setValue(txVal, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
	    boolean status = false;
	    status = this.mBluetoothGatt.writeCharacteristic(txPower);
	    Log.d(TAG, "writeTxPower() - status=" + status);
    }

    public boolean readRssi() {
	    if (this.mBluetoothGatt != null)
	    	this.mBluetoothGatt.readRemoteRssi();
	    return true;
    }

    public void ReadBattery()
    {
	    BluetoothGattService BatteryService = this.mBluetoothGatt.getService(BATTERY_SERVICE);
	    if (BatteryService == null) {
	    	Log.e(TAG, "Battery service not found!");
	    	return;
	    }
	
	    BluetoothGattCharacteristic BatteryLevel = BatteryService.getCharacteristic(BATTERY_LEVEL);
	    if (BatteryLevel == null) {
	    	Log.e(TAG, "Battery Level charateristic not found!");
	    	return;
	    }
	
	    boolean result = this.mBluetoothGatt.readCharacteristic(BatteryLevel);
	    if (!result)
	    	Log.e(TAG, "Battery level reading is failed!");
    }

    public byte[] intToByteArray(int value)
    {
	    byte[] byteArray = new byte[1];
	    byteArray[0] = ((byte)(value & 0xFF));
	
	    return byteArray;
    }

    public void readUUID()
    {
	    BluetoothGattService goplusService = this.mBluetoothGatt.getService(GOPLUS_SERVICE);
	    if (goplusService == null) {
	    	Log.e(TAG, "GOPLUS_SERVICE not found!");
	    	return;
	    }
	
	    BluetoothGattCharacteristic major = goplusService.getCharacteristic(GOPLUS_UUID);
	    if (major == null) {
	    	Log.e(TAG, "GOPLUS_UUID charateristic not found!");
	    	return;
	    }
	
	    boolean result = this.mBluetoothGatt.readCharacteristic(major);
	    if (!result)
	    	Log.e(TAG, "GOPLUS_UUID charateristic reading is failed!");
    }
    
    public void readMajor()
    {
	    BluetoothGattService goplusService = this.mBluetoothGatt.getService(GOPLUS_SERVICE);
	    if (goplusService == null) {
	    	Log.e(TAG, "GOPLUS_SERVICE not found!");
	    	return;
	    }
	
	    BluetoothGattCharacteristic major = goplusService.getCharacteristic(GOPLUS_MAJOR);
	    if (major == null) {
	    	Log.e(TAG, "GOPLUS_MAJOR charateristic not found!");
	    	return;
	    }
	
	    boolean result = this.mBluetoothGatt.readCharacteristic(major);
	    if (!result)
	    	Log.e(TAG, "GOPLUS_MAJOR charateristic reading is failed!");
    }

    public void readMinor()
    {
	    BluetoothGattService goplusService = this.mBluetoothGatt.getService(GOPLUS_SERVICE);
	    if (goplusService == null) {
	    	Log.e(TAG, "GOPLUS_SERVICE not found!");
	    	return;
	    }
	
	    BluetoothGattCharacteristic minor = goplusService.getCharacteristic(GOPLUS_MINOR);
	    if (minor == null) {
	    	Log.e(TAG, "GOPLUS_MINOR charateristic not found!");
	    	return;
	    }
	
	    boolean result = this.mBluetoothGatt.readCharacteristic(minor);
	    if (!result)
	    	Log.e(TAG, "GOPLUS_MINOR charateristic reading is failed!");
    }

    public void readMeasuredPower()
    {
	    BluetoothGattService goplusService = this.mBluetoothGatt.getService(GOPLUS_SERVICE);
	    if (goplusService == null) {
	    	Log.e(TAG, "GOPLUS_SERVICE not found!");
	    	return;
	    }
	
	    BluetoothGattCharacteristic measured = goplusService.getCharacteristic(GOPLUS_MEASUERED_POWER);
	    if (measured == null) {
	    	Log.e(TAG, "GOPLUS_MEASUERED_POWER charateristic not found!");
	    	return;
	    }
	
	    boolean result = this.mBluetoothGatt.readCharacteristic(measured);
	    if (!result)
	    	Log.e(TAG, "GOPLUS_MEASUERED_POWER reading is failed!");
    }

    public void readAdvertisingInterval()
    {
	    BluetoothGattService goplusService = this.mBluetoothGatt.getService(GOPLUS_SERVICE);
	    if (goplusService == null) {
	    	Log.e(TAG, "GOPLUS_SERVICE not found!");
	    	return;
	    }
	
	    BluetoothGattCharacteristic advInterval = goplusService.getCharacteristic(GOPLUS_ADV_INTERVAL);
	    if (advInterval == null) {
	    	Log.e(TAG, "GOPLUS_ADV_INTERVAL charateristic not found!");
	    	return;
	    }
	
	    boolean result = this.mBluetoothGatt.readCharacteristic(advInterval);
	    if (!result)
	    	Log.e(TAG, "GOPLUS_ADV_INTERVAL charateristic reading is failed!");
    }

    public void readTxPower()
    {
	    BluetoothGattService goplusService = this.mBluetoothGatt.getService(GOPLUS_SERVICE);
	    if (goplusService == null) {
	    	Log.e(TAG, "GOPLUS_SERVICE not found!");
	    	return;
	    }
	
	    BluetoothGattCharacteristic txPower = goplusService.getCharacteristic(GOPLUS_TXPOWER);
	    if (txPower == null) {
	    	Log.e(TAG, "GOPLUS_TXPOWER charateristic not found!");
	    	return;
	    }
	
	    boolean result = this.mBluetoothGatt.readCharacteristic(txPower);
	    if (!result)
	    	Log.e(TAG, "GOPLUS_TXPOWERr charateristic reading is failed!");
    }
  
    private static byte[] shortTobyte(int value, ByteOrder order) {
		ByteBuffer buff = ByteBuffer.allocate(Short.SIZE/8);
		buff.order(order);
		buff.putShort((short) value);
		
		return buff.array();
	}

	private static int byteToShort(byte[] bytes, ByteOrder order) {
		ByteBuffer buff = ByteBuffer.allocate(Short.SIZE/8);
		buff.order(order);
		buff.put(bytes);
		buff.flip();

		return buff.getShort();
	}
	
	// hex to byte[]
	private static byte[] hexToByteArray(String hex) {
	    if (hex == null || hex.length() == 0) {
	        return null;
	    }

	    byte[] ba = new byte[hex.length() / 2];
	    for (int i = 0; i < ba.length; i++) {
	        ba[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
	    }
	    return ba;
	}

	// byte[] to hex
	private static String byteArrayToHex(byte[] ba) {
	    if (ba == null || ba.length == 0) {
	        return null;
	    }

	    StringBuffer sb = new StringBuffer(ba.length * 2);
	    String hexNumber;
	    for (int x = 0; x < ba.length; x++) {
	        hexNumber = "0" + Integer.toHexString(0xff & ba[x]);

	        sb.append(hexNumber.substring(hexNumber.length() - 2));
	    }
	    return sb.toString();
	} 
}