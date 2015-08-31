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

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_GPSSETTINGSSTATE_GPSUPDATESTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEO_RECORD_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIASTREAMINGSTATE_VIDEOENABLECHANGED_ENABLED_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PICTURESETTINGS_AUTOWHITEBALANCESELECTION_TYPE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PICTURESETTINGS_PICTUREFORMATSELECTION_TYPE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_ALERTSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_NAVIGATEHOMESTATECHANGED_REASON_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_NAVIGATEHOMESTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_DECODER_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_GENERATOR_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCommand;
import com.parrot.arsdk.arcommands.ARCommandARDrone3AnimationsFlipListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3GPSSettingsStateGPSFixStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3GPSSettingsStateGPSUpdateStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3GPSSettingsStateHomeChangedListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3MediaRecordStatePictureStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3MediaRecordStateVideoStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3MediaStreamingStateVideoEnableChangedListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3MediaStreamingVideoEnableListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3PilotingFlatTrimListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3PilotingPCMDListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3PilotingStateAlertStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3PilotingStateAltitudeChangedListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3PilotingStateFlyingStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3PilotingStateNavigateHomeStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3PilotingStatePositionChangedListener;
import com.parrot.arsdk.arcommands.ARCommandARDrone3PilotingStateSpeedChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCommonStateAllStatesChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCommonStateBatteryStateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCommonStateMassStorageInfoRemainingListChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCommonStateMassStorageInfoStateListChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCommonStateMassStorageStateListChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCommonStateWifiSignalChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonControllerStateIsPilotingChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonSettingsAllSettingsListener;
import com.parrot.arsdk.arcommands.ARCommandCommonSettingsStateProductVersionChangedListener;
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
import com.parrot.arsdk.arstream.ARSTREAM_READER_CAUSE_ENUM;
import com.parrot.arsdk.arstream.ARStreamReader;
import com.parrot.arsdk.arstream.ARStreamReaderListener;
import com.sonymobile.dronecontrol.alert.AlertTutorial;
import com.sonymobile.dronecontrol.utils.EnumDevices;
import com.sonymobile.dronecontrol.utils.Logger;
import com.sonymobile.dronecontrol.utils.Notifier;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.SystemClock;
import android.widget.Toast;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;

