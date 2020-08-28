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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.exthmui.game.misc.Constants;

import java.util.HashMap;
import java.util.Map;

public class DanmakuService extends NotificationListenerService {

    private static final String TAG = "DanmakuService";

    private Map<String, String> mLastNotificationMap = new HashMap<>();

    private int filterThreshold = 3;
    private boolean showDanmaku = Constants.ConfigDefaultValues.SHOW_DANMAKU;
    private boolean useFilter = Constants.ConfigDefaultValues.DYNAMIC_NOTIFICATION_FILTER;
    private String[] mNotificationBlacklist = new String[]{};
    private BroadcastReceiver configReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.Broadcasts.BROADCAST_CONFIG_CHANGED.equals(intent.getAction())) {
                updateConfig(intent);
            }
        }
    };

    public DanmakuService() {
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        LocalBroadcastManager.getInstance(this).registerReceiver(configReceiver, new IntentFilter(Constants.Broadcasts.BROADCAST_CONFIG_CHANGED));
    }

    @Override
    public void onListenerDisconnected() {
        try {
            unregisterReceiver(configReceiver);
        } catch (Exception e) {
            // do nothing
        }
        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!showDanmaku) return;
        Bundle extras = sbn.getNotification().extras;
        if (!isInBlackList(sbn.getPackageName())){
            String lastNotification = mLastNotificationMap.getOrDefault(sbn.getPackageName(), "");
            String title = extras.getString(Notification.EXTRA_TITLE);
            if (TextUtils.isEmpty(title)) title = extras.getString(Notification.EXTRA_TITLE_BIG);
            String text = extras.getString(Notification.EXTRA_TEXT);
            StringBuilder builder = new StringBuilder();
            if (!TextUtils.isEmpty(title)) {
                builder.append("[");
                builder.append(title);
                builder.append("] ");
            }
            if (!TextUtils.isEmpty(text)) {
                builder.append(text);
            }
            String danmakuText = builder.toString();
            if (!TextUtils.isEmpty(danmakuText) && (!useFilter || compareDanmaku(danmakuText, lastNotification))) {
                sendDanmaku(danmakuText);
            }
            mLastNotificationMap.put(sbn.getPackageName(), danmakuText);
        }
    }

    private boolean isInBlackList(String packageName) {
        if (mNotificationBlacklist == null) return false;
        for (String str : mNotificationBlacklist) {
            if (TextUtils.equals(str, packageName)) {
                return true;
            }
        }
        return false;
    }

    private void sendDanmaku(String text) {
        Intent intent = new Intent(Constants.Broadcasts.BROADCAST_NEW_DANMAKU);
        intent.putExtra("danmaku_text", text);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private boolean compareDanmaku(String a, String b) {
        String tA = a.replaceAll("[\\d]+\\.[\\d]+|[\\d]", "");
        String tB = b.replaceAll("[\\d]+\\.[\\d]+|[\\d]", "");
        if (TextUtils.isEmpty(tA) || TextUtils.isEmpty(tB)) {
            return levenshtein(a, b) > filterThreshold;
        } else {
            return levenshtein(tA, tB) > filterThreshold;
        }
    }

    private void updateConfig(Intent intent) {
        if (intent != null) {
            showDanmaku = intent.getBooleanExtra(Constants.ConfigKeys.SHOW_DANMAKU, showDanmaku);
            useFilter = intent.getBooleanExtra(Constants.ConfigKeys.DYNAMIC_NOTIFICATION_FILTER, useFilter);
            mNotificationBlacklist = intent.getStringArrayExtra(Constants.ConfigKeys.NOTIFICATION_APP_BLACKLIST);
        }
    }

    // 最小编辑距离
    public static int levenshtein(CharSequence a, CharSequence b) {
        if (TextUtils.isEmpty(a)) {
            return TextUtils.isEmpty(b) ? 0 : b.length();
        } else if (TextUtils.isEmpty(b)) {
            return TextUtils.isEmpty(a) ? 0 : a.length();
        }
        final int lenA = a.length(), lenB = b.length();
        int[][] dp = new int[lenA+1][lenB+1];
        int flag = 0;
        for (int i = 0; i <= lenA; i++) {
            for (int j = 0; j <= lenB; j++) dp[i][j] = lenA + lenB;
        }
        for(int i=1; i <= lenA; i++) dp[i][0] = i;
        for(int j=1; j <= lenB; j++) dp[0][j] = j;
        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                if (a.charAt(i-1) == b.charAt(j-1)) {
                    flag = 0;
                } else {
                    flag = 1;
                }
                dp[i][j] = Math.min(dp[i-1][j-1] + flag, Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1));
            }
        }
        return dp[lenA][lenB];
    }

}
