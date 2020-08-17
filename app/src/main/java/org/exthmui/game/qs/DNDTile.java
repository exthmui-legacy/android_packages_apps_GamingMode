package org.exthmui.game.qs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;

public class DNDTile extends TileBase {
    public DNDTile(Context context) {
        super(context, "勿扰模式", Constants.GamingActionTargets.DISABLE_RINGTONE, R.drawable.ic_qs_dnd);
    }
}
