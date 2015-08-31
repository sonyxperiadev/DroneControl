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

import com.parrot.arsdk.arcommands.ARCOMMANDS_COMMON_CALIBRATIONSTATE_MAGNETOCALIBRATIONAXISTOCALIBRATECHANGED_AXIS_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_DECODER_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_GENERATOR_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_ALERTSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCommand;
import com.parrot.arsdk.arcommands.ARCommandCommonCalibrationMagnetoCalibrationListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCalibrationStateMagnetoCalibrationRequiredStateListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCalibrationStateMagnetoCalibrationStartedChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCalibrationStateMagnetoCalibrationStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCommonStateAllStatesChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCommonStateBatteryStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonSettingsAllSettingsListener;
import com.parrot.arsdk.arcommands.ARCommandMiniDroneAnimationsFlipListener;
import com.parrot.arsdk.arcommands.ARCommandMiniDronePilotingStateAlertStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandMiniDronePilotingStateFlyingStateChangedListener;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceBLEService;
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
import com.sonymobile.dronecontrol.alert.AlertTutorial;
import com.sonymobile.dronecontrol.utils.EnumDevices;
import com.sonymobile.dronecontrol.utils.Logger;
import com.sonymobile.dronecontrol.utils.Notifier;

import android.content.Context;
import android.os.SystemClock;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RollingSpiderDeviceController extends DeviceController implements ARCommandCommonCommonStateBatteryStateChangedListener,
        ARCommandCommonCommonStateAllStatesChangedListener, ARCommandCommonCalibrationMagnetoCalibrationListener,
        ARCommandCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChangedListener,
        ARCommandCommonCalibrationStateMagnetoCalibrationRequiredStateListener,
        ARCommandCommonCalibrationStateMagnetoCalibrationStartedChangedListener,
        ARCommandCommonCalibrationStateMagnetoCalibrationStateChangedListener, ARCommandCommonSettingsAllSettingsListener,
        ARCommandMiniDronePilotingStateFlyingStateChangedListener, ARCommandMiniDronePilotingStateAlertStateChangedListener, ARCommandMiniDroneAnimationsFlipListener {

    private static String TAG = RollingSpiderDeviceController.class.getSimpleName();

    protected static List<ARNetworkIOBufferParam> c2dParams = new ArrayList<>();
    protected static List<ARNetworkIOBufferParam> d2cParams = new ArrayList<>();
    protected static int commandsBuffers[] = {};
    private static int iobufferC2dNack = 10;
    private static int iobufferC2dAck = 11;
    private static int iobufferC2dEmergency = 12;
    private static int iobufferD2cNavdata = 127;
    private static int iobufferD2cEvents = 126;
    private static int ackOffset = 16;
    protected static int bleNotificationIDs[] = new int[]{iobufferD2cNavdata, iobufferD2cEvents, (iobufferC2dAck + ackOffset),
            (iobufferC2dEmergency + ackOffset)};

    private Context mContext;
    private ARNetworkALManager alManager;
    private ARNetworkManager netManager;
    private boolean mediaOpened;
    private Thread rxThread;
    private Thread txThread;
    private List<ReaderThread> readerThreads = null;
    private LooperThread looperThread;
    private DataPCMD dataPCMD;
    private ARDiscoveryDeviceService deviceService;
    private boolean mIsConnected = false;
    private final Object lock = new Object();

    private float mSensitivity = 0.5f;

    private static RollingSpiderDeviceController rollingSpiderDeviceController = new RollingSpiderDeviceController();

    static {
        c2dParams.clear();
        c2dParams.add(new ARNetworkIOBufferParam(iobufferC2dNack, ARNETWORKAL_FRAME_TYPE_ENUM.ARNETWORKAL_FRAME_TYPE_DATA, 20,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER, ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER, 1,
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

        commandsBuffers = new int[]{iobufferD2cNavdata, iobufferD2cEvents,};

    }

    @Override
    public EnumDevices getDeviceInfo() {
        return EnumDevices.MINIDRONE;
    }

    public RollingSpiderDeviceController() {

    }

    public byte[] getPCMDData() {
        byte arrayPCMD[] = new byte[4];
        arrayPCMD[0] = dataPCMD.pitch;
        arrayPCMD[1] = dataPCMD.roll;
        arrayPCMD[2] = dataPCMD.yaw;
        arrayPCMD[3] = dataPCMD.gaz;
        return arrayPCMD;
    }

    public static RollingSpiderDeviceController getInstance() {
        if (rollingSpiderDeviceController == null) {
            rollingSpiderDeviceController = new RollingSpiderDeviceController();
        }

        return rollingSpiderDeviceController;
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
        this.mContext = context;
        if (readerThreads == null) {
            readerThreads = new ArrayList<ReaderThread>();
        }
    }

    private boolean start() {
        Logger.d(TAG, "start ...");

        boolean failed = false;

        registerARCommandsListener();

        failed = startNetwork();

        if (!failed) {
            startReadThreads();
            startLooperThread();

            notifyConnection();
        }

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

    private void stop() {
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

        /* setup ARNetworkAL for BLE */

        ARDiscoveryDeviceBLEService bleDevice = (ARDiscoveryDeviceBLEService)deviceService.getDevice();

        netALError = alManager.initBLENetwork(mContext, bleDevice.getBluetoothDevice(), 1, bleNotificationIDs);

        if (netALError == ARNETWORKAL_ERROR_ENUM.ARNETWORKAL_OK) {
            mediaOpened = true;
            pingDelay = -1; /* Disable ping for BLE networks */
        } else {
            Logger.e(TAG, "error occured: " + netALError.toString());
            failed = true;
        }
        if (failed == false) {
            /* Create the ARNetworkManager */

            netManager = new ARNetworkManagerExtend(alManager, c2dParams.toArray(new ARNetworkIOBufferParam[c2dParams.size()]),
                    d2cParams.toArray(new ARNetworkIOBufferParam[d2cParams.size()]), pingDelay);

            if (netManager.isCorrectlyInitialized() == false) {
                Logger.e(TAG, "new ARNetworkManager failed");
                failed = true;
            }
        }

        if (failed == false) {

            if (netManager != null) {
            /* Create and start Tx and Rx threads */
                rxThread = new Thread(netManager.m_receivingRunnable);
                rxThread.start();

                txThread = new Thread(netManager.m_sendingRunnable);
                txThread.start();
            }
        }

        return failed;
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
            } else if (deviceService.getDevice() instanceof ARDiscoveryDeviceBLEService) {
                alManager.closeBLENetwork(mContext);
            }

            mediaOpened = false;
            alManager.dispose();
        }
    }

    protected void registerARCommandsListener() {
        ARCommand.setCommonCommonStateBatteryStateChangedListener(this);
        ARCommand.setCommonCalibrationMagnetoCalibrationListener(this);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChangedListener(this);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationRequiredStateListener(this);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationStartedChangedListener(this);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationStateChangedListener(this);
        ARCommand.setCommonCommonStateAllStatesChangedListener(this);
        ARCommand.setCommonSettingsAllSettingsListener(this);
        ARCommand.setMiniDronePilotingStateAlertStateChangedListener(this);
        ARCommand.setMiniDronePilotingStateFlyingStateChangedListener(this);
        ARCommand.setMiniDroneAnimationsFlipListener(this);
    }

    protected void unregisterARCommandsListener() {
        ARCommand.setCommonCommonStateBatteryStateChangedListener(null);
        ARCommand.setCommonCalibrationMagnetoCalibrationListener(null);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChangedListener(null);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationRequiredStateListener(null);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationStartedChangedListener(null);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationStateChangedListener(null);
        ARCommand.setCommonCommonStateAllStatesChangedListener(null);
        ARCommand.setCommonSettingsAllSettingsListener(null);
        ARCommand.setMiniDronePilotingStateAlertStateChangedListener(null);
        ARCommand.setMiniDronePilotingStateFlyingStateChangedListener(null);
        ARCommand.setMiniDroneAnimationsFlipListener(null);
    }

    public void setGamepadSensitivity(float value) {
        mSensitivity = value;
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

        cmdError = cmd.setMiniDronePilotingPCMD(dataPCMD.flag, dataPCMD.roll, dataPCMD.pitch, dataPCMD.yaw, dataPCMD.gaz, dataPCMD.psi);
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

    /**
     * Send a command <code>Flip</code> of class <code>Animations</code> in project
     * <code>MiniDrone</code>
     *
     * @param direction Direction for the flip
     */
    public boolean sendFlip(ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM direction) {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setMiniDroneAnimationsFlip(direction);
        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                /** send the command */
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
            Logger.e(TAG, "Failed to send flip command.");
        }

        return sentStatus;
    }

    public boolean sendTakeoff() {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setMiniDronePilotingTakeOff();
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
            Logger.e(TAG, "Failed to send TakeOff command.");
        }

        return sentStatus;
    }

    public boolean sendLanding() {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setMiniDronePilotingLanding();
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
            Logger.e(TAG, "Failed to send Landing command.");
        }
        return sentStatus;
    }

    public boolean sendEmergency() {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setMiniDronePilotingEmergency();
        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dEmergency, cmd, null, true);

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
            Logger.e(TAG, "Failed to send Emergency command.");
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


    public boolean sendTakePicture() {

        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setMiniDroneMediaRecordPicture((byte)0);

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


    public void setFlag(byte flag) {
        dataPCMD.flag = flag;
    }

    public void setGaz(byte gaz) {
        dataPCMD.gaz = (byte)(gaz * mSensitivity);
    }

    public void setRoll(byte roll) {
        dataPCMD.roll = (byte)(roll * mSensitivity);
    }

    public void setPitch(byte pitch) {
        dataPCMD.pitch = (byte)(pitch * mSensitivity);
    }

    public void setYaw(byte yaw) {
        dataPCMD.yaw = (byte)(yaw * mSensitivity);
    }

    @Override
    public void onCommonCommonStateBatteryStateChangedUpdate(final byte b) {
        Logger.d(TAG, "onCommonCommonStateBatteryStateChangedUpdate ..." + b);
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
    public void onCommonCalibrationMagnetoCalibrationUpdate(byte calibrate) {
        Logger.d(TAG, "onCommonCalibrationMagnetoCalibrationUpdate ...");
    }

    @Override
    public void onCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChangedUpdate(
            ARCOMMANDS_COMMON_CALIBRATIONSTATE_MAGNETOCALIBRATIONAXISTOCALIBRATECHANGED_AXIS_ENUM axis) {
        Logger.d(TAG, "onCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChangedUpdate ...");
    }

    @Override
    public void onCommonCalibrationStateMagnetoCalibrationRequiredStateUpdate(byte required) {
        Logger.d(TAG, "onCommonCalibrationStateMagnetoCalibrationRequiredStateUpdate ...");
    }

    @Override
    public void onCommonCalibrationStateMagnetoCalibrationStartedChangedUpdate(byte started) {
        Logger.d(TAG, "onCommonCalibrationStateMagnetoCalibrationStartedChangedUpdate ...");
    }

    @Override
    public void onCommonCalibrationStateMagnetoCalibrationStateChangedUpdate(byte xAxisCalibration, byte yAxisCalibration, byte zAxisCalibration,
            byte calibrationFailed) {
        Logger.d(TAG, "onCommonCalibrationStateMagnetoCalibrationStateChangedUpdate ...");
    }

    @Override
    public void onCommonCommonStateAllStatesChangedUpdate() {
        Logger.d(TAG, "onCommonCommonStateAllStatesChangedUpdate ...");

    }

    @Override
    public void onCommonSettingsAllSettingsUpdate() {
        Logger.d(TAG, "onCommonSettingsAllSettingsUpdate ...");
    }

    public boolean getInitialSettings() {
        /* Attempt to get initial settings */
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setCommonSettingsAllSettings();

        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {

            if (netManager != null) {
            /* Send data with ARNetwork */
                // The command emergency should be sent to its own buffer acknowledged  ; here
                // iobufferC2dAck

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
            Logger.e(TAG, "Failed to send get settings command.");
        }

        return sentStatus;
    }

    public boolean getInitialStates() {
        /* Attempt to get initial states */
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setCommonCommonAllStates();

        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            /* Send data with ARNetwork */

            if (netManager != null) {
                // The command emergency should be sent to its own buffer acknowledged  ; here
                // iobufferC2dAck

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
            Logger.e(TAG, "Failed to send get state command.");
        }
        return sentStatus;
    }

    @Override
    public void onMiniDronePilotingStateFlyingStateChangedUpdate(ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM e) {
        switch (e) {
            case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                setDeviceState(DeviceState.FLYING);
                new Notifier() {
                    @Override
                    public void onNotify(DeviceControllerListener listener) {
                        if (listener != null) {
                            listener.onStateChange(DeviceState.FLYING);
                        }
                    }
                };
                break;
            case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                setDeviceState(DeviceState.HOVERING);
                new Notifier() {
                    @Override
                    public void onNotify(DeviceControllerListener listener) {
                        if (listener != null) {
                            listener.onStateChange(DeviceState.FLYING);
                        }
                    }
                };
                break;
            case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                setDeviceState(DeviceState.LANDED);
                new Notifier() {
                    @Override
                    public void onNotify(DeviceControllerListener listener) {
                        if (listener != null) {
                            listener.onStateChange(DeviceState.LANDED);
                        }
                    }
                };
                break;
            case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDING:
                setDeviceState(DeviceState.LANDING);
                new Notifier() {
                    @Override
                    public void onNotify(DeviceControllerListener listener) {
                        if (listener != null) {
                            listener.onStateChange(DeviceState.LANDING);
                        }
                    }
                };
                break;
            case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_TAKINGOFF:
                setDeviceState(DeviceState.TAKING_OFF);
                new Notifier() {
                    @Override
                    public void onNotify(DeviceControllerListener listener) {
                        if (listener != null) {
                            listener.onStateChange(DeviceState.TAKING_OFF);
                        }
                    }
                };
                break;
            case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_EMERGENCY:
                setDeviceState(DeviceState.EMERGENCY);
                new Notifier() {
                    @Override
                    public void onNotify(DeviceControllerListener listener) {
                        if (listener != null) {
                            listener.onStateChange(DeviceState.EMERGENCY );
                        }
                    }
                };
                break;
        }
    }

    @Override
    public void onMiniDronePilotingStateAlertStateChangedUpdate(final ARCOMMANDS_MINIDRONE_PILOTINGSTATE_ALERTSTATECHANGED_STATE_ENUM state) {
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    if (state == ARCOMMANDS_MINIDRONE_PILOTINGSTATE_ALERTSTATECHANGED_STATE_ENUM
                            .ARCOMMANDS_MINIDRONE_PILOTINGSTATE_ALERTSTATECHANGED_STATE_CRITICAL_BATTERY) {
                        listener.onAlertStateChange(AlertTutorial.AlertState.CRITICAL_BATTERY);
                    }
                }
            }
        };
    }

    @Override
    public void onMiniDroneAnimationsFlipUpdate(
            ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM anim) {
        if (anim == ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_BACK ||
                anim == ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_FRONT ||
                anim == ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_LEFT ||
                anim == ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_MINIDRONE_ANIMATIONS_FLIP_DIRECTION_RIGHT) {
            new Notifier() {
                @Override
                public void onNotify(DeviceControllerListener listener) {
                    if (listener != null) {
                        listener.onTrickDone();
                    }
                }
            };
        }
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
        public void onDisconnect(ARNetworkALManager arNetworkALManager) {
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
        public byte flag;
        public byte roll;
        public byte pitch;
        public byte yaw;
        public byte gaz;
        public float psi;

        public DataPCMD() {
            flag = 0;
            roll = 0;
            pitch = 0;
            yaw = 0;
            gaz = 0;
            psi = 0.0f;
        }

        public void reset() {
            flag = 0;
            roll = 0;
            pitch = 0;
            yaw = 0;
            gaz = 0;
            psi = 0.0f;
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
                    Logger.e(TAG, "ReaderThread readDataWithTimeout() failed. " +
                            netError + " bufferId: " + bufferId);
                }
                skip = true;
            }

            if (skip == false) {
                ARCOMMANDS_DECODER_ERROR_ENUM decodeStatus = dataRecv.decode();
                if ((decodeStatus != ARCOMMANDS_DECODER_ERROR_ENUM.ARCOMMANDS_DECODER_OK) &&
                        (decodeStatus != ARCOMMANDS_DECODER_ERROR_ENUM.
                                ARCOMMANDS_DECODER_ERROR_NO_CALLBACK) && (decodeStatus != ARCOMMANDS_DECODER_ERROR_ENUM
                        .ARCOMMANDS_DECODER_ERROR_UNKNOWN_COMMAND)) {
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

}
