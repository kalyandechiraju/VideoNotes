package com.kalyan.videonotes.dialog;

import android.app.Dialog;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kalyan.videonotes.Constants;
import com.kalyan.videonotes.R;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by kalyandechiraju on 19/07/17.
 */

public class RecorderDialog extends DialogFragment implements View.OnClickListener {

    private static final String LOG_TAG = RecorderDialog.class.getName();
    private boolean isRecording;
    private MediaRecorder mediaRecorder;
    private String fileName;
    private FloatingActionButton recordButton;
    private LinearLayout postRecordingLayout;
    private TextView recordingStatusLabel, timerLabel;
    private RecorderDialogListener listener;
    private long startTime;
    private Timer stopwatchTimer;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View dialogView = inflater.inflate(R.layout.dialog_record_audio, null);
        builder.setView(dialogView);

        AppCompatButton saveButton = (AppCompatButton) dialogView.findViewById(R.id.dialog_save_button);
        saveButton.setOnClickListener(this);
        AppCompatButton discardButton = (AppCompatButton) dialogView.findViewById(R.id.dialog_discard_button);
        discardButton.setOnClickListener(this);
        recordButton = (FloatingActionButton) dialogView.findViewById(R.id.dialog_record_button);
        recordButton.setOnClickListener(this);

        postRecordingLayout = (LinearLayout) dialogView.findViewById(R.id.post_record_actions_layout);
        postRecordingLayout.setVisibility(View.GONE);

        recordingStatusLabel = (TextView) dialogView.findViewById(R.id.dialog_recording_status_label);
        timerLabel = (TextView) dialogView.findViewById(R.id.dialog_timer_label);

        return builder.create();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dialog_save_button:
                listener.onDialogSaveClick(fileName);
                Toast.makeText(getContext(), "Voice Note Saved", Toast.LENGTH_SHORT).show();
                this.dismiss();
                break;
            case R.id.dialog_record_button:
                if (isRecording) {
                    recordButton.setVisibility(View.GONE);
                    postRecordingLayout.setVisibility(View.VISIBLE);
                    endRecording();
                    recordingStatusLabel.setText("Recording complete");
                } else {
                    recordButton.setImageResource(R.drawable.ic_stop_white_24dp);
                    recordingStatusLabel.setText("Recording...");
                    startRecording();
                }
                break;
            case R.id.dialog_discard_button:
                File recordedFile = new File(fileName);
                if (recordedFile.exists()) {
                    recordedFile.delete();
                }
                Toast.makeText(getContext(), "Voice Note Discarded", Toast.LENGTH_SHORT).show();
                this.dismiss();
                break;
        }
    }

    private void endRecording() {
        isRecording = false;
        if (stopwatchTimer != null) {
            stopwatchTimer.cancel();
            stopwatchTimer.purge();
        }
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void startRecording() {
        isRecording = true;
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        fileName = getActivity().getFilesDir().getAbsolutePath() + Constants.VOICE_FILE_PREFIX + System.currentTimeMillis()
                + Constants.VOICE_FILE_EXT;
        mediaRecorder.setOutputFile(fileName);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        mediaRecorder.start();
        startTimer();
    }

    private void startTimer() {
        stopwatchTimer = new Timer();
        startTime = System.currentTimeMillis();
        stopwatchTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        long nowTime = System.currentTimeMillis();
                        long diff = nowTime - startTime;
                        int seconds = (int) (diff / 1000);
                        int minutes = seconds / 60;
                        seconds = seconds % 60;
                        timerLabel.setText("" + minutes + ":"
                                + String.format(Locale.ENGLISH, "%02d", seconds));
                    }
                });

            }
        }, 0, 10);
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (RecorderDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement RecorderDialogListener");
        }
    }

    public interface RecorderDialogListener {
        void onDialogSaveClick(String noteFilePath);
    }
}
