/*
 * Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.watchme;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Ibrahim Ulukaya <ulukaya@google.com>
 *         <p/>
 *         AudioFrameGrabber class which records audio.
 */
public class AudioFrameGrabber {
    private Thread thread;
    private boolean cancel = false;
    private int frequency;
    private FrameCallback frameCallback;
    File recordingFile;
    public void setFrameCallback(FrameCallback callback) {
        frameCallback = callback;
    }

    /**
     * Starts recording.
     *
     * @param frequency - Recording frequency.
     */
    public void start(int frequency) {
        Log.d(MainActivity.APP_NAME, "start");

        this.frequency = frequency;
        File root = android.os.Environment.getExternalStorageDirectory();
        File path = new File(root.getAbsolutePath() + "/Android/");
        if (!path.exists()) {
            path.mkdirs();
        }
        try {
            recordingFile = File.createTempFile("recording", ".pcm", path);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create file on SD card", e);
        }
        cancel = false;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                recordThread();
            }
        });
        thread.start();
    }

    /**
     * Records audio and pushes to buffer.
     */
    public void recordThread() {
        Log.d(MainActivity.APP_NAME, "recordThread");

        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        int channelConfiguration = AudioFormat.CHANNEL_IN_STEREO;
        int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
        Log.i(MainActivity.APP_NAME, "AudioRecord buffer size: " + bufferSize);
        try {
            DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(
                            recordingFile)));
            // 16 bit PCM stereo recording was chosen as example.
            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration,
                    audioEncoding, bufferSize);
            recorder.startRecording();

            // Make bufferSize be in samples instead of bytes.
            bufferSize /= 2;
            short[] buffer = new short[bufferSize];
            while (!cancel) {
                int bufferReadResult = recorder.read(buffer, 0, bufferSize);
                // Utils.Debug("bufferReadResult: " + bufferReadResult);

                for (int i = 0; i < bufferReadResult; i++) {
                    dos.writeShort(buffer[i]);
                }
                if (bufferReadResult > 0) {
                    frameCallback.handleFrame(buffer, bufferReadResult);
                } else if (bufferReadResult < 0) {
                    Log.w(MainActivity.APP_NAME, "Error calling recorder.read: " + bufferReadResult);
                }
            }
            recorder.stop();

            dos.close();
        } catch (Throwable t) {
            Log.e("AudioRecord", "Recording Failed");
        }

        Log.d(MainActivity.APP_NAME, "exit recordThread");
    }

    /**
     * Stops recording.
     */
    public void stop() {
        Log.d(MainActivity.APP_NAME, "stop");

        cancel = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            Log.e(MainActivity.APP_NAME, "", e);
        }
    }

    public interface FrameCallback {
        void handleFrame(short[] audio_data, int length);
    }
}
