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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.exthmui.game.R;

import static android.content.Context.BATTERY_SERVICE;

public class TimeAndBatteryView extends LinearLayout {

    private Context mContext;

    private TextView currentTime;
    private TextView currentDate;
    private TextView currentBattery;

    private TimeChangeReceiver timeChangeReceiver = new TimeChangeReceiver();
    private BatteryChangeReceiver batteryChangeReceiver = new BatteryChangeReceiver();

    public TimeAndBatteryView(Context context) {
        this(context, null);
    }

    public TimeAndBatteryView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimeAndBatteryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TimeAndBatteryView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;

        LayoutInflater.from(context).inflate(R.layout.time_battery_layout, this, true);

        currentBattery = findViewById(R.id.current_battery);
        currentDate = findViewById(R.id.current_date);
        currentTime = findViewById(R.id.current_time);

        mContext.registerReceiver(timeChangeReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        mContext.registerReceiver(batteryChangeReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        updateTime();

        BatteryManager batteryManager = (BatteryManager) mContext.getSystemService(BATTERY_SERVICE);
        currentBattery.setText(mContext.getString(R.string.battery_format, batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)));

    }

    private void updateTime() {
        long sysTime = System.currentTimeMillis();
        currentDate.setText(DateFormat.format(mContext.getString(R.string.date_format), sysTime));
        currentTime.setText(DateFormat.format(mContext.getString(R.string.time_format), sysTime));
    }

    @Override
    public void onDetachedFromWindow() {
        mContext.unregisterReceiver(timeChangeReceiver);
        mContext.unregisterReceiver(batteryChangeReceiver);
        super.onDetachedFromWindow();
    }

    private class TimeChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
                updateTime();
            }
        }
    }

    private class BatteryChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                int level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
                int scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,0);
                int percent = (int)(((float)level / scale) * 100);
                currentBattery.setText(mContext.getString(R.string.battery_format, percent));
            }
        }
    }
}
