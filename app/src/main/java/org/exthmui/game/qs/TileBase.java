package org.exthmui.game.qs;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;

public class TileBase extends LinearLayout implements View.OnClickListener {
    ImageView qsIcon;
    TextView qsText;

    Context mContext;

    private boolean isSelected;
    private String key;
    Intent intent;
    private boolean needInverse;

    public TileBase(Context context, CharSequence text, String key, int iconRes) {
        this(context, null);
        qsIcon.setImageResource(iconRes);
        qsText.setText(text);
        this.key = key;
        intent = new Intent(Constants.Broadcasts.BROADCAST_GAMING_ACTION).putExtra("target", key);
    }

    public TileBase(Context context, CharSequence text, String key, Drawable icon) {
        this(context, null);
        qsIcon.setImageDrawable(icon);
        qsText.setText(text);
        this.key = key;
        intent = new Intent(Constants.Broadcasts.BROADCAST_GAMING_ACTION).putExtra("target", key);
    }

    public TileBase(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TileBase(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TileBase(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.gaming_qs_view, this, true);
        qsIcon = findViewById(R.id.qs_icon);
        qsText = findViewById(R.id.qs_text);
        this.setOnClickListener(this);
    }

    public void setConfig(Bundle bundle) {
        isSelected = bundle.getBoolean(key, isSelected);
        qsIcon.setSelected(needInverse ? !isSelected : isSelected);
    }

    public String getKey() {
        return key;
    }

    public void setNeedInverse(boolean val) {
        needInverse = val;
    }

    @Override
    public void onClick(View v) {
        intent.putExtra("value", !isSelected);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
}
