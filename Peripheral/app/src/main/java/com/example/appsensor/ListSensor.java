package com.example.appsensor;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ListSensor {
    private int step_counter;
    private float heart_rate, acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z;
    private String timeActivity, timeVital;

    // ini untuk kebutuhan print log
    @Override
    @NonNull
    public String toString(){
        return "\nHR : " + heart_rate +  " SC : " + step_counter +
                " Acc X : " + acc_x +  " Acc Y : " + acc_y +  " Acc Z : " + acc_z +
                " Gyro X : " + gyro_x +  " Gyro Y : " + gyro_y +  " Gyro Z : " + gyro_z + "\n" +
                "Timestamp Vital : " + timeVital + "\n" + "Timestamp Activity : " + timeActivity + "\n";
    }

    public float getHeart_rate() {
        return heart_rate;
    }

    public void setHeart_rate(float heart_rate) {
        this.heart_rate = heart_rate;
    }

    public int getStep_counter() {
        return step_counter;
    }

    public void setStep_counter(int step_counter) {
        this.step_counter = step_counter;
    }

    public float getAcc_x() {
        return acc_x;
    }

    public void setAcc_x(float acc_x) {
        this.acc_x = acc_x;
    }

    public float getAcc_y() {
        return acc_y;
    }

    public void setAcc_y(float acc_y) {
        this.acc_y = acc_y;
    }

    public float getAcc_z() {
        return acc_z;
    }

    public void setAcc_z(float acc_z) {
        this.acc_z = acc_z;
    }


    public Long getTimeVital() throws ParseException {
        return getDateTimeLong(timeVital);
    }

    public void setTimeVital(String timeVital) {
        this.timeVital = timeVital;
    }

    public Long getTimeActivity() throws ParseException {
        return getDateTimeLong(timeActivity);
    }

    public void setTimeActivity(String timeActivity) {
        this.timeActivity = timeActivity;
    }


    public float getGyro_x() {
        return gyro_x;
    }

    public void setGyro_x(float gyro_x) {
        this.gyro_x = gyro_x;
    }

    public float getGyro_y() {
        return gyro_y;
    }

    public void setGyro_y(float gyro_y) {
        this.gyro_y = gyro_y;
    }

    public float getGyro_z() {
        return gyro_z;
    }

    public void setGyro_z(float gyro_z) {
        this.gyro_z = gyro_z;
    }


    // method getDateTime Long
    private Long getDateTimeLong(String string_date) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

        Date d = dateFormat.parse(string_date);
        long milliseconds = d.getTime();

        return milliseconds;
    }
}
