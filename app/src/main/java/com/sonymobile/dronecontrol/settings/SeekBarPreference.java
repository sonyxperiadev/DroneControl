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

import com.sonymobile.dronecontrol.R;
import com.sonymobile.dronecontrol.utils.Logger;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {

    private final String TAG = SeekBarPreference.class.getSimpleName();

    private static final int DEFAULT_VALUE = 1;
    private static int MAX_DRONE_ALTITUDE = 100; // 100 meters
    private static int MIN_DRONE_ALTITUDE = 1; // 1 meter

    private int mCurrentValue;
    private TextView mStatusText;

    public SeekBarPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public SeekBarPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        RelativeLayout layout = null;

        try {
            final LayoutInflater mInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            layout = (RelativeLayout)mInflater.inflate(R.layout.altitude_seek_bar_preference, parent, false);

            final SeekBar seekBar = (SeekBar)layout.findViewById(R.id.seekBarPrefSeekBar);
            seekBar.setMax(MAX_DRONE_ALTITUDE);
            seekBar.setProgress(mCurrentValue);
            seekBar.incrementProgressBy(1);
            seekBar.setOnSeekBarChangeListener(this);

            mStatusText = (TextView)layout.findViewById(R.id.seekBarPrefValue);
            mStatusText.setText(String.valueOf(mCurrentValue) + " m");
            mStatusText.setMinimumWidth(30);

        } catch (final Exception e) {
            Logger.e(TAG, "Error building seek bar preference", e);
        }

        return layout;
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        // change accepted, store it
        if (progress < MIN_DRONE_ALTITUDE) {
            mCurrentValue = MIN_DRONE_ALTITUDE;
        } else if (progress > MAX_DRONE_ALTITUDE) {
            mCurrentValue = MAX_DRONE_ALTITUDE;
        } else {
            mCurrentValue = progress;
        }
        notifyChanged();
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        mStatusText.setText(String.valueOf(mCurrentValue) + " m");
        Preferences.setMaxAltitude(getContext(), mCurrentValue);
        notifyChanged();
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray ta, final int index) {
        final int defaultValue = ta.getInt(index, DEFAULT_VALUE);
        return defaultValue;
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        mCurrentValue = Preferences.getMaxAltitude(getContext());

        if (mCurrentValue < MIN_DRONE_ALTITUDE) {
            mCurrentValue = MIN_DRONE_ALTITUDE;
        }
    }
}