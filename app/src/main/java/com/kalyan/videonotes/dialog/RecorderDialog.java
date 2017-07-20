package com.kalyan.videonotes.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioRecord;
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
import com.kalyan.videonotes.util.WaveFileConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    private String fileName;
    private String tempFileName;
    private FloatingActionButton recordButton;
    private LinearLayout postRecordingLayout;
    private TextView recordingStatusLabel, timerLabel;
    private RecorderDialogListener listener;
    private long startTime;
    private Timer stopwatchTimer;

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;

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

        bufferSize = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                    recorder = null;
                }
            }
        });

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

        if (recorder != null) {
            isRecording = false;
            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                recorder.stop();
                recorder.release();
                recorder = null;
                recordingThread = null;
            }
            fileName = getActivity().getFilesDir().getAbsolutePath() + Constants.VOICE_FILE_PREFIX + System.currentTimeMillis()
                    + Constants.VOICE_FILE_EXT;
            copyWaveFile(tempFileName, fileName);
            File file = new File(tempFileName);
            file.delete();
        }
    }

    private void startRecording() {
        isRecording = true;
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);
        if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            recorder.startRecording();
            recordingThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    writeAudioDataToFile();
                }
            }, "AudioRecorder Thread");

            recordingThread.start();
        }
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


    // Helpers to write raw audio to .wav file

    private void writeAudioDataToFile() {
        byte data[] = new byte[bufferSize];
        tempFileName = getActivity().getFilesDir().getAbsolutePath() + Constants.VOICE_FILE_PREFIX + System.currentTimeMillis()
                + "_temp.raw";
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(tempFileName);
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        int read = 0;

        if (os != null) {
            while (isRecording) {
                read = recorder.read(data, 0, bufferSize);

                if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = 16 * RECORDER_SAMPLERATE * channels / 8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            Log.d(LOG_TAG, "File size: " + totalDataLen);

            WaveFileConverter.writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }


}
