package org.exthmui.game.qs;

import android.content.Context;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;

public class LockHwKeysTile extends TileBase {
    public LockHwKeysTile(Context context) {
        super(context, "屏蔽按键", Constants.GamingActionTargets.DISABLE_HW_KEYS, R.drawable.ic_qs_disable_hw_key);
    }
}
