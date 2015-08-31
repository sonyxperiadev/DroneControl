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

import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceBLEService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;
import com.sonymobile.dronecontrol.BuildConfig;
import com.sonymobile.dronecontrol.R;
import com.sonymobile.dronecontrol.bluetooth.BluetoothListener;
import com.sonymobile.dronecontrol.fragment.ScreenFragment;
import com.sonymobile.dronecontrol.fragment.ScreenFragment.FragmentCommunicator;
import com.sonymobile.dronecontrol.fragment.SetupGuideFragment;
import com.sonymobile.dronecontrol.liveware.Constants;
import com.sonymobile.dronecontrol.liveware.GlassesDroneControl;
import com.sonymobile.dronecontrol.utils.EnumDevices;
import com.sonymobile.dronecontrol.utils.Logger;
import com.sonymobile.dronecontrol.utils.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends FragmentActivity implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate, FragmentCommunicator {

    private static String TAG = MainActivity.class.getSimpleName();

    // List refresh
    private Handler mHandler = new Handler();

    // AR services
    public IBinder discoveryServiceBinder;
    private ARDiscoveryService ardiscoveryService;
    private ServiceConnection ardiscoveryServiceConnection;
    private BroadcastReceiver ardiscoveryServicesDevicesListUpdatedReceiver;
    private boolean ardiscoveryServiceBound = false;
    public List<ARDiscoveryDeviceService> mDeviceList;

    // Bluetooth
    private BluetoothListener mBtListener;

    // Screen setup
    private SetupGuideFragment mSetupGuideFragment = new SetupGuideFragment();
    private ScreenFragment mScreenFragment;
    private EnumDevices mDeviceToConnect;
    private static final String SETUP_GUIDE = "setup_guide";
    private static final String CONNECTION = "connection";

    // Connection information
    private static final long MAX_CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    private int mConnectionCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "Drone Control Application - Version: " + BuildConfig.VERSION_NAME);

        setContentView(R.layout.activity_step_one);

        initServiceConnection();
        initBroadcastReceiver();

        mBtListener = new BluetoothListener();
        mDeviceList = new ArrayList<>();

        prepareUI();
    }

    private void prepareUI() {
        ImageView ivBebop = (ImageView)findViewById(R.id.iv_bebop);
        ivBebop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSetupGuideFragment != null && mSetupGuideFragment.isDetached() && (mDeviceToConnect == null)) {
                    if (verifyWifi()) {
                        connectToDevice(EnumDevices.BEBOP);
                    }
                }
            }
        });

        ImageView ivSumo = (ImageView)findViewById(R.id.iv_js);
        ivSumo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSetupGuideFragment != null && mSetupGuideFragment.isDetached() && (mDeviceToConnect == null)) {
                    if (verifyWifi()) {
                        connectToDevice(EnumDevices.JUMPINGSUMO);
                    }
                }
            }
        });

        ImageView ivSpider = (ImageView)findViewById(R.id.iv_rs);
        ivSpider.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSetupGuideFragment != null && mSetupGuideFragment.isDetached() && (mDeviceToConnect == null)) {
                    if (verifyBluetooth()) {
                        connectToDevice(EnumDevices.MINIDRONE);
                    }
                }
            }
        });
    }

    private void initDevice() {
        ARDiscoveryDeviceService selectedService = null;

        if (mDeviceToConnect != null && mScreenFragment.isAdded()) {
            for (ARDiscoveryDeviceService service : mDeviceList) {

                // check available devices
                if (service.getDevice() instanceof ARDiscoveryDeviceBLEService) {
                    if (EnumDevices.MINIDRONE.equals(mDeviceToConnect)) {
                        selectedService = service;
                        break;
                    }
                } else if (ardiscoveryService.getProductFromProductID(service.getProductID())
                        .toString() == ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_ARDRONE.toString()) {
                    if (EnumDevices.BEBOP.equals(mDeviceToConnect)) {
                        selectedService = service;
                        break;
                    }

                } else if (ardiscoveryService.getProductFromProductID(service.getProductID())
                        .toString() == ARDISCOVERY_PRODUCT_ENUM.ARDISCOVERY_PRODUCT_JS.toString()) {
                    if (EnumDevices.JUMPINGSUMO.equals(mDeviceToConnect)) {
                        selectedService = service;
                        break;
                    }
                }
            }

            // if selected device was found
            if (selectedService != null) {
                final FragmentManager manager = getSupportFragmentManager();
                final FragmentTransaction fragmentTransaction = manager.beginTransaction();

                ((TextView)findViewById(R.id.tv_connecting)).setText(getResources().getString(R.string.device_found));
                ((ProgressBar)findViewById(R.id.progress)).setVisibility(View.INVISIBLE);

                if (Utils.DEBUG || (!Utils.DEBUG && mBtListener.isGamepadConnected())) {
                    if (mDeviceToConnect != null) {
                        mHandler.removeCallbacks(mListRefresh);
                        final Intent intent = new Intent(MainActivity.this, mDeviceToConnect.className);
                        intent.putExtra(Constants.EXTRA_DEVICE_SERVICE, selectedService);
                        startActivity(intent);
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), R.string.gamepad_needed, Toast.LENGTH_SHORT).show();
                        }
                    });
                    fragmentTransaction.detach(mScreenFragment);
                    manager.popBackStack();
                    manager.popBackStack();
                }

            }
        }
    }

    private final Runnable mListRefresh = new Runnable() {
        public void run() {
            try {
                onServicesDevicesListUpdated();

                if (mBtListener.isGamepadConnected()) {
                    if (!mSetupGuideFragment.isDetached()) {
                        Utils.setVisible((ImageView)findViewById(R.id.controller_connected_check));
                        ((ImageView)findViewById(R.id.iv_controller_connected))
                                .setImageDrawable(getResources().getDrawable(R.drawable.ic_gamepad_setup_guide_normal));

                    }
                } else {
                    if (!mSetupGuideFragment.isDetached()) {
                        Utils.setInvisible((ImageView)findViewById(R.id.controller_connected_check));
                        ((ImageView)findViewById(R.id.iv_controller_connected))
                                .setImageDrawable(getResources().getDrawable(R.drawable.ic_gamepad_setup_guide_disabled));
                    }
                }
                if (mSetupGuideFragment != null && mSetupGuideFragment.isVisible()) {
                    enableConnectionToParrotDevice(mBtListener.isGamepadConnected());
                }

                if (GlassesDroneControl.getInstance() != null) {
                    if (!mSetupGuideFragment.isDetached()) {
                        Utils.setVisible((ImageView)findViewById(R.id.connection_seg_check));
                        ((ImageView)findViewById(R.id.iv_seg_connected)).setImageResource(R.drawable.ic_glass_setup_guide_normal);
                    }
                } else {
                    if (!mSetupGuideFragment.isDetached()) {
                        Utils.setInvisible((ImageView)findViewById(R.id.connection_seg_check));
                        ((ImageView)findViewById(R.id.iv_seg_connected)).setImageResource(R.drawable.ic_glass_setup_guide_disabled);
                    }
                }

                if (mDeviceToConnect != null && mScreenFragment != null && mScreenFragment.isVisible()) {
                    mConnectionCounter++;
                    if (mConnectionCounter == MAX_CONNECTION_TIMEOUT) {
                        showConnectionTimeoutMessage();
                        mConnectionCounter = 0;
                    }
                } else {
                    mConnectionCounter = 0;
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mHandler.postDelayed(this, 1000);
            }
        }
    };

    private void showConnectionTimeoutMessage() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), R.string.device_not_found, Toast.LENGTH_SHORT).show();
            }
        });

        final FragmentManager manager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = manager.beginTransaction();
        fragmentTransaction.detach(mScreenFragment);
        manager.popBackStack();
    }

    private boolean verifyWifi() {
        boolean result = false;
        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("WiFi").setMessage(R.string.DG_CONNECT_TO_WIFI)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                            dialog.dismiss();
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).show();
        } else {
            result = true;
        }
        return result;
    }

    private boolean verifyBluetooth() {
        boolean result = false;
        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            // Device does not support Bluetooth
        } else if (btAdapter.isEnabled()) {
            result = true;
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth").setMessage(R.string.DG_TURN_ON_BT)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            btAdapter.enable();
                            dialog.dismiss();
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).show();
        }
        return result;
    }

    private void connectToDevice(final EnumDevices device) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mScreenFragment = new ScreenFragment();
        mScreenFragment.setDevice(device);
        mScreenFragment.setCommunicator(this);
        fragmentTransaction.replace(R.id.fragments_layout, mScreenFragment);
        fragmentTransaction.addToBackStack(CONNECTION);
        fragmentTransaction.commit();

        mDeviceToConnect = device;
        initDevice();
    }

    private void enableConnectionToParrotDevice(boolean enable) {
        ImageView drone = (ImageView)findViewById(R.id.iv_drone_connected);
        TextView text = (TextView)findViewById(R.id.tv_drone_connected);
        TextView check = (TextView)findViewById(R.id.drone_connected_check);
        if (enable) {
            text.setTextColor(getResources().getColor(R.color.DG_BLUE));
            Utils.setVisible(check);
            drone.setImageDrawable(getResources().getDrawable(R.drawable.ic_drone_setup_guide_normal));
        } else {
            text.setTextColor(Color.GRAY);
            Utils.setInvisible(check);
            drone.setImageDrawable(getResources().getDrawable(R.drawable.ic_drone_setup_guide_disabled));
        }
    }

    private void initServices() {
        if (discoveryServiceBinder == null) {
            Intent intent = new Intent(getApplicationContext(), ARDiscoveryService.class);
            startService(intent);
            getApplicationContext().bindService(intent, ardiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            ardiscoveryService = ((ARDiscoveryService.LocalBinder)discoveryServiceBinder).getService();
            ardiscoveryServiceBound = true;
            ardiscoveryService.start();
        }
    }

    private void closeServices() {
        if (ardiscoveryServiceBound) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ardiscoveryService.stop();
                        getApplicationContext().unbindService(ardiscoveryServiceConnection);
                    } catch (IllegalArgumentException e) {
                        Logger.d(TAG, "Error disconnection service...");
                    } finally {
                        ardiscoveryServiceBound = false;
                        discoveryServiceBinder = null;
                        ardiscoveryService = null;
                    }
                }
            }).start();
        }
    }

    private void initBroadcastReceiver() {
        ardiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
    }

    private void initServiceConnection() {
        ardiscoveryServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                discoveryServiceBinder = service;
                ardiscoveryService = ((ARDiscoveryService.LocalBinder)service).getService();
                ardiscoveryServiceBound = true;
                ardiscoveryService.start();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                ardiscoveryService = null;
                ardiscoveryServiceBound = false;
            }
        };
    }

    private void registerReceivers() {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(ardiscoveryServicesDevicesListUpdatedReceiver,
                new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }

    private void unregisterReceivers() {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.unregisterReceiver(ardiscoveryServicesDevicesListUpdatedReceiver);
    }

    @Override
    public void onResume() {
        Logger.d(TAG, "onResume ...");
        super.onResume();

        mDeviceList.clear();

        onServicesDevicesListUpdated();
        registerReceivers();

        mHandler.post(mListRefresh);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (!mSetupGuideFragment.isAdded()) {
            fragmentTransaction.add(R.id.fragments_layout, mSetupGuideFragment, SETUP_GUIDE);
            fragmentTransaction.commit();
        }

        clearBackStack();
    }

    private void clearBackStack() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        while (fragmentManager.getBackStackEntryCount() != 0) {
            fragmentManager.popBackStackImmediate();
        }
    }

    @Override
    public void onPause() {
        Logger.d(TAG, "onPause ...");

        mHandler.removeCallbacks(mListRefresh);

        unregisterReceivers();
        closeServices();
        Intent intent = new Intent(getApplicationContext(), ARDiscoveryService.class);
        stopService(intent);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //super.onSaveInstanceState(outState); //crashes when turning 3G on/off
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(this, AboutActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                break;
            case R.id.action_license:
                loadLicense();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadLicense() {
        View view = getLayoutInflater().inflate(R.layout.licenses, null);

        Dialog dialog = new AlertDialog.Builder(this).setPositiveButton(R.string.settings_button_ok, null).setView(view)
                .setTitle(getString(R.string.settings_item_licenses)).create();

        final WebView webView = (WebView)view.findViewById(R.id.license_view);
        final ProgressBar webProgress = (ProgressBar)view.findViewById(R.id.license_progress);
        webProgress.setVisibility(View.VISIBLE);

        dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                webView.stopLoading();
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                super.onScaleChanged(view, oldScale, newScale);
                webProgress.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                webProgress.setVisibility(View.GONE);
            }
        });

        webView.loadUrl("file:///android_asset/licenses.html");
        dialog.show();
    }

    @Override
    public void onServicesDevicesListUpdated() {
        Logger.d(TAG, "onServicesDevicesListUpdated ...");

        List<ARDiscoveryDeviceService> list;

        // restart services if not running
        if (ardiscoveryService == null) {
            initServices();
        }

        if (ardiscoveryService != null) {
            list = ardiscoveryService.getDeviceServicesArray();

            mDeviceList = new ArrayList<>();
            List<String> deviceNames = new ArrayList<>();

            if (list != null && !list.isEmpty()) {
                for (ARDiscoveryDeviceService service : list) {
                    Logger.d(TAG, "mService :  " + service);
                    if (service.getDevice() instanceof ARDiscoveryDeviceBLEService) {
                        mDeviceList.add(service);
                        deviceNames.add(service.getName());
                    } else if (service.getDevice() instanceof ARDiscoveryDeviceNetService) {
                        mDeviceList.add(service);
                        deviceNames.add(service.getName());
                    }
                }
                initDevice();
            } else {
                mDeviceList.clear();
                return;
            }
        }
    }

    @Override
    public void fragmentDetached() {
        mDeviceToConnect = null;
    }
}