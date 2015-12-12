package com.example.android.isima;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;


public class MyService extends Service  implements SensorEventListener {
    private LocationManager locationMgr = null;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float[] sensorValue;
    private String sensorXValueString = "0";
    private String sensorYValueString = "0";
    private String sensorZValueString = "0";
    private String sensorRezValueString = "0";
    private float sensorXValue=0, sensorYValue=0, sensorZValue=0;


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        Handler handler  = new Handler();
//        final Vibrator vibrator = (Vibrator) getSystemService(getApplicationContext().VIBRATOR_SERVICE);
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//
//                if (vibrator.hasVibrator()) {
//                    long[] pattern = {0, 500, 600};
//                    vibrator.vibrate(pattern, 1);
//                }
//            }
//        });
        Intent dialogIntent = new Intent(this, MainActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.equals(mAccelerometer)) {
            sensorValue = event.values;

            sensorXValue = sensorValue[0];
            sensorYValue = sensorValue[1];
            sensorZValue = sensorValue[2];
            if(sensorXValue > 10)
            {
                Intent dialogIntent = new Intent(this, MainActivity.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dialogIntent);
            }


            sensorXValueString = String.valueOf(sensorXValue);
            sensorYValueString = String.valueOf(sensorYValue);
            sensorZValueString = String.valueOf(sensorZValue);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}