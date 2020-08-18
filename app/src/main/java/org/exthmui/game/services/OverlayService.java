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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;
import org.exthmui.game.ui.GamingPerformanceView;
import org.exthmui.game.ui.QuickStartAppView;
import org.exthmui.game.ui.QuickSettingsView;

import top.littlefogcat.danmakulib.danmaku.Danmaku;
import top.littlefogcat.danmakulib.danmaku.DanmakuManager;
import top.littlefogcat.danmakulib.utils.ScreenUtil;

public class OverlayService extends Service {

    private View mGamingFloatingButton;
    private LinearLayout mGamingOverlayView;
    private ScrollView mGamingMenu;
    private FrameLayout mDanmakuContainer;
    private QuickSettingsView mQSView;
    private QuickStartAppView mQSAppView;

    private DanmakuManager mDanmakuManager;

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mGamingFBLayoutParams;

    private GamingPerformanceView performanceController;

    private Bundle configBundle;

    private OMReceiver mOMReceiver = new OMReceiver();
    private BroadcastReceiver mSysConfigChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction())) updateConfig();
        }
    };

    private SharedPreferences mPreferences;

    public OverlayService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        configBundle = new Bundle();

        if (Settings.canDrawOverlays(this)) {
            initView();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.Broadcasts.BROADCAST_NEW_DANMAKU);
        intentFilter.addAction(Constants.Broadcasts.BROADCAST_CONFIG_CHANGED);
        intentFilter.addAction(Constants.Broadcasts.BROADCAST_GAMING_MENU_CONTROL);
        LocalBroadcastManager.getInstance(this).registerReceiver(mOMReceiver, intentFilter);

        registerReceiver(mSysConfigChangedReceiver, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getExtras() != null) configBundle.putAll(intent.getExtras());
        updateConfig();
        return super.onStartCommand(intent, flags, startId);
    }

    private void initView() {
        mWindowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        initGamingMenu();
        initFloatingButton();
        initDanmaku();
    }

    private void updateConfig() {
        ScreenUtil.init(this);

        if (mQSView != null) {
            mQSView.setConfig(configBundle);
        }

        if (configBundle.containsKey(Constants.ConfigKeys.QUICK_START_APPS)) {
            mQSAppView.setConfig(configBundle);
        }

        // 悬浮球位置调整
        if (mGamingFloatingButton != null && mGamingFBLayoutParams != null) {
            int defaultX = ((int) getResources().getDimension(R.dimen.game_button_size) - ScreenUtil.getScreenWidth()) / 2;
            if (ScreenUtil.isPortrait()) {
                mGamingFBLayoutParams.x = mPreferences.getInt(Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_VERTICAL_X, defaultX);
                mGamingFBLayoutParams.y = mPreferences.getInt(Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_VERTICAL_Y, 10);
            } else {
                mGamingFBLayoutParams.x = mPreferences.getInt(Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_HORIZONTAL_X, defaultX);
                mGamingFBLayoutParams.y = mPreferences.getInt(Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_HORIZONTAL_Y, 10);
            }
            if (mWindowManager != null) {
                mWindowManager.updateViewLayout(mGamingFloatingButton, mGamingFBLayoutParams);
            }
        }

        // 弹幕设置
        mDanmakuManager.setMaxDanmakuSize(20); // 设置同屏最大弹幕数
        DanmakuManager.Config config = mDanmakuManager.getConfig(); // 弹幕相关设置
        config.setScrollSpeed(ScreenUtil.isPortrait() ?
                configBundle.getInt(Constants.ConfigKeys.DANMAKU_SPEED_VERTICAL, Constants.ConfigDefaultValues.DANMAKU_SPEED_VERTICAL) :
                configBundle.getInt(Constants.ConfigKeys.DANMAKU_SPEED_HORIZONTAL, Constants.ConfigDefaultValues.DANMAKU_SPEED_HORIZONTAL));
        config.setLineHeight(ScreenUtil.autoSize(ScreenUtil.isPortrait() ?
                configBundle.getInt(Constants.ConfigKeys.DANMAKU_SIZE_VERTICAL,Constants.ConfigDefaultValues.DANMAKU_SIZE_VERTICAL) + 4 :
                configBundle.getInt(Constants.ConfigKeys.DANMAKU_SIZE_HORIZONTAL,Constants.ConfigDefaultValues.DANMAKU_SIZE_HORIZONTAL) + 4)); // 设置行高
        config.setMaxScrollLine(ScreenUtil.getScreenHeight() / 2 / config.getLineHeight());

        // 性能配置
        if (performanceController != null) {
            performanceController.setLevel(configBundle.getInt(Constants.ConfigKeys.PERFORMANCE_LEVEL, Constants.ConfigDefaultValues.PERFORMANCE_LEVEL));
        }
    }

    private WindowManager.LayoutParams getBaseLayoutParams() {
        return new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
    }

    private void initGamingMenu() {
        if (mGamingOverlayView == null && mWindowManager != null) {
            mGamingOverlayView = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.gaming_overlay_layout, null);
            mGamingOverlayView.setVisibility(View.GONE);
            WindowManager.LayoutParams mGamingViewLayoutParams = getBaseLayoutParams();
            mWindowManager.addView(mGamingOverlayView, mGamingViewLayoutParams);

            mGamingMenu = mGamingOverlayView.findViewById(R.id.gaming_menu);
            mQSView = mGamingOverlayView.findViewById(R.id.gaming_qs);
            mQSAppView = mGamingOverlayView.findViewById(R.id.gaming_qsapp);

            performanceController = mGamingOverlayView.findViewById(R.id.performance_controller);
            mGamingOverlayView.setOnClickListener(v -> showHideGamingMenu(0));
        }
    }

    private void initFloatingButton() {
        if (mGamingFloatingButton == null && mWindowManager != null) {
            mGamingFloatingButton = LayoutInflater.from(this).inflate(R.layout.gaming_button_layout, null);

            mGamingFBLayoutParams = getBaseLayoutParams();
            mGamingFBLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            mGamingFBLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

            mGamingFloatingButton.setOnClickListener(v -> showHideGamingMenu(0));
            mGamingFloatingButton.setOnTouchListener(new View.OnTouchListener() {
                private int origX;
                private int origY;
                private int touchX;
                private int touchY;
                private boolean isMoved;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int x = (int) event.getRawX();
                    int y = (int) event.getRawY();

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            origX = mGamingFBLayoutParams.x;
                            origY = mGamingFBLayoutParams.y;
                            touchX = x;
                            touchY = y;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            isMoved = true;
                            mGamingFBLayoutParams.x = origX + x - touchX;
                            mGamingFBLayoutParams.y = origY + y - touchY;
                            if (mWindowManager != null) {
                                mWindowManager.updateViewLayout(mGamingFloatingButton, mGamingFBLayoutParams);
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            if (!isMoved) {
                                v.performClick();
                            } else {
                                if (ScreenUtil.isPortrait()) {
                                    mPreferences.edit()
                                            .putInt(Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_VERTICAL_X, mGamingFBLayoutParams.x)
                                            .putInt(Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_VERTICAL_Y, mGamingFBLayoutParams.y)
                                            .apply();
                                } else {
                                    mPreferences.edit()
                                            .putInt(Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_HORIZONTAL_X, mGamingFBLayoutParams.x)
                                            .putInt(Constants.LocalConfigKeys.FLOATING_BUTTON_COORDINATE_HORIZONTAL_Y, mGamingFBLayoutParams.y)
                                            .apply();
                                }
                            }
                            isMoved = false;
                            break;
                        default:
                            return false;
                    }
                    return true;
                }
            });

            mWindowManager.addView(mGamingFloatingButton, mGamingFBLayoutParams);
        }
    }

    /*
     * mode: 0=auto, 1=show, 2=hide
     */
    private void showHideGamingMenu(int mode) {
        if (mGamingOverlayView.getVisibility() == View.VISIBLE && mode != 1) {
            // hide
            mGamingOverlayView.setVisibility(View.GONE);
            mGamingFloatingButton.setVisibility(View.VISIBLE);
        } else if (mode != 2) {
            // show
            int gravity = 0;
            if (mGamingFBLayoutParams.x > 0) {
                gravity |= Gravity.RIGHT;
            } else {
                gravity |= Gravity.LEFT;
            }
            if (mGamingFBLayoutParams.y > 0) {
                gravity |= Gravity.BOTTOM;
            } else {
                gravity |= Gravity.TOP;
            }

            mGamingFloatingButton.setVisibility(View.GONE);
            mGamingOverlayView.setGravity(gravity);
            ViewGroup.LayoutParams gamingMenuLayoutParams =  mGamingMenu.getLayoutParams();
            if (ScreenUtil.isPortrait()) {
                gamingMenuLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            } else {
                gamingMenuLayoutParams.width = ScreenUtil.getScreenWidth() / 2;
            }
            mGamingMenu.setLayoutParams(gamingMenuLayoutParams);

            mGamingOverlayView.setVisibility(View.VISIBLE);
        }
    }

    private void initDanmaku() {
        if (mWindowManager != null && mDanmakuContainer == null) {
            mDanmakuContainer = new FrameLayout(this);
            WindowManager.LayoutParams danmakuParams = getBaseLayoutParams();
            danmakuParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            mWindowManager.addView(mDanmakuContainer, danmakuParams);

            mDanmakuManager = DanmakuManager.getInstance();
            mDanmakuManager.init(this, mDanmakuContainer);
        }
    }

    private void sendDanmaku(String danmakuText) {
        if (mDanmakuManager == null) return;
        Danmaku danmaku = new Danmaku();
        danmaku.text = danmakuText;
        danmaku.mode = Danmaku.Mode.scroll;
        danmaku.size = ScreenUtil.autoSize(
                configBundle.getInt(Constants.ConfigKeys.DANMAKU_SIZE_HORIZONTAL,Constants.ConfigDefaultValues.DANMAKU_SIZE_HORIZONTAL),
                configBundle.getInt(Constants.ConfigKeys.DANMAKU_SIZE_VERTICAL,Constants.ConfigDefaultValues.DANMAKU_SIZE_VERTICAL));
        mDanmakuManager.send(danmaku);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mOMReceiver);
        unregisterReceiver(mSysConfigChangedReceiver);
        if (mWindowManager != null) {
            if (mGamingFloatingButton != null) mWindowManager.removeViewImmediate(mGamingFloatingButton);
            if (mDanmakuContainer != null) mWindowManager.removeViewImmediate(mDanmakuContainer);
            if (mGamingOverlayView != null) mWindowManager.removeViewImmediate(mGamingOverlayView);
        }
        super.onDestroy();
    }

    private class OMReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case Constants.Broadcasts.BROADCAST_NEW_DANMAKU:
                    String danmaku = intent.getStringExtra("danmaku_text");
                    if (TextUtils.isEmpty(danmaku)) return;
                    sendDanmaku(danmaku);
                    break;
                case Constants.Broadcasts.BROADCAST_CONFIG_CHANGED:
                    if (intent.getExtras() == null) return;
                    configBundle.putAll(intent.getExtras());
                    updateConfig();
                    break;
                case Constants.Broadcasts.BROADCAST_GAMING_MENU_CONTROL:
                    if ("hide".equals(intent.getStringExtra("cmd"))) {
                        showHideGamingMenu(2);
                    } else if ("show".equals(intent.getStringExtra("cmd"))) {
                        showHideGamingMenu(1);
                    }
                    break;
            }
        }
    }
}
