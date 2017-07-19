package com.kalyan.videonotes.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.kalyan.videonotes.model.VoiceNote;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.realm.RealmResults;

/**
 * Copyright (c) 2017 Codelight Studios
 * Created by kalyandechiraju on 19/07/17.
 */

public class NotesAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater inflater;
    private RealmResults<VoiceNote> data;

    public NotesAdapter(Context context, RealmResults<VoiceNote> data) {
        this.context = context;
        this.data = data;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        TextView textView = (TextView) view.findViewById(android.R.id.text1);

        VoiceNote note = data.get(position);

        // Computing the text to be displayed
        long millis = note.getTimestamp();
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        String timeString;
        if (hours != 0) {
            timeString = String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeString = String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds);
        }

        String rowData = "" + (position + 1) + ". " + note.getYtVideoId() + " @ " + timeString;
        textView.setText(rowData);
        return view;
    }
}
