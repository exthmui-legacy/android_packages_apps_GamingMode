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

package org.exthmui.game.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telecom.CallAudioState;
import android.telecom.InCallService;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.internal.statusbar.IStatusBarService;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lineageos.hardware.LineageHardwareManager;

public class GamingService extends Service {

    private static final String TAG = "GamingService";

    private static final int NOTIFICATION_ID = 1;

    private Intent mOverlayServiceIntent;
    private Notification mGamingNotification;

    private Intent mCallStatusIntent;

    private String mCurrentPackage;

    private Bundle mCurrentConfig = new Bundle();

    private AudioManager mAudioManager;
    private IStatusBarService mStatusBarService;
    private LineageHardwareManager mLineageHardware;
    private TelephonyManager mTelephonyManager;
    private TelecomManager mTelecomManager;

    private GamingPhoneStateListener mPhoneStateListener;
    
    private BroadcastReceiver mGamingModeOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.Broadcasts.SYS_BROADCAST_GAMING_MODE_OFF.equals(intent.getAction())) {
                stopSelf();
            }
        }
    };

    private BroadcastReceiver mCallControlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mTelecomManager == null) return;
            if (intent.getIntExtra("cmd", 1) == 1) {
                mTelecomManager.endCall();
            } else {
                mTelecomManager.acceptRingingCall();
            }
        }
    };

    private BroadcastReceiver mGamingActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String target = intent.getStringExtra("target");
            Intent configChangedIntent = new Intent(Constants.Broadcasts.BROADCAST_CONFIG_CHANGED);
            if (Constants.GamingActionTargets.DISABLE_AUTO_BRIGHTNESS.equals(target)) {
                setDisableAutoBrightness(intent.getBooleanExtra("value", Constants.ConfigDefaultValues.DISABLE_AUTO_BRIGHTNESS), false);
            } else if (Constants.GamingActionTargets.DISABLE_GESTURE.equals(target)) {
                setDisableGesture(intent.getBooleanExtra("value", Constants.ConfigDefaultValues.DISABLE_GESTURE));
            } else if (Constants.GamingActionTargets.DISABLE_HW_KEYS.equals(target)) {
                setDisableHwKeys(intent.getBooleanExtra("value", Constants.ConfigDefaultValues.DISABLE_HW_KEYS), false);
            } else if (Constants.GamingActionTargets.DISABLE_RINGTONE.equals(target)) {
                setDisableRingtone(intent.getBooleanExtra("value", Constants.ConfigDefaultValues.DISABLE_RINGTONE));
            } else if (Constants.GamingActionTargets.SHOW_DANMAKU.equals(target)) {
                setShowDanmaku(intent.getBooleanExtra("value", Constants.ConfigDefaultValues.SHOW_DANMAKU));
            } else if (Constants.GamingActionTargets.PERFORMANCE_LEVEL.equals(target)) {
                setPerformanceLevel(intent.getIntExtra("value", Constants.ConfigDefaultValues.PERFORMANCE_LEVEL));
            } else {
                return;
            }
            configChangedIntent.putExtras(mCurrentConfig);
            LocalBroadcastManager.getInstance(GamingService.this).sendBroadcast(configChangedIntent);
        }
    };

    public GamingService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(this, Constants.CHANNEL_GAMING_MODE_STATUS, getString(R.string.channel_gaming_mode_status), NotificationManager.IMPORTANCE_LOW);

        checkNotificationListener();
        checkFreeFormSettings();

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mTelecomManager = (TelecomManager) getSystemService(TELECOM_SERVICE);
        mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService(Context.STATUS_BAR_SERVICE));
        try {
            mLineageHardware = LineageHardwareManager.getInstance(this);
        } catch (Error e) {
            Log.e(TAG, "get LineageHardwareManager failed!", e);
        }

        registerReceiver(mGamingModeOffReceiver, new IntentFilter(Constants.Broadcasts.SYS_BROADCAST_GAMING_MODE_OFF));
        LocalBroadcastManager.getInstance(this).registerReceiver(mCallControlReceiver, new IntentFilter(Constants.Broadcasts.BROADCAST_CALL_CONTROL));
        LocalBroadcastManager.getInstance(this).registerReceiver(mGamingActionReceiver, new IntentFilter(Constants.Broadcasts.BROADCAST_GAMING_ACTION));

        mPhoneStateListener = new GamingPhoneStateListener();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        mCallStatusIntent = new Intent(Constants.Broadcasts.BROADCAST_CALL_STATUS);

        mOverlayServiceIntent = new Intent(this, OverlayService.class);

        PendingIntent stopGamingIntent = PendingIntent.getBroadcast(this, 0, new Intent(Constants.Broadcasts.SYS_BROADCAST_GAMING_MODE_OFF), 0);
        Notification.Builder builder = new Notification.Builder(this, Constants.CHANNEL_GAMING_MODE_STATUS);
        Notification.Action.Builder actionBuilder = new Notification.Action.Builder(null, getString(R.string.action_stop_gaming_mode), stopGamingIntent);
        builder.addAction(actionBuilder.build());
        builder.setContentText(getString(R.string.gaming_mode_running));
        builder.setSmallIcon(R.drawable.ic_notification_game);

        mGamingNotification = builder.build();
        startForeground(NOTIFICATION_ID, mGamingNotification);

        Toast.makeText(this, R.string.gaming_mode_on, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && !TextUtils.equals(intent.getStringExtra("package"), mCurrentPackage)) {
            mCurrentPackage = intent.getStringExtra("package");
            updateConfig();
        }

        mOverlayServiceIntent.putExtras(mCurrentConfig);
        startServiceAsUser(mOverlayServiceIntent, UserHandle.CURRENT);
        Settings.System.putInt(getContentResolver(), Settings.System.GAMING_MODE_ACTIVE, 1);
        if (mTelephonyManager != null) {
            mCallStatusIntent.putExtra("state", mTelephonyManager.getCallState());
            LocalBroadcastManager.getInstance(this).sendBroadcast(mCallStatusIntent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void checkNotificationListener() {
        String notificationListeners = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_NOTIFICATION_LISTENERS);
        List<String> listenersList;
        listenersList = new ArrayList<>();
        if (!TextUtils.isEmpty(notificationListeners)) {
            listenersList.addAll(Arrays.asList(notificationListeners.split(":")));
        }
        ComponentName danmakuComponent = new ComponentName(this, DanmakuService.class);
        if (!listenersList.contains(danmakuComponent.flattenToString())) {
            listenersList.add(danmakuComponent.flattenToString());
            Settings.Secure.putString(getContentResolver(), Settings.Secure.ENABLED_NOTIFICATION_LISTENERS, String.join(":", listenersList));
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!notificationManager.isNotificationListenerAccessGranted(danmakuComponent)) {
            notificationManager.setNotificationListenerAccessGranted(danmakuComponent, true);
        }
    }

    private void checkFreeFormSettings() {
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVELOPMENT_ENABLE_FREEFORM_WINDOWS_SUPPORT, 1);
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVELOPMENT_FORCE_RESIZABLE_ACTIVITIES, 1);
    }

    private void updateConfig() {
        // danmaku
        mCurrentConfig.putBoolean(Constants.ConfigKeys.SHOW_DANMAKU, getBooleanSetting(Constants.ConfigKeys.SHOW_DANMAKU, Constants.ConfigDefaultValues.SHOW_DANMAKU));
        mCurrentConfig.putInt(Constants.ConfigKeys.DANMAKU_SPEED_HORIZONTAL, getIntSetting(Constants.ConfigKeys.DANMAKU_SPEED_HORIZONTAL, Constants.ConfigDefaultValues.DANMAKU_SPEED_HORIZONTAL));
        mCurrentConfig.putInt(Constants.ConfigKeys.DANMAKU_SPEED_VERTICAL, getIntSetting(Constants.ConfigKeys.DANMAKU_SPEED_VERTICAL, Constants.ConfigDefaultValues.DANMAKU_SPEED_VERTICAL));
        mCurrentConfig.putInt(Constants.ConfigKeys.DANMAKU_SIZE_HORIZONTAL, getIntSetting(Constants.ConfigKeys.DANMAKU_SIZE_HORIZONTAL, Constants.ConfigDefaultValues.DANMAKU_SIZE_HORIZONTAL));
        mCurrentConfig.putInt(Constants.ConfigKeys.DANMAKU_SIZE_VERTICAL, getIntSetting(Constants.ConfigKeys.DANMAKU_SIZE_VERTICAL, Constants.ConfigDefaultValues.DANMAKU_SIZE_VERTICAL));
        mCurrentConfig.putBoolean(Constants.ConfigKeys.DYNAMIC_NOTIFICATION_FILTER, getBooleanSetting(Constants.ConfigKeys.DYNAMIC_NOTIFICATION_FILTER, Constants.ConfigDefaultValues.DYNAMIC_NOTIFICATION_FILTER));
        mCurrentConfig.putStringArray(Constants.ConfigKeys.NOTIFICATION_APP_BLACKLIST, getStringArraySetting(Constants.ConfigKeys.NOTIFICATION_APP_BLACKLIST));

        // performance
        boolean changePerformance = getBooleanSetting(Constants.ConfigKeys.CHANGE_PERFORMANCE_LEVEL, Constants.ConfigDefaultValues.CHANGE_PERFORMANCE_LEVEL);
        int performanceLevel = getIntSetting(Constants.ConfigKeys.PERFORMANCE_LEVEL, Constants.ConfigDefaultValues.PERFORMANCE_LEVEL);
        if (changePerformance) {
            setPerformanceLevel(performanceLevel);
        } else {
            mCurrentConfig.putInt(Constants.ConfigKeys.PERFORMANCE_LEVEL, performanceLevel);
        }

        // hw keys & gesture
        boolean disableHwKeys = getBooleanSetting(Constants.ConfigKeys.DISABLE_HW_KEYS, Constants.ConfigDefaultValues.DISABLE_HW_KEYS);
        boolean disableGesture = getBooleanSetting(Constants.ConfigKeys.DISABLE_GESTURE, Constants.ConfigDefaultValues.DISABLE_GESTURE);
        setDisableHwKeys(disableHwKeys, false);
        setDisableGesture(disableGesture);

        // quick-start apps
        mCurrentConfig.putStringArray(Constants.ConfigKeys.QUICK_START_APPS, getStringArraySetting(Constants.ConfigKeys.QUICK_START_APPS));
        setAutoRotation(false);

        // misc
        boolean disableRingtone = getBooleanSetting(Constants.ConfigKeys.DISABLE_RINGTONE, Constants.ConfigDefaultValues.DISABLE_RINGTONE);
        setDisableRingtone(disableRingtone);
        boolean disableAutoBrightness = getBooleanSetting(Constants.ConfigKeys.DISABLE_AUTO_BRIGHTNESS, Constants.ConfigDefaultValues.DISABLE_AUTO_BRIGHTNESS);
        setDisableAutoBrightness(disableAutoBrightness, false);

        // menu opacity
        mCurrentConfig.putInt(Constants.ConfigKeys.MENU_OPACITY, getIntSetting(Constants.ConfigKeys.MENU_OPACITY, Constants.ConfigDefaultValues.MENU_OPACITY));

        Intent intent = new Intent(Constants.Broadcasts.BROADCAST_CONFIG_CHANGED);
        intent.putExtras(mCurrentConfig);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void setDisableHwKeys(boolean disable, boolean restore) {
        if (mLineageHardware == null) return;
        if (!mCurrentConfig.containsKey("old_disable_hw_keys")) {
            boolean oldValue = mLineageHardware.get(LineageHardwareManager.FEATURE_KEY_DISABLE);
            mCurrentConfig.putBoolean("old_disable_hw_keys", oldValue);
        }
        if (!restore) {
            mCurrentConfig.putBoolean(Constants.ConfigKeys.DISABLE_HW_KEYS, disable);
            mLineageHardware.set(LineageHardwareManager.FEATURE_KEY_DISABLE, disable);
        } else {
            boolean oldValue = mCurrentConfig.getBoolean("old_disable_hw_keys");
            mCurrentConfig.putBoolean(Constants.ConfigKeys.DISABLE_HW_KEYS, oldValue);
            mLineageHardware.set(LineageHardwareManager.FEATURE_KEY_DISABLE, oldValue);
        }
    }

    private void setDisableGesture(boolean disable) {
        mCurrentConfig.putBoolean(Constants.ConfigKeys.DISABLE_GESTURE, disable);
        try {
            mStatusBarService.setBlockedGesturalNavigation(disable);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to disable/enable gesture!", e);
        }
    }

    private void setShowDanmaku(boolean show) {
        mCurrentConfig.putBoolean(Constants.ConfigKeys.SHOW_DANMAKU, show);
    }

    private void setPerformanceLevel(int level) {
        SystemProperties.set(Constants.PROP_GAMING_PERFORMANCE, String.valueOf(level));
        mCurrentConfig.putInt(Constants.ConfigKeys.PERFORMANCE_LEVEL, level);
    }

    private void setDisableRingtone(boolean disable) {
        if (!mCurrentConfig.containsKey("old_ringer_mode")) {
            mCurrentConfig.putInt("old_ringer_mode", mAudioManager.getRingerModeInternal());
        }
        int oldRingerMode = mCurrentConfig.getInt("old_ringer_mode", AudioManager.RINGER_MODE_NORMAL);
        mAudioManager.setRingerModeInternal(disable ? AudioManager.RINGER_MODE_SILENT : oldRingerMode);
        mCurrentConfig.putBoolean(Constants.ConfigKeys.DISABLE_RINGTONE, disable);
    }

    private void setDisableAutoBrightness(boolean disable, boolean restore) {
        if (!mCurrentConfig.containsKey("old_auto_brightness")) {
            int oldValue = getIntSetting(Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            mCurrentConfig.putInt("old_auto_brightness", oldValue);
        }
        if (!restore) {
            if (disable) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            } else {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            }
            mCurrentConfig.putBoolean(Constants.ConfigKeys.DISABLE_AUTO_BRIGHTNESS, disable);
        } else {
            int oldValue = mCurrentConfig.getInt("old_auto_brightness");
            mCurrentConfig.putBoolean(Constants.ConfigKeys.DISABLE_AUTO_BRIGHTNESS, oldValue == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, oldValue);
        }
    }

    private void setAutoRotation(boolean restore) {
        if (!mCurrentConfig.containsKey("old_auto_rotation")) {
            mCurrentConfig.putInt("old_auto_rotation", getIntSetting(Settings.System.ACCELEROMETER_ROTATION, 0));
        }
        if (!restore) {
            Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
        } else {
            int oldValue = mCurrentConfig.getInt("old_auto_rotation");
            Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, oldValue);
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mGamingModeOffReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCallControlReceiver);
        stopServiceAsUser(mOverlayServiceIntent, UserHandle.CURRENT);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        setDisableGesture(false);
        setDisableHwKeys(false, true);
        setDisableAutoBrightness(false, true);
        setDisableRingtone(false);
        setPerformanceLevel(-1);
        setAutoRotation(true);
        Settings.System.putInt(getContentResolver(), Settings.System.GAMING_MODE_ACTIVE, 0);
        Toast.makeText(this, R.string.gaming_mode_off, Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static void createNotificationChannel(Context context, String channelId, String channelName, int importance) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }

    private boolean getBooleanSetting(String key, boolean def) {
        return Settings.System.getInt(getContentResolver(), key, def ? 1 : 0) != 0;
    }

    private int getIntSetting(String key, int def) {
        return Settings.System.getInt(getContentResolver(), key, def);
    }

    private String[] getStringArraySetting(String key) {
        String val = Settings.System.getString(getContentResolver(), key);
        if (!TextUtils.isEmpty(val)) {
            return val.split(";");
        } else {
            return null;
        }
    }

    private class GamingPhoneStateListener extends PhoneStateListener {
        private int mPrevState = -1;
        private int mPrevMode = AudioManager.MODE_NORMAL;
        private AudioManager mAudioManager = getSystemService(AudioManager.class);

        private boolean isHeadsetPluggedIn() {
            AudioDeviceInfo[] audioDeviceInfoArr = mAudioManager.getDevices(AudioManager.GET_DEVICES_ALL);
            for (AudioDeviceInfo info : audioDeviceInfoArr) {
                if (info.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || info.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    info.getType() == AudioDeviceInfo.TYPE_USB_HEADSET) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            if (Settings.System.getInt(getContentResolver(), Settings.System.GAMING_MODE_AUTO_ANSWER_CALL, 0) != 0) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        mTelecomManager.acceptRingingCall();
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if (mPrevState == TelephonyManager.CALL_STATE_RINGING) {
                            mPrevMode = mAudioManager.getMode();
                            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                            if (isHeadsetPluggedIn()) {
                                mAudioManager.setSpeakerphoneOn(false);
                                AudioSystem.setForceUse(AudioSystem.FOR_COMMUNICATION, AudioSystem.FORCE_NONE);
                            } else {
                                mAudioManager.setSpeakerphoneOn(true);
                                AudioSystem.setForceUse(AudioSystem.FOR_COMMUNICATION, AudioSystem.FORCE_SPEAKER);
                            }
                            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (mPrevState == TelephonyManager.CALL_STATE_OFFHOOK) {
                            mAudioManager.setMode(mPrevMode);
                            AudioSystem.setForceUse(AudioSystem.FOR_COMMUNICATION, AudioSystem.FORCE_NONE);
                            mAudioManager.setSpeakerphoneOn(false);
                        }
                        break;
                }
            }
            mCallStatusIntent.putExtra("state", state);
            LocalBroadcastManager.getInstance(GamingService.this).sendBroadcast(mCallStatusIntent);
            mPrevState = state;
            super.onCallStateChanged(state, phoneNumber);
        }
    }

}
