package org.exthmui.game.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

import com.android.systemui.screenrecord.ScreenRecordingAdapter;
import com.android.systemui.screenrecord.ScreenRecordingAudioSource;

import org.exthmui.game.R;
import org.exthmui.game.misc.Constants;

import java.util.ArrayList;

import static com.android.systemui.screenrecord.ScreenRecordingAudioSource.INTERNAL;
import static com.android.systemui.screenrecord.ScreenRecordingAudioSource.MIC;
import static com.android.systemui.screenrecord.ScreenRecordingAudioSource.MIC_AND_INTERNAL;

public class ScreenRecordConfDialogActivity extends Activity {

    static ArrayList<ScreenRecordingAudioSource> sModes = new ArrayList<>();
    static {
        sModes.add(INTERNAL);
        sModes.add(MIC);
        sModes.add(MIC_AND_INTERNAL);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.screenrecord_config_dialog, null);
        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showTap = mPreferences.getBoolean(Constants.LocalConfigKeys.SCREEN_RECORDING_SHOW_TAP, Constants.LocalConfigDefaultValues.SCREEN_RECORDING_SHOW_TAP);
        int audioSource = mPreferences.getInt(Constants.LocalConfigKeys.SCREEN_RECORDING_AUDIO_SOURCE, Constants.LocalConfigDefaultValues.SCREEN_RECORDING_AUDIO_SOURCE);

        Switch mAudioSwitch = dialogView.findViewById(R.id.screenrecord_audio_switch);
        Switch mTapsSwitch = dialogView.findViewById(R.id.screenrecord_taps_switch);
        Spinner mOptions = dialogView.findViewById(R.id.screen_recording_options);
        ArrayAdapter a = new ScreenRecordingAdapter(this,
                android.R.layout.simple_spinner_dropdown_item,
                sModes);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mOptions.setAdapter(a);

        mTapsSwitch.setChecked(showTap);
        if (audioSource != 0) {
            mAudioSwitch.setChecked(true);
            mOptions.setSelection(audioSource - 1);
        } else {
            mAudioSwitch.setChecked(false);
            mOptions.setSelection(Constants.LocalConfigDefaultValues.SCREEN_RECORDING_AUDIO_SOURCE - 1);
        }
        mOptions.setOnItemClickListenerInt((parent, view, position, id) -> mAudioSwitch.setChecked(true));
        builder.setView(dialogView);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            mPreferences.edit()
                    .putBoolean(Constants.LocalConfigKeys.SCREEN_RECORDING_SHOW_TAP, mTapsSwitch.isChecked())
                    .putInt(Constants.LocalConfigKeys.SCREEN_RECORDING_AUDIO_SOURCE, mAudioSwitch.isChecked() ? mOptions.getSelectedItemPosition() + 1 : 0)
                    .apply();
        });
        builder.setOnDismissListener(dialog -> finish());
        builder.create().show();
    }
}