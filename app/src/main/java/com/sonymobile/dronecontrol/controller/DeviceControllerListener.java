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

import com.sonymobile.dronecontrol.alert.AlertTutorial;
import com.sonymobile.dronecontrol.utils.EnumDevices;

public interface DeviceControllerListener {

    public void onDisconnect();

    public void onUpdateBattery(final byte percent);

    public void onAltitudeChange(final double meters);

    public void onWifiSignalChange(final short rssi);

    public void onSpeedChange(final double speedX, final double speedY, final double speedZ);

    public void onAlertStateChange(AlertTutorial.AlertState e);

    public void onStateChange(DeviceController.DeviceState state);

    public void onNewVideoFrame(byte[] frame, int size, boolean flush);

    public void onConnectionTimeout();

    public void onDroneDeviceInit(EnumDevices device);

    public void onTakePicture(Boolean result);

    public void onPictureDownloaded(Boolean result, String path);

    public void onSDCardFull(boolean full);

    public void onDroneDeviceStop();

    public void onGPSHomeChangedUpdate(String msg);

    public void onGPSControllerPosition(double lat, double longi, double alt);

    public void onToggleHUD();

    public void onGPSFixed(boolean fixed);

    public void onTrickDone();

    public void onVideoRecording(Boolean isRecording);

}
