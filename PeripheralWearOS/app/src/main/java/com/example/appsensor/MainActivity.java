package com.example.appsensor;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private SensorManager mSensorManager = null;
    // dibuat juga selain implements, biar bisa diakses di kelas runnable
    private SensorEventListener listener;

    private static final String TAG = "MainActivityTAG";
    private TextView mTextViewHeart;
    private TextView mTextViewStep;
//    private TextView mTextViewAcc;
//    private TextView mTextViewPress;

    private float accx, accy, accz, gyrox, gyroy, gyroz;
    // ada 12 data?
    private float[] accX = new float[12];
    private float[] accY = new float[12];
    private float[] accZ = new float[12];
    private float[] gyroX = new float[12];
    private float[] gyroY = new float[12];
    private float[] gyroZ = new float[12];

    int counterAcc = 0;
    // private int heartRate;
    private float heartRate;
    private int onBody;
    private int counterStep = 0;
    long timestampActivity, timestampVital;

    Sensor mHeartRate, mPedometer;

    /* Bluetooth API */
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    /* Collection of notification subscribers */
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    boolean statusDatabaseActivity, statusDatabaseVital = false;
    int akumulasi_waktu = 1;

    // kebutuhan pengujian
    int data_ke = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextViewHeart = (TextView) findViewById(R.id.text);
        mTextViewStep = (TextView) findViewById(R.id.textStep);
//        mTextViewAcc = (TextView) findViewById(R.id.textAcc);
//        mTextViewPress = (TextView) findViewById(R.id.textPres);

        // Enables Always-on
        setAmbientEnabled();
        // Devices with a display should not go to sleep
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Cek Permission Sensor
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.BODY_SENSORS},
                    1234);
        } else {
            Log.d(TAG, "ALREADY GRANTED");
        }

        // Cek Bluetooth
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        // We can't continue without proper Bluetooth support
        if (!checkBluetoothSupport(bluetoothAdapter)) {
            finish();
        }

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, filter);
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services");
            startAdvertising();
            startServer();
        }

        // Memanggil method main sensor
        mainSensor();

        // bagian ini untuk menambahkan data sensor
        final Handler handler = new Handler();
        final DatabaseHelper db = new DatabaseHelper(this);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // hanya simpan data kalau dipakai atau tidak ada koneksi BLE
                if (onBody != 0 && mRegisteredDevices.isEmpty() && akumulasi_waktu <= 30) {
                    // dalam 1 detik, ada 12 data yang disimpan, bagaimana caranya? untuk ada interval 0.5 detik?
                    for (int i = 1; i <= 12; i++) {
                        // ambil index saat ini
                        db.insertActivity(new float[]{accX[i-1], accY[i-1], accZ[i-1], gyroX[i-1], gyroY[i-1], gyroZ[i-1]});
                    }
                    // simpan data satu kali dalam 10 detik ke tabel vital
                    db.insertVital(new float[]{heartRate, counterStep});
                }

