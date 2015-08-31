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

package com.sonymobile.dronecontrol.controller;

import com.parrot.arsdk.arcommands.ARCOMMANDS_DECODER_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_GENERATOR_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_JUMPINGSUMO_AUDIOSETTINGS_THEME_THEME_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_JUMPINGSUMO_PILOTINGSTATE_ALERTSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCommand;
import com.parrot.arsdk.arcommands.ARCommandCommonCommonStateAllStatesChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCommonStateBatteryStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCommonStateWifiSignalChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonSettingsAllSettingsListener;
import com.parrot.arsdk.arcommands.ARCommandJumpingSumoMediaRecordStatePictureStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandJumpingSumoPilotingStateAlertStateChangedListener;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_ERROR_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryConnection;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.arnetwork.ARNETWORK_ERROR_ENUM;
import com.parrot.arsdk.arnetwork.ARNETWORK_MANAGER_CALLBACK_RETURN_ENUM;
import com.parrot.arsdk.arnetwork.ARNETWORK_MANAGER_CALLBACK_STATUS_ENUM;
import com.parrot.arsdk.arnetwork.ARNetworkIOBufferParam;
import com.parrot.arsdk.arnetwork.ARNetworkManager;
import com.parrot.arsdk.arnetworkal.ARNETWORKAL_ERROR_ENUM;
import com.parrot.arsdk.arnetworkal.ARNETWORKAL_FRAME_TYPE_ENUM;
import com.parrot.arsdk.arnetworkal.ARNetworkALManager;
import com.parrot.arsdk.arsal.ARNativeData;
import com.sonymobile.dronecontrol.alert.AlertTutorial.AlertState;
import com.sonymobile.dronecontrol.utils.EnumDevices;
import com.sonymobile.dronecontrol.utils.Logger;
import com.sonymobile.dronecontrol.utils.Notifier;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.SystemClock;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

