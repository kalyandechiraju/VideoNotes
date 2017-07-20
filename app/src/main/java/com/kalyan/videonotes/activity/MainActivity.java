package com.kalyan.videonotes.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.kalyan.videonotes.Constants;
import com.kalyan.videonotes.R;
import com.kalyan.videonotes.adapter.NotesAdapter;
import com.kalyan.videonotes.model.VoiceNote;
import com.kalyan.videonotes.service.SpeechService;
import com.kalyan.videonotes.util.ConnectionUtil;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText searchTextView;
    private ListView notesListView;
    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        notesListView = (ListView) findViewById(R.id.notes_listview);
        searchTextView = (TextInputEditText) findViewById(R.id.yt_search_input);
        AppCompatButton searchButton = (AppCompatButton) findViewById(R.id.yt_search_button);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchText = searchTextView.getText().toString();
                if (!searchText.isEmpty()) {
                    Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
                    intent.putExtra(Constants.VIDEO_ID, searchText);
                    startActivity(intent);
                } else {
                    searchTextView.setError("Enter video url/id");
                }
            }
        });

        realm = Realm.getDefaultInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();

        if (ConnectionUtil.isConnected(this)) {
            RealmResults<VoiceNote> notes = realm.where(VoiceNote.class).isNull(Constants.VOICE_NOTE_NOTES_TEXT).findAll();
            for (VoiceNote note: notes) {
                Intent intent = new Intent(this, SpeechService.class);
                intent.putExtra(Constants.NOTE_ID, note.getId());
                startService(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void loadData() {
        final RealmResults<VoiceNote> notesList = realm.where(VoiceNote.class).findAllSorted("updatedAt", Sort.DESCENDING);
        NotesAdapter adapter = new NotesAdapter(this, notesList, Constants.NOTES_AUDIO_MODE);
        notesListView.setAdapter(adapter);

        notesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
                intent.putExtra(Constants.NOTE_ID, notesList.get(position).getId());
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
