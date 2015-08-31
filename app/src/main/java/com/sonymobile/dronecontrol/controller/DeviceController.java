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
import com.sonymobile.dronecontrol.alert.AlertTutorial.AlertState;
import com.sonymobile.dronecontrol.liveware.GlassesDroneControl;
import com.sonymobile.dronecontrol.utils.EnumDevices;
import com.sonymobile.dronecontrol.utils.Logger;


public abstract class DeviceController {

    public static final String TAG = "DeviceController";

    private DeviceState mDeviceState = DeviceState.LANDED;

    public abstract EnumDevices getDeviceInfo();

    private static DeviceController sInstance;

    public static DeviceController getInstance() {
        return sInstance;
    }

    public void setCurrentDevice(DeviceController currentDevice) {
        sInstance = currentDevice;
    }

    public static void performAlertTutorialAction(AlertTutorial.DroneAction action) {
        GlassesDroneControl glassesDroneControl = GlassesDroneControl.getInstance();
        if (glassesDroneControl != null) {
            glassesDroneControl.performAction(action);
        }
    }

    public static void performCenteredBlinkAlert(AlertState alert) {
        GlassesDroneControl glassesDroneControl = GlassesDroneControl.getInstance();
        if (glassesDroneControl != null) {
            glassesDroneControl.doCenteredBlinkingAlert(alert);
        }
    }

    public enum DeviceState {
        FLYING,
        HOVERING,
        LANDED,
        LANDING,
        TAKING_OFF,
        EMERGENCY
    }

    public DeviceState getDeviceState() {
        return mDeviceState;
    }

    public void setDeviceState(DeviceState state) {
        Logger.d(TAG, "Changed device state to " + state.name());
        mDeviceState = state;
    }

}
