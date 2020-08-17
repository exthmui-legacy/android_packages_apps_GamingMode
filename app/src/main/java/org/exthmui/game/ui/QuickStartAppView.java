package org.exthmui.game.ui;

import android.app.ActivityOptions;
import android.app.WindowConfiguration;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;
import org.exthmui.game.qs.AppTile;

public class QuickStartAppView extends LinearLayout implements View.OnClickListener {

    private Context mContext;
    private PackageManager mPackageManager;
    private ActivityOptions mActivityOptions;
    private Intent mHideMenuIntent = new Intent(Constants.Broadcasts.BROADCAST_GAMING_MENU_CONTROL).putExtra("cmd", "hide");
    int left, right, top, bottom;

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

    public void setFloatingButtonCoordinate(int x, int y) {
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
        left = 100;
        right = left + width;
        top = 150;
        bottom = top + height;
    }

    @Override
    public void onClick(View v) {
        if (v instanceof AppTile) {
            AppTile appTile = (AppTile) v;
            LocalBroadcastManager.getInstance(mContext).sendBroadcastSync(mHideMenuIntent);
            Intent startAppIntent = mPackageManager.getLaunchIntentForPackage(appTile.getKey());
            startAppIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startAppIntent.setPackage(null);
            Rect appWindowRect = new Rect(left, top, right, bottom);
            mActivityOptions.setLaunchBounds(appWindowRect);
            mContext.startActivityAsUser(startAppIntent, mActivityOptions.toBundle(), UserHandle.CURRENT);

        }
    }
}
