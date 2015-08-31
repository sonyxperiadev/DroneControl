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

package com.sonymobile.dronecontrol.utils;

import com.parrot.arsdk.arcommands.ARCOMMANDS_COMMON_CALIBRATIONSTATE_MAGNETOCALIBRATIONAXISTOCALIBRATECHANGED_AXIS_ENUM;
import com.parrot.arsdk.arcommands.ARCommand;
import com.parrot.arsdk.arcommands.ARCommandCommonCalibrationMagnetoCalibrationListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCalibrationStateMagnetoCalibrationRequiredStateListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCalibrationStateMagnetoCalibrationStartedChangedListener;
import com.parrot.arsdk.arcommands.ARCommandCommonCalibrationStateMagnetoCalibrationStateChangedListener;

import com.sonymobile.dronecontrol.R;
import com.sonymobile.dronecontrol.activity.BebopActivity;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class BebopCalibratorFragment extends Fragment implements ARCommandCommonCalibrationMagnetoCalibrationListener,
        ARCommandCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChangedListener,
        ARCommandCommonCalibrationStateMagnetoCalibrationStateChangedListener,
        ARCommandCommonCalibrationStateMagnetoCalibrationStartedChangedListener,
        ARCommandCommonCalibrationStateMagnetoCalibrationRequiredStateListener {

    public final static String TAG = BebopCalibratorFragment.class.getSimpleName();
    public boolean mCalibrationStarted = false;
    private View mView = null;
    private CalibrationState mCalibrationState = null;
    private TextView mInfoText;
    private Activity mActivity = null;
    private Button mButtonOk;
    private Handler mHandler = new Handler();
    private ImageView mDroneYaw;
    private ImageView mDronePitch;
    private ImageView mDroneRoll;
    private ObjectAnimator mAnimPitch;
    private ObjectAnimator mAnimYaw;
    private ObjectAnimator mAnimRoll;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = getActivity();
    }

    public void executeCalibration() {

        if (mActivity != null && !mCalibrationStarted) {
            mCalibrationStarted = true;
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    showCalibration();
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    mCalibrationStarted = false;
                                    break;
                            }
                        }
                    };
                    AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                    builder.setMessage(mActivity.getResources().getString(R.string.calibration_calibrate_magnets))
                            .setPositiveButton(android.R.string.yes, dialogClickListener).setNegativeButton(android.R.string.no, dialogClickListener)
                            .show();
                }
            });
        }
    }

    private enum CalibrationState {
        DONE,
        X,
        Y,
        Z;
    }

    private void showCalibration() {

        ((BebopActivity)mActivity).setCalibrationStarted(true);
        mButtonOk = (Button)mView.findViewById(R.id.btOk);
        mButtonOk.setVisibility(View.INVISIBLE);
        mView.setVisibility(View.VISIBLE);

        if (mCalibrationState == CalibrationState.DONE) {
            mCalibrationState = CalibrationState.Z;
            mView.findViewById(R.id.btStarCalib).setVisibility(View.VISIBLE);
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mInfoText.setText("");
                }
            });
        }
    }

    public void hideCalibration() {
        mView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCommonCalibrationMagnetoCalibrationUpdate(byte calibrate) {
        Logger.d(TAG, "onCommonCalibrationMagnetoCalibrationUpdate ..." + calibrate);
    }

    @Override
    public void onCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChangedUpdate(
            ARCOMMANDS_COMMON_CALIBRATIONSTATE_MAGNETOCALIBRATIONAXISTOCALIBRATECHANGED_AXIS_ENUM axis) {
        Logger.d(TAG, "onCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChangedUpdate " + axis.name());
    }

    @Override
    public void onCommonCalibrationStateMagnetoCalibrationRequiredStateUpdate(byte required) {
        Logger.d(TAG, "onCommonCalibrationStateMagnetoCalibrationRequiredStateUpdate ..." + required);

        if (required == 1) {
            executeCalibration();
        }
    }

    @Override
    public void onCommonCalibrationStateMagnetoCalibrationStartedChangedUpdate(byte started) {
        Logger.d(TAG, "onCommonCalibrationStateMagnetoCalibrationStartedChangedUpdate ..." + started);
    }

    @Override
    public void onCommonCalibrationStateMagnetoCalibrationStateChangedUpdate(byte xAxisCalibration, byte yAxisCalibration, byte zAxisCalibration,
            byte calibrationFailed) {

        Logger.d(TAG, "Calib Change ..." + xAxisCalibration + " - " + yAxisCalibration + " - " + zAxisCalibration + " - " + calibrationFailed + " " +
                "calibState= " + (mCalibrationState == null ? "Null" : mCalibrationState.name()));

        if (mInfoText != null) {
            if (mCalibrationState == CalibrationState.Z && zAxisCalibration == 1) {

                mCalibrationState = CalibrationState.Y;
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDronePitch.setVisibility(View.VISIBLE);
                        mDroneYaw.setVisibility(View.INVISIBLE);
                        mAnimPitch.resume();
                        mInfoText.setText(mActivity.getResources().getString(R.string.calibrate_flip_forward));
                    }
                });
            } else if (mCalibrationState == CalibrationState.Y && yAxisCalibration == 1) {

                mCalibrationState = CalibrationState.X;
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDronePitch.setVisibility(View.INVISIBLE);
                        mDroneRoll.setVisibility(View.VISIBLE);
                        mAnimRoll.resume();
                        mInfoText.setText(mActivity.getResources().getString(R.string.calibrate_roll));
                    }
                });
            } else if (mCalibrationState == CalibrationState.X && xAxisCalibration == 1) {

                mCalibrationState = CalibrationState.DONE;
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mInfoText.setText(mActivity.getString(R.string.calibration_message_done));
                        mHandler.removeCallbacks(mAnimationRepeat);
                        mDroneYaw.setVisibility(View.GONE);
                        mDronePitch.setVisibility(View.GONE);
                        mDroneRoll.setVisibility(View.GONE);
                        mButtonOk.setVisibility(View.VISIBLE);
                        mButtonOk.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                hideCalibration();
                                mButtonOk.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                });
            }
        }
    }

    private Runnable mAnimationRepeat = new Runnable() {
        @Override
        public void run() {
            mAnimPitch.start();
            mAnimYaw.start();
            mAnimRoll.start();
            mHandler.postDelayed(this, 2000);
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        ((BebopActivity)mActivity).setCalibrationStarted(false);
        mHandler.removeCallbacks(mAnimationRepeat);
        mCalibrationStarted = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.calibration, container, false);
        mView = view;
        view.setVisibility(View.INVISIBLE);

        mInfoText = (TextView)view.findViewById(R.id.txtMsg);
        mDroneYaw = (ImageView)view.findViewById(R.id.iv_drone_calib_yaw);
        mDronePitch = (ImageView)view.findViewById(R.id.iv_drone_calib_pitch);
        mDroneRoll = (ImageView)view.findViewById(R.id.iv_drone_calib_roll);

        final Button btCalib = ((Button)view.findViewById(R.id.btStarCalib));
        btCalib.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                btCalib.setVisibility(View.INVISIBLE);
                mCalibrationState = CalibrationState.Z;

                Logger.d(TAG, "State:" + mCalibrationState.name());

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.removeCallbacks(mAnimationRepeat);
                        mAnimYaw = (ObjectAnimator)AnimatorInflater.loadAnimator(getActivity(), R.anim.rotate_yaw);
                        mAnimYaw.setTarget(mDroneYaw);
                        mAnimYaw.setDuration(2000);
                        mAnimRoll = (ObjectAnimator)AnimatorInflater.loadAnimator(mActivity.getApplicationContext(), R.anim.rotate_roll);
                        mAnimRoll.setTarget(mDroneRoll);
                        mAnimRoll.setDuration(2000);
                        mAnimPitch = (ObjectAnimator)AnimatorInflater.loadAnimator(mActivity.getApplicationContext(), R.anim.rotate_pitch);
                        mAnimPitch.setTarget(mDronePitch);
                        mAnimPitch.setDuration(2000);
                        mHandler.post(mAnimationRepeat);
                        mAnimPitch.pause();
                        mAnimRoll.pause();
                        mInfoText.setText(mActivity.getResources().getString(R.string.calibrate_yaw));
                    }
                });
                return true;
            }
        });
        return view;
    }

    public void enableListeners() {
        ARCommand.setCommonCalibrationMagnetoCalibrationListener(this);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChangedListener(this);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationRequiredStateListener(this);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationStartedChangedListener(this);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationStateChangedListener(this);
    }

    public void disableListeners() {
        ARCommand.setCommonCalibrationMagnetoCalibrationListener(null);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChangedListener(null);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationRequiredStateListener(null);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationStartedChangedListener(null);
        ARCommand.setCommonCalibrationStateMagnetoCalibrationStateChangedListener(null);
    }
}
