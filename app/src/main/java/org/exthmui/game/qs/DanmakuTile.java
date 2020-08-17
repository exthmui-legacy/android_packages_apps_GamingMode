package org.exthmui.game.qs;

import android.content.Context;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;

public class DanmakuTile extends TileBase {
    public DanmakuTile(Context context) {
        super(context, "通知弹幕", Constants.GamingActionTargets.SHOW_DANMAKU, R.drawable.ic_qs_danmaku);
    }
}
