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

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.sonymobile.dronecontrol.R;
import com.sonymobile.dronecontrol.alert.AlertTutorial;
import com.sonymobile.dronecontrol.alert.AlertTutorial.AlertState;
import com.sonymobile.dronecontrol.connection.FTPConnectionDeleteAll;
import com.sonymobile.dronecontrol.connection.FTPConnectionListener;
import com.sonymobile.dronecontrol.connection.FTPConnectionTask;
import com.sonymobile.dronecontrol.controller.BebopDeviceController;
import com.sonymobile.dronecontrol.controller.DeviceController.DeviceState;
import com.sonymobile.dronecontrol.controller.DeviceControllerListener;
import com.sonymobile.dronecontrol.controller.DeviceListenerHandler;
import com.sonymobile.dronecontrol.fragment.BebopKeymapFragment;
import com.sonymobile.dronecontrol.fragment.SettingsFragment;
import com.sonymobile.dronecontrol.liveware.Constants;
import com.sonymobile.dronecontrol.liveware.GlassesDroneControl;
import com.sonymobile.dronecontrol.settings.Preferences;
import com.sonymobile.dronecontrol.utils.BebopCalibratorFragment;
import com.sonymobile.dronecontrol.utils.EnumDevices;
import com.sonymobile.dronecontrol.utils.Logger;
import com.sonymobile.dronecontrol.utils.Notifier;
import com.sonymobile.dronecontrol.utils.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class BebopActivity extends FragmentActivity implements DeviceControllerListener, Callback {

    private static String TAG = BebopActivity.class.getSimpleName();

    // Device controller
    public BebopDeviceController bebopDeviceController;
    public ARDiscoveryDeviceService mService;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    // Video cofiguration
    private MediaCodec mVideoDecoder;
    private MediaFormat mVideoFormat;
    private ByteBuffer mCSDBuffer;
    private ByteBuffer mVideoCodecData;
    private ByteBuffer[] mVideoInputBuffers;
    private BufferInfo mVideoBufferInfo;
    private boolean mWaitVideo = true;
    private boolean mWaitIFrame = true;
    private Surface mSurface = null;
    private SurfaceView mSurfaceView;
    private boolean mIsVideoConfigured = false;
    private boolean mIsVideoRecording = false;
    private boolean mVideoRecRequested = false;
    private Lock mVideoLock;
    private static final String VIDEO_MIME_TYPE = "video/avc";
    private static final int VIDEO_DEQUEUE_TIMEOUT = 33000;
    private static final int VIDEO_WIDTH = 640;
    private static final int VIDEO_HEIGHT = 368;
    private static final short RSSI_LIMIT = -90;
    private long startMs = System.currentTimeMillis();

    // UI elements
    private ImageView mBatteryValue;
    private TextView mEmergencyBt;
    private TextView mAltitudeValue;
    private TextView mSpeedValue;
    private RelativeLayout mDroneDrawingDisplay;
    private RelativeLayout mAltitudeDisplay;
    private RelativeLayout mSpeedDisplay;
    private RelativeLayout mMinimizedDisplay;
    private RelativeLayout mLeftBar;
    private ImageButton mLeftButton;
    private ImageView mGlassIcon;
    private ImageButton mRightButton;
    private ImageView mDroneDrawing;
    private ImageView mWifiView;
    private ImageView mBackgroundView;
    private RelativeLayout mThumbnailLayout;
    private RelativeLayout mRecLayout;
    private ImageView mPhotoDownload;
    private Bitmap mThumbnailBackground = null;
    private Animation mPhotoAnimation = null;
    private boolean mImageResized = false;
    private Chronometer mRecChronometer;
    private Dialog mSDCardDialog;

    // FTP connection
    private static String LOCAL_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/DroneControl/";
    private static final String BEBOP_REMOTE_FOLDER = "internal_000/Bebop_Drone/media/";
    private static final String BEBOP_REMOTE_PORT = "21";

    // Screen elements
    private FragmentManager mFragmentManager;
    private FragmentTransaction mFragmentTransaction;
    private SettingsFragment mSettingsFragment = new SettingsFragment();
    private BebopKeymapFragment mKeymapFragment = new BebopKeymapFragment();
    private BebopCalibratorFragment mCalibrationFragment = null;
    private List<View> mHideForSettings;

    // Other stuff
    private short mWifiRSSI;
    private boolean mIsPCMDBlocked;
    private boolean mTrianglePressed = false;
    private boolean mIsGlassConnected = false;
    private boolean mLowBattery = false;
    private boolean mShowSDFullAlert = false;
    private boolean mIsSDFull = false;
    private boolean mCanTakePicture = true;
    private static final int CAMERA_TILT_STEP = 12;
    private static final int CAMERA_MAX_PAN_RIGHT = 100;
    private static final int CAMERA_MAX_PAN_LEFT = -100;
    private static final int CAMERA_DEFAULT = 0;
    private static final int CAMERA_MAX_TILT_UP = 35;
    private static final int CAMERA_MAX_TILT_DOWN = -48;
    private static final double JOYSTICK_THRESHOLD = 0.20;
    private byte mTilt = 0;
    private byte mPan = 0;

    private AlertDialog mConnectionAlert;
    private boolean mDidConnectionSucceed;
    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(15);

    private boolean mEnableFence = false;
    private boolean mOutdoorMode = false;
    private boolean mAutoPilotMode = false;
    private float mSensitivityValue;
    private int mMaxAltitude;

    private boolean mButtonR1 = false;
    private boolean mButtonR2 = false;
    private boolean mButtonL1 = false;
    private boolean mButtonL2 = false;
    private ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM flipDirection = ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM
            .ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_BACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVideoLock = new ReentrantLock();

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

        // Reset home position when it starts
        Preferences.setGoHomeSettingState(this, false);

        mFragmentManager = getSupportFragmentManager();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mCalibrationFragment = new BebopCalibratorFragment();
        fragmentTransaction.add(R.id.calibration_fragment, mCalibrationFragment, "CALIBRATION");
        fragmentTransaction.commit();
    }

    private void prepareUI() {
        mSpeedDisplay = (RelativeLayout)findViewById(R.id.shadow_speed);
        mSpeedDisplay.setVisibility(View.VISIBLE);
        mAltitudeDisplay = (RelativeLayout)findViewById(R.id.shadow_altitude);
        mRecChronometer = (Chronometer)findViewById(R.id.rec_chrono);
        mAltitudeDisplay.setVisibility(View.VISIBLE);
        mDroneDrawingDisplay = (RelativeLayout)findViewById(R.id.shadow_drone_img);
        mMinimizedDisplay = (RelativeLayout)findViewById(R.id.shadow_drone_name);
        mLeftBar = (RelativeLayout)findViewById(R.id.left_vertical_bar);
        mLeftBar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMinimizedDisplay.getVisibility() == View.INVISIBLE) {
                    Utils.setInvisible(mSpeedDisplay, mAltitudeDisplay, mDroneDrawingDisplay);
                    mMinimizedDisplay.setVisibility(View.VISIBLE);
                } else {
                    Utils.setInvisible(mMinimizedDisplay);
                    mSpeedDisplay.setVisibility(View.VISIBLE);
                    mAltitudeDisplay.setVisibility(View.VISIBLE);
                    mDroneDrawingDisplay.setVisibility(View.VISIBLE);
                }
            }
        });

        TextView tvDroneName = (TextView)findViewById(R.id.tv_drone_name);
        tvDroneName.setText("BEBOP");

        mRightButton = (ImageButton)findViewById(R.id.bt_right);
        mRightButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mKeymapFragment.isVisible() || mSettingsFragment.isVisible()) {
                    closeCurrentFragment();
                    updateUserPreferences();
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

        mDroneDrawing = (ImageView)findViewById(R.id.iv_drone_img);
        mDroneDrawing.setBackground(getResources().getDrawable(R.drawable.bebop_outlines));

        mBackgroundView = (ImageView)findViewById(R.id.background);
        mBackgroundView.setVisibility(View.INVISIBLE);

        mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        mSurfaceView.getHolder().addCallback(this);

        mThumbnailLayout = (RelativeLayout)findViewById(R.id.thumbnail_shadow);
        mRecLayout = (RelativeLayout)findViewById(R.id.rec_shadow);

        mPhotoDownload = (ImageView)findViewById(R.id.photo_download);
        mPhotoDownload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhotoDownload.getVisibility() == View.VISIBLE) {
                    resizePicture();
                }
            }
        });

        mSurfaceView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhotoDownload.getVisibility() == View.VISIBLE) {
                    resizePicture();
                }
            }
        });

        mGlassIcon = (ImageView)findViewById(R.id.iv_glass_status);

        mEmergencyBt = (TextView)findViewById(R.id.emergencyBt);
        mEmergencyBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (bebopDeviceController != null) {
                    bebopDeviceController.sendEmergency();
                }
            }
        });

        mBatteryValue = (ImageView)findViewById(R.id.iv_battery_status);
        mAltitudeValue = (TextView)findViewById(R.id.tv_altitude);
        mAltitudeValue.setText("");
        mSpeedValue = (TextView)findViewById(R.id.tv_speed);
        mWifiView = (ImageView)findViewById(R.id.iv_wifi_status);
        mSpeedValue.setText("");
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
            mHideForSettings.add(findViewById(R.id.status_shadow));
            mHideForSettings.add(mGlassIcon);
            mHideForSettings.add(mBatteryValue);
            mHideForSettings.add(mWifiView);
            mHideForSettings.add(mEmergencyBt);
            return true;
        }
        return false;
    }

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

    //Update app glasses icon
    private void updateGlassInformation() {
        if (GlassesDroneControl.getInstance() != null) {
            if (bebopDeviceController != null) {
                bebopDeviceController.notifyConnection();
                bebopDeviceController.getInitialStates();
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

        if (!mImageResized) {
            mPhotoAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_in);
            mImageResized = true;
        } else {
            mPhotoAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_out);
            mImageResized = false;
        }

        mThumbnailLayout.startAnimation(mPhotoAnimation);
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
        mCalibrationFragment.disableListeners();

        if (bebopDeviceController != null) {
            bebopDeviceController.clearCurrentCommands();
            bebopDeviceController.sendCameraOrientation((byte)0, (byte)0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mDidConnectionSucceed = false;

        showConnectionAlert();
        LocalBroadcastManager.getInstance(this).registerReceiver(mGlassIconReceiver, new IntentFilter(Constants.INTENT_DRONE_CONTROL));
        mWaitVideo = true;
        DeviceListenerHandler.registerListener(this);
        BebopDeviceController.getInstance().configureDevice(this, mService);
        bebopDeviceController = BebopDeviceController.getInstance();
        if (bebopDeviceController != null) {
            bebopDeviceController.setVideoFrameListener(this);
        }
        mHandler.postDelayed(mConnectionRunnable, 200);
        mCalibrationFragment.enableListeners();
        mSDCardDialog = new Dialog(this);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mDidConnectionSucceed) {
                    dismissConnectionAlert();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_connection_failed, "Bebop"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    finish();
                }
            }
        }, CONNECTION_TIMEOUT);
    }

    private void updateUserPreferences() {
        mEnableFence = Preferences.getFenceState(getApplication());
        mOutdoorMode = Preferences.getOutdoorMode(getApplication());
        mSensitivityValue = Preferences.getGamepadSensitivity(getApplication());
        if (mSensitivityValue == 0.0f) {
            mSensitivityValue = Utils.MEDIUM_SENSITIVITY; //default sensitivity
            Preferences.setGamepadSensitivity(getApplication(), mSensitivityValue);
        }

        mMaxAltitude = Preferences.getMaxAltitude(getApplication());
        mAutoPilotMode = Preferences.getAutoPilotMode(getApplication());

        if (bebopDeviceController != null) {
            bebopDeviceController.sendOutdoorMode(mOutdoorMode);
            bebopDeviceController.sendMaxAltitude(mMaxAltitude);
            bebopDeviceController.setGamepadSensitivity(mSensitivityValue);
        }
    }

    @Override
    public void onBackPressed() {
        showBackDialog();
    }

    private final Runnable mConnectionRunnable = new Runnable() {
        boolean res = true;

        @Override
        public void run() {
            res = BebopDeviceController.getInstance().initDevice();

            if (!res) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Could not connect to " + EnumDevices.BEBOP.deviceName, Toast.LENGTH_SHORT).show();
                    }
                });
                finish();
            }

            if (bebopDeviceController != null) {
                Date currentDate = new Date(System.currentTimeMillis());
                bebopDeviceController.setCurrentTime(currentDate);
                bebopDeviceController.setCurrentDate(currentDate);
                bebopDeviceController.getInitialSettings();
                bebopDeviceController.getInitialStates();
                bebopDeviceController.sendFlatTrim();
                bebopDeviceController.sendRecordVideo(false);
                bebopDeviceController.setPictureFormatSnapshot();
                bebopDeviceController.setPictureAutoWhiteBalance();
                bebopDeviceController.setVideoStreaming(true);
            }
            updateUserPreferences();
            updateGlassInformation();
        }
    };

    public void setCalibrationStarted(boolean state) {
        if (bebopDeviceController != null) {
            bebopDeviceController.sendMagnetCalibration(state);
        }
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

        if (bebopDeviceController != null) {
            bebopDeviceController.deinitDevice();
        }

        if (mIsVideoRecording) {
            bebopDeviceController.sendRecordVideo(false);
        }

        DeviceListenerHandler.unregisterListener(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGlassIconReceiver);
        bebopDeviceController = null;
    }

    @Override
    public void onDisconnect() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Could not connect to " + EnumDevices.BEBOP.deviceName, Toast.LENGTH_SHORT).show();
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
    public void onConnectionTimeout() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Could not connect to " + EnumDevices.BEBOP.deviceName, Toast.LENGTH_SHORT).show();
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
    public void onAltitudeChange(final double meters) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAltitudeValue.setText(String.format(" %.2f m", meters));
            }
        });
    }

    @Override
    public void onSpeedChange(final double speedX, final double speedY, final double speedZ) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSpeedValue.setText(Utils.getKmModularVector(speedX, speedY, speedZ) + " km/h");
            }
        });
    }

    @Override
    public void onAlertStateChange(AlertTutorial.AlertState alert) {
        final int alertMessage;
        if (alert == AlertState.CRITICAL_BATTERY) {
            if (bebopDeviceController.getDeviceState() == DeviceState.FLYING) {
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
        bebopDeviceController.setDeviceState(state);
    }

    @Override
    public void onNewVideoFrame(byte[] frame, int size, boolean flush) {
        decodeVideo(frame, size, flush);
    }


    @Override
    public void onDroneDeviceInit(EnumDevices device) {

    }

    @Override
    public void onGPSHomeChangedUpdate(final String msg) {
        Logger.d(TAG, "Home changed update - " + msg);
    }

    @Override
    public void onGPSControllerPosition(double latitude, double longitude, double altitude) {

    }

    @Override
    public void onToggleHUD() {
    }

    @Override
    public void onGPSFixed(boolean fixed) {
        if (fixed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "GPS signal FIXED!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onTrickDone() {

    }

    @Override
    public void onVideoRecording(Boolean isRecording) {
        if (isRecording && !mVideoRecRequested) {
            bebopDeviceController.sendRecordVideo(false);
            return;
        }
        mIsVideoRecording = isRecording;
        if (isRecording) {
            mRecChronometer.setBase(SystemClock.elapsedRealtime());
            mRecChronometer.start();
            mRecLayout.setVisibility(View.VISIBLE);
        } else {
            mRecLayout.setVisibility(View.INVISIBLE);
            mRecChronometer.stop();
        }
    }

    @Override
    public void onTakePicture(Boolean result) {
        if (result) {
            if (bebopDeviceController != null) {
                new FTPConnectionTask(this, mFtpListener)
                        .execute(bebopDeviceController.getIPAddress(), BEBOP_REMOTE_FOLDER, BEBOP_REMOTE_PORT, LOCAL_PATH);
            }
        } else {
            // enable new picture only after
            mCanTakePicture = true;
        }
    }

    @Override
    public void onPictureDownloaded(Boolean result, final String path) {
        if (!result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_picture_error), Toast.LENGTH_SHORT).show();
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
        mIsSDFull = full;

        if (bebopDeviceController != null) {
            if (mIsSDFull && bebopDeviceController.getDeviceState() == DeviceState.LANDED) {
                if (!mShowSDFullAlert) {
                    mShowSDFullAlert = true;
                    bebopDeviceController.performCenteredBlinkAlert(AlertState.SDCARD_FULL);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.dialog_sdcard_full_title));
                    builder.setMessage(getString(R.string.dialog_sdcard_full_message));
                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (bebopDeviceController != null) {
                                new FTPConnectionDeleteAll(BebopActivity.this)
                                        .execute(bebopDeviceController.getIPAddress(), BEBOP_REMOTE_FOLDER, BEBOP_REMOTE_PORT);
                                mShowSDFullAlert = false;
                            }
                        }
                    });
                    builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    mSDCardDialog = builder.create();
                    mSDCardDialog.show();
                }
            } else {
                mShowSDFullAlert = false;
            }
        }
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
    public void onDroneDeviceStop() {
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
        mWifiRSSI = rssi;
        enableDistanceLimiting();
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

    private void enableDistanceLimiting() {
        if (mEnableFence) {
            //define wifi minimum acceptable rssi
            if (mWifiRSSI < RSSI_LIMIT) {
                //block movements
                blockPCMDFromActivity(5000);
                //send alert
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), getString(R.string.toast_wifi_connection_lost), Toast.LENGTH_LONG).show();

                    }
                });
                if (bebopDeviceController != null) {
                    bebopDeviceController.performCenteredBlinkAlert(AlertState.WEAK_SIGNAL);
                }
                long[] pattern = new long[]{0, 1000, 1000, 1000, 1000, 1000, 1000, 1000};
                Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    private void blockPCMDFromActivity(long time) {
        setPCMDBlocked(true);
        bebopDeviceController.setRoll((byte)0);
        bebopDeviceController.setPitch((byte)0);
        bebopDeviceController.setFlag((byte)0);
        bebopDeviceController.setYaw((byte)0);
        bebopDeviceController.setGaz((byte)0);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                setPCMDBlocked(false);
            }
        }, time);
    }

    public void setPCMDBlocked(boolean block) {
        mIsPCMDBlocked = block;
    }

    public boolean isPCMDBlocked() {
        return mIsPCMDBlocked;
    }

    public void doTrick(MotionEvent ev) {
        if (bebopDeviceController != null) {
            if (ev.getAxisValue(MotionEvent.AXIS_HAT_Y) == -1) {
                flipDirection = ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_FRONT;
            } else if (ev.getAxisValue(MotionEvent.AXIS_HAT_Y) == 1) {
                flipDirection = ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_BACK;
            } else if (ev.getAxisValue(MotionEvent.AXIS_HAT_X) == 1) {
                flipDirection = ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_RIGHT;
            } else if (ev.getAxisValue(MotionEvent.AXIS_HAT_X) == -1) {
                flipDirection = ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_LEFT;
            }
            bebopDeviceController.sendFlip(flipDirection);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        // if the button is pressed
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {

                case KeyEvent.KEYCODE_BUTTON_X: //square button
                    if (bebopDeviceController != null) {
                        if (mIsSDFull) {
                            bebopDeviceController.performCenteredBlinkAlert(AlertState.SDCARD_FULL);
                        } else if (mCanTakePicture && bebopDeviceController.sendTakePicture()) {
                            bebopDeviceController.performCenteredBlinkAlert(AlertState.TAKING_PICTURE);
                            mCanTakePicture = false;
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_BUTTON_A: // X button
                    if (bebopDeviceController != null) {
                        if (bebopDeviceController.getDeviceState().equals(DeviceState.LANDED)) {
                            bebopDeviceController.sendTakeoff();
                        } else if (bebopDeviceController.getDeviceState().equals(DeviceState.FLYING)) {
                            bebopDeviceController.sendLanding();
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_BUTTON_B: // circle button
                    if (!mIsVideoRecording) {
                        if (mIsSDFull) {
                            bebopDeviceController.performCenteredBlinkAlert(AlertState.SDCARD_FULL);
                        } else if (!mVideoRecRequested && bebopDeviceController.sendRecordVideo(true)) {
                            bebopDeviceController.performCenteredBlinkAlert(AlertState.RECORDING_STARTED);
                            mRecLayout.setVisibility(View.VISIBLE);
                            mVideoRecRequested = true;
                        }
                    } else {
                        if (bebopDeviceController.sendRecordVideo(false)) {
                            bebopDeviceController.performCenteredBlinkAlert(AlertState.VIDEO_REC_FINISHED);
                            mRecLayout.setVisibility(View.INVISIBLE);
                            mVideoRecRequested = false;
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y: // triangle button
                    if (bebopDeviceController != null) {
                        mTrianglePressed = true;
                    }
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    mButtonL1 = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    mButtonR1 = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_L2:
                    mButtonL2 = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_R2:
                    mButtonR2 = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_SELECT:
                    break;
                case KeyEvent.KEYCODE_BUTTON_START: // options button
                    break;
                case KeyEvent.KEYCODE_BUTTON_THUMBL: // R3
                    break;
                case KeyEvent.KEYCODE_BUTTON_THUMBR: // L3
                    break;
                case KeyEvent.KEYCODE_BACK:
                    onBackPressed();
                    break;

            }
            if (mButtonR1 && mButtonR2 && mButtonL1 && mButtonL2) {
                if (bebopDeviceController != null) {
                    bebopDeviceController.sendEmergency();
                }
            }
        }

        // if the button is released
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {

                case KeyEvent.KEYCODE_BUTTON_L2:
                    mButtonL2 = false;
                    break;
                case KeyEvent.KEYCODE_BUTTON_R2:
                    mButtonR2 = false;
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    mButtonL1 = false;
                    break;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    mButtonR1 = false;
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y: // Triangle
                    if (bebopDeviceController != null) {
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

            if (bebopDeviceController != null) {
                if (mAutoPilotMode) {
                    autoPiloting(ev);
                } else {
                    normalPiloting(ev);
                }

                if (mTrianglePressed) {
                    doTrick(ev);
                } else {

                    if (ev.getAxisValue(MotionEvent.AXIS_HAT_X) == 1) {
                        mPan = CAMERA_MAX_PAN_RIGHT;
                    } else if (ev.getAxisValue(MotionEvent.AXIS_HAT_X) == -1) {
                        mPan = CAMERA_MAX_PAN_LEFT;
                    } else {
                        mPan = CAMERA_DEFAULT;
                    }

                    if (mTilt < CAMERA_MAX_TILT_UP) {
                        if (ev.getAxisValue(MotionEvent.AXIS_HAT_Y) == -1) {
                            mTilt += CAMERA_TILT_STEP;
                        }
                    }
                    if (mTilt > CAMERA_MAX_TILT_DOWN) {
                        if (ev.getAxisValue(MotionEvent.AXIS_HAT_Y) == 1) {
                            mTilt -= CAMERA_TILT_STEP;
                        }
                    }
                    bebopDeviceController.sendCameraOrientation(mTilt, mPan);
                }
            }
        }
        return true;
    }

    private void autoPiloting(MotionEvent ev) {
        if (!isPCMDBlocked()) {
            //stabilize the drone when right stick is near the middle
            if ((ev.getAxisValue(MotionEvent.AXIS_Z) < JOYSTICK_THRESHOLD) && (ev.getAxisValue(MotionEvent.AXIS_Z) > -JOYSTICK_THRESHOLD)) {
                bebopDeviceController.setRoll((byte)0);
                bebopDeviceController.setFlag((byte)0);
            }
            if ((ev.getAxisValue(MotionEvent.AXIS_RZ) < JOYSTICK_THRESHOLD) && (ev.getAxisValue(MotionEvent.AXIS_RZ) > -JOYSTICK_THRESHOLD)) {
                bebopDeviceController.setPitch((byte)0);
                bebopDeviceController.setFlag((byte)0);
            }
            if ((ev.getAxisValue(MotionEvent.AXIS_Z) < JOYSTICK_THRESHOLD) &&
                    (ev.getAxisValue(MotionEvent.AXIS_Z) > -JOYSTICK_THRESHOLD) &&
                    (ev.getAxisValue(MotionEvent.AXIS_RZ) < JOYSTICK_THRESHOLD) &&
                    (ev.getAxisValue(MotionEvent.AXIS_RZ) > -JOYSTICK_THRESHOLD)) {
                bebopDeviceController.setRoll((byte)0);
                bebopDeviceController.setPitch((byte)0);
                bebopDeviceController.setFlag((byte)0);
            }

            //right joystick - forward and back
            if (ev.getAxisValue(MotionEvent.AXIS_RZ) < -JOYSTICK_THRESHOLD) {
                bebopDeviceController.setPitch((byte)-(ev.getAxisValue(MotionEvent.AXIS_RZ) * 100));
                bebopDeviceController.setFlag((byte)1);
            } else if (ev.getAxisValue(MotionEvent.AXIS_RZ) > JOYSTICK_THRESHOLD) {
                bebopDeviceController.setPitch((byte)-(ev.getAxisValue(MotionEvent.AXIS_RZ) * 100));
                bebopDeviceController.setFlag((byte)1);
            }

            //right joystick - yaw  left and right
            if ((ev.getAxisValue(MotionEvent.AXIS_Z) < JOYSTICK_THRESHOLD) && (ev.getAxisValue(MotionEvent.AXIS_Z) > -JOYSTICK_THRESHOLD)) {
                bebopDeviceController.setYaw((byte)0);
            } else if (ev.getAxisValue(MotionEvent.AXIS_Z) < -JOYSTICK_THRESHOLD) {
                bebopDeviceController.setYaw((byte)(ev.getAxisValue(MotionEvent.AXIS_Z) * 100));
            } else if (ev.getAxisValue(MotionEvent.AXIS_Z) > JOYSTICK_THRESHOLD) {
                bebopDeviceController.setYaw((byte)(ev.getAxisValue(MotionEvent.AXIS_Z) * 100));
            }

            // left joystick - gaz up and down
            if (ev.getAxisValue(MotionEvent.AXIS_Y) < JOYSTICK_THRESHOLD && ev.getAxisValue(MotionEvent.AXIS_Y) > -JOYSTICK_THRESHOLD) {
                bebopDeviceController.setGaz((byte)0);
            } else if (ev.getAxisValue(MotionEvent.AXIS_Y) > JOYSTICK_THRESHOLD) {
                bebopDeviceController.setGaz((byte)-(ev.getAxisValue(MotionEvent.AXIS_Y) * 100));
            } else if (ev.getAxisValue(MotionEvent.AXIS_Y) < -JOYSTICK_THRESHOLD) {
                bebopDeviceController.setGaz((byte)-(ev.getAxisValue(MotionEvent.AXIS_Y) * 100));
            }
        }
    }

    private void normalPiloting(MotionEvent ev) {
        if (!isPCMDBlocked()) {

            //stabilize the drone when right stick is near the middle
            if ((ev.getAxisValue(MotionEvent.AXIS_Z) < JOYSTICK_THRESHOLD) && (ev.getAxisValue(MotionEvent.AXIS_Z) > -JOYSTICK_THRESHOLD)) {
                bebopDeviceController.setRoll((byte)0);
                bebopDeviceController.setFlag((byte)0);
            }
            if ((ev.getAxisValue(MotionEvent.AXIS_RZ) < JOYSTICK_THRESHOLD) && (ev.getAxisValue(MotionEvent.AXIS_RZ) > -JOYSTICK_THRESHOLD)) {
                bebopDeviceController.setPitch((byte)0);
                bebopDeviceController.setFlag((byte)0);
            }
            if ((ev.getAxisValue(MotionEvent.AXIS_Z) < JOYSTICK_THRESHOLD) &&
                    (ev.getAxisValue(MotionEvent.AXIS_Z) > -JOYSTICK_THRESHOLD) &&
                    (ev.getAxisValue(MotionEvent.AXIS_RZ) < JOYSTICK_THRESHOLD) &&
                    (ev.getAxisValue(MotionEvent.AXIS_RZ) > -JOYSTICK_THRESHOLD)) {
                bebopDeviceController.setRoll((byte)0);
                bebopDeviceController.setPitch((byte)0);
                bebopDeviceController.setFlag((byte)0);
            }

            //right joystick - forward and back
            if (ev.getAxisValue(MotionEvent.AXIS_RZ) < -JOYSTICK_THRESHOLD) {
                bebopDeviceController.setPitch((byte)-(ev.getAxisValue(MotionEvent.AXIS_RZ) * 100));
                bebopDeviceController.setFlag((byte)1);
            } else if (ev.getAxisValue(MotionEvent.AXIS_RZ) > JOYSTICK_THRESHOLD) {
                bebopDeviceController.setPitch((byte)-(ev.getAxisValue(MotionEvent.AXIS_RZ) * 100));
                bebopDeviceController.setFlag((byte)1);
            }

            //right joystick - roll left and right
            if (ev.getAxisValue(MotionEvent.AXIS_Z) < -JOYSTICK_THRESHOLD) {
                bebopDeviceController.setRoll((byte)(ev.getAxisValue(MotionEvent.AXIS_Z) * 100));
                bebopDeviceController.setFlag((byte)1);
            } else if (ev.getAxisValue(MotionEvent.AXIS_Z) > JOYSTICK_THRESHOLD) {
                bebopDeviceController.setRoll((byte)(ev.getAxisValue(MotionEvent.AXIS_Z) * 100));
                bebopDeviceController.setFlag((byte)1);
            }

            // left joystick - yaw left and right
            if ((ev.getAxisValue(MotionEvent.AXIS_X) < JOYSTICK_THRESHOLD) && (ev.getAxisValue(MotionEvent.AXIS_X) > -JOYSTICK_THRESHOLD)) {
                bebopDeviceController.setYaw((byte)0);
            } else if (ev.getAxisValue(MotionEvent.AXIS_X) < -JOYSTICK_THRESHOLD) {
                bebopDeviceController.setYaw((byte)(ev.getAxisValue(MotionEvent.AXIS_X) * 100));
            } else if (ev.getAxisValue(MotionEvent.AXIS_X) > JOYSTICK_THRESHOLD) {
                bebopDeviceController.setYaw((byte)(ev.getAxisValue(MotionEvent.AXIS_X) * 100));
            }

            // left joystick - gaz up and down
            if (ev.getAxisValue(MotionEvent.AXIS_Y) < JOYSTICK_THRESHOLD && ev.getAxisValue(MotionEvent.AXIS_Y) > -JOYSTICK_THRESHOLD) {
                bebopDeviceController.setGaz((byte)0);
            } else if (ev.getAxisValue(MotionEvent.AXIS_Y) > JOYSTICK_THRESHOLD) {
                bebopDeviceController.setGaz((byte)-(ev.getAxisValue(MotionEvent.AXIS_Y) * 100));
            } else if (ev.getAxisValue(MotionEvent.AXIS_Y) < -JOYSTICK_THRESHOLD) {
                bebopDeviceController.setGaz((byte)-(ev.getAxisValue(MotionEvent.AXIS_Y) * 100));
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurface = holder.getSurface();
        mVideoLock.lock();
        initMediaCodec(VIDEO_MIME_TYPE);
        mVideoLock.unlock();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mVideoLock.lock();
        releaseMediaCodec();
        mVideoLock.unlock();
        mSurface = null;
    }

    private void decodeVideo(byte[] frame, int size, boolean isIFrame) {
        int indexDecode = 0;

        mVideoLock.lock();

        if (mVideoDecoder != null) {
            if (!mIsVideoConfigured && isIFrame) {
                mCSDBuffer = getCSD(frame, isIFrame);
                if (mCSDBuffer != null) {
                    configureMediaCodec();
                }
            }

            if (mIsVideoConfigured && (!mWaitIFrame || isIFrame)) {
                mWaitIFrame = false;

                try {
                    indexDecode = mVideoDecoder.dequeueInputBuffer(VIDEO_DEQUEUE_TIMEOUT);
                } catch (IllegalStateException e) {
                    Logger.e(TAG, "Error while dequeueing input buffer");
                }

                if (indexDecode >= 0) {
                    mVideoCodecData = mVideoInputBuffers[indexDecode];
                    mVideoCodecData.clear();
                    if (size >= 0) {
                        mVideoCodecData.put(frame, 0, size);
                        mVideoCodecData.clear();
                        mVideoDecoder.queueInputBuffer(indexDecode, 0, size, (System.currentTimeMillis() - startMs), 0);
                    }
                } else {
                    Logger.e(TAG, "Cannot decode frame...");
                    mWaitIFrame = true;
                }
            }

            int outIndex = -1;
            try {
                outIndex = mVideoDecoder.dequeueOutputBuffer(mVideoBufferInfo, 10000);
                while (outIndex >= 0) {
                    while (mVideoBufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }

                    if (mWaitVideo) {
                        mWaitVideo = false;
                        mDidConnectionSucceed = dismissConnectionAlert();
                    }

                    mVideoDecoder.releaseOutputBuffer(outIndex, true);
                    outIndex = mVideoDecoder.dequeueOutputBuffer(mVideoBufferInfo, 10000);
                }
            } catch (IllegalStateException e) {
                Logger.e(TAG, "Error while dequeue output buffer");
            }
        }

        mVideoLock.unlock();
    }

    private void initMediaCodec(String mime) {
        try {
            mVideoDecoder = MediaCodec.createDecoderByType(VIDEO_MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mCSDBuffer != null) {
            configureMediaCodec();
        }
    }

    private void configureMediaCodec() {
        mVideoFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
        mVideoFormat.setByteBuffer("csd-0", mCSDBuffer);

        try {
            if (mSurface != null) {
                mVideoDecoder.configure(mVideoFormat, mSurface, null, 0);
                mVideoDecoder.start();

                mVideoInputBuffers = mVideoDecoder.getInputBuffers();
                mVideoBufferInfo = new BufferInfo();

                mIsVideoConfigured = true;
            }
        } catch (Exception e) {
            Logger.e(TAG, "Could not configure mediacodec");
        }
    }

    private void releaseMediaCodec() {
        if ((mVideoDecoder != null)) {
            if (mIsVideoConfigured) {
                mVideoDecoder.stop();
                mVideoDecoder.release();
            }
            mIsVideoConfigured = false;
            mVideoDecoder = null;
        }
    }

    private ByteBuffer getCSD(byte[] frame, boolean isIFrame) {
        int spsSize = -1;

        if (isIFrame) {
            int searchIndex = 0;
            for (searchIndex = 4; searchIndex <= frame.length - 4; searchIndex++) {
                if (0 == frame[searchIndex] &&
                        0 == frame[searchIndex + 1] &&
                        0 == frame[searchIndex + 2] &&
                        1 == frame[searchIndex + 3]) {

                    break;
                }
            }
            spsSize = searchIndex;
            for (searchIndex = spsSize + 4; searchIndex <= frame.length - 4; searchIndex++) {
                if (0 == frame[searchIndex] &&
                        0 == frame[searchIndex + 1] &&
                        0 == frame[searchIndex + 2] &&
                        1 == frame[searchIndex + 3]) {
                    break;
                }
            }
            int csdSize = searchIndex;
            byte[] csdInfo = new byte[csdSize];
            System.arraycopy(frame, 0, csdInfo, 0, csdSize);
            return ByteBuffer.wrap(csdInfo);
        }
        return null;
    }
}
