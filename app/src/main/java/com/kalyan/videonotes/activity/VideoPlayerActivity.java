package com.kalyan.videonotes.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
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
import com.kalyan.videonotes.dialog.RecorderDialog;
import com.kalyan.videonotes.model.VoiceNote;
import com.kalyan.videonotes.util.UriUtil;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.SpeechAudioFormat;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class VideoPlayerActivity extends AppCompatActivity implements YouTubePlayer.OnInitializedListener,
        RecorderDialog.RecorderDialogListener, ISpeechRecognitionServerEvents {

    private YouTubePlayerSupportFragment youTubePlayerFragment;
    private YouTubePlayer youTubePlayer;
    private String videoId;
    private boolean isFullScreen;
    private FloatingActionButton fab;
    private long videoPausedTime;

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

        // Read the video ID
        if (getIntent().hasExtra(Constants.VIDEO_ID)) {
            videoId = getIntent().getStringExtra(Constants.VIDEO_ID);
        } else if (getIntent().hasExtra(Constants.NOTE_ID)) {
            Realm realm = Realm.getDefaultInstance();
            voiceNote = realm.where(VoiceNote.class).equalTo(Constants.VOICE_NOTE_ID, getIntent().getStringExtra(Constants.NOTE_ID)).findFirst();
            videoId = voiceNote.getYtVideoId();
            allNotesOfThisVideo = realm.where(VoiceNote.class).equalTo(Constants.VOICE_NOTE_YTVIDEOID, videoId)
                    .findAllSorted(Constants.VOICE_NOTE_TIMESTAMP, Sort.ASCENDING);

        }

        if (videoId == null || videoId.isEmpty()) {
            Toast.makeText(this, "Invalid YouTube Video ID", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        DataRecognitionClient client = SpeechRecognitionServiceFactory.createDataClient(this, SpeechRecognitionMode.LongDictation, "en-US",
                this, "28f876f2cf354c0ba723756f1e7391d2");
        client.setAuthenticationUri("");
        sendAudioHelper(voiceNote.getNotesFilePath(), client);
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
        Toast.makeText(this, noteFilePath, Toast.LENGTH_LONG).show();
        Realm realm = Realm.getDefaultInstance();

        // Create the object
        VoiceNote note = new VoiceNote();
        note.setId(UUID.randomUUID().toString());
        note.setNotesFilePath(noteFilePath);
        note.setNotesText(null);
        note.setTimestamp(videoPausedTime);
        note.setYtVideoId(videoId);
        note.setCreatedAt(new Date());
        note.setUpdatedAt(new Date());

        // Save it
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(note);
        realm.commitTransaction();
    }


    private void sendAudioHelper(String filename, DataRecognitionClient client) {
        RecognitionTask doDataReco = new RecognitionTask(client, SpeechRecognitionMode.LongDictation, filename);
        try {
            doDataReco.execute().get(200, TimeUnit.SECONDS);
        } catch (Exception e) {
            doDataReco.cancel(true);
        }
    }

    @Override
    public void onPartialResponseReceived(String s) {

    }

    @Override
    public void onFinalResponseReceived(RecognitionResult recognitionResult) {
        String transcript = "";
        for (int i = 0; i < recognitionResult.Results.length; i++) {
            transcript += recognitionResult.Results[i].DisplayText;
        }
        if (!transcript.isEmpty()) {
            /*Realm realm = Realm.getDefaultInstance();
            VoiceNote note = realm.where(VoiceNote.class).equalTo(Constants.VOICE_NOTE_ID, voiceNote.getId()).findFirst();
            note.setNotesText(transcript);
            note.setUpdatedAt(new Date());
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(note);
            realm.commitTransaction();*/
            Toast.makeText(this, "Transcript: " + transcript, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Empty Transcript", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onIntentReceived(String s) {

    }

    @Override
    public void onError(int i, String s) {

    }

    @Override
    public void onAudioEvent(boolean b) {

    }

    private class RecognitionTask extends AsyncTask<Void, Void, Void> {
        DataRecognitionClient dataClient;
        SpeechRecognitionMode recoMode;
        String filename;

        RecognitionTask(DataRecognitionClient dataClient, SpeechRecognitionMode recoMode, String filename) {
            this.dataClient = dataClient;
            this.recoMode = recoMode;
            this.filename = filename;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Note for wave files, we can just send data from the file right to the server.
                // In the case you are not an audio file in wave format, and instead you have just
                // raw data (for example audio coming over bluetooth), then before sending up any
                // audio data, you must first send up an SpeechAudioFormat descriptor to describe
                // the layout and format of your raw audio data via DataRecognitionClient's sendAudioFormat() method.
                // String filename = recoMode == SpeechRecognitionMode.ShortPhrase ? "whatstheweatherlike.wav" : "batman.wav";
                dataClient.sendAudioFormat(SpeechAudioFormat.createSiren7Format(16000));
                File file = new File(filename);
                InputStream fileStream = new FileInputStream(file); //getAssets().open("whatstheweatherlike.wav");
                int bytesRead = 0;
                byte[] buffer = new byte[1024];

                do {
                    // Get  Audio data to send into byte buffer.
                    bytesRead = fileStream.read(buffer);

                    if (bytesRead > -1) {
                        // Send of audio data to service.
                        dataClient.sendAudio(buffer, bytesRead);
                    }
                } while (bytesRead > 0);

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                dataClient.endAudio();
            }

            return null;
        }
    }
}
