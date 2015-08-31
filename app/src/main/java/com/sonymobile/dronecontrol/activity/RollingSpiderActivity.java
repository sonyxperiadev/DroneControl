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

package com.sonymobile.dronecontrol.activity;

import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.sonymobile.dronecontrol.R;
import com.sonymobile.dronecontrol.alert.AlertTutorial;
import com.sonymobile.dronecontrol.alert.AlertTutorial.AlertState;
import com.sonymobile.dronecontrol.controller.DeviceController.DeviceState;
import com.sonymobile.dronecontrol.controller.DeviceControllerListener;
import com.sonymobile.dronecontrol.controller.DeviceListenerHandler;
import com.sonymobile.dronecontrol.controller.RollingSpiderDeviceController;
import com.sonymobile.dronecontrol.fragment.RSpiderKeymapFragment;
import com.sonymobile.dronecontrol.fragment.SettingsFragment;
import com.sonymobile.dronecontrol.liveware.Constants;
import com.sonymobile.dronecontrol.liveware.GlassesDroneControl;
import com.sonymobile.dronecontrol.settings.Preferences;
import com.sonymobile.dronecontrol.utils.EnumDevices;
import com.sonymobile.dronecontrol.utils.Logger;
import com.sonymobile.dronecontrol.utils.Notifier;
import com.sonymobile.dronecontrol.utils.Utils;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RollingSpiderActivity extends FragmentActivity implements DeviceControllerListener {

    private static String TAG = RollingSpiderActivity.class.getSimpleName();

    // Device controller
    public RollingSpiderDeviceController rollingSpiderDeviceController;
    public ARDiscoveryDeviceService mService;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    // UI Elements
    private TextView mEmergencyBt;
    private Bitmap mBitmapBackground;
    private ImageView mGlassIcon;
    private RelativeLayout mDroneDrawingDisplay;
    private RelativeLayout mMinimizedDisplay;
    private RelativeLayout mLeftBar;
    private ImageView mDroneDrawing;
    private List<View> mHideForSettings;
    private ImageButton mRightButton;
    private ImageButton mLeftButton;
    private ImageView mWifiView;
    private ImageView mBatteryValue;
    private ImageView mBackgroundView;
    private SurfaceView mSurfaceView;

    // Screen elements
    private FragmentManager mFragmentManager;
    private FragmentTransaction mFragmentTransaction;
    private AlertDialog mConnectionAlert;
    private boolean mDidConnectionSucceed;
    private SettingsFragment mSettingsFragment = new SettingsFragment();
    private RSpiderKeymapFragment mKeymapFragment = new RSpiderKeymapFragment();
    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(18);
    private static final double JOYSTICK_THRESHOLD = 0.20;

    private boolean mButtonR1 = false;
    private boolean mButtonR2 = false;
    private boolean mButtonL1 = false;
    private boolean mButtonL2 = false;
    private boolean mAutoPilotMode = false;
    private float mSensitivityValue;
    private boolean mLowBattery = false;
    private boolean mTrianglePressed = false;
    private boolean mIsGlassConnected = false;
    private ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM direction = ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM
            .ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_BACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHideForSettings = new ArrayList<>();
        mHideForSettings.clear();

        final int flags = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View
                .SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        getWindow().getDecorView().setSystemUiVisibility(flags);

        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(flags);
                }
            }
        });

        Intent intent = getIntent();
        mService = intent.getParcelableExtra(Constants.EXTRA_DEVICE_SERVICE);

        setContentView(R.layout.activity_piloting);

        prepareUI();

        mIsGlassConnected = intent.getBooleanExtra(GlassesDroneControl.TAG, false);
        if (mIsGlassConnected) {
            mGlassIcon.setBackground(getResources().getDrawable(R.drawable.ic_glass_on));
        }

        mFragmentManager = getSupportFragmentManager();
        mHideForSettings.add(mGlassIcon);
        mHideForSettings.add(mBatteryValue);
        mHideForSettings.add(mWifiView);
        mHideForSettings.add(findViewById(R.id.status_shadow));
    }

    private void prepareUI() {
        mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        mSurfaceView.setVisibility(View.GONE);
        ImageView bar = (ImageView)findViewById(R.id.bar_1);
        bar.setVisibility(View.GONE);

        mDroneDrawingDisplay = (RelativeLayout)findViewById(R.id.shadow_drone_img);
        mMinimizedDisplay = (RelativeLayout)findViewById(R.id.shadow_drone_name);
        mLeftBar = (RelativeLayout)findViewById(R.id.left_vertical_bar);
        mLeftBar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMinimizedDisplay.getVisibility() == View.INVISIBLE) {
                    Utils.setInvisible(mDroneDrawingDisplay);
                    mMinimizedDisplay.setVisibility(View.VISIBLE);
                } else {
                    Utils.setInvisible(mMinimizedDisplay);
                    mDroneDrawingDisplay.setVisibility(View.VISIBLE);
                }
            }
        });
        TextView tvDroneName = (TextView)findViewById(R.id.tv_drone_name);
        tvDroneName.setText("ROLLING SPIDER");

        mGlassIcon = (ImageView)findViewById(R.id.iv_glass_status);

        mDroneDrawing = (ImageView)findViewById(R.id.iv_drone_img);
        mDroneDrawing.setBackground(getResources().getDrawable(R.drawable.rolling_spider_outlines));

        mRightButton = (ImageButton)findViewById(R.id.bt_right);
        mRightButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mKeymapFragment.isVisible() || mSettingsFragment.isVisible()) {
                    closeCurrentFragment();
                } else {
                    showSettingsFragment();
                }

            }
        });

        mLeftButton = (ImageButton)findViewById(R.id.bt_left);
        mLeftButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mKeymapFragment.isVisible() || mSettingsFragment.isVisible()) {
                    //Not visible
                } else {
                    showKeymapFragment();
                }
            }
        });

        mBackgroundView = (ImageView)findViewById(R.id.background);
        mBackgroundView.setVisibility(View.VISIBLE);

        mEmergencyBt = (TextView)findViewById(R.id.emergencyBt);
        mEmergencyBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (rollingSpiderDeviceController != null) {
                    rollingSpiderDeviceController.sendEmergency();
                }
            }
        });

        mBatteryValue = (ImageView)findViewById(R.id.iv_battery_status);

        mWifiView = (ImageView)findViewById(R.id.iv_wifi_status);
        mWifiView.setVisibility(View.GONE);
        setBackgroundImage();
    }

    public void showSettingsFragment() {
        if (!mSettingsFragment.isAdded()) {
            mFragmentTransaction = mFragmentManager.beginTransaction();
            mFragmentTransaction.add(R.id.fragments_frame, mSettingsFragment);
            mFragmentTransaction.commit();
            Utils.setInvisible(mHideForSettings);
        } else {
            if (!mSettingsFragment.isVisible()) {
                mFragmentTransaction = mFragmentManager.beginTransaction();
                mFragmentTransaction.show(mSettingsFragment);
                mFragmentTransaction.commit();
                Utils.setInvisible(mHideForSettings);
            }
        }
        mRightButton.setBackgroundResource(R.drawable.btn_exit);
        mLeftButton.setVisibility(View.INVISIBLE);
    }

    public void showKeymapFragment() {
        if (!mKeymapFragment.isAdded()) {
            mFragmentTransaction = mFragmentManager.beginTransaction();
            mFragmentTransaction.add(R.id.fragments_frame, mKeymapFragment);
            mFragmentTransaction.commit();
            Utils.setInvisible(mHideForSettings);
        } else {
            if (!mKeymapFragment.isVisible()) {
                mFragmentTransaction = mFragmentManager.beginTransaction();
                mFragmentTransaction.show(mKeymapFragment);
                mFragmentTransaction.commit();
                Utils.setInvisible(mHideForSettings);
            }
        }
        mRightButton.setBackgroundResource(R.drawable.btn_exit);
        mLeftButton.setVisibility(View.INVISIBLE);
    }

    public void closeCurrentFragment() {
        mFragmentTransaction = mFragmentManager.beginTransaction();
        if (mSettingsFragment.isVisible()) {
            mFragmentTransaction.hide(mSettingsFragment);
        } else if (mKeymapFragment.isVisible()) {
            mFragmentTransaction.hide(mKeymapFragment);
        }
        mFragmentTransaction.commit();
        Utils.setVisible(mHideForSettings);
        mRightButton.setBackgroundResource(R.drawable.btn_settings);
        mLeftButton.setVisibility(View.VISIBLE);
    }

    private void showBackDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.close_application);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog backAlertDialog = builder.create();
        backAlertDialog.show();
    }

    private BroadcastReceiver mGlassIconReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Constants.INTENT_DRONE_CONTROL)) {
                updateGlassInformation();
            }
        }
    };

    private void updateGlassInformation() {
        if (GlassesDroneControl.getInstance() != null) {
            if (rollingSpiderDeviceController != null) {
                rollingSpiderDeviceController.notifyConnection();
                rollingSpiderDeviceController.getInitialStates();
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (GlassesDroneControl.getInstance() != null) {
                    mGlassIcon.setBackground(getResources().getDrawable(R.drawable.ic_glass_on));
                } else {
                    mGlassIcon.setBackground(getResources().getDrawable(R.drawable.ic_glass_off));
                }
            }
        });
    }

    private void showConnectionAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.device_connection_title);
        builder.setMessage(R.string.device_connection_message);
        builder.setCancelable(false);
        mConnectionAlert = builder.create();
        mConnectionAlert.show();
    }

    private boolean dismissConnectionAlert() {
        if (mConnectionAlert != null) {
            mConnectionAlert.dismiss();
            return true;
        }
        return false;
    }

    private void setBackgroundImage() {
        if (mBitmapBackground == null) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            final DisplayMetrics metrics = new DisplayMetrics();

            int height = metrics.heightPixels;
            int width = metrics.widthPixels;

            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getResources(), R.drawable.background, options);

            if (options.outHeight > height || options.outWidth > width) {
                final int heightRatio = Math.round((float)height / (float)options.outHeight);
                final int widthRatio = Math.round((float)width / (float)options.outWidth);
                options.inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
            }

            options.inJustDecodeBounds = false;
            mBitmapBackground = BitmapFactory.decodeResource(getResources(), R.drawable.background, options);
        }

        if (mBitmapBackground != null) {
            mBackgroundView.setImageBitmap(mBitmapBackground);
        }
    }

    public void doTrick(MotionEvent ev) {
        if (rollingSpiderDeviceController != null) {
            //Call trick command

            if (ev.getAxisValue(MotionEvent.AXIS_HAT_Y) == -1) {
                direction = ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_FRONT;
            } else if (ev.getAxisValue(MotionEvent.AXIS_HAT_Y) == 1) {
                direction = ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_BACK;
            } else if (ev.getAxisValue(MotionEvent.AXIS_HAT_X) == 1) {
                direction = ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_RIGHT;
            } else if (ev.getAxisValue(MotionEvent.AXIS_HAT_X) == -1) {
                direction = ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_LEFT;
            }
            rollingSpiderDeviceController.sendFlip(direction);
        }
    }

    @Override
    protected void onResume() {
        Logger.d(TAG, "onResume");
        super.onResume();

        showConnectionAlert();
        setBackgroundImage();

        DeviceListenerHandler.registerListener(this);
        RollingSpiderDeviceController.getInstance().configureDevice(this, mService);
        rollingSpiderDeviceController = RollingSpiderDeviceController.getInstance();
        new Thread(mConnectionRunnable).start();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mDidConnectionSucceed) {
                    dismissConnectionAlert();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_connection_failed, "Rolling Spider"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    finish();
                }
            }
        }, CONNECTION_TIMEOUT);

        LocalBroadcastManager.getInstance(this).registerReceiver(mGlassIconReceiver, new IntentFilter(Constants.INTENT_DRONE_CONTROL));
    }

    @Override
    public void onAltitudeChange(final double meters) {
    }

    @Override
    public void onSpeedChange(double speedX, double speedY, double speedZ) {
    }

    @Override
    public void onAlertStateChange(AlertTutorial.AlertState alert) {
        final int alertMessage;
        if (alert == AlertState.CRITICAL_BATTERY) {
            if (rollingSpiderDeviceController.getDeviceState() != DeviceState.LANDED) {
                alertMessage = R.string.toast_message_battery_flying;
            } else {
                alertMessage = R.string.toast_message_battery_landed;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getResources().getString(alertMessage), Toast.LENGTH_LONG).show();
                }
            });
        } else if (alert == AlertState.LOW_BATTERY) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.toast_message_battery_low, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onStateChange(DeviceState state) {
        rollingSpiderDeviceController.setDeviceState(state);
    }

    @Override
    public void onNewVideoFrame(byte[] frame, int size, boolean flush) {

    }

    @Override
    public void onConnectionTimeout() {

    }

    @Override
    public void onDroneDeviceInit(EnumDevices device) {

    }

    @Override
    public void onTakePicture(Boolean result) {

    }

    @Override
    public void onPictureDownloaded(Boolean result, String path) {

    }

    @Override
    public void onSDCardFull(boolean full) {

    }

    @Override
    public void onDroneDeviceStop() {

    }

    @Override
    public void onGPSHomeChangedUpdate(String msg) {

    }

    @Override
    public void onGPSControllerPosition(double lat, double lon, double alt) {

    }

    @Override
    public void onToggleHUD() {
    }

    @Override
    public void onGPSFixed(boolean fixed) {

    }

    @Override
    public void onTrickDone() {
    }

    @Override
    public void onVideoRecording(Boolean isRecording) {

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View
                        .SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    public void onWifiSignalChange(short rssi) {
    }

    private final Runnable mConnectionRunnable = new Runnable() {
        boolean res = true;

        @Override
        public void run() {
            res = RollingSpiderDeviceController.getInstance().initDevice();

            if (!res) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Could not connect to " + EnumDevices.MINIDRONE.deviceName, Toast.LENGTH_SHORT)
                                .show();
                    }
                });
                finish();
            }

            // Start command might take a while. Check again for null pointer
            if (rollingSpiderDeviceController != null) {
                Date currentDate = new Date(System.currentTimeMillis());
                rollingSpiderDeviceController.sendDate(currentDate);
                rollingSpiderDeviceController.sendTime(currentDate);
                rollingSpiderDeviceController.getInitialSettings();
                rollingSpiderDeviceController.getInitialStates();
            }
            updateUserPreferences();
            updateGlassInformation();
            mDidConnectionSucceed = dismissConnectionAlert();
        }
    };

    @Override
    protected void onStop() {
        Logger.d(TAG, "onStop");
        super.onStop();

        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onDroneDeviceStop();
                }
            }
        };

        if (rollingSpiderDeviceController != null) {
            rollingSpiderDeviceController.deinitDevice();
        }

        DeviceListenerHandler.unregisterListener(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGlassIconReceiver);
        rollingSpiderDeviceController = null;
    }

    private void updateUserPreferences() {
        mAutoPilotMode = Preferences.getAutoPilotMode(getApplication());
        mSensitivityValue = Preferences.getGamepadSensitivity(getApplication());
        if (mSensitivityValue == 0.0f) {
            mSensitivityValue = Utils.MEDIUM_SENSITIVITY;
            Preferences.setGamepadSensitivity(getApplication(), mSensitivityValue);
        }

        if (rollingSpiderDeviceController != null) {
            rollingSpiderDeviceController.setGamepadSensitivity(mSensitivityValue);
        }
    }

    @Override
    public void onBackPressed() {
        showBackDialog();
    }

    @Override
    public void onDisconnect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Could not connect to " + EnumDevices.MINIDRONE.deviceName, Toast.LENGTH_SHORT).show();
            }
        });

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    @Override
    public void onUpdateBattery(final byte percent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (percent >= 70) {
                    mBatteryValue.setBackground(getResources().getDrawable(R.drawable.ic_battery_5));
                } else if (percent >= 50) {
                    mBatteryValue.setBackground(getResources().getDrawable(R.drawable.ic_battery_4));
                } else if (percent >= 30) {
                    mBatteryValue.setBackground(getResources().getDrawable(R.drawable.ic_battery_3));
                } else if (percent >= 10) {
                    mBatteryValue.setBackground(getResources().getDrawable(R.drawable.ic_battery_2));
                } else {
                    mBatteryValue.setBackground(getResources().getDrawable(R.drawable.ic_battery_1));
                }
            }
        });
        if (percent <= 20 && !mLowBattery) {
            mLowBattery = true;
            new Notifier() {
                @Override
                public void onNotify(DeviceControllerListener listener) {
                    if (listener != null) {
                        listener.onAlertStateChange(AlertState.LOW_BATTERY);
                    }
                }
            };
        }
    }

    @Override
    protected void onPause() {
        mFragmentTransaction = mFragmentManager.beginTransaction();
        if (mSettingsFragment != null) {
            mFragmentTransaction.remove(mSettingsFragment);
        }
        if (mKeymapFragment != null) {
            mFragmentTransaction.remove(mKeymapFragment);
        }
        mFragmentTransaction.commit();
        super.onPause();

        dismissConnectionAlert();

        if (rollingSpiderDeviceController != null) {
            rollingSpiderDeviceController.clearCurrentCommands();
        }
        mBackgroundView.setImageDrawable(null);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // if the button is pressed
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BUTTON_X: //square button
                    if (rollingSpiderDeviceController != null) {
                        if (rollingSpiderDeviceController.sendTakePicture()) {
                            rollingSpiderDeviceController.performCenteredBlinkAlert(AlertState.PICTURE_OK);
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_BUTTON_A: // X button
                    if (rollingSpiderDeviceController != null) {
                        if (rollingSpiderDeviceController.getDeviceState().equals(DeviceState.LANDED)) {
                            rollingSpiderDeviceController.sendTakeoff();
                        } else if (rollingSpiderDeviceController.getDeviceState().equals(DeviceState.FLYING)) {
                            rollingSpiderDeviceController.sendLanding();
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_BUTTON_B: // circle button
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y: // triangle button
                    if (rollingSpiderDeviceController != null) {
                        mTrianglePressed = true;
                    }
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    mButtonL2 = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    mButtonR2 = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_L2:
                    mButtonL1 = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_R2:
                    mButtonR1 = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_SELECT:
                    break;
                case KeyEvent.KEYCODE_BUTTON_START: // options button
                    new Notifier() {
                        @Override
                        public void onNotify(DeviceControllerListener listener) {
                            if (listener != null) {
                                listener.onToggleHUD();
                            }
                        }
                    };
                    break;
                case KeyEvent.KEYCODE_BUTTON_THUMBL: // R3
                    break;
                case KeyEvent.KEYCODE_BUTTON_THUMBR: // L3
                    break;
                case KeyEvent.KEYCODE_BACK:
                    onBackPressed();
                    break;

            }
            if (mButtonR2 && mButtonR1 && mButtonL2 && mButtonL1) {
                if (rollingSpiderDeviceController != null) {
                    rollingSpiderDeviceController.sendEmergency();
                }
            }
        }

        // if the button is released
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {

                case KeyEvent.KEYCODE_BUTTON_L2:
                    mButtonL1 = false;
                    break;
                case KeyEvent.KEYCODE_BUTTON_R2:
                    mButtonR1 = false;
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    mButtonL2 = false;
                    break;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    mButtonR2 = false;
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y: // triangle button
                    if (rollingSpiderDeviceController != null) {
                        mTrianglePressed = false;
                    }
                    break;
            }
        }
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        if ((ev.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK && ev.getAction() == MotionEvent.ACTION_MOVE) {

            if (rollingSpiderDeviceController != null) {

                if (mAutoPilotMode) {
                    autoPiloting(ev);
                } else {
                    normalPiloting(ev);
                }

                if (mTrianglePressed) {
                    doTrick(ev);
                }
            }
        }
        return true;
    }

    private void autoPiloting(MotionEvent ev) {
        //stabilize the drone when right stick is near the middle
        if ((ev.getAxisValue(MotionEvent.AXIS_Z) < JOYSTICK_THRESHOLD) && (ev.getAxisValue(MotionEvent.AXIS_Z) > -JOYSTICK_THRESHOLD)) {
            rollingSpiderDeviceController.setRoll((byte)0);
            rollingSpiderDeviceController.setFlag((byte)0);
        }
        if ((ev.getAxisValue(MotionEvent.AXIS_RZ) < JOYSTICK_THRESHOLD) && (ev.getAxisValue(MotionEvent.AXIS_RZ) > -JOYSTICK_THRESHOLD)) {
            rollingSpiderDeviceController.setPitch((byte)0);
            rollingSpiderDeviceController.setFlag((byte)0);
        }
        if ((ev.getAxisValue(MotionEvent.AXIS_Z) < JOYSTICK_THRESHOLD) &&
                (ev.getAxisValue(MotionEvent.AXIS_Z) > -JOYSTICK_THRESHOLD) &&
                (ev.getAxisValue(MotionEvent.AXIS_RZ) < JOYSTICK_THRESHOLD) &&
                (ev.getAxisValue(MotionEvent.AXIS_RZ) > -JOYSTICK_THRESHOLD)) {
            rollingSpiderDeviceController.setRoll((byte)0);
            rollingSpiderDeviceController.setPitch((byte)0);
            rollingSpiderDeviceController.setFlag((byte)0);
        }

        //right joystick - forward and back
        if (ev.getAxisValue(MotionEvent.AXIS_RZ) < -JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setPitch((byte)-(ev.getAxisValue(MotionEvent.AXIS_RZ) * 100));
            rollingSpiderDeviceController.setFlag((byte)1);
        } else if (ev.getAxisValue(MotionEvent.AXIS_RZ) > JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setPitch((byte)-(ev.getAxisValue(MotionEvent.AXIS_RZ) * 100));
            rollingSpiderDeviceController.setFlag((byte)1);
        }

        //right joystick - yaw  left and right
        if ((ev.getAxisValue(MotionEvent.AXIS_Z) < JOYSTICK_THRESHOLD) && (ev.getAxisValue(MotionEvent.AXIS_Z) > -JOYSTICK_THRESHOLD)) {
            rollingSpiderDeviceController.setYaw((byte)0);
        } else if (ev.getAxisValue(MotionEvent.AXIS_Z) < -JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setYaw((byte)(ev.getAxisValue(MotionEvent.AXIS_Z) * 100));
        } else if (ev.getAxisValue(MotionEvent.AXIS_Z) > JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setYaw((byte)(ev.getAxisValue(MotionEvent.AXIS_Z) * 100));
        }

        // left joystick - gaz up and down
        if (ev.getAxisValue(MotionEvent.AXIS_Y) < JOYSTICK_THRESHOLD && ev.getAxisValue(MotionEvent.AXIS_Y) > -JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setGaz((byte)0);
        } else if (ev.getAxisValue(MotionEvent.AXIS_Y) > JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setGaz((byte)-(ev.getAxisValue(MotionEvent.AXIS_Y) * 100));
        } else if (ev.getAxisValue(MotionEvent.AXIS_Y) < -JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setGaz((byte)-(ev.getAxisValue(MotionEvent.AXIS_Y) * 100));
        }
    }

    private void normalPiloting(MotionEvent ev) {
        //stabilize the drone when right stick is near the middle
        if ((ev.getAxisValue(MotionEvent.AXIS_Z) < JOYSTICK_THRESHOLD) && (ev.getAxisValue(MotionEvent.AXIS_Z) > -JOYSTICK_THRESHOLD)) {
            rollingSpiderDeviceController.setRoll((byte)0);
            rollingSpiderDeviceController.setFlag((byte)0);
        }
        if ((ev.getAxisValue(MotionEvent.AXIS_RZ) < JOYSTICK_THRESHOLD) && (ev.getAxisValue(MotionEvent.AXIS_RZ) > -JOYSTICK_THRESHOLD)) {
            rollingSpiderDeviceController.setPitch((byte)0);
            rollingSpiderDeviceController.setFlag((byte)0);
        }
        if ((ev.getAxisValue(MotionEvent.AXIS_Z) < JOYSTICK_THRESHOLD) &&
                (ev.getAxisValue(MotionEvent.AXIS_Z) > -JOYSTICK_THRESHOLD) &&
                (ev.getAxisValue(MotionEvent.AXIS_RZ) < JOYSTICK_THRESHOLD) &&
                (ev.getAxisValue(MotionEvent.AXIS_RZ) > -JOYSTICK_THRESHOLD)) {
            rollingSpiderDeviceController.setRoll((byte)0);
            rollingSpiderDeviceController.setPitch((byte)0);
            rollingSpiderDeviceController.setFlag((byte)0);
        }

        //right joystick - forward and back
        if (ev.getAxisValue(MotionEvent.AXIS_RZ) < -JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setPitch((byte)-(ev.getAxisValue(MotionEvent.AXIS_RZ) * 100));
            rollingSpiderDeviceController.setFlag((byte)1);
        } else if (ev.getAxisValue(MotionEvent.AXIS_RZ) > JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setPitch((byte)-(ev.getAxisValue(MotionEvent.AXIS_RZ) * 100));
            rollingSpiderDeviceController.setFlag((byte)1);
        }

        //right joystick - roll left and right
        if (ev.getAxisValue(MotionEvent.AXIS_Z) < -JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setRoll((byte)(ev.getAxisValue(MotionEvent.AXIS_Z) * 100));
            rollingSpiderDeviceController.setFlag((byte)1);
        } else if (ev.getAxisValue(MotionEvent.AXIS_Z) > JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setRoll((byte)(ev.getAxisValue(MotionEvent.AXIS_Z) * 100));
            rollingSpiderDeviceController.setFlag((byte)1);
        }

        // left joystick - yaw left and right
        if ((ev.getAxisValue(MotionEvent.AXIS_X) < JOYSTICK_THRESHOLD) && (ev.getAxisValue(MotionEvent.AXIS_X) > -JOYSTICK_THRESHOLD)) {
            rollingSpiderDeviceController.setYaw((byte)0);
        } else if (ev.getAxisValue(MotionEvent.AXIS_X) < -JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setYaw((byte)(ev.getAxisValue(MotionEvent.AXIS_X) * 100));
        } else if (ev.getAxisValue(MotionEvent.AXIS_X) > JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setYaw((byte)(ev.getAxisValue(MotionEvent.AXIS_X) * 100));
        }

        // left joystick - gaz up and down
        if (ev.getAxisValue(MotionEvent.AXIS_Y) < JOYSTICK_THRESHOLD && ev.getAxisValue(MotionEvent.AXIS_Y) > -JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setGaz((byte)0);
        } else if (ev.getAxisValue(MotionEvent.AXIS_Y) > JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setGaz((byte)-(ev.getAxisValue(MotionEvent.AXIS_Y) * 100));
        } else if (ev.getAxisValue(MotionEvent.AXIS_Y) < -JOYSTICK_THRESHOLD) {
            rollingSpiderDeviceController.setGaz((byte)-(ev.getAxisValue(MotionEvent.AXIS_Y) * 100));
        }
    }
}

