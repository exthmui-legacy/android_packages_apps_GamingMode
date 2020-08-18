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

package org.exthmui.game.ui;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.WindowConfiguration;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;
import org.exthmui.game.qs.AppTile;

import java.util.List;

public class QuickStartAppView extends LinearLayout implements View.OnClickListener {

    private static final String TAG = "QuickStartAppView";

    private Context mContext;
    private PackageManager mPackageManager;
    private ActivityManager mActivityManager;
    private ActivityOptions mActivityOptions;
    private Intent mHideMenuIntent = new Intent(Constants.Broadcasts.BROADCAST_GAMING_MENU_CONTROL).putExtra("cmd", "hide");

    public QuickStartAppView(Context context) {
        this(context, null);
    }

    public QuickStartAppView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickStartAppView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public QuickStartAppView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mContext = context;
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mPackageManager = mContext.getPackageManager();

        this.setDividerDrawable(context.getDrawable(R.drawable.qs_divider));
        this.setShowDividers(SHOW_DIVIDER_MIDDLE);
        this.setPadding(0,0, 0,8);
        this.setVisibility(GONE);

        mActivityOptions = ActivityOptions.makeBasic();
        mActivityOptions.setLaunchWindowingMode(WindowConfiguration.WINDOWING_MODE_FREEFORM);

    }

    public void setConfig(Bundle config) {
        this.removeAllViewsInLayout();
        this.setVisibility(GONE);
        String[] apps = config.getStringArray(Constants.ConfigKeys.QUICK_START_APPS);
        
        if (apps == null || apps.length == 0) return;
        this.setVisibility(VISIBLE);
        for (String app : apps) {
            AppTile appTile = new AppTile(mContext, app);
            appTile.setOnClickListener(this);
            this.addView(appTile);
        }
    }

    private void startActivity(@NonNull String packageName) {
        DisplayMetrics m = mContext.getResources().getDisplayMetrics();
        int width, height;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 竖屏
            width = m.widthPixels - 200;
            height = width * 4 / 3;
        } else {
            // 横屏
            height = m.heightPixels - 200;
            width = height * 4 / 3;
        }
        int left = 100;
        int right = left + width;
        int top = 100;
        int bottom = top + height;
        mActivityOptions.setLaunchBounds(new Rect(left, top, right, bottom));

        try {
            List<ActivityManager.RecentTaskInfo> recentTaskInfoList = mActivityManager.getRecentTasks(100, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
            for (ActivityManager.RecentTaskInfo info : recentTaskInfoList) {
                if (info.isRunning && info.topActivity != null && packageName.equals(info.topActivity.getPackageName())) {
                    mActivityManager.getService().startActivityFromRecents(info.taskId, mActivityOptions.toBundle());
                    return;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }

        Intent startAppIntent = mPackageManager.getLaunchIntentForPackage(packageName);
        startAppIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startAppIntent.setPackage(null);
        mContext.startActivityAsUser(startAppIntent, mActivityOptions.toBundle(), UserHandle.CURRENT);

    }

    @Override
    public void onClick(View v) {
        if (v instanceof AppTile) {
            AppTile appTile = (AppTile) v;
            LocalBroadcastManager.getInstance(mContext).sendBroadcastSync(mHideMenuIntent);
            startActivity(appTile.getKey());
        }
    }
}
