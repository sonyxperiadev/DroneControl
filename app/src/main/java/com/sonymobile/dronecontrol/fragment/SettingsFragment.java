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
import com.sonymobile.dronecontrol.alert.AlertTutorial.AlertState;
import com.sonymobile.dronecontrol.controller.BebopDeviceController;
import com.sonymobile.dronecontrol.controller.DeviceController;
import com.sonymobile.dronecontrol.settings.Preferences;
import com.sonymobile.dronecontrol.utils.EnumDevices;
import com.sonymobile.dronecontrol.utils.Utils;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.support.v4.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.settings, container, false);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (DeviceController.getInstance() != null) {
            if (DeviceController.getInstance().getDeviceInfo() == EnumDevices.BEBOP) {
                addPreferencesFromResource(R.xml.pref_bebop);
            } else if (DeviceController.getInstance().getDeviceInfo() == EnumDevices.JUMPINGSUMO) {
                addPreferencesFromResource(R.xml.pref_sumo);
            } else if (DeviceController.getInstance().getDeviceInfo() == EnumDevices.MINIDRONE) {
                addPreferencesFromResource(R.xml.pref_minidrone);
            }
        } else {
            addPreferencesFromResource(R.xml.pref_bebop);
        }

        final Preference saveHome = (Preference)findPreference("pref_check_set_home");
        final Preference goHome = (Preference)findPreference("pref_check_go_home");
        final CheckBoxPreference cbAutoPilot = (CheckBoxPreference)findPreference("pref_check_autopilot");
        final CheckBoxPreference cbFence = (CheckBoxPreference)findPreference("pref_check_maxfence");
        final CheckBoxPreference cbOutdoorMode = (CheckBoxPreference)findPreference("pref_outdoor_mode");

        if (cbAutoPilot != null) {
            cbAutoPilot.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Preferences.setAutoPilotMode(getActivity(), cbAutoPilot.isChecked());
                    return true;
                }
            });
        }

        if (cbFence != null) {
            cbFence.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Preferences.setMaxFenceState(getActivity(), cbFence.isChecked());
                    return true;
                }
            });
        }

        if (cbOutdoorMode != null) {
            cbOutdoorMode.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Preferences.setOutdoorMode(getActivity(), cbOutdoorMode.isChecked());
                    boolean isHomeset = Preferences.getGoHomeState(getActivity());
                    if (isHomeset && cbOutdoorMode.isChecked()) {
                        Preferences.setGoHomeSettingState(getActivity(), true);
                    } else {
                        Preferences.setGoHomeSettingState(getActivity(), false);
                    }
                    return true;
                }
            });
        }

        if (goHome != null) {
            goHome.setEnabled(Preferences.getGoHomeSettingState(getActivity()));
            goHome.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (DeviceController.getInstance() instanceof BebopDeviceController) {
                        final BebopDeviceController bebopDeviceController = (BebopDeviceController)DeviceController.getInstance();
                        if (Preferences.getGoHomeState(getActivity())) {
                            bebopDeviceController.sendGoHome();
                            bebopDeviceController.performCenteredBlinkAlert(AlertState.GO_HOME);
                            Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.go_home), Toast.LENGTH_SHORT).show();
                        }
                    }
                    return true;
                }
            });
        }

        if (saveHome != null) {
            saveHome.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (DeviceController.getInstance() instanceof BebopDeviceController && getActivity() != null) {
                        final BebopDeviceController bebopDeviceController = (BebopDeviceController)DeviceController.getInstance();
                        if (bebopDeviceController.getAltitude() != 500 && bebopDeviceController.getLongitude() != 500) {
                            bebopDeviceController.sendSetHome(bebopDeviceController.getLatitude(), bebopDeviceController.getLongitude(),
                                    bebopDeviceController.getAltitude());
                            Preferences.setGoHomeState(getActivity(), true);
                            bebopDeviceController.performCenteredBlinkAlert(AlertState.HOME_SET);
                            Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.home_set), Toast.LENGTH_SHORT).show();
                        } else {

                            final ProgressDialog ringProgressDialog = ProgressDialog
                                    .show(getActivity(), getActivity().getResources().getString(R.string.waiting_connection),
                                            getActivity().getResources().getString(R.string.wait_gps), true);
                            ringProgressDialog.setCancelable(true);
                            final Thread searchGPs = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        while (ringProgressDialog.isShowing()) {
                                            if (bebopDeviceController.getLatitude() != 500 && bebopDeviceController.getLongitude() != 500 &&
                                                    getActivity() != null) {
                                                Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.home_set),
                                                        Toast.LENGTH_SHORT).show();
                                                bebopDeviceController
                                                        .sendSetHome(bebopDeviceController.getLatitude(), bebopDeviceController.getLongitude(),
                                                                bebopDeviceController.getAltitude());
                                                Preferences.setGoHomeState(getActivity(), true);
                                                goHome.setEnabled(true);
                                                ringProgressDialog.dismiss();
                                                break;
                                            }
                                            Thread.sleep(1000);
                                        }
                                    } catch (Exception e) {
                                        ringProgressDialog.dismiss();
                                    }

                                }
                            });
                            ringProgressDialog.setOnCancelListener(new OnCancelListener() {

                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    dialog.dismiss();
                                    if (searchGPs.isAlive()) {
                                        searchGPs.interrupt();
                                    }
                                }
                            });

                            searchGPs.start();
                        }
                        if (goHome != null && getActivity() != null) {
                            goHome.setEnabled(Preferences.getGoHomeState(getActivity()));
                        }
                    }
                    return true;
                }
            });
        }

        final ListPreference listPref = (ListPreference)findPreference("pref_sensi");

        try {
            int v = Integer.valueOf(listPref.getValue());
            if (v == 0) {
                listPref.setSummary(getActivity().getResources().getString(R.string.low));
            } else if (v == 1) {
                listPref.setSummary(getActivity().getResources().getString(R.string.med));
            } else if (v == 2) {
                listPref.setSummary(getActivity().getResources().getString(R.string.high));
            }
        } catch (Exception e) {
            // if it comes here, then listPref.getValue() is null
            listPref.setSummary(getActivity().getResources().getString(R.string.med));
        }

        listPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String s = (String)newValue;
                int v = Integer.valueOf(s);
                if (v == 0) {
                    listPref.setSummary(getActivity().getResources().getString(R.string.low));
                    Preferences.setGamepadSensitivity(getActivity(), Utils.LOW_SENSITIVITY);
                } else if (v == 1) {
                    listPref.setSummary(getActivity().getResources().getString(R.string.med));
                    Preferences.setGamepadSensitivity(getActivity(), Utils.MEDIUM_SENSITIVITY);
                } else if (v == 2) {
                    listPref.setSummary(getActivity().getResources().getString(R.string.high));
                    Preferences.setGamepadSensitivity(getActivity(), Utils.HIGH_SENSITIVITY);
                }
                return true;
            }
        });
    }
}
