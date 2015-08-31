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

package com.sonymobile.dronecontrol.connection;

import com.sonymobile.dronecontrol.R;
import com.sonymobile.dronecontrol.utils.Logger;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class FTPConnectionDeleteAll extends AsyncTask<String, Integer, Void> {

    private final String TAG = FTPConnectionDeleteAll.class.getSimpleName();

    private static Context sContext;
    private ProgressDialog mProgressDialog = null;

    public FTPConnectionDeleteAll(Context context) {
        sContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        mProgressDialog = new ProgressDialog(sContext);
        mProgressDialog.setMessage(sContext.getString(R.string.dialog_delete_files_message));
        mProgressDialog.setCancelable(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.show();

    }

    @Override
    protected Void doInBackground(String... params) {
        boolean status = false;
        FTPClient mFtpClient = null;
        String IP_ADDRESS = params[0];
        String REMOTE_FOLDER = params[1];
        int FTP_PORT = Integer.parseInt(params[2]);

        try {
            mFtpClient = new FTPClient();
            mFtpClient.setConnectTimeout(10 * 1000);
            mFtpClient.connect(InetAddress.getByName(IP_ADDRESS));
            status = mFtpClient.login("", "");

            Logger.d(TAG, "Connected to " + IP_ADDRESS + ": " + String.valueOf(status));
            Logger.d(TAG, "Connected to folder: " + REMOTE_FOLDER);

            if (FTPReply.isPositiveCompletion(mFtpClient.getReplyCode())) {
                mFtpClient.setFileType(FTP.ASCII_FILE_TYPE);
                mFtpClient.enterLocalPassiveMode();
                mFtpClient.changeWorkingDirectory(REMOTE_FOLDER);
                FTPFile[] mFileArray = mFtpClient.listFiles();

                int size = mFileArray.length;
                if (size > 0) {
                    String filename;
                    if (mProgressDialog != null) {
                        mProgressDialog.setMax(size);
                    }

                    for (int i = 0; i < mFileArray.length; i++) {
                        filename = mFileArray[i].getName();
                        Logger.d(TAG, "Deleting file: " + filename);
                        mFtpClient.deleteFile(filename);
                        publishProgress(i);
                    }
                }
                mFtpClient.logout();
                mFtpClient.disconnect();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.setProgress((int)values[0]);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

}

