package com.example.appsensor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DebugDatabase";
    // Database name dan database versi
    static String DATABASE_NAME = "sensor";

    // Table Names
    static String TABLE_ACTIVITY = "activity";
    static String TABLE_VITAL = "vital";


    // FIELD dalam database activity
    static String time_activity = "time";
    static String acc_x = "acc_x";
    static String acc_y = "acc_y";
    static String acc_z = "acc_z";
    static String gyro_x = "gyro_x";
    static String gyro_y = "gyro_y";
    static String gyro_z = "gyro_z";

    // FIELD dalam database vital
    static String time_vital = "time";
    static String heart_rate = "heart_rate";
    static String step_counter = "step_counter";

    // method getDateTime String
    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }


    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        // tabel activity
        db.execSQL("CREATE TABLE activity (time DEFAULT CURRENT_TIMESTAMP," +
                "acc_x FLOAT," +
                "acc_y FLOAT," +
                "acc_z FLOAT," +
                "gyro_x FLOAT," +
                "gyro_y FLOAT," +
                "gyro_z FLOAT)");

        // tabel vital
        db.execSQL("CREATE TABLE vital (time DEFAULT CURRENT_TIMESTAMP," +
                "heart_rate FLOAT," +
                "step_counter FLOAT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // SAVE USER DATA FIRST!!!
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACTIVITY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VITAL);
        // create new tables
        onCreate(db);
    }

    // INSERT ACTIVITY
    public void insertActivity(float[] data ){
        ContentValues cv = new ContentValues();
        // waktu
        cv.put("time", getDateTime());
        cv.put("acc_x", data[0]);
        cv.put("acc_y", data[1]);
        cv.put("acc_z", data[2]);
        cv.put("gyro_x", data[3]);
        cv.put("gyro_y", data[4]);
        cv.put("gyro_z", data[5]);

        getWritableDatabase().insert(TABLE_ACTIVITY, null, cv);
    }

    public void insertVital(float[] data ){
        ContentValues cv = new ContentValues();
        // waktu
        cv.put("time", getDateTime());
        cv.put("heart_rate", (int) data[0]);
        cv.put("step_counter", (int) data[1]);

        getWritableDatabase().insert(TABLE_VITAL, null, cv);
    }


    public List<ListSensor> getDataActivity(){
        List<ListSensor> listSensorList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ACTIVITY, new String[]{acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z, time_activity},
                null, null, null, null, "time" + " asc");

        Log.d(TAG, "lagi ambil data activity");
        if (cursor.moveToFirst()) {
            do{
                float acc_x = cursor.getFloat(cursor.getColumnIndex("acc_x"));
                float acc_y = cursor.getFloat(cursor.getColumnIndex("acc_y"));
                float acc_z = cursor.getFloat(cursor.getColumnIndex("acc_z"));
                float gyro_x = cursor.getFloat(cursor.getColumnIndex("gyro_x"));
                float gyro_y = cursor.getFloat(cursor.getColumnIndex("gyro_y"));
                float gyro_z = cursor.getFloat(cursor.getColumnIndex("gyro_z"));
                String timestamp = cursor.getString(6);

                ListSensor listSensor = new ListSensor();
                listSensor.setAcc_x(acc_x);
                listSensor.setAcc_y(acc_y);
                listSensor.setAcc_z(acc_z);
                listSensor.setGyro_x(gyro_x);
                listSensor.setGyro_y(gyro_y);
                listSensor.setGyro_z(gyro_z);

                listSensor.setTimeActivity(timestamp);

                // Adding listSensor to list
                listSensorList.add(listSensor);
            } while(cursor.moveToNext());
        }
        // return contact list
        return listSensorList;
    }

    public List<ListSensor> getDataVital(){
        List<ListSensor> listSensorList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor2 = db.query(TABLE_VITAL, new String[]{heart_rate, step_counter, time_vital},
                null, null, null, null, "time" + " asc");

        Log.d(TAG, "lagi ambil data vital");
        if (cursor2.moveToFirst()) {
            do{
                float heart_rate = cursor2.getInt(cursor2.getColumnIndex("heart_rate"));
                int step_counter = cursor2.getInt(cursor2.getColumnIndex("step_counter"));
                String timestamp = cursor2.getString(2);

                ListSensor listSensor = new ListSensor();
                listSensor.setHeart_rate(heart_rate);
                listSensor.setStep_counter(step_counter);

                listSensor.setTimeVital(timestamp);

                // Adding listSensor to list
                listSensorList.add(listSensor);
            } while(cursor2.moveToNext());
        }
        // return contact list
        return listSensorList;
    }

    public void delete(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from "+ TABLE_ACTIVITY);
        db.execSQL("delete from "+ TABLE_VITAL);
    }


}
