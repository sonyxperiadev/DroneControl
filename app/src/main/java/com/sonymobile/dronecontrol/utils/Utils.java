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

import android.view.View;

import java.text.DecimalFormat;
import java.util.List;

public class Utils {

    //Change this to false when releasing an apk
    public static boolean DEBUG = false;

    public static final float LOW_SENSITIVITY = 0.4f;
    public static final float MEDIUM_SENSITIVITY = 0.7f;
    public static final float HIGH_SENSITIVITY = 1.0f;

    public static DecimalFormat decimal_two_places = new DecimalFormat("#.00");

    public static int getKmModularVector(double speedX, double speedY, double speedZ) {
        // transforming to KM
        speedX *= 3.6;
        speedY *= 3.6;
        speedZ *= 3.6;
        // pitagoras formula to determine modular speed vector
        return (int)Math.sqrt(Math.pow(speedX, 2) + Math.pow(speedY, 2) + Math.pow(speedZ, 2));
    }

    public static void setInvisible(View... views) {
        for (View v : views) {
            v.setVisibility(View.INVISIBLE);
        }
    }

    public static void setInvisible(List<View> views) {
        for (View v : views) {
            v.setVisibility(View.INVISIBLE);
        }
    }

    public static void setVisible(View... views) {
        for (View v : views) {
            v.setVisibility(View.VISIBLE);
        }
    }

    public static void setVisible(List<View> views) {
        for (View v : views) {
            v.setVisibility(View.VISIBLE);
        }
    }
}
