package org.exthmui.game.qs;

import android.content.Context;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;

public class AutoBrightnessTile extends TileBase {
    public AutoBrightnessTile(Context context) {
        super(context, "自动亮度", Constants.GamingActionTargets.DISABLE_AUTO_BRIGHTNESS, R.drawable.ic_qs_auto_brightness);
        setNeedInverse(true);
    }
}
