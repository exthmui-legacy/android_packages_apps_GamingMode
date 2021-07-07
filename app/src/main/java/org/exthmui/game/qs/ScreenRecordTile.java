/*
 * Copyright (C) 2020 The exTHmUI Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.exthmui.game.qs;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.View;


import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.systemui.screenrecord.IRemoteRecording;
import com.android.systemui.screenrecord.IRecordingCallback;
import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;
import org.exthmui.game.ui.ScreenRecordConfDialogActivity;

public class ScreenRecordTile extends TileBase implements View.OnLongClickListener {

    private static final String TAG = "ScreenRecordTile";
    private static final Intent hideMenuIntent = new Intent(Constants.Broadcasts.BROADCAST_GAMING_MENU_CONTROL).putExtra("cmd", "hide");

    private boolean mIsRecording;
    private IRemoteRecording mBinder;
    private Intent mRemoteRecordingServiceIntent;
    private RecordingCallback mCallback = new RecordingCallback();
    private Context mContext;
    SharedPreferences mPreferences;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                qsIcon.setSelected(mIsRecording);
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName,IBinder iBinder) {
            mBinder = IRemoteRecording.Stub.asInterface(iBinder);
            try {
                mBinder.addRecordingCallback(mCallback);
                setIsRecording(mBinder.isRecording());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } 
        
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBinder = null;
        }
    }; 

    public ScreenRecordTile(Context context) {
        super(context, context.getString(R.string.qs_screen_record), "", R.drawable.ic_qs_screenrecord);
        this.setOnLongClickListener(this);
        mContext = context.getApplicationContext();
        mRemoteRecordingServiceIntent = new Intent();
        mRemoteRecordingServiceIntent.setAction("com.android.systemui.screenrecord.RecordingService");
        mRemoteRecordingServiceIntent.setPackage("com.android.systemui");
        mContext.bindService(mRemoteRecordingServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void setConfig(Bundle bundle) {

    }

    @Override
    public void onClick(View v) {
        LocalBroadcastManager.getInstance(mContext).sendBroadcastSync(hideMenuIntent);
        if (mBinder == null) {
            return;
        }
        try {
            if (mIsRecording) {
                mBinder.stopRecording();
            } else {
                mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                boolean showTap = mPreferences.getBoolean(Constants.LocalConfigKeys.SCREEN_RECORDING_SHOW_TAP, Constants.LocalConfigDefaultValues.SCREEN_RECORDING_SHOW_TAP);
                int audioSource = mPreferences.getInt(Constants.LocalConfigKeys.SCREEN_RECORDING_AUDIO_SOURCE, Constants.LocalConfigDefaultValues.SCREEN_RECORDING_AUDIO_SOURCE);
                mBinder.startRecording(audioSource, showTap);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (!mIsRecording) {
            LocalBroadcastManager.getInstance(mContext).sendBroadcastSync(hideMenuIntent);
            Intent intent = new Intent(mContext, ScreenRecordConfDialogActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
        return true;
    }

    void setIsRecording(boolean val) {
        mIsRecording = val;
        mHandler.sendEmptyMessage(1);
    }

    @Override
    public void onDestroy() {
        if (mBinder != null) {
            try {
                mBinder.removeRecordingCallback(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mContext.unbindService(mServiceConnection);
    }

    private class RecordingCallback extends IRecordingCallback.Stub {
        @Override
        public void onRecordingStart() {
            setIsRecording(true);
        }

        @Override
        public void onRecordingEnd() {
            setIsRecording(false);
        }
    }
}
