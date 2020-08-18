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

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;

public class GamingPerformanceView extends LinearLayout implements SeekBar.OnSeekBarChangeListener {

    private Context mContext;

    private Intent mPerformanceIntent = new Intent(Constants.Broadcasts.BROADCAST_GAMING_ACTION)
            .putExtra("target", Constants.GamingActionTargets.PERFORMANCE_LEVEL);

    private SeekBar mSeekBar;

    public GamingPerformanceView(Context context) {
        this(context, null);
    }

    public GamingPerformanceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.gaming_perofrmance_layout, this, true);
        mSeekBar = findViewById(R.id.performance_seek);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    public void setLevel(int level) {
        mSeekBar.setProgress(level);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mPerformanceIntent.putExtra("value", mSeekBar.getProgress());
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(mPerformanceIntent);
    }
}
