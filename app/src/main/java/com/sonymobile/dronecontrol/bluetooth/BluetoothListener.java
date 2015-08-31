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

package com.sonymobile.dronecontrol.bluetooth;

import com.sonymobile.dronecontrol.utils.Logger;
import com.sonymobile.dronecontrol.utils.Utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;

import java.util.Set;

public class BluetoothListener {

    private static String TAG = BluetoothListener.class.getSimpleName();
    private final int INPUT_DEVICE = 4;
    private final String GAMEPAD_CLASS = "2508";
    private BluetoothAdapter mBluetoothAdapter;

    public BluetoothListener() {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean isBluetoothOn() {

        int state = mBluetoothAdapter.getState();

        if (state == mBluetoothAdapter.STATE_ON) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isGamepadConnected() {

        if (Utils.DEBUG) {
            return true;
        }

        boolean deviceState = false;
        boolean bluetoothState = isBluetoothOn();
        Logger.d(TAG, "Bluetooth on: " + bluetoothState);

        if (bluetoothState) {
            Set<BluetoothDevice> mBluetoothDevices = mBluetoothAdapter.getBondedDevices();

            for (BluetoothDevice device : mBluetoothDevices) {

                //if device is a gamepad controller, it's bonded and connected
                if (device.getBluetoothClass().toString().equals(GAMEPAD_CLASS) &&
                        device.getBondState() == BluetoothDevice.BOND_BONDED &&
                        mBluetoothAdapter.getProfileConnectionState(INPUT_DEVICE) == BluetoothProfile.STATE_CONNECTED) {

                    deviceState = true;
                }
            }
        } else {
            deviceState = false;
        }
        Logger.d(TAG, "Controller connected: " + deviceState);
        return deviceState;
    }
}
