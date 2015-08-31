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

package com.sonymobile.dronecontrol.activity;

import com.parrot.arsdk.arsal.ARSALPrint;
import com.sonymobile.dronecontrol.R;
import com.sonymobile.dronecontrol.utils.Logger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class SplashActivity extends Activity {

    private static String TAG = SplashActivity.class.getSimpleName();
    final private static long SPLASH_TIMEOUT = TimeUnit.SECONDS.toMillis(2);

    static {
        try {
            System.loadLibrary("json");
            System.loadLibrary("curl");
            System.loadLibrary("arutils");
            System.loadLibrary("arutils_android");
            System.loadLibrary("arsal");
            System.loadLibrary("arsal_android");
            System.loadLibrary("arnetworkal");
            System.loadLibrary("arnetworkal_android");
            System.loadLibrary("arnetwork");
            System.loadLibrary("arnetwork_android");
            System.loadLibrary("arcommands");
            System.loadLibrary("arcommands_android");
            System.loadLibrary("ardiscovery");
            System.loadLibrary("ardiscovery_android");
            System.loadLibrary("arstream");
            System.loadLibrary("arstream_android");
            System.loadLibrary("ardatatransfer");
            System.loadLibrary("ardatatransfer_android");
            System.loadLibrary("armedia");
            System.loadLibrary("armedia_android");

            ARSALPrint.enableDebugPrints();

        } catch (Exception e) {
            Logger.e(TAG, "Exception: ", e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        TextView title = (TextView) this.findViewById(R.id.splash_text);
        title.setText(Html.fromHtml(getResources().getString(R.string.application_title_splash)));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                SplashActivity.this.startActivity(mainIntent);
                SplashActivity.this.finish();
            }
        }, SPLASH_TIMEOUT);
    }
}
