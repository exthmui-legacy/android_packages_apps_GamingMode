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
}
