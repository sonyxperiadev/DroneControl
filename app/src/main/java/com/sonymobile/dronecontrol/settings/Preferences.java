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

package com.sonymobile.dronecontrol.settings;

import com.sonymobile.dronecontrol.utils.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {

    private static final String APP_PACKAGE = "com.sonymobile.dronecontrol";

    public static final String PREF_SENS = "controller_sensitivity";
    public static final String PREF_AUTO_PILOT = "autopilot";
    public static final String PREF_FENCE = "fence";
    public static final String PREF_OUTDOOR = "outdoor";
    public static final String PREF_ALTITUDE = "altitude";
    public static final String PREF_GOHOME = "gohome";
    public static final String PREF_SET_HOME = "sethome";
    public static final String PREF_LOG = "logging";

    public static void setLoggingEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_LOG, enabled).commit();
    }

    public static boolean getLoggingState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_LOG, false);
    }

    public static void setAutoPilotMode(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_AUTO_PILOT, enabled).commit();
    }

    public static boolean getAutoPilotMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AUTO_PILOT, false);
    }

    public static void setMaxFenceState(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_FENCE, enabled).commit();
    }

    public static boolean getFenceState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_FENCE, false);
    }

    public static void setOutdoorMode(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_OUTDOOR, enabled).commit();
    }

    public static boolean getOutdoorMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_OUTDOOR, false);
    }

    public static void setMaxAltitude(Context context, int max) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_ALTITUDE, max).commit();
    }

    public static int getMaxAltitude(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        return prefs.getInt(PREF_ALTITUDE, 2);
    }

    public static void setGamepadSensitivity(Context context, float sensitivity) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        prefs.edit().putFloat(PREF_SENS, sensitivity).commit();
    }

    public static float getGamepadSensitivity(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        return prefs.getFloat(PREF_SENS, Utils.MEDIUM_SENSITIVITY);
    }

    public static void setGoHomeState(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_GOHOME, enabled).commit();
    }

    public static boolean getGoHomeState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_GOHOME, false);
    }

    public static void setGoHomeSettingState(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_SET_HOME, enabled).commit();
    }

    public static boolean getGoHomeSettingState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(APP_PACKAGE, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_SET_HOME, false);
    }
}
