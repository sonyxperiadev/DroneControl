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

package com.sonymobile.dronecontrol.fragment;

import com.sonymobile.dronecontrol.R;
import com.sonymobile.dronecontrol.utils.EnumDevices;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ScreenFragment extends Fragment {

    private int mDeviceDrawable;
    private FragmentCommunicator mCommunicator;

    public void setCommunicator(FragmentCommunicator communicator) {
        mCommunicator = communicator;
    }

    public ScreenFragment(){

    }

    public void setDevice(EnumDevices device) {
        if (EnumDevices.BEBOP.equals(device)) {
            mDeviceDrawable = R.drawable.bebop_blue;
        } else if (EnumDevices.MINIDRONE.equals(device)) {
            mDeviceDrawable = R.drawable.rolling_spider_blue;
        } else if (EnumDevices.JUMPINGSUMO.equals(device)) {
            mDeviceDrawable = R.drawable.jumping_sumo_blue;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.connecting_fragment, container, false);

        ImageView deviceImage = (ImageView)view.findViewById(R.id.iv_device);
        deviceImage.setImageDrawable(getResources().getDrawable(mDeviceDrawable));

        return view;
    }

    @Override
    public void onPause() {
        mCommunicator.fragmentDetached();
        super.onPause();
    }

    public interface FragmentCommunicator {
        public void fragmentDetached();
    }
}
