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

import com.sonymobile.dronecontrol.R;
import com.sonymobile.dronecontrol.controller.DeviceController;
import com.sonymobile.dronecontrol.liveware.GlassesDroneControl;

import java.util.HashSet;

public abstract class AlertTutorial {

    public final static String TAG = AlertTutorial.class.getSimpleName();
    private HashSet<DroneAction> mPerformedActions = new HashSet<DroneAction>();

    public enum DroneAction {
        ACTION_TAKEOFF,
        ACTION_LAND,
        ACTION_INITDEVICE,
        ACTION_IDLE,
        ACTION_TRICK,
        ACTION_JUMP,
        ACTION_TURBO,
        ACTION_TRICK_DONE,
        ACTION_TOGGLE_HUD;
    }

    protected AlertTutorial() {
    }

    public void performAction(DroneAction action) {
        if (!mPerformedActions.contains(action)) {
            if (GlassesDroneControl.getInstance() != null) {
                if (callAction(action)) {
                    if (!mPerformedActions.contains(action)) {
                        mPerformedActions.add(action);
                    }
                }
            }
        }
    }

    public abstract boolean callAction(DroneAction action);

    public abstract boolean callActionByState(DeviceController.DeviceState state);

    public enum AlertState {
        // USAGE: ITEM(R.string.resource, R.drawable.resource, int duration)
        // use -1 for no icon or -1 for static alerts
        LOW_BATTERY(R.string.message_low_bat_glass, R.drawable.ic_glass_battery_20, 3),
        CRITICAL_BATTERY(R.string.message_critical_bat_glass, R.drawable.ic_glass_no_battery, -1),
        CRITICAL_BATTERY_FLYING(R.string.message_critical_bat_flying_glass, R.drawable.ic_glass_no_battery, -1),
        CUT_OUT(R.string.message_cut_out, -1, 1),
        PICTURE_OK(R.string.message_photo_success, R.drawable.glass_ic_take_pic, 1),
        PICTURE_NOK(R.string.message_photo_failed, R.drawable.glass_ic_pic_not_taken, 1),
        TAKING_PICTURE(R.string.message_taking_pic, R.drawable.glass_ic_pic_alert, -1),
        WEAK_SIGNAL(R.string.message_wifi_signal_weak, -1, -1),
        RECORDING_STARTED(R.string.message_recording_started, R.drawable.glass_ic_video_camera, 2),
        VIDEO_REC_FINISHED(R.string.message_rec_finished, -1, 2),
        SDCARD_FULL(R.string.toast_sdcard_full_message, R.drawable.ic_glass_sd_card_full, 3),
        TAKEOFF(R.string.message_take_off, R.drawable.glass_ic_x_large, -1),
        LAND(R.string.message_land, R.drawable.glass_ic_x_large, 2),
        TRICK(R.string.message_trick, R.drawable.glass_ic_triangle_large, 2),
        TRICK_INTRO(R.string.message_trick_introduction, -1, -1),
        TRICK_DONE(R.string.message_trick_done, -1, 1),
        JUMP(R.string.message_l3_jump, -1, 3),
        TURBO(R.string.message_turbo, -1, 3),
        SHOW_HUD(R.string.message_show_hud, R.drawable.glass_ic_options, 2),
        ANALOG_MOVE(R.string.message_analog_to_move, -1, 3),
        GOOD_JOB(R.string.message_good_job, -1, 2),
        BATTERY(R.string.message_battery_level, -1, 2),
        GO_HOME(R.string.go_home, -1, -1),
        HOME_SET(R.string.home_set, -1, 2),
        TAKING_OFF(R.string.message_taking_off, -1, -1),
        LANDING(R.string.message_landing, -1, -1),
        EMERGENCY(R.string.message_emergency, -1, -1),
        NONE(0, -1, 0);

        public int alertName;
        public int alertIcon;
        public int alertDuration;

        private AlertState(int name, int icon, int duration) {
            alertName = name;
            alertIcon = icon;
            alertDuration = duration;
        }
    }

}