//                int maxLogSize = 1000;
//                String veryLongString = Arrays.toString(db.getDataActivity().toArray());
//                String veryLongString2 = Arrays.toString(db.getDataVital().toArray());
//                for (int i = 0; i <= veryLongString.length() / maxLogSize; i++) {
//                    int start = i * maxLogSize;
//                    int end = (i + 1) * maxLogSize;
//                    end = end > veryLongString.length() ? veryLongString.length() : end;
//                    Log.v("DataActivity", veryLongString.substring(start, end));
//                }
//                for (int i = 0; i <= veryLongString2.length() / maxLogSize; i++) {
//                    int start = i * maxLogSize;
//                    int end = (i + 1) * maxLogSize;
//                    end = end > veryLongString2.length() ? veryLongString2.length() : end;
//                    Log.v("DataVital", veryLongString2.substring(start, end));
//                }

                akumulasi_waktu++;
                // misal 5 menit, maka di eksekusi berapa kali? 5 menit = 300 d6etik, 30 kali
                // misal 15 menit, maka di eksekusi berapa kali? 15 menit = 900 detik, 90 kali
                // misal 30 menit, maka di eksekusi berapa kali? 30 menit = 1800 detik, 180 kali

                // lakukan lagi simpan data 10 detik kemudian dan tepat dilakukan hanya 1 kali saja (removeCallback)
                handler.removeCallbacks(this);
                handler.postDelayed(this, 10000);
            }
        };

        // masuk ke thread untuk simpan data
        new Thread(runnable).start();
        handler.postDelayed(runnable, 10000);

        // Tombol on/off
        ToggleButton toggle = (ToggleButton) findViewById(R.id.btn1);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO do again startAdvertiser and Server
                BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
                if (isChecked) {
                    Log.d(TAG, "Bluetooth enabled...starting services");
                    startAdvertising();
                    startServer();
                } else {
                    stopServer();
                    stopAdvertising();
                    unregisterReceiver(mBluetoothReceiver);
                }

            }
        });

        // Tombol keluar
        Button btn2 = (Button) findViewById(R.id.btn2);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // delete database untuk keperluan debug
                db.delete();

                // TODO Auto-generated method stub
                finish();
                System.exit(0);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for system clock events
        registerReceiver(deviceFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(deviceFoundReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(deviceFoundReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(deviceFoundReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter.isEnabled()) {
            stopServer();
            stopAdvertising();
        }
        unregisterReceiver(mBluetoothReceiver);
    }

    /**
     * Verify the level of Bluetooth support provided by the hardware.
     *
     * @param bluetoothAdapter System {@link BluetoothAdapter}.
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }
        return true;
    }

    private BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // notifyRegisteredDevices(false, null, 0);
        }
    };


    /**
     * Listens for Bluetooth adapter events to enable/disable
     * advertising and server functionality.
     */
    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startAdvertising();
                    startServer();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopServer();
                    stopAdvertising();
                    break;
                default:
                    // Do nothing
            }
        }
    };

    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .build();

        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, mAdvertiseCallback);
    }

    /**
     * Stop Bluetooth advertisements.
     */
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    private void startServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
            return;
        }
        mBluetoothGattServer.addService(HealthProfile.createTimeService());
    }

    /**
     * Shut down the GATT server.
     */
    private void stopServer() {
        if (mBluetoothGattServer == null) return;
        mBluetoothGattServer.close();
    }

    /**
     * Callback to receive information about the advertisement process.
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: " + errorCode);
        }
    };

    /**
     * Send a time service notification to any devices that are subscribed
     * to the characteristic.
     */
    private void notifyRegisteredDevices(boolean database, byte[] dataSensor, int type) {
        if (mRegisteredDevices.isEmpty()) {
            // Log.i(TAG, "No subscribers registered");
            return;
        }
        byte[] value;

        if (type == 1) { // tipe vital
            if (database && dataSensor != null) {
                value = dataSensor;
            } else {
                value = getVitalValue();
                Log.i("HeartSensor", "Heartrate: " + value);
            }
            for (BluetoothDevice device : mRegisteredDevices) {
                BluetoothGattCharacteristic characteristic = mBluetoothGattServer
                        .getService(UUID.fromString("c86be7d3-12d4-4766-9c13-5d60cd5ee41e"))
                        .getCharacteristic(UUID.fromString("3d10750f-791d-48fb-af80-2219598e60f3"));
                characteristic.setValue(value);
                mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, true);
            }
        } else if (type == 2) { // tipe step
            if (database && dataSensor != null) {
                value = dataSensor;
            } else {
                value = getStepValue();
                Log.i("StepSensor", "Pedometer: " + value);
            }
            for (BluetoothDevice device : mRegisteredDevices) {
                BluetoothGattCharacteristic characteristic = mBluetoothGattServer
                        .getService(UUID.fromString("c86be7d3-12d4-4766-9c13-5d60cd5ee41e"))
                        .getCharacteristic(UUID.fromString("365bdc33-e898-41f3-baf3-75d16f20bf2b"));
                characteristic.setValue(value);
                mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, true);
            }
        } else if (type == 3) { // tipe akselerasi
            if (database && dataSensor != null) {
                value = dataSensor;
            } else {
                value = getAccValue();
                Log.i("ActivitySensor", "Accelerometer: " + value);
            }
            for (BluetoothDevice device : mRegisteredDevices) {
                BluetoothGattCharacteristic characteristic = mBluetoothGattServer
                        .getService(UUID.fromString("c86be7d3-12d4-4766-9c13-5d60cd5ee41e"))
                        .getCharacteristic(UUID.fromString("3d435b53-955e-48ef-88ab-aae0b86a621f"));
                characteristic.setValue(value);
                mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, true);
            }
        } else { // tipe gyroscope
            if (database && dataSensor != null) {
                value = dataSensor;
            } else {
                value = getGyroValue();
                Log.i("ActivitySensor", "Gyroscope: " + value);
            }
            for (BluetoothDevice device : mRegisteredDevices) {
                BluetoothGattCharacteristic characteristic = mBluetoothGattServer
                        .getService(UUID.fromString("c86be7d3-12d4-4766-9c13-5d60cd5ee41e"))
                        .getCharacteristic(UUID.fromString("3f42bbff-a975-474c-b254-2aac532da7ad"));
                characteristic.setValue(value);
                mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, true);
            }
        }
    }


    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
                // baru ditambah
                mRegisteredDevices.add(device);
                Log.d(TAG, "onConnectionStateChange: ");

                // koneksi baru masuk, baru ditambah
                // untuk kirim data di awal

                DatabaseHelper db = new DatabaseHelper(MainActivity.this);
                final List<ListSensor> listSensorsActivity = db.getDataActivity();
                final List<ListSensor> listSensorsVital = db.getDataVital();

                Thread thread = new Thread() {
                    // ambil data pertama
                    int count = 0;
                    ListSensor listSensorActivity = listSensorsActivity.get(0);

                    @Override
                    public void run() {
                        if (listSensorActivity != null) {
                            statusDatabaseActivity = true;
                            while (listSensorsActivity.size() > count) {
                                // isi listSensor tunggal adalah yang kedua
                                listSensorActivity = listSensorsActivity.get(count);

                                // and allocating size capacity
                                ByteBuffer activityAcc = ByteBuffer.allocate(20);
                                ByteBuffer activityGyro = ByteBuffer.allocate(20);
                                // time
                                try {
                                    activityAcc.putLong(listSensorActivity.getTimeActivity());
                                    activityGyro.putLong(listSensorActivity.getTimeActivity());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                // data dipisah menjadi dua
                                activityAcc.putFloat(listSensorActivity.getAcc_x());
                                activityAcc.putFloat(listSensorActivity.getAcc_y());
                                activityAcc.putFloat(listSensorActivity.getAcc_z());
                                activityAcc.rewind();

                                activityGyro.putFloat(listSensorActivity.getGyro_x());
                                activityGyro.putFloat(listSensorActivity.getGyro_y());
                                activityGyro.putFloat(listSensorActivity.getGyro_z());
                                activityGyro.rewind();

                                Log.i("ActivityDB", "Accelerometer: " + activityAcc.array() + "Gyroscope: " + activityGyro.array());
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                Log.d("DATA ACTIVITY", "mengirimkan data ke " + count);
                                notifyRegisteredDevices(true, activityAcc.array(), 3);
                                notifyRegisteredDevices(true, activityGyro.array(), 4);
                                count++;
                            }
                        }
                    }
                };

                Thread threadTwo = new Thread() {
                    ListSensor listSensorVital = listSensorsVital.get(0);
                    int count2 = 0;

                    @Override
                    public void run() {
                        if (listSensorVital != null) {
                            statusDatabaseVital = true;
                            while (listSensorsVital.size() > count2) {
                                // isi listSensor tunggal adalah yang kedua
                                listSensorVital = listSensorsVital.get(count2);

                                // and allocating size capacity
                                ByteBuffer vital = ByteBuffer.allocate(12);
                                ByteBuffer step = ByteBuffer.allocate(12);
                                // time
                                try {
                                    vital.putLong(listSensorVital.getTimeVital());
                                    step.putLong(listSensorVital.getTimeVital());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                // data
                                vital.putFloat(listSensorVital.getHeart_rate());
                                vital.rewind();
                                step.putFloat(listSensorVital.getStep_counter());
                                step.rewind();

                                Log.i("VitalDB", "Heartrate: " + vital.array() + "Pedometer: " + step.array());
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                Log.d("DATA VITAL", "mengirimkan data ke " + count2);
                                notifyRegisteredDevices(true, vital.array(), 1);
                                notifyRegisteredDevices(true, step.array(), 2);
                                count2++;
                            }
                        }
                    }
                };
                thread.start();
                threadTwo.start();
                try {
                    thread.join(); // memastikan kedua thread telah selesai
                    threadTwo.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                db.delete(); // hapus data
                // reset kembali count simpen data selama 5 menit jadi 1, kebutuhan debug
                akumulasi_waktu = 1;
                statusDatabaseActivity = false;
                statusDatabaseVital = false;

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
                //Remove device from any active subscriptions
                mRegisteredDevices.remove(device);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            Log.d("aaaaa", "aaaaaaaaaaaaa onCharacteristicReadRequest(577)");
            if (HealthProfile.VITAL_CHAR.equals(characteristic.getUuid())) {
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        // ini bakal diubah untuk pengujian jadi null biar database aja yg di send
                        getVitalValue());
            } else if (HealthProfile.STEP_CHAR.equals(characteristic.getUuid())) {
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        getStepValue());
            } else if (HealthProfile.ACC_CHAR.equals(characteristic.getUuid())) {
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        getAccValue());
            } else if (HealthProfile.GYRO_CHAR.equals(characteristic.getUuid())) {
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        getGyroValue());
            } else {
                // Invalid characteristic
                Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            Log.d("aaaaa", "aaaaaaaaaaaaa onDescriptorReadRequest(617)");
            if (HealthProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                Log.d(TAG, "Config descriptor read");
                byte[] returnValue;
                if (mRegisteredDevices.contains(device)) {
                    returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                } else {
                    returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                }
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        returnValue);
            } else {
                Log.w(TAG, "Unknown descriptor read request");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
            Log.d("aaaaa", "aaaaaaaaaaaaa onDescriptorReadRequest(646)");
            if (HealthProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: " + device);
                    mRegisteredDevices.add(device);
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Unsubscribe device from notifications: " + device);
                    mRegisteredDevices.remove(device);
                }

                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            } else {
                Log.w(TAG, "Unknown descriptor write request");
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
            }
        }
    };


    // realtime
    public byte[] getVitalValue() {
        // and allocating size capacity
        ByteBuffer vital = ByteBuffer.allocate(12);
        vital.putLong(timestampVital); // 8 byte
        vital.putFloat(heartRate); // 4 byte
        vital.rewind();
        return vital.array();
    }

    public byte[] getStepValue() {
        // and allocating size capacity
        ByteBuffer step = ByteBuffer.allocate(12);
        step.putLong(timestampVital); // 8 byte
        step.putFloat(counterStep); // 4 byte
        step.rewind();
        return step.array();
    }

    // realtime
    public byte[] getAccValue() {
        // and allocating size capacity
        ByteBuffer activity = ByteBuffer.allocate(20);
        activity.putLong(timestampActivity); // 8 byte
        activity.putFloat(accx);
        activity.putFloat(accy);
        activity.putFloat(accz);
        activity.rewind();
        return activity.array();
    }

    public byte[] getGyroValue() {
        // and allocating size capacity
        ByteBuffer activity = ByteBuffer.allocate(20);
        activity.putLong(timestampActivity); // 8 byte
        activity.putFloat(gyrox);
        activity.putFloat(gyroy);
        activity.putFloat(gyroz);
        activity.rewind();
        return activity.array();
    }


    public void mainSensor() {
        mSensorManager = ((SensorManager) getSystemService(Context.SENSOR_SERVICE));

//        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
//        for(Sensor s : deviceSensors){
//            Log.i(TAG, "" + s.getName() + " Tipe_(String): " + s.getStringType()+ " Tipe_(number): "+ s.getType());
//        }

        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mAccelerometer != null) {
//        SENSOR_DELAY_NORMAL = 200.000 microseconds
//        SENSOR_DELAY_GAME = 20.000 microseconds
//        SENSOR_DELAY_UI = 60.000 microseconds
//        SENSOR_DELAY_FASTEST = 0 microseconds
            // berapa sampling? kalau 12 data dalam milis 83,333e -> 83333,... microsecond, angka bukan bulat
            mSensorManager.registerListener(this, mAccelerometer, 83333);
        } else {
            Toast.makeText(getBaseContext(), "Error: Sensor Accelerometer", Toast.LENGTH_LONG).show();
        }

        Sensor mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (mGyroscope != null) {
            mSensorManager.registerListener(this, mGyroscope, 83333);
        } else {
            Toast.makeText(getBaseContext(), "Error: Sensor Accelerometer", Toast.LENGTH_LONG).show();
        }

        mPedometer = mSensorManager.getDefaultSensor(33171009);
        if (mPedometer != null) {
            mSensorManager.registerListener(this, mPedometer, 1000000);
        } else {
            Toast.makeText(getBaseContext(), "Error: Sensor Pedometer", Toast.LENGTH_LONG).show();
        }

        mHeartRate = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        if (mHeartRate != null) {
            mSensorManager.registerListener(this, mHeartRate, 1000000);
        } else {
            Toast.makeText(getBaseContext(), "Error: Sensor Heat Rate", Toast.LENGTH_LONG).show();
        }

        Sensor mBody = mSensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT);
        if (mBody != null) {
            mSensorManager.registerListener(this, mBody, 1000000);
        } else {
            Toast.makeText(getBaseContext(), "Error: Sensor OnBody", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Log.d(TAG, "onAccuracyChanged - accuracy: " + accuracy);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Sensor Pedometer
        if (event.sensor.getType() == 33171009) {
            String msg = "Langkah: " + (int) event.values[0];
            counterStep = (int) event.values[0];
            mTextViewStep.setText(msg);
            if (!statusDatabaseVital) {
                notifyRegisteredDevices(false, null, 2);
            }
        }

        // Sensor Accelerometer
        else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // baru ditambah, menotify perlu data akselerasi
            if (!statusDatabaseActivity) {
                accx = event.values[0];
                accy = event.values[1];
                accz = event.values[2];
                // dapatin waktu tiap kali saat pembacaan real-time
                try {
                    timestampActivity = getDateTimeLong();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                notifyRegisteredDevices(false, null, 3);
            }

            accX[counterAcc] = event.values[0];
            accY[counterAcc] = event.values[1];
            accZ[counterAcc] = event.values[2];
            counterAcc++;
            if (counterAcc == 12)
                counterAcc = 0;

            String msg = "x: " + event.values[0] +
                    " y: " + event.values[1] +
                    " z: " + event.values[2];
            // mTextViewAcc.setText(msg);
            Log.d("Accelerometer", msg);
        }

        // Sensor Gyroscope, sekalian sama Accelerometer
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // baru ditambah, menotify perlu data gyroscope
            if (!statusDatabaseActivity) {
                gyrox = event.values[0];
                gyroy = event.values[1];
                gyroz = event.values[2];
                // langsung notify
                notifyRegisteredDevices(false, null, 4);
            }

            gyroX[counterAcc] = event.values[0];
            gyroY[counterAcc] = event.values[1];
            gyroZ[counterAcc] = event.values[2];
            String msg = "x: " + event.values[0] +
                    " y: " + event.values[1] +
                    " z: " + event.values[2];
            Log.d("Gyroscope", msg);
        }

        // Sensor jantung
        else if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            String msg = "Detak: " + event.values[0];
            Log.d("HeartRate", msg);
            mTextViewHeart.setText(msg);
            heartRate = event.values[0];
            if (!statusDatabaseVital) {
                // dapatin waktu tiap kali saat pembacaan real-time
                try {
                    timestampVital = getDateTimeLong();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                notifyRegisteredDevices(false, null, 1);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT) {
            String msg = "Pakai? " + (int) event.values[0];
            onBody = (int) event.values[0];
            if ((int) event.values[0] == 0) {
                // unregis sensor jantung dan langkah biar menghemat daya
                mSensorManager.unregisterListener(this, mHeartRate);
                mSensorManager.unregisterListener(this, mPedometer);
                Toast.makeText(getBaseContext(), "Perangkat tidak dipakai", Toast.LENGTH_LONG).show();
                // kembalikan data jantung jadi 0 sehingga ketika dikirimkan adalah data asli
                heartRate = 0;
            } else {
                Toast.makeText(getBaseContext(), "Perangkat dipakai", Toast.LENGTH_LONG).show();
                // jalankan sensor jantung lagi disini dengan cara register
                // baru diubah
                mSensorManager.registerListener(this, mHeartRate, 1000000);
            }
        } else
            Log.d(TAG, "Unknown sensor type");
    }

    // method getDateTime Long
    private Long getDateTimeLong() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        Date date = new Date();
        Date date2 = dateFormat.parse(dateFormat.format(date));
        long milliseconds = date2.getTime();

        return milliseconds;
    }

    // untuk debugging, cek kesamaan hexa
    public String getHexa(byte[] data) {
        final StringBuilder stringBuilder = new StringBuilder(data.length);
        // DATA ADALAH BYTE
        for (byte byteChar : data)
            stringBuilder.append(String.format("%02X ", byteChar));

        String message_achieve = stringBuilder.toString();
        // Log.i(TAG, "onCharacteristicChanged: " + message_achieve);
        return message_achieve;
    }

}
