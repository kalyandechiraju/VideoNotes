package com.kalyan.videonotes.model;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by kalyandechiraju on 19/07/17.
 */

public class VoiceNote extends RealmObject {
    @PrimaryKey
    @Required
    private String id;

    @Required
    private String ytVideoId;

    private long timestamp;

    @Required
    private String notesFilePath;

    private String notesText;

    @Required
    private Date createdAt;

    @Required
    private Date updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getYtVideoId() {
        return ytVideoId;
    }

    public void setYtVideoId(String ytVideoId) {
        this.ytVideoId = ytVideoId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNotesFilePath() {
        return notesFilePath;
    }

    public void setNotesFilePath(String notesFilePath) {
        this.notesFilePath = notesFilePath;
    }

    public String getNotesText() {
        return notesText;
    }

    public void setNotesText(String notesText) {
        this.notesText = notesText;
    }
}
