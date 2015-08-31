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

import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.registration.DeviceInfo;
import com.sonyericsson.extras.liveware.extension.util.registration.DisplayInfo;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationAdapter;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

import java.util.List;

public final class DroneControlExtensionService extends ExtensionService {

    public DroneControlExtensionService() {
        super(Constants.EXTENSION_KEY);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected RegistrationInformation getRegistrationInformation() {
        return new GlassesDroneRegistrationInformation(this);
    }

    @Override
    protected boolean keepRunningWhenConnected() {
        return false;
    }

    @Override
    public ControlExtension createControlExtension(final String hostAppPackageName) {
        ScreenSize size = new ScreenSize(this);
        final int width = size.getWidth();
        final int height = size.getHeight();
        List<DeviceInfo> list = RegistrationAdapter.getHostApplication(this, hostAppPackageName).getDevices();
        for (DeviceInfo device : list) {
            for (DisplayInfo display : device.getDisplays()) {
                if (display.sizeEquals(width, height)) {
                    GlassesDroneControl glassesDroneControl = new GlassesDroneControl(this, hostAppPackageName);
                    return glassesDroneControl;
                }
            }
        }
        throw new IllegalArgumentException("No control for: " + hostAppPackageName);
    }
}
