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

package com.sonymobile.dronecontrol.alert;

import com.sonymobile.dronecontrol.controller.DeviceController.DeviceState;
import com.sonymobile.dronecontrol.liveware.GlassesDroneControl;

public class SumoTutorial extends AlertTutorial {

    public SumoTutorial() {
    }

    @Override
    public boolean callAction(DroneAction action) {

        // chain actions
            switch (action) {
                case ACTION_INITDEVICE:
                    GlassesDroneControl.getInstance().doCenteredBlinkingAlert(AlertState.ANALOG_MOVE);
                    return true;
                case ACTION_TAKEOFF:
                    GlassesDroneControl.getInstance().doCenteredBlinkingAlert(AlertState.JUMP);
                    return true;
                case ACTION_JUMP:
                    GlassesDroneControl.getInstance().doCenteredBlinkingAlert(AlertState.TURBO);
                    return true;
                case ACTION_TURBO:
                    GlassesDroneControl.getInstance().doCenteredBlinkingAlert(AlertState.TRICK);
                    return true;
                case ACTION_TRICK:
                    GlassesDroneControl.getInstance().doCenteredBlinkingAlert(AlertState.GOOD_JOB);
                    return true;
                case ACTION_TRICK_DONE:
                    return true;
            }
        return false;
    }

    @Override
    public boolean callActionByState(DeviceState state) {
        return true;
    }
}
