package com.example.appsensor;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public class HealthProfile {

    /* Current Time Service UUID */
    public static UUID HEALTH_SERVICE = UUID.fromString("c86be7d3-12d4-4766-9c13-5d60cd5ee41e");
    /* Mandatory Current Time Information Characteristic */
    public static UUID VITAL_CHAR    = UUID.fromString("3d10750f-791d-48fb-af80-2219598e60f3");
    /* Optional Local Time Information Characteristic */
    public static UUID STEP_CHAR = UUID.fromString("365bdc33-e898-41f3-baf3-75d16f20bf2b");
    /* Optional Local Time Information Characteristic */
    public static UUID ACC_CHAR = UUID.fromString("3d435b53-955e-48ef-88ab-aae0b86a621f");
    /* Optional Local Time Information Characteristic */
    public static UUID GYRO_CHAR = UUID.fromString("3f42bbff-a975-474c-b254-2aac532da7ad");
    /* Mandatory Client Characteristic Config Descriptor */
    public static UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * Current Time Service.
     */
    public static BluetoothGattService createTimeService() {
        BluetoothGattService service = new BluetoothGattService(HEALTH_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // REGISTER CHARACTERISTIC
        // Vital characteristic
        BluetoothGattCharacteristic vitalService = new BluetoothGattCharacteristic(VITAL_CHAR,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(CLIENT_CONFIG,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        // baru ditambah
        configDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        vitalService.addDescriptor(configDescriptor);

        // Activity characteristic
        BluetoothGattCharacteristic stepService = new BluetoothGattCharacteristic(STEP_CHAR,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor configDescriptor2 = new BluetoothGattDescriptor(CLIENT_CONFIG,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        // baru ditambah
        configDescriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        stepService.addDescriptor(configDescriptor2);

        // Activity characteristic
        BluetoothGattCharacteristic accService = new BluetoothGattCharacteristic(ACC_CHAR,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor configDescriptor3 = new BluetoothGattDescriptor(CLIENT_CONFIG,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        // baru ditambah
        configDescriptor3.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        accService.addDescriptor(configDescriptor3);

        // Activity characteristic
        BluetoothGattCharacteristic gyroService = new BluetoothGattCharacteristic(GYRO_CHAR,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor configDescriptor4 = new BluetoothGattDescriptor(CLIENT_CONFIG,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        // baru ditambah, sebenarnya ga perlu, pakai descriptor pertama
        configDescriptor4.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gyroService.addDescriptor(configDescriptor4);

        service.addCharacteristic(gyroService);
        service.addCharacteristic(accService);
        service.addCharacteristic(vitalService);
        service.addCharacteristic(stepService);

        return service;
    }

}