public class BebopDeviceController extends DeviceController implements ARCommandCommonCommonStateBatteryStateChangedListener,
        ARCommandARDrone3PilotingStateAltitudeChangedListener, ARCommandCommonCommonStateAllStatesChangedListener,
        ARCommandCommonSettingsAllSettingsListener, ARCommandARDrone3PilotingFlatTrimListener, ARCommandARDrone3PilotingPCMDListener,
        ARCommandARDrone3PilotingStatePositionChangedListener, ARCommandCommonCommonStateWifiSignalChangedListener,
        ARCommandARDrone3PilotingStateSpeedChangedListener, ARCommandCommonControllerStateIsPilotingChangedListener,
        ARCommandARDrone3MediaStreamingVideoEnableListener, ARCommandARDrone3PilotingStateFlyingStateChangedListener,
        ARCommandARDrone3GPSSettingsStateHomeChangedListener, ARCommandARDrone3MediaStreamingStateVideoEnableChangedListener,
        ARCommandARDrone3MediaRecordStateVideoStateChangedListener, ARCommandARDrone3MediaRecordStatePictureStateChangedListener,
        ARCommandCommonCommonStateMassStorageInfoRemainingListChangedListener, ARCommandCommonCommonStateMassStorageStateListChangedListener,
        ARCommandCommonCommonStateMassStorageInfoStateListChangedListener, ARCommandARDrone3PilotingStateNavigateHomeStateChangedListener,
        ARCommandARDrone3PilotingStateAlertStateChangedListener, ARCommandARDrone3GPSSettingsStateGPSFixStateChangedListener,
        ARCommandARDrone3GPSSettingsStateGPSUpdateStateChangedListener, ARCommandCommonSettingsStateProductVersionChangedListener, ARCommandARDrone3AnimationsFlipListener {

    private static final int DEFAULT_VIDEO_FRAGMENT_SIZE = 65000;
    private static final int DEFAULT_VIDEO_FRAGMENT_MAXIMUM_NUMBER = 4;
    private static final int VIDEO_RECEIVE_TIMEOUT = 500;

    protected static List<ARNetworkIOBufferParam> c2dParams = new ArrayList<>();
    protected static List<ARNetworkIOBufferParam> d2cParams = new ArrayList<>();
    protected static int commandsBuffers[] = {};

    private DeviceControllerListener mControllerListener;
    private static String TAG = BebopDeviceController.class.getSimpleName();

    private final static int iobufferC2dNack = 10;
    private final static int iobufferC2dAck = 11;
    private final static int iobufferC2dEmergency = 12;
    private final static int iobufferC2dArstreamAck = 13;
    private final static int iobufferD2cNavdata = 127;
    private final static int iobufferD2cEvents = 126;
    private final static int iobufferD2cArstreamData = 125;

    private String discoveryIp;
    private int discoveryPort;
    private Context mContext;
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
    private ARStreamReader streamReader = null;
    private ARNativeData nativeData = null;
    private ARStreamReaderListener streamReaderListener;
    private Thread mStreamReaderAckThread;
    private Thread mStreamReaderDataThread;

    private double mLatitude = 500;
    private double mLongitude = 500;
    private double mAltitude = 500;

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public double getAltitude() {
        return mAltitude;
    }

    private boolean mIsWaitingForPicture = false;

    private boolean mIsConnected = false;
    private final Object lock = new Object();

    private float mSensitivity = 0.5f;

    private static BebopDeviceController bebopDeviceController = new BebopDeviceController();

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
        c2dParams.add(ARStreamReader.newAckARNetworkIOBufferParam(iobufferC2dArstreamAck));

        d2cParams.clear();
        d2cParams.add(new ARNetworkIOBufferParam(iobufferD2cNavdata, ARNETWORKAL_FRAME_TYPE_ENUM.ARNETWORKAL_FRAME_TYPE_DATA, 20,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER, ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_INFINITE_NUMBER, 20,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_DATACOPYMAXSIZE_USE_MAX, false));
        d2cParams.add(new ARNetworkIOBufferParam(iobufferD2cEvents, ARNETWORKAL_FRAME_TYPE_ENUM.ARNETWORKAL_FRAME_TYPE_DATA_WITH_ACK, 20, 500, 3, 20,
                ARNetworkIOBufferParam.ARNETWORK_IOBUFFERPARAM_DATACOPYMAXSIZE_USE_MAX, false));
        d2cParams.add(ARStreamReader
                .newDataARNetworkIOBufferParam(iobufferD2cArstreamData, DEFAULT_VIDEO_FRAGMENT_SIZE, DEFAULT_VIDEO_FRAGMENT_MAXIMUM_NUMBER));

        commandsBuffers = new int[]{iobufferD2cNavdata, iobufferD2cEvents};

    }

    public BebopDeviceController() {

    }

    public static BebopDeviceController getInstance() {
        if (bebopDeviceController == null) {
            bebopDeviceController = new BebopDeviceController();
        }

        return bebopDeviceController;
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
        mIsWaitingForPicture = false;
        deviceService = service;
        this.mContext = context;
        if (readerThreads == null) {
            readerThreads = new ArrayList<ReaderThread>();
        }
        c2dPort = 54321;
        d2cPort = 43210;

    }

    public void setVideoFrameListener(DeviceControllerListener l) {
        this.mControllerListener = l;
    }

    public EnumDevices getDeviceInfo() {
        return EnumDevices.BEBOP;
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

        nativeData = new ARNativeData();

        streamReaderListener = new ARStreamReaderListener() {
            private int lastValidBufferCapacity = 0;

            @Override
            public ARNativeData didUpdateFrameStatus(ARSTREAM_READER_CAUSE_ENUM cause, final ARNativeData currentFrame, final boolean isFlushFrame,
                    int nbSkippedFrames, int newBufferCapacity) {

                if (cause == ARSTREAM_READER_CAUSE_ENUM.ARSTREAM_READER_CAUSE_FRAME_COMPLETE) {

                    if (mControllerListener != null) {
                        mControllerListener.onNewVideoFrame(currentFrame.getByteData(), currentFrame.getDataSize(), isFlushFrame);
                    }
                    currentFrame.dispose();
                    return new ARNativeData(lastValidBufferCapacity);
                } else if (cause == ARSTREAM_READER_CAUSE_ENUM.ARSTREAM_READER_CAUSE_FRAME_TOO_SMALL) {
                    lastValidBufferCapacity = newBufferCapacity;
                    return new ARNativeData(lastValidBufferCapacity);
                } else if (cause == ARSTREAM_READER_CAUSE_ENUM.ARSTREAM_READER_CAUSE_COPY_COMPLETE) {
                    currentFrame.dispose();
                    return null;
                } else if (cause == ARSTREAM_READER_CAUSE_ENUM.ARSTREAM_READER_CAUSE_CANCEL) {
                    currentFrame.dispose();
                    return null;
                }
                return null;
            }
        };

        if (netManager != null) {
            streamReader = new ARStreamReader(netManager, iobufferD2cArstreamData, iobufferC2dArstreamAck, nativeData, streamReaderListener,
                    DEFAULT_VIDEO_FRAGMENT_SIZE, VIDEO_RECEIVE_TIMEOUT);

            mStreamReaderDataThread = new Thread(streamReader.getDataRunnable());
            mStreamReaderAckThread = new Thread(streamReader.getAckRunnable());

            mStreamReaderDataThread.start();
            mStreamReaderAckThread.start();

        } else {
            Logger.d(TAG, "Could not start video streaming");
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

    private void stop() {
        Logger.d(TAG, "stop ...");

        // stop recording video
        sendRecordVideo(false);

        if (streamReader != null) {
            streamReader.stop();
        }

        stopLooperThread();
        stopReaderThreads();


        if (mStreamReaderDataThread != null) {
            mStreamReaderDataThread.interrupt();
            mStreamReaderDataThread = null;
        }

        if (mStreamReaderAckThread != null) {
            mStreamReaderAckThread.interrupt();
            mStreamReaderAckThread = null;
        }

        if (streamReader != null) {
            streamReader.dispose();
            streamReader = null;
        }

        nativeData.dispose();
        nativeData = null;

        /* ARNetwork cleanup */
        stopNetwork();

        unregisterARCommandsListener();

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

        if (failed == false) {
            /* Create and start Tx and Rx threads */
            rxThread = new Thread(netManager.m_receivingRunnable);
            rxThread.start();

            txThread = new Thread(netManager.m_sendingRunnable);
            txThread.start();
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
            }

            mediaOpened = false;
            alManager.dispose();
        }
    }

    protected void registerARCommandsListener() {
        ARCommand.setCommonCommonStateBatteryStateChangedListener(this);
        ARCommand.setARDrone3PilotingStateAltitudeChangedListener(this);
        ARCommand.setCommonCommonStateAllStatesChangedListener(this);
        ARCommand.setARDrone3PilotingFlatTrimListener(this);
        ARCommand.setARDrone3PilotingPCMDListener(this);
        ARCommand.setCommonCommonStateWifiSignalChangedListener(this);
        ARCommand.setARDrone3PilotingStatePositionChangedListener(this);
        ARCommand.setARDrone3PilotingStateSpeedChangedListener(this);
        ARCommand.setARDrone3MediaStreamingStateVideoEnableChangedListener(this);
        ARCommand.setARDrone3MediaStreamingVideoEnableListener(this);
        ARCommand.setARDrone3PilotingStateAlertStateChangedListener(this);
        ARCommand.setARDrone3PilotingStateFlyingStateChangedListener(this);
        ARCommand.setARDrone3MediaRecordStatePictureStateChangedListener(this);
        ARCommand.setCommonCommonStateMassStorageInfoRemainingListChangedListener(this);
        ARCommand.setCommonCommonStateMassStorageInfoStateListChangedListener(this);
        ARCommand.setCommonSettingsAllSettingsListener(this);
        ARCommand.setARDrone3GPSSettingsStateHomeChangedListener(this);
        ARCommand.setARDrone3GPSSettingsStateGPSFixStateChangedListener(this);
        ARCommand.setARDrone3GPSSettingsStateGPSUpdateStateChangedListener(this);
        ARCommand.setCommonSettingsStateProductVersionChangedListener(this);
        ARCommand.setARDrone3AnimationsFlipListener(this);
        ARCommand.setARDrone3MediaRecordStateVideoStateChangedListener(this);
    }

    protected void unregisterARCommandsListener() {
        ARCommand.setCommonCommonStateBatteryStateChangedListener(null);
        ARCommand.setARDrone3PilotingStateAltitudeChangedListener(null);
        ARCommand.setCommonCommonStateAllStatesChangedListener(null);
        ARCommand.setARDrone3PilotingFlatTrimListener(null);
        ARCommand.setARDrone3PilotingPCMDListener(null);
        ARCommand.setCommonCommonStateWifiSignalChangedListener(null);
        ARCommand.setARDrone3PilotingStatePositionChangedListener(null);
        ARCommand.setARDrone3PilotingStateSpeedChangedListener(null);
        ARCommand.setARDrone3MediaStreamingStateVideoEnableChangedListener(null);
        ARCommand.setARDrone3MediaStreamingVideoEnableListener(null);
        ARCommand.setARDrone3MediaRecordStateVideoStateChangedListener(null);
        ARCommand.setARDrone3PilotingStateAlertStateChangedListener(null);
        ARCommand.setARDrone3PilotingStateFlyingStateChangedListener(null);
        ARCommand.setARDrone3MediaRecordStatePictureStateChangedListener(null);
        ARCommand.setCommonCommonStateMassStorageInfoRemainingListChangedListener(null);
        ARCommand.setCommonCommonStateMassStorageInfoStateListChangedListener(null);
        ARCommand.setCommonSettingsAllSettingsListener(null);
        ARCommand.setARDrone3GPSSettingsStateHomeChangedListener(null);
        ARCommand.setARDrone3GPSSettingsStateGPSFixStateChangedListener(null);
        ARCommand.setARDrone3GPSSettingsStateGPSUpdateStateChangedListener(null);
        ARCommand.setCommonSettingsStateProductVersionChangedListener(null);
        ARCommand.setARDrone3AnimationsFlipListener(null);
        ARCommand.setARDrone3MediaRecordStateVideoStateChangedListener(null);
    }

    public void setGamepadSensitivity(float value) {
        mSensitivity = value;
    }

    private boolean sendPCMD() {

        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setARDrone3PilotingPCMD(dataPCMD.flag, dataPCMD.roll, dataPCMD.pitch, dataPCMD.yaw, dataPCMD.gaz, dataPCMD.psi);
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
     * Clear current commands
     */
    public void clearCurrentCommands() {
        if (dataPCMD != null) {
            dataPCMD.reset();
        }
    }

    public boolean sendCameraOrientation(byte tilt, byte pan) {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setARDrone3CameraOrientation(tilt, pan);
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
            Logger.e(TAG, "Failed to send Orientation command.");
        }

        return true;
    }


    public boolean sendTakeoff() {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setARDrone3PilotingTakeOff();
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

    public void sendOutdoorMode(boolean outdoor) {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setARDrone3SpeedSettingsOutdoor(outdoor ? (byte)1 : (byte)0);
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
            Logger.e(TAG, "Failed to send Outdoor mode command.");
        }
    }

    /**
     * Send a command <code>Flip</code> of class <code>Animations</code> in project
     * <code>MiniDrone</code>
     *
     * @param direction Direction for the flip
     */
    public boolean sendFlip(ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM direction) {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setARDrone3AnimationsFlip(direction);
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
            Logger.e(TAG, "Failed to send Flip command.");
        }

        return sentStatus;
    }

    public boolean sendLanding() {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setARDrone3PilotingLanding();
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

        cmdError = cmd.setARDrone3PilotingEmergency();
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
        Logger.d(TAG, "onCommonCommonStateBatteryStateChangedUpdate ...: " + b + " %");
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onUpdateBattery(b);
                }
            }
        };
    }

    public byte[] getPCMDData() {
        byte arrayPCMD[] = new byte[4];
        arrayPCMD[0] = dataPCMD.pitch;
        arrayPCMD[1] = dataPCMD.roll;
        arrayPCMD[2] = dataPCMD.yaw;
        arrayPCMD[3] = dataPCMD.gaz;
        return arrayPCMD;
    }

    @Override
    public void onARDrone3PilotingStateAltitudeChangedUpdate(final double v) {
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onAltitudeChange(v);
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

    @Override
    public void onCommonCommonStateWifiSignalChangedUpdate(final short i) {
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onWifiSignalChange(i);
                }
            }
        };
    }

    @Override
    public void onARDrone3PilotingStateSpeedChangedUpdate(final float speedX, final float speedY, final float speedZ) {
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onSpeedChange(speedX, speedY, speedZ);
                }
            }
        };
    }

    @Override
    public void onARDrone3MediaStreamingVideoEnableUpdate(byte b) {
        Logger.d(TAG, "onARDrone3MediaStreamingVideoEnableUpdate ... " + b);
    }

    @Override
    public void onARDrone3MediaStreamingStateVideoEnableChangedUpdate(ARCOMMANDS_ARDRONE3_MEDIASTREAMINGSTATE_VIDEOENABLECHANGED_ENABLED_ENUM e) {
        Logger.d(TAG, "onARDrone3MediaStreamingStateVideoEnableChangedUpdate ... " + e.toString());

    }

    @Override
    public void onARDrone3MediaRecordStateVideoStateChangedUpdate(final ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGED_STATE_ENUM e, byte b) {
        Logger.d(TAG, "onARDrone3MediaRecordStateVideoStateChangedUpdate ... " + e.toString() + " value " + b);
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    if (e == ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGED_STATE_ENUM
                            .ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGED_STATE_STARTED) {
                        listener.onVideoRecording(true);
                    } else if (e == ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGED_STATE_ENUM
                            .ARCOMMANDS_ARDRONE3_MEDIARECORDSTATE_VIDEOSTATECHANGED_STATE_STOPPED) {
                        listener.onVideoRecording(false);
                    }
                }
            }
        };
    }

    @Override
    public void onARDrone3PilotingStateAlertStateChangedUpdate(final ARCOMMANDS_ARDRONE3_PILOTINGSTATE_ALERTSTATECHANGED_STATE_ENUM e) {
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    if (e == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_ALERTSTATECHANGED_STATE_ENUM
                            .ARCOMMANDS_ARDRONE3_PILOTINGSTATE_ALERTSTATECHANGED_STATE_CRITICAL_BATTERY) {
                        listener.onAlertStateChange(AlertTutorial.AlertState.CRITICAL_BATTERY);
                    }
                }
            }
        };
    }

    public void onCommonControllerStateIsPilotingChangedUpdate(byte b) {
    }

    public boolean sendFlatTrim() {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setARDrone3PilotingFlatTrim();
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
        }

        cmd.dispose();

        if (sentStatus == false) {
            Logger.e(TAG, "Failed to send flat trim command.");
        }

        return true;
    }

    public boolean setPictureFormatSnapshot() {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setARDrone3PictureSettingsPictureFormatSelection(
                ARCOMMANDS_ARDRONE3_PICTURESETTINGS_PICTUREFORMATSELECTION_TYPE_ENUM
                        .ARCOMMANDS_ARDRONE3_PICTURESETTINGS_PICTUREFORMATSELECTION_TYPE_SNAPSHOT);

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
            Logger.e(TAG, "Failed to send take picture format command.");
        }

        return true;
    }

    public boolean setPictureAutoWhiteBalance() {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setARDrone3PictureSettingsAutoWhiteBalanceSelection(
                ARCOMMANDS_ARDRONE3_PICTURESETTINGS_AUTOWHITEBALANCESELECTION_TYPE_ENUM
                        .ARCOMMANDS_ARDRONE3_PICTURESETTINGS_AUTOWHITEBALANCESELECTION_TYPE_AUTO);

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
            Logger.e(TAG, "Failed to send auto white balance command.");
        }
        return true;
    }

    public boolean sendTakePicture() {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setARDrone3MediaRecordPicture((byte)0);

        if (cmdError == ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK) {
            if (netManager != null) {
                ARNETWORK_ERROR_ENUM netError = netManager.sendData(iobufferC2dAck, cmd, null, true);
                mIsWaitingForPicture = true;
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

        return sentStatus;
    }

    public boolean sendRecordVideo(boolean start) {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setARDrone3MediaRecordVideo(
                start ? ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEO_RECORD_ENUM.ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEO_RECORD_START :
                        ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEO_RECORD_ENUM.ARCOMMANDS_ARDRONE3_MEDIARECORD_VIDEO_RECORD_STOP,
                (byte)0);
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
            Logger.e(TAG, "Failed to send record video command.");
        }

        return sentStatus;
    }

    public boolean sendSetHome(double lat, double lon, double alt) {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();
        cmdError = cmd.setARDrone3GPSSettingsSetHome(lat, lon, alt);
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
            Logger.e(TAG, "Failed to send set home command.");
        }
        return true;
    }

    public boolean sendGoHome() {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();
        cmdError = cmd.setARDrone3PilotingNavigateHome((byte)1);
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
            Logger.e(TAG, "Failed to send go home command.");
        }
        return true;
    }

    public boolean sendMaxAltitude(float maxAlt) {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setARDrone3PilotingSettingsMaxAltitude(maxAlt);
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
            Logger.e(TAG, "Failed to send max altitude command.");
        }

        return true;
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

    // send
    public boolean setVideoStreaming(boolean enable) {
        /* Attempt to get initial settings */
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();
        cmdError = cmd.setARDrone3MediaStreamingVideoEnable((byte)(enable ? 1 : 0));
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
            Logger.e(TAG, "Failed to send video streaming command.");
        }
        return sentStatus;
    }

    public boolean getInitialSettings() {
        /* Attempt to get initial settings */
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setCommonSettingsAllSettings();

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
            Logger.e(TAG, "Failed to send initial settings command.");
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

        return sentStatus;
    }

    @Override
    public void onARDrone3PilotingFlatTrimUpdate() {
        Logger.d(BebopDeviceController.TAG, "onARDrone3PilotingFlatTrimUpdate ...");
    }

    @Override
    public void onARDrone3PilotingPCMDUpdate(byte flag, byte roll, byte pitch, byte yaw, byte gaz, float psi) {
        Logger.d(TAG, "nARDrone3PilotingPCMDUpdate Flag:" + flag + " Roll:" + roll + " Pitch:" +
                pitch + " Yaw:" + yaw + " gaz:" + gaz + " Psi:" + psi + " ...");
    }

    @Override
    public void onARDrone3PilotingStateFlyingStateChangedUpdate(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM e) {
        switch (e) {
            case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
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
            case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
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
            case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
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
            case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDING:
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
            case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_TAKINGOFF:
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
            case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_EMERGENCY:
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

    public void sendMagnetCalibration(boolean b) {
        ARCOMMANDS_GENERATOR_ERROR_ENUM cmdError = ARCOMMANDS_GENERATOR_ERROR_ENUM.ARCOMMANDS_GENERATOR_OK;
        boolean sentStatus = true;
        ARCommand cmd = new ARCommand();

        cmdError = cmd.setCommonCalibrationMagnetoCalibration(b ? (byte)1 : (byte)0);
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
            Logger.e(TAG, "Failed to send magnet command");
        }
    }

    @Override
    public void onARDrone3MediaRecordStatePictureStateChangedUpdate(byte state, byte mass_storage_id) {

        final Boolean gotPicture = (state == 1);

        if (mIsWaitingForPicture) {
            if (mControllerListener != null) {
                mControllerListener.onTakePicture(gotPicture);
                mIsWaitingForPicture = false;
            }
        }
    }

    public String getIPAddress() {
        return discoveryIp;
    }

    @Override
    public void onCommonCommonStateMassStorageInfoRemainingListChangedUpdate(int free_space, short rec_time, int photo_remaining) {
        Logger.d(TAG, "Mass storage State: free space: " + free_space + "MB" + " rec_free_time: " +
                rec_time + " min" + " photo_free_space: " + photo_remaining + " pics");
    }

    @Override
    public void onCommonCommonStateMassStorageInfoStateListChangedUpdate(byte mass_storage_id, int size, int used_size, byte plugged, final byte full,
            byte internal) {
        Logger.d(TAG,
                "Mass storage state: Mass storage id: " + mass_storage_id + (internal == 1 ? " -> It is internal storage" : " -> It is external " +
                        "storage"));
        Logger.d(TAG, "Mass storage state: Total space: " + size + "MB");
        Logger.d(TAG, "Mass storage state: " + (full == 1 ? "Storage is FULL!" : "Storage has " + (size - used_size) + "MB of free space"));

        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onSDCardFull(full == 1 ? true : false);
                }
            }
        };
    }

    @Override
    public void onCommonCommonStateMassStorageStateListChangedUpdate(byte b, String s) {

    }

    //Home gps value
    @Override
    public void onARDrone3GPSSettingsStateHomeChangedUpdate(final double latitude, final double longitude, final double altitude) {
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onGPSHomeChangedUpdate(String.valueOf(latitude) + String.valueOf(longitude) + String.valueOf(altitude));
                }
            }
        };
    }

    //Get gps value updated
    @Override
    public void onARDrone3PilotingStatePositionChangedUpdate(final double latitude, final double longitude, final double altitude) {
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onGPSControllerPosition(latitude, longitude, altitude);
                }
            }
        };
        if (latitude != 500 && longitude != 500) {
            mAltitude = altitude;
            mLongitude = longitude;
            mLatitude = latitude;
        }
    }

    @Override
    public void onARDrone3PilotingStateNavigateHomeStateChangedUpdate(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_NAVIGATEHOMESTATECHANGED_STATE_ENUM e,
            ARCOMMANDS_ARDRONE3_PILOTINGSTATE_NAVIGATEHOMESTATECHANGED_REASON_ENUM e2) {

        if (e2 == ARCOMMANDS_ARDRONE3_PILOTINGSTATE_NAVIGATEHOMESTATECHANGED_REASON_ENUM
                .ARCOMMANDS_ARDRONE3_PILOTINGSTATE_NAVIGATEHOMESTATECHANGED_REASON_FINISHED) {
            Toast.makeText(mContext, "Parrot arrived!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onARDrone3GPSSettingsStateGPSFixStateChangedUpdate(final byte res) {
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    listener.onGPSFixed(res == 1 ? true : false);
                }
            }
        };
    }

    @Override
    public void onARDrone3GPSSettingsStateGPSUpdateStateChangedUpdate(
            final ARCOMMANDS_ARDRONE3_GPSSETTINGSSTATE_GPSUPDATESTATECHANGED_STATE_ENUM state) {
        new Notifier() {
            @Override
            public void onNotify(DeviceControllerListener listener) {
                if (listener != null) {
                    /*if (state == ARCOMMANDS_ARDRONE3_GPSSETTINGSSTATE_GPSUPDATESTATECHANGED_STATE_ENUM
                            .ARCOMMANDS_ARDRONE3_GPSSETTINGSSTATE_GPSUPDATESTATECHANGED_STATE_INPROGRESS) {
                        Toast.makeText(mContext, "GPS signal in progress...", Toast.LENGTH_SHORT).show();
                    } else if (state == ARCOMMANDS_ARDRONE3_GPSSETTINGSSTATE_GPSUPDATESTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_GPSSETTINGSSTATE_GPSUPDATESTATECHANGED_STATE_UPDATED) {
                        Toast.makeText(mContext, "GPS signal updated!", Toast.LENGTH_SHORT).show();
                    } else if (state == ARCOMMANDS_ARDRONE3_GPSSETTINGSSTATE_GPSUPDATESTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_GPSSETTINGSSTATE_GPSUPDATESTATECHANGED_STATE_MAX) {
                        Toast.makeText(mContext, "GPS signal max!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mContext, "GPS signal failed..", Toast.LENGTH_SHORT).show();
                    }*/
                }
            }
        };
    }

    @Override
    public void onCommonSettingsStateProductVersionChangedUpdate(String software, String hardware) {
        Logger.e(TAG, "Software: " + software);
        Logger.e(TAG, "Hardware: " + hardware);
    }

    @Override
    public void onARDrone3AnimationsFlipUpdate(
            ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM anim) {
        if (anim == ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_BACK ||
                anim == ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_FRONT ||
                anim == ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_LEFT ||
                anim == ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_ENUM.ARCOMMANDS_ARDRONE3_ANIMATIONS_FLIP_DIRECTION_RIGHT) {
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

            /* read data*/
            if (netManager != null) {
                netError = netManager.readDataWithTimeout(bufferId, dataRecv, 1000);
            }

            if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_OK) {
                if (netError != ARNETWORK_ERROR_ENUM.ARNETWORK_ERROR_BUFFER_EMPTY) {
                    Logger.e(TAG, "ReaderThread readDataWithTimeout() failed. " +
                            netError + " bufferId: " + bufferId + " " + netError.toString());
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
