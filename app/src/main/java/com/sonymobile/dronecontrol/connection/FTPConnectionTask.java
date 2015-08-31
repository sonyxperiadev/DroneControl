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

import com.parrot.arsdk.ardatatransfer.ARDATATRANSFER_DOWNLOADER_RESUME_ENUM;
import com.parrot.arsdk.ardatatransfer.ARDATATRANSFER_ERROR_ENUM;
import com.parrot.arsdk.ardatatransfer.ARDataTransferDownloader;
import com.parrot.arsdk.ardatatransfer.ARDataTransferDownloaderCompletionListener;
import com.parrot.arsdk.ardatatransfer.ARDataTransferDownloaderProgressListener;
import com.parrot.arsdk.ardatatransfer.ARDataTransferException;
import com.parrot.arsdk.ardatatransfer.ARDataTransferManager;
import com.parrot.arsdk.arutils.ARUtilsException;
import com.parrot.arsdk.arutils.ARUtilsFtpConnection;
import com.parrot.arsdk.arutils.ARUtilsManager;
import com.sonymobile.dronecontrol.utils.Logger;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class FTPConnectionTask extends AsyncTask<String, Integer, Void> implements ARDataTransferDownloaderCompletionListener,
        ARDataTransferDownloaderProgressListener {

    private final String TAG = FTPConnectionTask.class.getSimpleName();

    private static Context sContext;
    private Boolean mTransferComplete = false;
    private FTPConnectionListener mFtpListener;
    private String mImagePath;

    public FTPConnectionTask(Context context, FTPConnectionListener listener) {
        sContext = context;
        mFtpListener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

    }

    @Override
    protected Void doInBackground(String... params) {
        boolean status = false;
        FTPClient mFtpClient = null;
        String IP_ADDRESS = params[0];
        String REMOTE_FOLDER = params[1];
        int FTP_PORT = Integer.parseInt(params[2]);
        String LOCAL_FOLDER = params[3];
        String fileName = null;

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

                String fileExtension;
                String filename;
                for (int i = 0; i < mFileArray.length; i++) {
                    filename = mFileArray[i].getName();

                    if (filename.lastIndexOf(".") != -1 && filename.lastIndexOf(".") != 0) {
                        fileExtension = filename.substring(filename.lastIndexOf(".") + 1);
                        if (fileExtension.equals("jpg")) {
                            fileName = mFileArray[i].getName();
                        }
                    }
                }

                String remoteFile = REMOTE_FOLDER + fileName;
                String localFile = LOCAL_FOLDER + fileName;
                mImagePath = localFile;

                // get file using ARDrone SDK
                ARDataTransferManager dataTransferManager = new ARDataTransferManager();
                ARUtilsManager utilsManager = new ARUtilsManager();
                utilsManager.initWifiFtp(IP_ADDRESS, FTP_PORT, ARUtilsFtpConnection.FTP_ANONYMOUS, "");
                ARDataTransferDownloader downloadManager = dataTransferManager.getARDataTransferDownloader();

                Logger.d(TAG, "Getting file: " + remoteFile);
                Logger.d(TAG, "Saving at: " + localFile);

                File file = new File(LOCAL_FOLDER);
                if (!file.exists()) {
                    file.mkdirs();
                }

                downloadManager.createDownloader(utilsManager, remoteFile, localFile, this, this, this, this,
                        ARDATATRANSFER_DOWNLOADER_RESUME_ENUM.ARDATATRANSFER_DOWNLOADER_RESUME_FALSE);
                Runnable downloader = downloadManager.getDownloaderRunnable();
                Thread downloaderThread = new Thread(downloader);
                downloaderThread.start();
                downloaderThread.join();
                downloadManager.dispose();
                dataTransferManager.dispose();
                utilsManager.closeWifiFtp();
                utilsManager.dispose();


                // Running MediaScanner to scan for new media and show it in the gallery
                scanFile(localFile, "image/jpeg", null);

                mFtpClient.deleteFile(fileName);
                mFtpClient.logout();
                mFtpClient.disconnect();
                Logger.d(TAG, "Selected file: " + fileName);
                Logger.d(TAG, "Disconnected from " + IP_ADDRESS);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ARDataTransferException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ARUtilsException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void scanFile(String filePath, final String type, final MediaScannerConnection.OnScanCompletedListener listener) {

        String[] paths = new String[1];
        paths[0] = filePath;

        String[] mimeTypes = new String[1];
        mimeTypes[0] = type;

        MediaScannerConnection.scanFile(sContext, paths, mimeTypes, listener);
    }

    @Override
    public void didDownloadComplete(Object o, ARDATATRANSFER_ERROR_ENUM ardatatransfer_error_enum) {

        if (ardatatransfer_error_enum == ARDATATRANSFER_ERROR_ENUM.ARDATATRANSFER_OK) {
            mTransferComplete = true;
        } else {
            mTransferComplete = false;
        }

    }

    @Override
    public void didDownloadProgress(Object o, float v) {
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        if (mFtpListener != null) {
            mFtpListener.onFTPConnectionFinish(mTransferComplete, mImagePath);
        }
    }

}
