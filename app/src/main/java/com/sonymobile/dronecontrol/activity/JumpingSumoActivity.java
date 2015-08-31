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

import com.parrot.arsdk.arcommands.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.sonymobile.dronecontrol.R;
import com.sonymobile.dronecontrol.alert.AlertTutorial;
import com.sonymobile.dronecontrol.alert.AlertTutorial.AlertState;
import com.sonymobile.dronecontrol.alert.AlertTutorial.DroneAction;
import com.sonymobile.dronecontrol.connection.FTPConnectionListener;
import com.sonymobile.dronecontrol.connection.FTPConnectionTask;
import com.sonymobile.dronecontrol.controller.DeviceController;
import com.sonymobile.dronecontrol.controller.DeviceControllerListener;
import com.sonymobile.dronecontrol.controller.DeviceListenerHandler;
import com.sonymobile.dronecontrol.controller.JumpingSumoDeviceController;
import com.sonymobile.dronecontrol.fragment.JSumoKeymapFragment;
import com.sonymobile.dronecontrol.fragment.SettingsFragment;
import com.sonymobile.dronecontrol.liveware.Constants;
import com.sonymobile.dronecontrol.liveware.GlassesDroneControl;
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
import android.os.Environment;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JumpingSumoActivity extends FragmentActivity implements DeviceControllerListener {

    private static String TAG = JumpingSumoActivity.class.getSimpleName();

    // Device controller
    public JumpingSumoDeviceController jumpingSumoDeviceController;
    public ARDiscoveryDeviceService mService;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    // UI elements
    private TextView mEmergencyBt;
    private Bitmap mBitmapBackground;
    private ImageView mPhotoDownload;
    private RelativeLayout mThumbnailLayout;
    private ImageView mGlassIcon;
    private ImageButton mRightButton;
    private ImageButton mLeftButton;
    private ImageView mWifiView;
    private ImageView mBatteryValue;
    private RelativeLayout mDroneDrawingDisplay;
    private RelativeLayout mMinimizedDisplay;
    private RelativeLayout mLeftBar;
    private ImageView mDroneDrawing;
    private List<View> mHideForSettings;
    private Bitmap mThumbnailBackground = null;
    private SurfaceView mSurfaceView;
    private ImageView mBackgroundView;
    private Animation mPhotoAnimation = null;

    // FTP connection
    private static String LOCAL_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/DroneControl/";
    private static final String JS_REMOTE_FOLDER = "internal_000/Jumping_Sumo/media/";
    private static final String JS_REMOTE_PORT = "21";

    // Screen elements
    private FragmentManager mFragmentManager;
    private FragmentTransaction mFragmentTransaction;
    private AlertDialog mConnectionAlert;
    private boolean mDidConnectionSucceed;
    private SettingsFragment mSettingsFragment = new SettingsFragment();
    private JSumoKeymapFragment mKeymapFragment = new JSumoKeymapFragment();
    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    private boolean mTrianglePressed = false;
    private boolean mIsGlassConnected = false;
    private boolean mCanTakePicture = true;
    private boolean mImageSize = false;
    private static double PI_2 = (Math.PI / 2);
    private static double PI = Math.PI;
    private static final double JOYSTICK_THRESHOLD = 0.20;

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
        mHideForSettings.add(findViewById(R.id.status_shadow));
        mHideForSettings.add(mGlassIcon);
        mHideForSettings.add(mBatteryValue);
        mHideForSettings.add(mWifiView);
        mHideForSettings.add(mEmergencyBt);
    }

    private void prepareUI() {
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

        mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        mSurfaceView.setVisibility(View.GONE);

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
        tvDroneName.setText("JUMPING SUMO");

        mGlassIcon = (ImageView)findViewById(R.id.iv_glass_status);

        mDroneDrawing = (ImageView)findViewById(R.id.iv_drone_img);
        mDroneDrawing.setBackground(getResources().getDrawable(R.drawable.jumping_sumo_outlines));
        mBackgroundView = (ImageView)findViewById(R.id.background);
        mBackgroundView.setVisibility(View.VISIBLE);

        mEmergencyBt = (TextView)findViewById(R.id.emergencyBt);
        mEmergencyBt.setVisibility(View.INVISIBLE);

        mThumbnailLayout = (RelativeLayout)findViewById(R.id.thumbnail_shadow);
        mPhotoDownload = (ImageView)findViewById(R.id.photo_download);
        mPhotoDownload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhotoDownload.getVisibility() == View.VISIBLE) {
                    resizePicture();
                }
            }
        });

        mBackgroundView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhotoDownload.getVisibility() == View.VISIBLE) {
                    resizePicture();
                }
            }
        });

        mBatteryValue = (ImageView)findViewById(R.id.iv_battery_status);
        mWifiView = (ImageView)findViewById(R.id.iv_wifi_status);
        setBackgroundImage();
    }

    private FTPConnectionListener mFtpListener = new FTPConnectionListener() {
        @Override
        public void onFTPConnectionFinish(final Boolean result, final String path) {
            new Notifier() {
                @Override
                public void onNotify(DeviceControllerListener listener) {
                    if (listener != null) {
                        listener.onPictureDownloaded(result, path);
                    }

                }
            };
        }
    };

    public void showSettingsFragment() {
        if (!mSettingsFragment.isAdded()) {
            mFragmentTransaction = mFragmentManager.beginTransaction();
            mFragmentTransaction.add(R.id.fragments_frame, mSettingsFragment);
            mFragmentTransaction.commit();
            if (mHideForSettings.contains(mThumbnailLayout)) {
                mThumbnailLayout.setBackground(null);
            }
            Utils.setInvisible(mHideForSettings);
        } else {
            if (!mSettingsFragment.isVisible()) {
                mFragmentTransaction = mFragmentManager.beginTransaction();
                mFragmentTransaction.show(mSettingsFragment);
                mFragmentTransaction.commit();
                if (mHideForSettings.contains(mThumbnailLayout)) {
                    mThumbnailLayout.setBackground(null);
                }
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
            if (mHideForSettings.contains(mThumbnailLayout)) {
                mThumbnailLayout.setBackground(null);
            }
            Utils.setInvisible(mHideForSettings);
        } else {
            if (!mKeymapFragment.isVisible()) {
                mFragmentTransaction = mFragmentManager.beginTransaction();
                mFragmentTransaction.show(mKeymapFragment);
                mFragmentTransaction.commit();
                if (mHideForSettings.contains(mThumbnailLayout)) {
                    mThumbnailLayout.setBackground(null);
                }
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
        if (mHideForSettings.contains(mThumbnailLayout)) {
            mThumbnailLayout.setBackgroundResource(R.drawable.rounded_shape_overlay);
        }
        Utils.setVisible(mHideForSettings);
        mRightButton.setBackgroundResource(R.drawable.btn_settings);
        mLeftButton.setVisibility(View.VISIBLE);

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
            if (jumpingSumoDeviceController != null) {
                jumpingSumoDeviceController.notifyConnection();
                jumpingSumoDeviceController.getInitialStates();
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

    private void resizePicture() {
        if (mPhotoAnimation != null) {
            mPhotoAnimation = null;
        }
        if (!mImageSize) {
            mPhotoAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_in);
            mImageSize = true;
        } else {
            mPhotoAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_out);
            mImageSize = false;
        }
        mThumbnailLayout.startAnimation(mPhotoAnimation);
    }

    private void setBackgroundImage() {
        if (mBitmapBackground == null) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            final DisplayMetrics metrics = new DisplayMetrics();

            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            int height = metrics.heightPixels;
            int width = metrics.widthPixels;

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

        if (jumpingSumoDeviceController != null) {
            jumpingSumoDeviceController.clearCurrentCommands();
        }

        mBackgroundView.setImageDrawable(null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setBackgroundImage();
        showConnectionAlert();

        LocalBroadcastManager.getInstance(this).registerReceiver(mGlassIconReceiver, new IntentFilter(Constants.INTENT_DRONE_CONTROL));

        DeviceListenerHandler.registerListener(this);
        JumpingSumoDeviceController.getInstance().configureDevice(this, mService);
        jumpingSumoDeviceController = JumpingSumoDeviceController.getInstance();

        mHandler.postDelayed(mConnectionRunnable, 200);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mDidConnectionSucceed) {
                    dismissConnectionAlert();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_connection_failed, "Jumping Sumo"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    finish();
                }
            }
        }, CONNECTION_TIMEOUT);
    }

    private final Runnable mConnectionRunnable = new Runnable() {
        boolean res = true;

        @Override
        public void run() {
            res = JumpingSumoDeviceController.getInstance().initDevice();

            if (!res) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Could not connect to " + EnumDevices.JUMPINGSUMO.deviceName, Toast.LENGTH_SHORT)
                                .show();
                    }
                });
                finish();
            }

            if (jumpingSumoDeviceController != null) {
                Date currentDate = new Date(System.currentTimeMillis());
                jumpingSumoDeviceController.setCurrentTime(currentDate);
                jumpingSumoDeviceController.setCurrentDate(currentDate);
                jumpingSumoDeviceController.getInitialSettings();
                jumpingSumoDeviceController.getInitialStates();
            }

            updateGlassInformation();

            mDidConnectionSucceed = dismissConnectionAlert();
        }
    };

    @Override
    public void onBackPressed() {
        showBackDialog();
    }

    @Override
    protected void onStop() {
        super.onStop();

        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onDroneDeviceStop();
                }
            }
        };

        if (jumpingSumoDeviceController != null) {
            jumpingSumoDeviceController.deinitDevice();
        }

        DeviceListenerHandler.unregisterListener(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGlassIconReceiver);
        jumpingSumoDeviceController = null;
    }

    @Override
    public void onDisconnect() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Could not connect to " + EnumDevices.JUMPINGSUMO.deviceName, Toast.LENGTH_SHORT).show();
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
    }

    @Override
    public void onAltitudeChange(double meters) {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View
                        .SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWifiSignalChange(final short rssi) {
        Logger.d(TAG, "Wifi RSSI: " + rssi);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (rssi >= -70) {
                    mWifiView.setImageDrawable(getResources().getDrawable(R.drawable.ic_signal_strong));
                } else if (rssi >= -80) {
                    mWifiView.setImageDrawable(getResources().getDrawable(R.drawable.ic_signal_good));
                } else if (rssi >= -90) {
                    mWifiView.setImageDrawable(getResources().getDrawable(R.drawable.ic_signal_fair));
                } else if (rssi >= -100) {
                    mWifiView.setImageDrawable(getResources().getDrawable(R.drawable.ic_signal_poor));
                } else {
                    mWifiView.setImageDrawable(getResources().getDrawable(R.drawable.ic_no_signal));
                }
            }
        });
    }

    @Override
    public void onSpeedChange(final double speedX, final double speedY, final double speedZ) {

    }

    @Override
    public void onAlertStateChange(AlertTutorial.AlertState alert) {
        if (alert == AlertState.CRITICAL_BATTERY) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.toast_message_battery_landed, Toast.LENGTH_LONG).show();
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
    public void onStateChange(DeviceController.DeviceState state) {

    }

    @Override
    public void onNewVideoFrame(byte[] frame, int size, boolean flush) {

    }

    @Override
    public void onDroneDeviceInit(EnumDevices device) {

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
    public void onConnectionTimeout() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Could not connect to " + EnumDevices.JUMPINGSUMO.deviceName, Toast.LENGTH_SHORT).show();
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
    public void onTakePicture(Boolean result) {
        if (result) {
            new FTPConnectionTask(this, mFtpListener)
                    .execute(jumpingSumoDeviceController.getIPAddress(), JS_REMOTE_FOLDER, JS_REMOTE_PORT, LOCAL_PATH);
        } else {
            // enable taking picture after finishing previous
            mCanTakePicture = true;
        }
    }

    @Override
    public void onPictureDownloaded(Boolean result, final String path) {
        if (!result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Could not take picture!", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setPhotoThumbnailImage(path);
                    mThumbnailLayout.setMinimumHeight(mPhotoDownload.getHeight());
                    mThumbnailLayout.setVisibility(View.VISIBLE);
                    mHideForSettings.add(mPhotoDownload);
                    if (!mHideForSettings.contains(mThumbnailLayout)) {
                        mHideForSettings.add(mThumbnailLayout);
                    }
                }
            });
        }

        // enable taking picture after finishing previous
        mCanTakePicture = true;
    }

    @Override
    public void onSDCardFull(boolean full) {

    }

    private void setPhotoThumbnailImage(String path) {
        if (mThumbnailBackground != null) {
            mThumbnailBackground.recycle();
            mThumbnailBackground = null;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();

        int height = mPhotoDownload.getHeight();
        int width = mPhotoDownload.getWidth();

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        if (options.outHeight > height || options.outWidth > width) {
            final int heightRatio = Math.round((float)options.outHeight / (float)height);
            final int widthRatio = Math.round((float)options.outWidth / (float)width);
            options.inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        options.inJustDecodeBounds = false;
        mThumbnailBackground = BitmapFactory.decodeFile(path, options);
        mPhotoDownload.setImageBitmap(mThumbnailBackground);
        if (mPhotoDownload.getVisibility() != View.VISIBLE) {
            mPhotoDownload.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (jumpingSumoDeviceController == null) {
            return super.dispatchKeyEvent(event);
        }
        // if the button is pressed
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {

                case KeyEvent.KEYCODE_BUTTON_X: //square button
                    if (jumpingSumoDeviceController != null) {
                        if (mCanTakePicture && jumpingSumoDeviceController.sendTakePicture()) {
                            jumpingSumoDeviceController.performCenteredBlinkAlert(AlertState.TAKING_PICTURE);
                            mCanTakePicture = false;
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_BUTTON_A: // X button
                    break;
                case KeyEvent.KEYCODE_BUTTON_B: // circle button
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y: // triangle button
                    mTrianglePressed = true;
                    jumpingSumoDeviceController.performAlertTutorialAction(DroneAction.ACTION_TRICK);
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    jumpingSumoDeviceController.sendPilotingAddCapOffset(-(float)PI_2);

                    break;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    jumpingSumoDeviceController.sendPilotingAddCapOffset((float)PI_2);

                    break;
                case KeyEvent.KEYCODE_BUTTON_SELECT:
                    break;
                case KeyEvent.KEYCODE_BUTTON_START:
                    break;
                case KeyEvent.KEYCODE_BUTTON_THUMBL: // R3
                    jumpingSumoDeviceController.sendAnimationsSimpleAnimation(
                            ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_ENUM.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_SPINJUMP);
                    jumpingSumoDeviceController.performAlertTutorialAction(DroneAction.ACTION_JUMP);
                    break;
                case KeyEvent.KEYCODE_BUTTON_THUMBR: // L3
                    jumpingSumoDeviceController.sendAnimationsSimpleAnimation(
                            ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_ENUM.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_SPIN);
                    break;
                case KeyEvent.KEYCODE_BACK:
                    onBackPressed();
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BUTTON_Y: // triangle button
                    mTrianglePressed = false;
            }
        }
        return true;

    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        float speedRotationX;
        float speedY;
        float turboRotation;
        float turboSpeed;

        if (jumpingSumoDeviceController != null && (ev.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK && ev
                .getAction() == MotionEvent.ACTION_MOVE) {

            if (mTrianglePressed) { // Animations
                if (ev.getAxisValue(MotionEvent.AXIS_HAT_Y) == -1) {
                    jumpingSumoDeviceController.performAlertTutorialAction(DroneAction.ACTION_TRICK_DONE);
                    jumpingSumoDeviceController.sendAnimationsSimpleAnimation(ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_ENUM.
                            ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_SLOWSHAKE);
                } else if (ev.getAxisValue(MotionEvent.AXIS_HAT_Y) == 1) {
                    jumpingSumoDeviceController.performAlertTutorialAction(DroneAction.ACTION_TRICK_DONE);
                    jumpingSumoDeviceController.sendAnimationsSimpleAnimation(ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_ENUM.
                            ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_SPIRAL);
                } else if (ev.getAxisValue(MotionEvent.AXIS_HAT_X) == 1) {
                    jumpingSumoDeviceController.performAlertTutorialAction(DroneAction.ACTION_TRICK_DONE);
                    jumpingSumoDeviceController.sendAnimationsSimpleAnimation(ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_ENUM.
                            ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_ONDULATION);
                } else if (ev.getAxisValue(MotionEvent.AXIS_HAT_X) == -1) {
                    jumpingSumoDeviceController.performAlertTutorialAction(DroneAction.ACTION_TRICK_DONE);
                    jumpingSumoDeviceController.sendAnimationsSimpleAnimation(ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_ENUM.
                            ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_SLALOM);
                }
            }

            //diretional - up/down
            //Jump high/long
            if (ev.getAxisValue(MotionEvent.AXIS_HAT_Y) == 1) {
                jumpingSumoDeviceController.sendAnimationsJump(ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_ENUM.
                        ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_LONG);
            }
            if (ev.getAxisValue(MotionEvent.AXIS_HAT_Y) == -1) {
                jumpingSumoDeviceController.sendAnimationsJump(ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_ENUM.
                        ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_HIGH);
            }

            //diretional - right/left - turn
            if (ev.getAxisValue(MotionEvent.AXIS_HAT_Y) == 1) {
                jumpingSumoDeviceController.setTurnRatio((byte)PI);
            }
            if (ev.getAxisValue(MotionEvent.AXIS_HAT_Y) == -1) {
                jumpingSumoDeviceController.setTurnRatio((byte)-PI);
            }

            //left joystick - forward and back
            speedY = ev.getAxisValue(MotionEvent.AXIS_Y);
            if (speedY > 0) {
                turboSpeed = (-1 * (movementIntensity(speedY)));
            } else {
                turboSpeed = (movementIntensity(speedY));
            }

            // left joystick intensity with turbo (L2)
            if (turboSpeed == 100) {
                turboSpeed += movementIntensity((ev.getAxisValue(MotionEvent.AXIS_LTRIGGER)));
            } else if (turboSpeed == -100) {
                turboSpeed -= movementIntensity((ev.getAxisValue(MotionEvent.AXIS_LTRIGGER)));
            }

            if (ev.getAxisValue(MotionEvent.AXIS_LTRIGGER) > JOYSTICK_THRESHOLD || ev.getAxisValue(MotionEvent.AXIS_RTRIGGER) > JOYSTICK_THRESHOLD) {
                jumpingSumoDeviceController.performAlertTutorialAction(DroneAction.ACTION_TURBO);
            }

            //right joystick - rotational
            speedRotationX = ev.getAxisValue(MotionEvent.AXIS_Z);
            // right joystick intensity with turbo (R2)
            if (speedRotationX > 0) {
                turboRotation = movementIntensity(speedRotationX) + ((ev.getAxisValue(MotionEvent.AXIS_RTRIGGER) * 300));
            } else {
                turboRotation = (-1 * (movementIntensity(speedRotationX) + (ev.getAxisValue(MotionEvent.AXIS_RTRIGGER) * 300)));
            }

            jumpingSumoDeviceController.setSpeed((byte)(turboSpeed / 2));
            jumpingSumoDeviceController.setTurnRatio((byte)(turboRotation / 4));
            jumpingSumoDeviceController.setFlag((byte)1);

            return true;
        }

        return false;
    }

    private float movementIntensity(float variation) {
        return (float)(Math.pow(100 * Math.abs(variation), 2) / 100);
    }

}
