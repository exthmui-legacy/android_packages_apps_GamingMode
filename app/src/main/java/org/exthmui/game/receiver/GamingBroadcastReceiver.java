package org.exthmui.game.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Log;

import org.exthmui.game.misc.Constants;
import org.exthmui.game.services.GamingService;

public class GamingBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (Constants.Broadcasts.SYS_BROADCAST_GAMING_MODE_ON.equals(intent.getAction())) {
                Intent serviceIntent = new Intent(context, GamingService.class);
                serviceIntent.putExtras(intent);
                context.startForegroundServiceAsUser(serviceIntent, UserHandle.CURRENT);
            }
        }
    }
}
