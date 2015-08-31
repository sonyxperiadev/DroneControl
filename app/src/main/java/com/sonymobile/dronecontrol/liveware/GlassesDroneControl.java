/*
 *  Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. Neither the name of the copyright holder nor the names
 *     of its contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 *  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.sonymobile.dronecontrol.liveware;

import com.sony.smarteyeglass.extension.util.SmartEyeglassControlUtils;
import com.sonyericsson.extras.liveware.aef.control.Control.Intents;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonymobile.dronecontrol.R;
import com.sonymobile.dronecontrol.alert.AlertTutorial;
import com.sonymobile.dronecontrol.alert.AlertTutorial.AlertState;
import com.sonymobile.dronecontrol.alert.AlertTutorial.DroneAction;
import com.sonymobile.dronecontrol.alert.BebopTutorial;
import com.sonymobile.dronecontrol.alert.RollingSpiderTutorial;
import com.sonymobile.dronecontrol.alert.SumoTutorial;
import com.sonymobile.dronecontrol.controller.BebopDeviceController;
import com.sonymobile.dronecontrol.controller.DeviceController;
import com.sonymobile.dronecontrol.controller.DeviceController.DeviceState;
import com.sonymobile.dronecontrol.controller.DeviceControllerListener;
import com.sonymobile.dronecontrol.controller.DeviceListenerHandler;
import com.sonymobile.dronecontrol.controller.RollingSpiderDeviceController;
import com.sonymobile.dronecontrol.settings.Preferences;
import com.sonymobile.dronecontrol.utils.EnumDevices;
import com.sonymobile.dronecontrol.utils.Logger;
import com.sonymobile.dronecontrol.utils.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("deprecation")
public final class GlassesDroneControl extends ControlExtension implements DeviceControllerListener {

    public static final String TAG = GlassesDroneControl.class.getSimpleName();

    // Log utils
    private static final String LOG_ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DroneControlLogs/";
    private String LOG_FOLDER_PATH;
    private String GLASS_LOG_FILE;
    private String GPS_LOG_FILE;
    private long START_TIME = 0;
    private static boolean LOG_STATUS = false;

    private static GlassesDroneControl sInstance;
    private SmartEyeglassControlUtils mUtils;

    // UI Elements
    private List<Alert> mAlertList = new ArrayList<>();
    private Context mContext;
    private TextView mTvAltitude;
    private TextView mTvSpeed;
    private TextView mTvAlert;
    private ImageView mImgWifi;
    private ImageView mImgBattery;
    private TextView mTvBattery;
    private RelativeLayout mRoot;
    private RelativeLayout mLayout;
    private Bitmap mBitmap;
    private String mAltitude = "";
    private String mSpeed = "";
    private RelativeLayout mSpeedLayout;
    private RelativeLayout mAltitudeLayout;
    private LinearLayout mInstructionLayout;
    private LinearLayout mRecordingLayout;
    private RelativeLayout mDrawingCanvasLayout;
    private ImageView mAlertIcon;
    private ImageView mPictureView;

    private Handler mHandler = new Handler();
    private static final int PNG_QUALITY = 100;
    private static final int PNG_DEFAULT_CAPACITY = 256;
    private static final int SMARTEYEGLASS_API_VERSION = 1;
    private static final int LOW_BATTERY_PERCENTAGE = 20;
    private boolean mGlassReady = false;
    private Alert mCurrentAlert;
    private int mSpeedFactor = 5;
    private int mRollFactor = 5;
    private int mYawFactor = 5;
    private int mGazFactor = 5;
    private double mLastSpeed = -1;
    private double mLastAltitude = -1;
    private int mLastWifiSignal = -1;
    private int mBatteryLevel = 100;

    private boolean mIsDeviceConnected = false;
    private EnumDevices mDeviceName = EnumDevices.NONE;
    private AlertTutorial mAlertTutorial = null;
    private DeviceState mCurrentState;

    private Timer mTimer;
    private long mIntervalCounter = 0;
    private long mBatteryBlinkCounter = -1;
    private static final long REFRESH_RATE = TimeUnit.MILLISECONDS.toMillis(200);
    private static final long ALERT_INTERVAL = TimeUnit.MILLISECONDS.toMillis(400);
    private static final long BLINK_INTERVAL_MULTIPLIER = (400 / REFRESH_RATE);

    private Lock mLock;
    private boolean mRender = true;

    public static GlassesDroneControl getInstance() {
        return sInstance;
    }

    public GlassesDroneControl(final Context context, final String hostAppPackageName) {
        super(context, hostAppPackageName);
        Logger.d(TAG, "Initializing SmartEyeGlasses");

        mLock = new ReentrantLock();

        sInstance = this;
        this.mContext = context;
        mUtils = new SmartEyeglassControlUtils(hostAppPackageName, null);
        mUtils.setRequiredApiVersion(SMARTEYEGLASS_API_VERSION);
        mUtils.activate(context);
        mGlassReady = initLayout();

        initGlassLoop();
    }

    private class Alert {

        private long duration = 0;
        private long start = 0;

        public AlertState alert;
        public int msg;
        public int icon;

        public Alert(AlertState alertState) {
            alert = alertState;

            msg = alert.alertName;
            icon = alert.alertIcon;
            duration = alert.alertDuration;
        }

        public boolean isRunning() {
            if (start > 0 && duration > 0) {
                long diff = System.currentTimeMillis() - start;
                if (diff >= TimeUnit.SECONDS.toMillis((duration))) {
                    return false;
                }
                return true;
            } else if (duration == -1) {
                return true;
            }
            return false;
        }

        public void setRunning() {
            start = System.currentTimeMillis();
        }

        public boolean isStaticAlert() {
            return (duration == -1 ? true : false);
        }

    }

    private void initGlassLoop() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mLock.lock();
                if (mRender) {
                    updateBitmap();
                }
                mLock.unlock();
            }
        }, 0, REFRESH_RATE);
    }

    private void stopGlassLoop() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    public void doCenteredBlinkingAlert(AlertState alert) {
        appendLog("centeralert," + alert.toString());
        Logger.d(TAG, "Alert: " + alert.toString());

        if (alert != null) {
            Alert alertItem = new Alert(alert);
            mAlertList.add(alertItem);
        }
    }

    private void processAlerts() {
        // Wait until any device connection
        if (mIsDeviceConnected && mGlassReady) {

            // Remove waiting message
            if (mTvAlert.getText().equals(mContext.getResources().getString(R.string.waiting_connection))) {
                mTvAlert.setText("");
            }

            // Wait current alert to finish
            if (mCurrentAlert != null) {
                if (!mCurrentAlert.isRunning()) {
                    mCurrentAlert = null;
                    Utils.setInvisible(mAlertIcon, mTvAlert);

                    // set interval counter to display next alert
                    mIntervalCounter = System.currentTimeMillis();
                } else if (!mCurrentAlert.isStaticAlert()) {
                    // we are still displaying an alert
                    return;
                }
            }

            // Wait a fixed interval before displaying next alert
            if (mIntervalCounter > 0 && (System.currentTimeMillis() - mIntervalCounter <= ALERT_INTERVAL)) {
                return;
            }
            mIntervalCounter = 0;

            // Display new alert if needed
            if (mAlertList.size() > 0) {

                // Get alert and remove from list
                mCurrentAlert = mAlertList.get(0);
                mCurrentAlert.setRunning();
                mAlertList.remove(0);

                mAlertIcon.setImageDrawable(null);
                mTvAlert.setText("");

                switch (mCurrentAlert.alert) {
                    case BATTERY:
                        switch (mBatteryLevel) {
                            case 50:
                                mAlertIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_glass_battery_50));
                                mTvAlert.setText(mContext.getResources().getString(mCurrentAlert.msg, mBatteryLevel));
                                break;
                            case 40:
                                mAlertIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_glass_battery_40));
                                mTvAlert.setText(mContext.getResources().getString(mCurrentAlert.msg, mBatteryLevel));
                                break;
                            case 30:
                                mAlertIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_glass_battery_30));
                                mTvAlert.setText(mContext.getResources().getString(mCurrentAlert.msg, mBatteryLevel));
                                break;
                            case 25:
                                mAlertIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_glass_battery_25));
                                mTvAlert.setText(mContext.getResources().getString(mCurrentAlert.msg, mBatteryLevel));
                                break;
                        }
                        break;
                    case NONE:
                        break;
                    default:
                        mTvAlert.setText(mContext.getResources().getString(mCurrentAlert.msg));
                        if (mCurrentAlert.icon != -1) {
                            mAlertIcon.setImageDrawable(mContext.getResources().getDrawable(mCurrentAlert.icon));
                        }
                }
                Utils.setVisible(mAlertIcon, mTvAlert);
            } else {

                // Display static alerts (we only have the LANDED case and small X to land case)
                if (mCurrentState != null && mCurrentState != DeviceState.LANDED && mCurrentState != DeviceState.EMERGENCY) {
                    updateInstruction(true);
                } else if (mCurrentState == DeviceState.LANDED) {
                    updateInstruction(false);
                    if (mCurrentAlert == null) {
                        doCenteredBlinkingAlert(AlertState.TAKEOFF);
                    }
                } else if (mCurrentState == DeviceState.EMERGENCY) {
                    updateInstruction(false);
                    if (mCurrentAlert == null) {
                        doCenteredBlinkingAlert(AlertState.EMERGENCY);
                    }
                }
            }

        } else if (!mIsDeviceConnected && mGlassReady) {
            // Set waiting message
            mTvAlert.setVisibility(View.VISIBLE);
            mTvAlert.setText(mContext.getResources().getString(R.string.waiting_connection));
            mAlertIcon.setImageDrawable(null);
            mAlertIcon.setVisibility(View.INVISIBLE);
        }
    }

    // When flying, land instruction should be fixed on right below screen
    private void updateInstruction(boolean state) {
        if (state) {
            mInstructionLayout.setVisibility(View.VISIBLE);
        } else {
            mInstructionLayout.setVisibility(View.INVISIBLE);
        }
    }

    public void dispatchGlassBroadcast(boolean state) {
        Intent intent = new Intent(Constants.INTENT_DRONE_CONTROL);
        intent.putExtra("state", state);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        Logger.d(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.d(TAG, "onResume");
        DeviceListenerHandler.registerListener(this);
        dispatchGlassBroadcast(true);
        setStartScreen();
        loadDroneInfo(mDeviceName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "onDestroy");
        dispatchGlassBroadcast(false);
        mUtils.deactivate();
        LOG_STATUS = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        Logger.d(TAG, "onPause");
        mGlassReady = false;
        mIsDeviceConnected = false;
        stopGlassLoop();
        sInstance = null;
        DeviceListenerHandler.unregisterListener(this);
        LOG_STATUS = false;
        mCurrentAlert = null;
    }

    private boolean initLayout() {
        mRoot = new RelativeLayout(mContext);
        mRoot.setLayoutParams(new RelativeLayout.LayoutParams(R.dimen.smarteyeglass_control_width, R.dimen.smarteyeglass_control_height));
        final ScreenSize size = new ScreenSize(mContext);
        final int width = size.getWidth();
        final int height = size.getHeight();
        mBitmap = Bitmap.createBitmap(width, height, Config.RGB_565);
        mBitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, new ByteArrayOutputStream(PNG_DEFAULT_CAPACITY));
        mBitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        mLayout = (RelativeLayout)RelativeLayout.inflate(mContext, R.layout.layout, mRoot);
        mLayout.measure(height, width);
        mLayout.layout(0, 0, mLayout.getMeasuredWidth(), mLayout.getMeasuredHeight());
        mImgWifi = (ImageView)mRoot.findViewById(R.id.wifi);
        mImgBattery = (ImageView)mRoot.findViewById(R.id.battery);
        mTvBattery = (TextView)mRoot.findViewById(R.id.tv_battery_level);
        mTvAltitude = (TextView)mLayout.findViewById(R.id.tv_altitude_label);
        mTvSpeed = (TextView)mLayout.findViewById(R.id.tv_speed_label);
        mTvAlert = (TextView)mLayout.findViewById(R.id.tv_center_text);
        mAlertIcon = (ImageView)mRoot.findViewById(R.id.iv_alert_icon);
        mDrawingCanvasLayout = (RelativeLayout)mRoot.findViewById(R.id.custom_canvas);
        mSpeedLayout = (RelativeLayout)mRoot.findViewById(R.id.speed_field);
        mAltitudeLayout = (RelativeLayout)mRoot.findViewById(R.id.altitude_field);
        mInstructionLayout = (LinearLayout)mRoot.findViewById(R.id.instruction_field);
        mRecordingLayout = (LinearLayout)mRoot.findViewById(R.id.recording_field);
        mAlertIcon.setVisibility(View.INVISIBLE);
        mPictureView = (ImageView)mRoot.findViewById(R.id.picture_view);
        this.setScreenState(Intents.SCREEN_STATE_ON);

        if (mBitmap != null && mLayout != null && mImgBattery != null && mImgWifi != null &&
                mTvSpeed != null && mTvAltitude != null && mTvAlert != null && mPictureView != null) {
            return true;
        }

        return false;
    }

    private Canvas drawCanvas() {
        Canvas canvas = new Canvas(mBitmap);
        mLayout.draw(canvas);
        if (DeviceController.getInstance() != null) {
            if (mDrawingCanvasLayout.getVisibility() == View.VISIBLE) {
                if (DeviceController.getInstance().getDeviceInfo().equals(EnumDevices.BEBOP)) {
                    drawGraph(BebopDeviceController.getInstance().getPCMDData());
                } else if (DeviceController.getInstance().getDeviceInfo().equals(EnumDevices.MINIDRONE)) {
                    drawGraph(RollingSpiderDeviceController.getInstance().getPCMDData());
                }
            }
        }
        mUtils.showBitmap(mBitmap);
        return canvas;
    }

    private void drawGraph(byte[] pcmd) {
        byte dPitch = pcmd[0];
        byte dRoll = pcmd[1];
        byte dYaw = pcmd[2];
        byte dGaz = pcmd[3];
        checkValues(dPitch, dRoll, dYaw, dGaz);
        Paint paint = new Paint();
        int width = 300;
        int height = 80;
        int STROKE_WIDTH = 2;
        int radius = height / 2 - 10;
        paint.setColor(Color.BLACK);
        Bitmap bg = Bitmap.createBitmap(width, height, Config.RGB_565);
        bg.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, new ByteArrayOutputStream(PNG_DEFAULT_CAPACITY));
        bg.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        Canvas canvas = new Canvas(bg);
        canvas.drawPaint(paint);
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setTextSize(25);
        canvas.drawCircle(width / 2, height / 2, radius, paint);
        canvas.drawLine(width - STROKE_WIDTH, 0, width - STROKE_WIDTH, height, paint);
        double pitch = (((double)dPitch / mSpeedFactor) / 100); // speed
        double roll = (((double)dRoll / mRollFactor) / 100); // left right speed
        double gaz = (((double)dGaz / mGazFactor) / 100); // up down speed
        double yaw = (((double)dYaw / mYawFactor) / 100); // left right turning

        // Left right tilt
        int tiltOffset = (int)(roll * height / 2);
        canvas.drawLine(0, height / 2 - tiltOffset, width, height / 2 + tiltOffset, paint);
        int speed = Utils.getKmModularVector(pitch * 5, roll * 5, 0);
        mSpeed = speed + " km/h";
        mSpeedLayout.setVisibility(View.VISIBLE);
        mTvSpeed.setText(mSpeed);

        // Vertical speed
        float verSpeedOffset = (int)(gaz * height / 2);
        paint.setStrokeWidth(STROKE_WIDTH * 4);
        canvas.drawLine(width - 15, height / 2 - verSpeedOffset, width, height / 2 - verSpeedOffset, paint);

        // Turning speed
        double maxAngle = Math.PI / 2;
        double angle = yaw * maxAngle;
        float cX = (float)(width / 2 + Math.sin(angle) * radius);
        float cY = (float)(height / 2 - Math.cos(angle) * radius);
        canvas.drawCircle(cX, cY, 2, paint);
        mDrawingCanvasLayout.setBackgroundDrawable(new BitmapDrawable(bg));
    }

    private void checkValues(byte pitch, byte roll, byte yaw, byte gaz) {
        if (pitch != 0 && mSpeedFactor > 1) {
            mSpeedFactor--;
        } else if (pitch == 0) {
            mSpeedFactor = 5;
        }
        if (roll != 0 && mRollFactor > 1) {
            mRollFactor--;
        } else if (roll == 0) {
            mRollFactor = 5;
        }
        if (yaw != 0 && mYawFactor > 1) {
            mYawFactor--;
        } else if (yaw == 0) {
            mYawFactor = 5;
        }
        if (gaz != 0 && mGazFactor > 1) {
            mGazFactor--;
        } else if (gaz == 0) {
            mGazFactor = 5;
        }
    }

    private void updateBitmap() {
        if (!mGlassReady) {
            Logger.d(TAG, "Glass is not ready yet");
            return;
        }
        mTvAltitude.setText(mAltitude);
        mTvSpeed.setText(mSpeed);
        if (mBatteryLevel <= LOW_BATTERY_PERCENTAGE) {
            blinkBatteryText(true);
        } else {
            blinkBatteryText(false);
        }
        processAlerts();
        drawCanvas();
    }

    private void blinkBatteryText(boolean enable) {
        if (mDeviceName != EnumDevices.NONE) {
            if (!enable) {
                Utils.setVisible(mImgBattery, mTvBattery);
            } else {
                if (mBatteryBlinkCounter < BLINK_INTERVAL_MULTIPLIER) {
                    mBatteryBlinkCounter++;
                } else {
                    if (mBatteryBlinkCounter >= BLINK_INTERVAL_MULTIPLIER) {
                        if (mImgBattery.getVisibility() == View.VISIBLE) {
                            Utils.setInvisible(mImgBattery, mTvBattery);
                        } else if (mImgBattery.getVisibility() == View.INVISIBLE) {
                            Utils.setVisible(mImgBattery, mTvBattery);
                        }
                    }
                    mBatteryBlinkCounter = -1;
                }
            }
        }
    }

    @Override
    public void onUpdateBattery(byte percent) {
        appendLog("battery," + percent);
        mBatteryLevel = percent;

        if (!mGlassReady) {
            return;
        }

        mTvBattery.setText(percent + "%");
        if (mTvBattery != null && mTvBattery.getVisibility() == View.INVISIBLE) {
            mTvBattery.setVisibility(View.VISIBLE);
        }
        if (percent == 25 || percent == 30 || percent == 40 || percent == 50) {
            doCenteredBlinkingAlert(AlertState.BATTERY);
        } else if (percent <= 8) {
            doCenteredBlinkingAlert(AlertState.CRITICAL_BATTERY);
        }
    }

    @Override
    public void onAltitudeChange(double meters) {
        appendLog("altitude," + meters);

        if (!mGlassReady) {
            return;
        }

        if (mLastAltitude != meters) {
            mLastAltitude = meters;
        } else {
            return;
        }


        if (mLastAltitude != 0) {
            mAltitude = String.format(" %.2f m", mLastAltitude);
        } else {
            mAltitude = "0.0 m";
        }
    }

    @Override
    public void onWifiSignalChange(short rssi) {
        appendLog("wifi," + rssi);

        if (!mGlassReady) {
            return;
        }

        if (rssi == 0) {
            return;
        }

        if (mLastWifiSignal != rssi) {
            mLastWifiSignal = rssi;
        } else {
            return;
        }

        if (rssi >= -70) {
            mImgWifi.setImageDrawable(mContext.getResources().getDrawable(R.drawable.glass_ic_signal_1));
        } else if (rssi >= -80) {
            mImgWifi.setImageDrawable(mContext.getResources().getDrawable(R.drawable.glass_ic_signal_2));
        } else if (rssi >= -90) {
            mImgWifi.setImageDrawable(mContext.getResources().getDrawable(R.drawable.glass_ic_signal_3));
        } else if (rssi >= -100) {
            mImgWifi.setImageDrawable(mContext.getResources().getDrawable(R.drawable.glass_ic_signal_4));
        } else {
            mImgWifi.setImageDrawable(mContext.getResources().getDrawable(R.drawable.glass_ic_signal_5));
        }
    }

    @Override
    public void onSpeedChange(double speedX, double speedY, double speedZ) {
        appendLog("speed," + speedX + "," + speedY + "," + speedZ);
        double speed = Utils.getKmModularVector(speedX, speedY, speedZ);
        if (!mGlassReady) {
            return;
        }

        if (speedX == -1 && speedY == -1 && speedZ == -1) {
            mSpeed = "0 km/h";
            return;
        }

        if (mLastSpeed != speed) {
            mLastSpeed = speed;
            mSpeed = speed + " km/h";
        }
    }

    @Override
    public void onNewVideoFrame(byte[] frame, int size, boolean flush) {

    }

    @Override
    public void onConnectionTimeout() {

    }

    @Override
    public void onAlertStateChange(AlertState state) {
        appendLog("alert," + state.toString());

        Logger.d(TAG, "onAlertStateChange: " + state.alertName);

        if (state != null) {
            this.doCenteredBlinkingAlert(state);
        }
    }

    @Override
    public void onStateChange(DeviceController.DeviceState state) {
        appendLog("state," + state.toString());

        if (mCurrentState != state) {
            mCurrentState = state;
            mAlertTutorial.callActionByState(mCurrentState);
        }
    }

    @Override
    public void onDroneDeviceInit(EnumDevices device) {
        if (!mIsDeviceConnected) {
            appendLog("init," + device.toString());
            mDeviceName = device;
            Logger.d(TAG, "Init device on glass, glass is ready = " + mGlassReady);
            mIsDeviceConnected = true;
            initGlassLoop();
            loadDroneInfo(mDeviceName);
        }
    }

    public void loadDroneInfo(EnumDevices device) {
        appendLog("droneinfo," + device.toString());
        mAlertTutorial = null;
        if (EnumDevices.BEBOP.equals(device)) {
            Utils.setVisible(mImgWifi, mImgBattery, mSpeedLayout, mAltitudeLayout);
            mAlertTutorial = new BebopTutorial();
        } else if (EnumDevices.MINIDRONE.equals(device)) {
            Utils.setVisible(mImgBattery);
            mAlertTutorial = new RollingSpiderTutorial();
        } else if (EnumDevices.JUMPINGSUMO.equals(device)) {
            Utils.setVisible(mImgWifi, mImgBattery);
            mAlertTutorial = new SumoTutorial();
            performAction(DroneAction.ACTION_INITDEVICE);
        }
    }

    public void performAction(DroneAction action) {
        appendLog("perform," + action.toString());
        if (mAlertTutorial != null) {
            mAlertTutorial.performAction(action);
        }
    }

    @Override
    public void onTakePicture(Boolean result) {

    }

    @Override
    public void onPictureDownloaded(Boolean result, String path) {
        Logger.d(TAG, "onPictureDownload: " + result);
        if (result) {
            showPicture(path);
        } else {
            doCenteredBlinkingAlert(AlertState.PICTURE_NOK);
        }
    }

    @Override
    public void onSDCardFull(boolean full) {

    }

    private Bitmap resizeBitmap(String file, int width, int height) {

        BitmapFactory.Options bmpOpt = new BitmapFactory.Options();
        bmpOpt.inJustDecodeBounds = true;

        int heightRatio = (int)Math.ceil(bmpOpt.outHeight / (float)height);
        int widthRatio = (int)Math.ceil(bmpOpt.outWidth / (float)width);

        if (heightRatio > 1 || widthRatio > 1) {
            if (heightRatio > widthRatio) {
                bmpOpt.inSampleSize = heightRatio;
            } else {
                bmpOpt.inSampleSize = widthRatio;
            }
        }

        bmpOpt.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(file, bmpOpt);
        return bitmap;
    }

    private void showPicture(String path) {
        Bitmap photo = resizeBitmap(path, 138, 138);

        mLock.lock();
        mPictureView.setImageBitmap(photo);
        mPictureView.setVisibility(View.VISIBLE);
        doCenteredBlinkingAlert(AlertState.NONE); // remove current alert
        updateBitmap();
        mRender = false;
        mLock.unlock();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPictureView.setImageBitmap(null);
                mPictureView.setVisibility(View.INVISIBLE);
                mRender = true;

                /*AnimatorSet picAnim = new AnimatorSet();

                ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(mPictureView, "scaleX", 0.9f).setDuration(1000);
                ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(mPictureView, "scaleY", 0.9f).setDuration(1000);
                ObjectAnimator transX = ObjectAnimator.ofFloat(mPictureView, "translationX", 0, 150).setDuration(1000);
                ObjectAnimator transY = ObjectAnimator.ofFloat(mPictureView, "translationY", 0, 20).setDuration(1000);
                ObjectAnimator alpha = ObjectAnimator.ofFloat(mPictureView, "alpha", 1.0f, 0.0f).setDuration(1000);

                picAnim.addListener(new AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mRender = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mPictureView.setVisibility(View.INVISIBLE);
                        mPictureView.setImageBitmap(null);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                picAnim.play(scaleDownX).with(scaleDownY).with(transX).with(transY);
                picAnim.play(alpha).after(transX);
                picAnim.start();*/

            }
        }, 2000);
    }

    @Override
    public void onDroneDeviceStop() {
        appendLog("dronestop");
        mDeviceName = EnumDevices.NONE;
        mIsDeviceConnected = false;
        Utils.setInvisible(mPictureView, mDrawingCanvasLayout, mTvBattery, mImgWifi, mImgBattery, mSpeedLayout, mAltitudeLayout, mInstructionLayout,
                mRecordingLayout);
        setStartScreen();
        Logger.d(TAG, "Device Stop");
        mCurrentState = null;
    }

    @Override
    public void onGPSHomeChangedUpdate(String msg) {

    }

    @Override
    public void onGPSControllerPosition(double lat, double lon, double alt) {
        appendLog("gps," + lat + "," + lon + "," + alt);
        appendGPSLog(lat, lon);
    }

    @Override
    public void onToggleHUD() {
        if (DeviceController.getInstance() != null && !DeviceController.getInstance().getDeviceInfo().equals(EnumDevices.JUMPINGSUMO)) {
            if (mDrawingCanvasLayout.getVisibility() == View.VISIBLE) {
                mDrawingCanvasLayout.setVisibility(View.INVISIBLE);
                if (DeviceController.getInstance().getDeviceInfo().equals(EnumDevices.MINIDRONE)) {
                    mSpeedLayout.setVisibility(View.INVISIBLE);
                }
            } else if (mDrawingCanvasLayout.getVisibility() == View.INVISIBLE) {
                mDrawingCanvasLayout.setVisibility(View.VISIBLE);
                if (DeviceController.getInstance().getDeviceInfo().equals(EnumDevices.MINIDRONE)) {
                    mSpeedLayout.setVisibility(View.VISIBLE);
                }

            }
        }
    }

    @Override
    public void onGPSFixed(boolean fixed) {
        if (fixed) {
            appendLog("gpsfixed");
        } else {
            appendLog("gpsnotfixed");
        }
    }

    @Override
    public void onVideoRecording(Boolean isRecording) {
        appendLog("videorec," + isRecording);
        if (isRecording) {
            mRecordingLayout.setVisibility(View.VISIBLE);
        } else {
            mRecordingLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onTrickDone() {
        doCenteredBlinkingAlert(AlertState.TRICK_DONE);
    }

    @Override
    public void onDisconnect() {
        appendLog("disconnect");
        setStartScreen();
    }

    public void setStartScreen() {
        if (mGlassReady) {
            Logger.d(TAG, "Setting startup screen");
            mDrawingCanvasLayout.setBackground(null);
            Utils.setInvisible(mPictureView, mDrawingCanvasLayout, mTvBattery, mImgWifi, mImgBattery, mSpeedLayout, mAltitudeLayout,
                    mInstructionLayout, mRecordingLayout);
            mTvSpeed.setText("0.0 km/h");
            mTvAltitude.setText("0.0 m");
        }
    }

    private void createLogFolder() {
        if (!LOG_STATUS) {
            LOG_STATUS = true;

            SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
            LOG_FOLDER_PATH = LOG_ROOT_PATH + dateFormat.format(System.currentTimeMillis());

            File file = new File(LOG_FOLDER_PATH);
            if (!file.exists()) {
                file.mkdirs();
            }

            GLASS_LOG_FILE = LOG_FOLDER_PATH + "/glass_log.txt";
            GPS_LOG_FILE = LOG_FOLDER_PATH + "/gps_log.txt";
            START_TIME = System.currentTimeMillis();
        }
    }

    public void appendLog(String text) {

        // only log when user requests
        if (!Preferences.getLoggingState(mContext)) {
            return;
        }

        createLogFolder();

        File logFile = new File(GLASS_LOG_FILE);

        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            SimpleDateFormat dateFormat = new SimpleDateFormat("mm:ss:SS");
            buf.append(dateFormat.format(System.currentTimeMillis() - START_TIME) + ";");
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void appendGPSLog(double lat, double lon) {

        // only log when user requests
        if (!Preferences.getLoggingState(mContext)) {
            return;
        }

        createLogFolder();

        File gpsLog = new File(GPS_LOG_FILE);

        if (!gpsLog.exists()) {
            try {
                gpsLog.createNewFile();
                BufferedWriter buf = new BufferedWriter(new FileWriter(gpsLog, true));
                buf.append("latitude,longitude");
                buf.newLine();
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(gpsLog, true));
            buf.append(lat + "," + lon);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
