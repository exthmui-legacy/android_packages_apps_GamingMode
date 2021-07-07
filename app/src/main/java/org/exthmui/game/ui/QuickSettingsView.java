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
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import org.exthmui.game.R;
import org.exthmui.game.qs.AutoBrightnessTile;
import org.exthmui.game.qs.DNDTile;
import org.exthmui.game.qs.DanmakuTile;
import org.exthmui.game.qs.LockGestureTile;
import org.exthmui.game.qs.LockHwKeysTile;
import org.exthmui.game.qs.ScreenCaptureTile;
import org.exthmui.game.qs.ScreenRecordTile;
import org.exthmui.game.qs.TileBase;

public class QuickSettingsView extends LinearLayout {

    private TileBase[] qsTiles;

    public QuickSettingsView(Context context) {
        this(context, null);
    }

    public QuickSettingsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickSettingsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public QuickSettingsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.setDividerDrawable(context.getDrawable(R.drawable.qs_divider));
        this.setShowDividers(SHOW_DIVIDER_MIDDLE);
        this.setPadding(0,0, 0,8);

        qsTiles = new TileBase[]{
                new ScreenCaptureTile(context),
                new ScreenRecordTile(context),
                new DanmakuTile(context),
                new DNDTile(context),
                new LockHwKeysTile(context),
                new LockGestureTile(context),
                new AutoBrightnessTile(context)
        };

        for (TileBase tileBase : qsTiles) {
            addView(tileBase);
        }
    }

    public void setConfig(Bundle config) {
        for (TileBase tileBase : qsTiles) {
            tileBase.setConfig(config);
        }
    }

    public void onDestroy() {
        for (TileBase tileBase : qsTiles) {
            tileBase.onDestroy();
        }
    }
}
