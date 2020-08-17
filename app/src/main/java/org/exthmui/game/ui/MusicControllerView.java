package org.exthmui.game.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.exthmui.game.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MusicControllerView extends LinearLayout implements View.OnClickListener {
    private MediaSessionManager mMediaSessionManager;
    private MediaController mMediaController;

    private TextView mediaTitle;
    private TextView mediaArtist;
    private ImageView mediaAlbumImg;
    private ImageView prevButton;
    private ImageView nextButton;
    private ImageView playPauseButton;

    private Context mContext;

    private MediaController.Callback mMediaCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(@Nullable PlaybackState state) {
            super.onPlaybackStateChanged(state);
            if (state != null) updatePlayPauseStatus(state.getState());
        }

        @Override
        public void onSessionDestroyed() {
            MusicControllerView.this.setVisibility(GONE);
            super.onSessionDestroyed();
        }

        @Override
        public void onMetadataChanged(@Nullable MediaMetadata metadata) {
            updateMetaData(metadata);
        }
    };

    private MediaSessionManager.OnActiveSessionsChangedListener onActiveSessionsChangedListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
        @Override
        public void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {
            if (mMediaController != null) mMediaController.unregisterCallback(mMediaCallback);
            MusicControllerView.this.setVisibility(GONE);
            if (controllers == null) return;
            for (MediaController controller : controllers) {
                mMediaController = controller;
                if (getMediaControllerPlaybackState(controller) == PlaybackState.STATE_PLAYING) {
                    break;
                }
            }
            if (mMediaController != null) {
                mMediaController.registerCallback(mMediaCallback);
                mMediaCallback.onMetadataChanged(mMediaController.getMetadata());
                mMediaCallback.onPlaybackStateChanged(mMediaController.getPlaybackState());
                MusicControllerView.this.setVisibility(VISIBLE);
            }
        }
    };

    public MusicControllerView(Context context) {
        this(context, null);
    }

    public MusicControllerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MusicControllerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MusicControllerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mMediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        LayoutInflater.from(context).inflate(R.layout.music_control_layout, this, true);

        mContext = context;

        mediaTitle = findViewById(R.id.music_title);
        mediaArtist = findViewById(R.id.music_artist);
        mediaAlbumImg = findViewById(R.id.music_cover);

        prevButton = findViewById(R.id.prev_button);
        nextButton = findViewById(R.id.next_button);
        playPauseButton = findViewById(R.id.play_pause_button);

        prevButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        playPauseButton.setOnClickListener(this);

        mMediaSessionManager.addOnActiveSessionsChangedListener(onActiveSessionsChangedListener, null);
        onActiveSessionsChangedListener.onActiveSessionsChanged(mMediaSessionManager.getActiveSessions(null));
    }

    private int getMediaControllerPlaybackState(MediaController controller) {
        if (controller != null) {
            final PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState != null) {
                return playbackState.getState();
            }
        }
        return PlaybackState.STATE_NONE;
    }

    private void updatePlayPauseStatus(int state) {
        if (state == PlaybackState.STATE_PLAYING) {
            playPauseButton.setImageResource(R.drawable.ic_music_pause);
        } else {
            playPauseButton.setImageResource(R.drawable.ic_music_play);
        }
    }

    private void updateMetaData(MediaMetadata mediaMetadata) {
        if (mediaMetadata == null) return;
        mediaTitle.setText(mediaMetadata.getText(MediaMetadata.METADATA_KEY_TITLE));
        mediaArtist.setText(mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST));
        // Try to get album cover
        if (!(setAlbumImgFromBitmap(mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART)) ||
                setAlbumImgFromBitmap(mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)) ||
                setAlbumImgFromUri(mediaMetadata.getString(MediaMetadata.METADATA_KEY_ART_URI)) ||
                setAlbumImgFromUri(mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)))) {
            mediaAlbumImg.setImageResource(R.drawable.default_album_cover);
        }
    }

    private boolean setAlbumImgFromBitmap(Bitmap bitmap) {
        if (bitmap == null) return false;
        mediaAlbumImg.setImageBitmap(bitmap);
        return true;
    }

    private boolean setAlbumImgFromUri(String uri) {
        try {
            Uri albumUri = Uri.parse(uri);
            InputStream is = mContext.getContentResolver().openInputStream(albumUri);
            if (is != null) {
                mediaAlbumImg.setImageBitmap(BitmapFactory.decodeStream(is));
                is.close();
            }
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (mMediaController == null) return;
        if (v == prevButton) {
            mMediaController.getTransportControls().skipToPrevious();
        } else if (v == nextButton) {
            mMediaController.getTransportControls().skipToNext();
        } else if (v == playPauseButton) {
            if (getMediaControllerPlaybackState(mMediaController) == PlaybackState.STATE_PLAYING) {
                mMediaController.getTransportControls().pause();
            } else {
                mMediaController.getTransportControls().play();
            }
        }
    }

    @Override
    public void onDetachedFromWindow() {
        mMediaSessionManager.removeOnActiveSessionsChangedListener(onActiveSessionsChangedListener);
        if (mMediaController != null) mMediaController.unregisterCallback(mMediaCallback);
        super.onDetachedFromWindow();
    }
}
