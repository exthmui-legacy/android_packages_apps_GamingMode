package org.exthmui.game.qs;

import android.content.Context;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;

public class LockGestureTile extends TileBase {
    public LockGestureTile(Context context) {
        super(context, "屏蔽手势", Constants.GamingActionTargets.DISABLE_GESTURE, R.drawable.ic_qs_disable_gesture);
    }
}
