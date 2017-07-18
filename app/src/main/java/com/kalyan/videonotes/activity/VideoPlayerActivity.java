package com.kalyan.videonotes.activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.kalyan.videonotes.Constants;
import com.kalyan.videonotes.Keys;
import com.kalyan.videonotes.R;
import com.kalyan.videonotes.util.UriUtil;

public class VideoPlayerActivity extends AppCompatActivity implements YouTubePlayer.OnInitializedListener {

    YouTubePlayerSupportFragment youTubePlayerFragment;
    YouTubePlayer youTubePlayer;
    String videoId;
    boolean isFullScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Video");
        }

        youTubePlayerFragment = (YouTubePlayerSupportFragment) getSupportFragmentManager().findFragmentById(R.id.youtube_fragment);
        youTubePlayerFragment.initialize(Keys.YT_DEV_KEY, this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                if (youTubePlayer != null) {
                    youTubePlayer.pause();
                    Toast.makeText(VideoPlayerActivity.this, "" + youTubePlayer.getCurrentTimeMillis(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Read the video ID
        videoId = getIntent().getStringExtra(Constants.VIDEO_ID);

        if (videoId == null || videoId.isEmpty()) {
            Toast.makeText(this, "Invalid YouTube Video ID", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
        /*if (!wasRestored) {
            youTubePlayer.cueVideo("wKJ9KzGQq0w");
        }*/
        if (!wasRestored) {
            this.youTubePlayer = youTubePlayer;
            this.youTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);
            this.youTubePlayer.setOnFullscreenListener(new YouTubePlayer.OnFullscreenListener() {
                @Override
                public void onFullscreen(boolean fullScreen) {
                    isFullScreen = fullScreen;
                }
            });
            this.youTubePlayer.loadVideo(UriUtil.getVideoTag(videoId));
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        Toast.makeText(this, "Unable to play the video", Toast.LENGTH_SHORT).show();
        this.finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (youTubePlayer != null) {
            youTubePlayer.setFullscreen(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
        }
    }

    @Override
    public void onBackPressed() {
        if (isFullScreen) {
            youTubePlayer.setFullscreen(false);
        } else {
            super.onBackPressed();
        }
    }
}
