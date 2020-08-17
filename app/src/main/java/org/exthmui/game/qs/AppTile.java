package org.exthmui.game.qs;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public class AppTile extends TileBase {

    public AppTile(Context context, String packageName) {
        super(context, packageName, packageName, null);
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            qsText.setText(applicationInfo.loadLabel(pm));
            qsIcon.setImageDrawable(applicationInfo.loadIcon(pm));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }
}
