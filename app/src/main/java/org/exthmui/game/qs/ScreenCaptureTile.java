package org.exthmui.game.qs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.WindowManagerGlobal;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;

public class ScreenCaptureTile extends TileBase {

    private static final String TAG = "ScreenCaptureTile";

    private Intent hideMenuIntent = new Intent(Constants.Broadcasts.BROADCAST_GAMING_MENU_CONTROL).putExtra("cmd", "hide");
    private Handler mHandler = new Handler();

    public ScreenCaptureTile(Context context) {
        super(context, "屏幕截图", "", R.drawable.ic_qs_screenshot);
        qsIcon.setSelected(true);
    }

    @Override
    public void setConfig(Bundle bundle) {

    }

    @Override
    public void onClick(View v) {
        LocalBroadcastManager.getInstance(mContext).sendBroadcastSync(hideMenuIntent);
        mHandler.postDelayed(() -> {
            try {
                WindowManagerGlobal.getWindowManagerService().takeScreenshot(1);
            } catch (RemoteException e) {
                Log.e(TAG, "Error while trying to take screenshot.", e);
            }
        }, 500);
    }
}
