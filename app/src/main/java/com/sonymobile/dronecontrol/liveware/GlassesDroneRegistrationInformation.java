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

import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;
import com.sonymobile.dronecontrol.R;
import com.sonymobile.dronecontrol.activity.AboutActivity;

import android.content.ContentValues;
import android.content.Context;

public final class GlassesDroneRegistrationInformation extends RegistrationInformation {

    private final Context context;
    private static final int CONTROL_API_VERSION = 4;

    public GlassesDroneRegistrationInformation(final Context context) {
        this.context = context;
    }

    @Override
    public int getRequiredControlApiVersion() {
        return CONTROL_API_VERSION;
    }

    @Override
    public int getTargetControlApiVersion() {
        return CONTROL_API_VERSION;
    }

    @Override
    public int getRequiredSensorApiVersion() {
        return API_NOT_REQUIRED;
    }

    @Override
    public int getRequiredNotificationApiVersion() {
        return API_NOT_REQUIRED;
    }

    @Override
    public int getRequiredWidgetApiVersion() {
        return API_NOT_REQUIRED;
    }

    @Override
    public ContentValues getExtensionRegistrationConfiguration() {
        String iconDrone = getUriString(R.drawable.drone);
        ContentValues values = new ContentValues();
        values.put(Registration.ExtensionColumns.CONFIGURATION_ACTIVITY, AboutActivity.class.getName());
        values.put(Registration.ExtensionColumns.CONFIGURATION_TEXT, context.getResources().getString(R.string.application_title_settings));
        values.put(Registration.ExtensionColumns.NAME, context.getResources().getString(R.string.application_title));
        values.put(Registration.ExtensionColumns.EXTENSION_KEY, Constants.EXTENSION_KEY);
        values.put(Registration.ExtensionColumns.HOST_APP_ICON_URI, iconDrone);
        values.put(Registration.ExtensionColumns.EXTENSION_ICON_URI, iconDrone);
        values.put(Registration.ExtensionColumns.EXTENSION_48PX_ICON_URI, iconDrone);
        values.put(Registration.ExtensionColumns.NOTIFICATION_API_VERSION, getRequiredNotificationApiVersion());
        values.put(Registration.ExtensionColumns.PACKAGE_NAME, context.getPackageName());
        return values;
    }

    @Override
    public boolean isDisplaySizeSupported(final int width, final int height) {
        ScreenSize size = new ScreenSize(context);
        return size.equals(width, height);
    }

    private String getUriString(final int id) {
        return ExtensionUtils.getUriString(context, id);
    }

}
