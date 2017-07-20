package com.kalyan.videonotes.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kalyan.videonotes.Constants;
import com.kalyan.videonotes.Keys;
import com.kalyan.videonotes.model.VoiceNote;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class SpeechService extends IntentService implements ISpeechRecognitionServerEvents {

    //private Realm realm;
    //private VoiceNote voiceNote;
    private String voiceNoteId;
    private boolean isTranscriptionReceived;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public SpeechService() {
        super("SpeechService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        DataRecognitionClient client = SpeechRecognitionServiceFactory.createDataClient(SpeechRecognitionMode.LongDictation,
                "en-US", this, Keys.MSFT_SPEECH_KEY);
        client.setAuthenticationUri("");

        if (intent != null && intent.hasExtra(Constants.NOTE_ID)) {
            Realm realm = Realm.getDefaultInstance();
            VoiceNote voiceNote = realm.where(VoiceNote.class).equalTo(Constants.VOICE_NOTE_ID, intent.getStringExtra(Constants.NOTE_ID)).findFirst();
            voiceNoteId = voiceNote.getId();
            sendAudioHelper(voiceNote.getNotesFilePath(), client);
        }
    }

    private void sendAudioHelper(String filename, DataRecognitionClient dataClient) {

        try {
            //dataClient.sendAudioFormat(SpeechAudioFormat.create16BitPCMFormat(16000));
            File file = new File(filename);
            InputStream fileStream = new FileInputStream(file);
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
    }

    @Override
    public void onPartialResponseReceived(String s) {

    }

    @Override
    public void onFinalResponseReceived(RecognitionResult recognitionResult) {
        String transcript = "";
        Log.d("TRANSCRIPT", "" + recognitionResult.Results.length);
        for (int i = 0; i < recognitionResult.Results.length; i++) {
            transcript += recognitionResult.Results[i].DisplayText;
        }
        RealmConfiguration config = new RealmConfiguration.Builder().build();
        Realm realm = Realm.getInstance(config);
        if (!transcript.isEmpty()) {
            Log.d("TRANSCRIPT", transcript);
            isTranscriptionReceived = true;
            VoiceNote note = realm.where(VoiceNote.class).equalTo(Constants.VOICE_NOTE_ID, voiceNoteId).findFirst();
            realm.beginTransaction();
            note.setNotesText(transcript);
            note.setUpdatedAt(new Date());
            realm.copyToRealmOrUpdate(note);
            realm.commitTransaction();
        } else {
            // This method is being called twice and second time with empty transcription.
            // To avoid empty values and multiple tries for a note, this check helps
            if (!isTranscriptionReceived) {
                VoiceNote note = realm.where(VoiceNote.class).equalTo(Constants.VOICE_NOTE_ID, voiceNoteId).findFirst();
                realm.beginTransaction();
                note.setNotesText("Transcript not available");
                note.setUpdatedAt(new Date());
                realm.copyToRealmOrUpdate(note);
                realm.commitTransaction();
            }
        }
        realm.close();
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
}