public class JumpingSumoDeviceController extends DeviceController implements ARCommandCommonCommonStateBatteryStateChangedListener,
        ARCommandCommonCommonStateWifiSignalChangedListener, ARCommandCommonCommonStateAllStatesChangedListener,
        ARCommandCommonSettingsAllSettingsListener, ARCommandJumpingSumoMediaRecordStatePictureStateChangedListener,
        ARCommandJumpingSumoPilotingStateAlertStateChangedListener {

    private static String TAG = JumpingSumoDeviceController.class.getSimpleName();

    protected static List<ARNetworkIOBufferParam> c2dParams = new ArrayList<>();
    protected static List<ARNetworkIOBufferParam> d2cParams = new ArrayList<>();
    protected static int commandsBuffers[] = {};
    private final static int iobufferC2dNack = 10;
    private final static int iobufferC2dAck = 11;
    private final static int iobufferC2dEmergency = 12;
    private final static int iobufferD2cNavdata = 127;
    private final static int iobufferD2cEvents = 126;

    private String discoveryIp;
    private int discoveryPort;

    private ARNetworkALManager alManager;
    private ARNetworkManager netManager;
    private boolean mediaOpened;

    private int c2dPort;
    private int d2cPort;
    private Thread rxThread;
    private Thread txThread;

    private List<ReaderThread> readerThreads = null;
    private Semaphore discoverSemaphore;
    private ARDiscoveryConnection discoveryData;

    private LooperThread looperThread;

    private DataPCMD dataPCMD;
    private ARDiscoveryDeviceService deviceService;

    private boolean mIsConnected = false;
    private final Object lock = new Object();

    private static JumpingSumoDeviceController jumpingSumoDeviceController = new JumpingSumoDeviceController();

    static {
        c2dParams.clear();
        c2dParams.add(new ARNetworkIOBufferParam(iobufferC2dNack, ARNETWORKAL_FRAME_TYPE_ENUM.ARNETWORKAL_FRAME_TYPE_DATA, 1,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER, ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER, 2,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_DATACOPYMAXSIZE_USE_MAX, true));
        c2dParams.add(new ARNetworkIOBufferParam(iobufferC2dAck, ARNETWORKAL_FRAME_TYPE_ENUM.ARNETWORKAL_FRAME_TYPE_DATA_WITH_ACK, 20, 500, 3, 20,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_DATACOPYMAXSIZE_USE_MAX, false));
        c2dParams.add(new ARNetworkIOBufferParam(iobufferC2dEmergency, ARNETWORKAL_FRAME_TYPE_ENUM.ARNETWORKAL_FRAME_TYPE_DATA_WITH_ACK, 1, 100,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER, 1,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_DATACOPYMAXSIZE_USE_MAX, false));

        d2cParams.clear();
        d2cParams.add(new ARNetworkIOBufferParam(iobufferD2cNavdata, ARNETWORKAL_FRAME_TYPE_ENUM.ARNETWORKAL_FRAME_TYPE_DATA, 20,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER, ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER, 20,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_DATACOPYMAXSIZE_USE_MAX, false));
        d2cParams.add(new ARNetworkIOBufferParam(iobufferD2cEvents, ARNETWORKAL_FRAME_TYPE_ENUM.ARNETWORKAL_FRAME_TYPE_DATA_WITH_ACK, 20, 500, 3, 20,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_DATACOPYMAXSIZE_USE_MAX, false));

        commandsBuffers = new int[]{iobufferD2cNavdata, iobufferD2cEvents};
    }

    public JumpingSumoDeviceController() {

    }

    public static JumpingSumoDeviceController getInstance() {
        if (jumpingSumoDeviceController == null) {
            jumpingSumoDeviceController = new JumpingSumoDeviceController();
        }

        return jumpingSumoDeviceController;
    }

    public boolean initDevice() {
        synchronized (lock) {
            if (!mIsConnected) {
                if (this.start()) {
                    mIsConnected = true;
                }
            }
        }
        return mIsConnected;
    }

    public void deinitDevice() {
        if (mIsConnected) {
            this.stop();
            mIsConnected = false;
        }
    }

    public void configureDevice(Context context, ARDiscoveryDeviceService service) {
        setCurrentDevice(this);
        dataPCMD = new DataPCMD();
        deviceService = service;
        if (readerThreads == null) {
            readerThreads = new ArrayList<>();
        }
        c2dPort = 54321;
        d2cPort = 43210;
    }

    private boolean start() {
        Logger.d(TAG, "start ...");

        boolean failed = false;

        registerARCommandsListener();

        failed = startNetwork();

        if (!failed) {
            startReadThreads();
            startLooperThread();
        }

        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onDroneDeviceInit(getDeviceInfo());
                }
            }
        };

        return (!failed);
    }

    public void notifyConnection() {
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onDroneDeviceInit(getDeviceInfo());
                }
            }
        };
    }

    public void stop() {
        Logger.d(TAG, "stop ...");

        unregisterARCommandsListener();

        stopLooperThread();

        stopReaderThreads();

        stopNetwork();

        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onDroneDeviceStop();
                }
            }
        };
    }

    private boolean startNetwork() {

        ARNETWORKAL_ERROR_ENUM netALError = ARNETWORKAL_ERROR_ENUM.ARNETWORKAL_OK;
        boolean failed = false;
        int pingDelay = 0; /* 0 means default, -1 means no ping */

        /* Create the looper ARNetworkALManager */
        alManager = new ARNetworkALManager();

        if (deviceService.getDevice() instanceof ARDiscoveryDeviceNetService) {
            /* setup ARNetworkAL for wifi */

            ARDiscoveryDeviceNetService netDevice = (ARDiscoveryDeviceNetService)deviceService.getDevice();
            discoveryIp = netDevice.getIp();
            discoveryPort = netDevice.getPort();

            Logger.d(TAG, "discovery IP: " + discoveryIp + " discovery PORT: " + discoveryPort);

            if (!ardiscoveryConnect()) {
                failed = true;
            }

            /* setup ARNetworkAL for wifi */
            netALError = alManager.initWifiNetwork(discoveryIp, c2dPort, d2cPort, 1);

            if (netALError == ARNETWORKAL_ERROR_ENUM.ARNETWORKAL_OK) {
                mediaOpened = true;
                Logger.d(TAG, "Wifi connection opened!");
            } else {
                Logger.e(TAG, "error occured: " + netALError.toString());
                failed = true;
            }
        } else {
            Logger.e(TAG, "Wrong network type!");
            failed = true;
        }

        if (failed == false) {
            /* Create the ARNetworkManager */
            netManager = new ARNetworkManagerExtend(alManager, c2dParams.toArray(new ARNetworkIOBufferParam[c2dParams.size()]),
                    d2cParams.toArray(new ARNetworkIOBufferParam[d2cParams.size()]), pingDelay);

            if (netManager.isCorrectlyInitialized() == false) {
                Logger.e(TAG, "new ARNetworkManager failed");
                failed = true;
            } else {
                Logger.d(TAG, "netManager connection successful!");
            }
        }

        if ((failed == false)) {
            /* Create and start Tx and Rx threads */
            rxThread = new Thread(netManager.m_receivingRunnable);
            rxThread.start();

            txThread = new Thread(netManager.m_sendingRunnable);
            txThread.start();
        }

        return failed;
    }

    public boolean sendTakePicture() {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setJumpingSumoMediaRecordPicture((byte)0);

        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dAck, cmd, null, true);
                if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                    Logger.e(TAG, "netManager.sendData() failed to calibrate drone. " + netError.toString());
                    sentStatus = false;
                }
            } else {
                sentStatus = false;
            }
            cmd.dispose();
        }

        if (sentStatus == false) {
            Logger.e(TAG, "Failed to send take picture command.");
        }

        return true;
    }

    public boolean getInitialStates() {

        // For some reason, JS keeps loosing connection if
        // code below is executed
        return false;

        /* Attempt to get initial states *//*
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setCommonCommonAllStates();

        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dAck, cmd, null, true);

                if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                    Logger.e(TAG, "netManager.sendData() failed. " + netError.toString());
                    sentStatus = false;
                }
            } else {
                sentStatus = false;
            }
            cmd.dispose();
        }

        if (sentStatus == false) {
            Logger.e(TAG, "Failed to send initial state command.");
        }
        return sentStatus;*/
    }

    private void startReadThreads() {
        /* Create the reader threads */
        for (int bufferId : commandsBuffers) {
            ReaderThread readerThread = new ReaderThread(bufferId);
            readerThreads.add(readerThread);
        }

        /* Mark all reader threads as started */
        for (ReaderThread readerThread : readerThreads) {
            readerThread.start();
        }
    }

    private void startLooperThread() {
        /* Create the looper thread */
        looperThread = new ControllerLooperThread();

        /* Start the looper thread. */
        looperThread.start();
    }

    private void stopLooperThread() {
        /* Cancel the looper thread and block until it is stopped. */
        if (null != looperThread) {
            looperThread.stopThread();
            try {
                looperThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopReaderThreads() {
        if (readerThreads != null) {
            /* cancel all reader threads and block until they are all stopped. */
            for (ReaderThread thread : readerThreads) {
                thread.stopThread();
            }
            for (ReaderThread thread : readerThreads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            readerThreads.clear();
            readerThreads = null;
        }
    }

    private void stopNetwork() {
        if (netManager != null) {
            netManager.stop();

            try {
                if (txThread != null) {
                    txThread.join();
                }
                if (rxThread != null) {
                    rxThread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            netManager.dispose();
        }

        if ((alManager != null) && (mediaOpened)) {
            if (deviceService.getDevice() instanceof ARDiscoveryDeviceNetService) {
                alManager.closeWifiNetwork();
            }

            mediaOpened = false;
            alManager.dispose();
        }
    }

    protected void registerARCommandsListener() {
        ARCommand.setCommonCommonStateBatteryStateChangedListener(this);
        ARCommand.setCommonCommonStateWifiSignalChangedListener(this);
        ARCommand.setCommonCommonStateAllStatesChangedListener(this);
        ARCommand.setCommonSettingsAllSettingsListener(this);
        ARCommand.setJumpingSumoMediaRecordStatePictureStateChangedListener(this);
        ARCommand.setJumpingSumoPilotingStateAlertStateChangedListener(this);
    }

    protected void unregisterARCommandsListener() {
        ARCommand.setCommonCommonStateBatteryStateChangedListener(null);
        ARCommand.setCommonCommonStateWifiSignalChangedListener(null);
        ARCommand.setCommonCommonStateAllStatesChangedListener(null);
        ARCommand.setCommonSettingsAllSettingsListener(null);
        ARCommand.setJumpingSumoMediaRecordStatePictureStateChangedListener(null);
        ARCommand.setJumpingSumoPilotingStateAlertStateChangedListener(null);
    }

    public String getIPAddress() {
        return discoveryIp;
    }

    /**
     * Clear current commands
     */
    public void clearCurrentCommands() {
        if (dataPCMD != null) {
            dataPCMD.reset();
        }
    }

    private boolean sendPCMD() {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setJumpingSumoPilotingPCMD(dataPCMD.flag, dataPCMD.speed, dataPCMD.turnRatio);
        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dNack, cmd, null, true);

                if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                    Logger.e(TAG, "netManager.sendData() failed. " + netError.toString());
                    sentStatus = false;
                }
            } else {
                sentStatus = false;
            }
            cmd.dispose();
        }

        if (sentStatus == false) {
            Logger.e(TAG, "Failed to send PCMD command.");
        }

        return sentStatus;
    }

    public void setCurrentTime(Date date) {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        SimpleDateFormat formattedDateISO8601 = new SimpleDateFormat("'T'HHmmssZZZ", Locale.getDefault());
        cmdError = cmd.setCommonCommonCurrentTime(formattedDateISO8601.format(date));

        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dAck, cmd, null, true);

                if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                    Logger.e(TAG, "netManager.sendData() failed. " + netError.toString());
                    sentStatus = false;
                }
            } else {
                sentStatus = false;
            }
        }
        cmd.dispose();
        if (sentStatus == false) {
            Logger.e(TAG, "Failed to set current time command");
        }
    }

    public void setCurrentDate(Date date) {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        SimpleDateFormat formattedDateISO8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        cmdError = cmd.setCommonCommonCurrentDate(formattedDateISO8601.format(date));

        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dAck, cmd, null, true);

                if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                    Logger.e(TAG, "netManager.sendData() failed. " + netError.toString());
                    sentStatus = false;
                }
            } else {
                sentStatus = false;
            }
        }
        cmd.dispose();
        if (sentStatus == false) {
            Logger.e(TAG, "Failed to set current date command");
        }
    }

    public boolean sendAnimationsJump(ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_ENUM type) {

        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setJumpingSumoAnimationsJump(type);
        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dAck, cmd, null, true);

                if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                    Logger.e(TAG, "netManager.sendData() failed. " + netError.toString());
                    sentStatus = false;
                }
            }
            cmd.dispose();
        }

        if (sentStatus == false) {
            Logger.e(TAG, "Failed to send Jump command.");
        }

        return sentStatus;
    }

    public boolean sendAnimationsSimpleAnimation(ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_SIMPLEANIMATION_ID_ENUM id) {

        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setJumpingSumoAnimationsSimpleAnimation(id);
        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dAck, cmd, null, true);

                if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                    Logger.e(TAG, "netManager.sendData() failed. " + netError.toString());
                    sentStatus = false;
                }
            } else {
                sentStatus = false;
            }
            cmd.dispose();
        }

        if (sentStatus == false) {
            Logger.e(TAG, "Failed to send SimpleAnimation command.");
        }

        return sentStatus;
    }

    public boolean sendAudioSettingsTheme(ARCOMMANDS_JUMPINGSUMO_AUDIOSETTINGS_THEME_THEME_ENUM theme) {

        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setJumpingSumoAudioSettingsTheme(theme);
        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dAck, cmd, null, true);

                if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                    Logger.e(TAG, "netManager.sendData() failed. " + netError.toString());
                    sentStatus = false;
                }
            } else {
                sentStatus = false;
            }
            cmd.dispose();
        }

        if (sentStatus == false) {
            Logger.e(TAG, "Failed to send Audio Theme command.");
        }

        return sentStatus;
    }

    public boolean sendPilotingAddCapOffset(float offset) {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = false;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setJumpingSumoPilotingAddCapOffset(offset);
        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dAck, cmd, null, true);

                if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                    Logger.e(TAG, "netManager.sendData() failed. " + netError.toString());
                    sentStatus = false;
                }
            } else {
                sentStatus = false;
            }
            cmd.dispose();
        }

        if (sentStatus == false) {
            Logger.e(TAG, "Failed to send AddCapOffset command.");
        }

        return sentStatus;
    }

    public boolean sendDate(Date currentDate) {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        cmdError = cmd.setCommonCommonCurrentDate(formattedDate.format(currentDate));
        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dAck, cmd, null, true);

                if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                    Logger.e(TAG, "netManager.sendData() failed. " + netError.toString());
                    sentStatus = false;
                }
            } else {
                sentStatus = false;
            }
            cmd.dispose();
        }

        if (sentStatus == false) {
            Logger.e(TAG, "Failed to send date command.");
        }

        return sentStatus;
    }

    public boolean sendTime(Date currentDate) {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        SimpleDateFormat formattedTime = new SimpleDateFormat("'T'HHmmssZZZ", Locale.getDefault());

        cmdError = cmd.setCommonCommonCurrentTime(formattedTime.format(currentDate));
        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dAck, cmd, null, true);

                if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                    Logger.e(TAG, "netManager.sendData() failed. " + netError.toString());
                    sentStatus = false;
                }
            } else {
                sentStatus = false;
            }
            cmd.dispose();
        }

        if (sentStatus == false) {
            Logger.e(TAG, "Failed to send time command.");
        }

        return sentStatus;
    }

    public void setSpeed(byte speed) {
        dataPCMD.speed = speed;
    }

    public void setFlag(byte flag) {
        dataPCMD.flag = flag;
    }

    public void setTurnRatio(byte turnRatio) {
        dataPCMD.turnRatio = turnRatio;
    }

    @Override
    public void onCommonCommonStateBatteryStateChangedUpdate(final byte b) {
        Logger.d(TAG, "onCommonCommonStateBatteryStateChangedUpdate ...");

        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onUpdateBattery(b);
                }
            }
        };
    }

    @Override
    public EnumDevices getDeviceInfo() {
        return EnumDevices.JUMPINGSUMO;
    }

    @Override
    public void onCommonCommonStateWifiSignalChangedUpdate(final short rssi) {
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onWifiSignalChange(rssi);
                }
            }
        };
    }

    @Override
    public void onCommonCommonStateAllStatesChangedUpdate() {
        Logger.d(TAG, "onCommonCommonStateAllStatesChangedUpdate ...");

    }

    @Override
    public void onCommonSettingsAllSettingsUpdate() {
        Logger.d(TAG, "onCommonSettingsAllSettingsUpdate ...");
    }

    private boolean ardiscoveryConnect() {
        boolean ok = true;
        ARDISCOVERY_ERROR_ENUM error = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_OK;
        discoverSemaphore = new Semaphore(0);

        discoveryData = new ARDiscoveryConnection() {

            @Override
            public String onSendJson() {
                    /* send a json with the Device to controller port */
                JSONObject jsonObject = new JSONObject();

                try {
                    jsonObject.put(ARDiscoveryConnection.ARDISCOVERY_CONNECTION_JSON_D2CPORT_KEY, d2cPort);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    Logger.e(TAG, "android.os.Build.MODEL: " + android.os.Build.MODEL);
                    jsonObject.put(ARDiscoveryConnection.ARDISCOVERY_CONNECTION_JSON_CONTROLLER_NAME_KEY, android.os.Build.MODEL);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    Logger.e(TAG, "android.os.Build.DEVICE: " + android.os.Build.DEVICE);
                    jsonObject.put(ARDiscoveryConnection.ARDISCOVERY_CONNECTION_JSON_CONTROLLER_TYPE_KEY, android.os.Build.DEVICE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return jsonObject.toString();
            }

            @Override
            public ARDISCOVERY_ERROR_ENUM onReceiveJson(String dataRx, String ip) {
                    /* Receive a json with the controller to Device port */
                ARDISCOVERY_ERROR_ENUM error = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_OK;
                try {
                        /* Convert String to json */
                    JSONObject jsonObject = new JSONObject(dataRx);
                    if (!jsonObject.isNull(ARDiscoveryConnection.ARDISCOVERY_CONNECTION_JSON_C2DPORT_KEY)) {
                        c2dPort = jsonObject.getInt(ARDiscoveryConnection.ARDISCOVERY_CONNECTION_JSON_C2DPORT_KEY);
                        Logger.d(TAG, "New JSON port reply: c2dPort: " + c2dPort);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    error = ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_ERROR;
                }
                return error;
            }
        };


        if (ok == true) {
            /* open the discovery connection data in another thread */
            ConnectionThread connectionThread = new ConnectionThread();
            connectionThread.start();
            /* wait the discovery of the connection data */

            try {
                discoverSemaphore.acquire();
                error = connectionThread.getError();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            /* dispose discoveryData it not needed more */
            discoveryData.dispose();
            discoveryData = null;
        }

        return ok && (error == ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_OK);
    }

    public boolean getInitialSettings() {
        /* Attempt to get initial settings */
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setCommonSettingsAllSettings();

        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager == null) {
                Logger.d(TAG, "netManager could not be configured");
                return false;
            }
            ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dAck, cmd, null, true);

            if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                Logger.e(TAG, "netManager.sendData() failed. " + netError.toString());
                sentStatus = false;
            }

            cmd.dispose();
        }

        if (sentStatus == false) {
            Logger.e(TAG, "Failed to send time command.");
        }

        return sentStatus;
    }

    @Override
    public void onJumpingSumoMediaRecordStatePictureStateChangedUpdate(final byte state, byte storageId) {
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onTakePicture(state == 1);
                }
            }
        };
    }

    @Override
    public void onJumpingSumoPilotingStateAlertStateChangedUpdate(final ARCOMMANDS_JUMPINGSUMO_PILOTINGSTATE_ALERTSTATECHANGED_STATE_ENUM state) {
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    if (state == ARCOMMANDS_JUMPINGSUMO_PILOTINGSTATE_ALERTSTATECHANGED_STATE_ENUM
                            .ARCOMMANDS_JUMPINGSUMO_PILOTINGSTATE_ALERTSTATECHANGED_STATE_CRITICAL_BATTERY) {
                        listener.onAlertStateChange(AlertState.CRITICAL_BATTERY);
                    } else if (state == ARCOMMANDS_JUMPINGSUMO_PILOTINGSTATE_ALERTSTATECHANGED_STATE_ENUM
                            .ARCOMMANDS_JUMPINGSUMO_PILOTINGSTATE_ALERTSTATECHANGED_STATE_LOW_BATTERY) {
                        listener.onAlertStateChange(AlertState.LOW_BATTERY);
                    }
                }
            }
        };
    }

    /**
     * Extend of ARNetworkManager implementing the callback
     */
    private class ARNetworkManagerExtend extends ARNetworkManager {
        private static final String TAG = "ARNetworkManagerExtend";

        public ARNetworkManagerExtend(ARNetworkALManager osSpecificManager, ARNetworkIOBufferParam[] inputParamArray,
                ARNetworkIOBufferParam[] outputParamArray, int timeBetweenPingsMs) {
            super(osSpecificManager, inputParamArray, outputParamArray, timeBetweenPingsMs);
        }

        @Override
        public ARNETWORK_MANAGER_CALLBACK_RETURN_ENUM onCallback(int ioBufferId, ARNativeData data, ARNETWORK_MANAGER_CALLBACK_STATUS_ENUM status,
                Object customData) {
            ARNETWORK_MANAGER_CALLBACK_RETURN_ENUM retVal = ARNETWORK_MANAGER_CALLBACK_RETURN_ENUM.ARNETWORK_MANAGER_CALLBACK_RETURN_DEFAULT;

            if (status == ARNETWORK_MANAGER_CALLBACK_STATUS_ENUM.ARNETWORK_MANAGER_CALLBACK_STATUS_TIMEOUT) {
                retVal = ARNETWORK_MANAGER_CALLBACK_RETURN_ENUM.ARNETWORK_MANAGER_CALLBACK_RETURN_DATA_POP;
            }

            return retVal;
        }


        @Override
        public void onDisconnect(ARNetworkALManager alManager) {
            Logger.d(TAG, "onDisconnect ...");
            new Notifier() {
                @Override
                public void onNotify(DeviceControllerListener listener) {
                    if (listener != null) {
                        listener.onDisconnect();
                    }
                }
            };
        }
    }

    private class DataPCMD {

        /* Target speed and turn ration. Sent each loop*/
        public byte speed;
        public byte turnRatio;
        public byte flag;

        /* Local state we want to set to the remote device */
        /* Nothing yet ... */

        public DataPCMD() {
            flag = 0;
            speed = 0;
            turnRatio = 0;
        }

        public void reset() {
            flag = 0;
            speed = 0;
            turnRatio = 0;
        }
    }

    private abstract class LooperThread extends Thread {
        private boolean isAlive;
        private boolean isRunning;

        public LooperThread() {
            this.isRunning = false;
            this.isAlive = true;
        }

        @Override
        public void run() {
            this.isRunning = true;

            onStart();

            while (this.isAlive) {
                onloop();
            }
            onStop();

            this.isRunning = false;
        }

        public void onStart() {

        }

        public abstract void onloop();

        public void onStop() {

        }

        public void stopThread() {
            isAlive = false;
        }

        public boolean isRunning() {
            return this.isRunning;
        }
    }

    private class ReaderThread extends LooperThread {
        int bufferId;
        ARCommand dataRecv;

        public ReaderThread(int bufferId) {
            this.bufferId = bufferId;
            dataRecv = new ARCommand(128 * 1024);
        }

        @Override
        public void onStart() {
        }


        @Override
        public void onloop() {
            boolean skip = false;
            ARNETWORK_ERROR_ENUM netError = ARNETWORK_ERROR_ENUM.ARNETWORK_OK;

            if (netManager != null) {
                netError = netManager.readDataWithTimeout(bufferId, dataRecv, 1000);
            }

            if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_ERROR_BUFFER_EMPTY) {
                    Logger.e(TAG, "ReaderThread readDataWithTimeout() failed. " + netError + " " +
                            "bufferId: " + bufferId);
                }
                skip = true;
            }

            if (skip == false) {
                ARCOMMANDS_DECODER_ERROR_ENUM decodeStatus = dataRecv.decode();
                if ((decodeStatus != ARCOMMANDS_DECODER_ERROR_ENUM.ARCOMMANDS_DECODER_OK) &&
                        (decodeStatus != ARCOMMANDS_DECODER_ERROR_ENUM.ARCOMMANDS_DECODER_ERROR_NO_CALLBACK) && (decodeStatus !=
                        ARCOMMANDS_DECODER_ERROR_ENUM.ARCOMMANDS_DECODER_ERROR_UNKNOWN_COMMAND)) {
                    Logger.e(TAG, "ARCommand.decode() failed. " + decodeStatus);
                }
            }
        }

        @Override
        public void onStop() {
            new Notifier() {
                @Override
                public void onNotify(DeviceControllerListener listener) {
                    if (listener != null) {
                        listener.onDroneDeviceStop();
                    }
                }
            };
            dataRecv.dispose();
            super.onStop();
        }
    }


    protected class ControllerLooperThread extends LooperThread {
        public ControllerLooperThread() {

        }

        @Override
        public void onloop() {
            long lastTime = SystemClock.elapsedRealtime();

            sendPCMD();

            long sleepTime = (SystemClock.elapsedRealtime() + 50) - lastTime;

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This class must be public
     */
    public class ConnectionThread extends Thread {
        private ARDISCOVERY_ERROR_ENUM error;

        public void run() {
            error = discoveryData.ControllerConnection(discoveryPort, discoveryIp);
            if (error != ARDISCOVERY_ERROR_ENUM.ARDISCOVERY_OK) {
                Logger.e(TAG, "Error while opening discovery connection : " + error);
            }

            /* discoverSemaphore can be disposed */
            discoverSemaphore.release();
        }

        public ARDISCOVERY_ERROR_ENUM getError() {
            return error;
        }
    }

}
