package com.kalyan.videonotes.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.kalyan.videonotes.Constants;
import com.kalyan.videonotes.Keys;
import com.kalyan.videonotes.R;
import com.kalyan.videonotes.adapter.NotesAdapter;
import com.kalyan.videonotes.dialog.RecorderDialog;
import com.kalyan.videonotes.model.VoiceNote;
import com.kalyan.videonotes.util.UriUtil;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class VideoPlayerActivity extends AppCompatActivity implements YouTubePlayer.OnInitializedListener,
        RecorderDialog.RecorderDialogListener {

    private static final String LOG_TAG = VideoPlayerActivity.class.getName();
    private YouTubePlayerSupportFragment youTubePlayerFragment;
    private YouTubePlayer youTubePlayer;
    private String videoId;
    private boolean isFullScreen;
    private FloatingActionButton fab;
    private long videoPausedTime;
    private ListView textNotesListView;
    private boolean isNotesPlayed;

    private VoiceNote voiceNote;
    private RealmResults<VoiceNote> allNotesOfThisVideo;

    // Requesting permission to RECORD_AUDIO
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 101;

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

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (youTubePlayer != null) {
                    // Pause the video
                    youTubePlayer.pause();

                    // Check for recording permission
                    if (ContextCompat.checkSelfPermission(VideoPlayerActivity.this,
                            permissions[0]) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(VideoPlayerActivity.this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
                    } else {
                        showRecordingDialog();
                    }
                }
            }
        });
        fab.setVisibility(View.GONE);
        textNotesListView = (ListView) findViewById(R.id.text_notes_listview);

        Realm realm = Realm.getDefaultInstance();

        if (getIntent().hasExtra(Constants.VIDEO_ID)) {
            videoId = getIntent().getStringExtra(Constants.VIDEO_ID);
            allNotesOfThisVideo = realm.where(VoiceNote.class).equalTo(Constants.VOICE_NOTE_YTVIDEOID, videoId)
                    .isNotNull(Constants.VOICE_NOTE_NOTES_TEXT)
                    .findAllSorted(Constants.VOICE_NOTE_TIMESTAMP, Sort.ASCENDING);
        } else if (getIntent().hasExtra(Constants.NOTE_ID)) {
            voiceNote = realm.where(VoiceNote.class).equalTo(Constants.VOICE_NOTE_ID, getIntent().getStringExtra(Constants.NOTE_ID)).findFirst();
            videoId = voiceNote.getYtVideoId();
            allNotesOfThisVideo = realm.where(VoiceNote.class).equalTo(Constants.VOICE_NOTE_YTVIDEOID, videoId)
                    .isNotNull(Constants.VOICE_NOTE_NOTES_TEXT)
                    .findAllSorted(Constants.VOICE_NOTE_TIMESTAMP, Sort.ASCENDING);
        }

        if (videoId == null || videoId.isEmpty()) {
            Toast.makeText(this, "Invalid YouTube Video ID", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        displayNotes();
    }

    private void displayNotes() {
        NotesAdapter adapter = new NotesAdapter(this, allNotesOfThisVideo, Constants.NOTES_TEXT_MODE);
        textNotesListView.setAdapter(adapter);

        textNotesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (youTubePlayer != null) {
                    youTubePlayer.seekToMillis((int) allNotesOfThisVideo.get(position).getTimestamp());
                }
            }
        });
    }

    private void showRecordingDialog() {
        DialogFragment recorderDialog = new RecorderDialog();
        recorderDialog.show(getSupportFragmentManager(), "recorder");
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
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
            this.youTubePlayer.setPlayerStateChangeListener(new YouTubePlayer.PlayerStateChangeListener() {
                @Override
                public void onLoading() {

                }

                @Override
                public void onLoaded(String s) {
                    if (voiceNote != null && !isNotesPlayed) {
                        isNotesPlayed = true;
                        VideoPlayerActivity.this.youTubePlayer.pause();
                        VideoPlayerActivity.this.youTubePlayer.seekToMillis((int) voiceNote.getTimestamp());
                        // Play the audio
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(VideoPlayerActivity.this);
                        alertDialogBuilder.setMessage("Playing Notes");
                        alertDialogBuilder.setCancelable(false);
                        final AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.setCanceledOnTouchOutside(false);
                        alertDialog.show();
                        final MediaPlayer mediaPlayer = new MediaPlayer();
                        try {
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.setDataSource(voiceNote.getNotesFilePath());
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    mediaPlayer.release();
                                    alertDialog.dismiss();
                                    VideoPlayerActivity.this.youTubePlayer.play();
                                }
                            });
                        } catch (IOException e) {
                            Log.e(LOG_TAG, e.getMessage());
                            Toast.makeText(VideoPlayerActivity.this, "Unable to play notes", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onAdStarted() {

                }

                @Override
                public void onVideoStarted() {

                }

                @Override
                public void onVideoEnded() {

                }

                @Override
                public void onError(YouTubePlayer.ErrorReason errorReason) {

                }
            });
            this.youTubePlayer.setPlaybackEventListener(new YouTubePlayer.PlaybackEventListener() {
                @Override
                public void onPlaying() {
                    fab.setVisibility(View.VISIBLE);
                }

                @Override
                public void onPaused() {
                    videoPausedTime = VideoPlayerActivity.this.youTubePlayer.getCurrentTimeMillis();
                }

                @Override
                public void onStopped() {

                }

                @Override
                public void onBuffering(boolean b) {
                    fab.setVisibility(View.GONE);
                }

                @Override
                public void onSeekTo(int i) {

                }
            });
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showRecordingDialog();
                } else {
                    Toast.makeText(this, "Need Audio Recording Permission. Please grant permission in settings.", Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }

    @Override
    public void onDialogSaveClick(String noteFilePath) {
        // Save the entry to database
        Realm realm = Realm.getDefaultInstance();

        // Create the object
        VoiceNote note = new VoiceNote();
        note.setId(UUID.randomUUID().toString());
        note.setNotesFilePath(noteFilePath);
        note.setNotesText(null);
        note.setTimestamp(videoPausedTime);
        note.setYtVideoId(UriUtil.getVideoTag(videoId));
        note.setCreatedAt(new Date());
        note.setUpdatedAt(new Date());

        // Save it
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(note);
        realm.commitTransaction();
    }
}
