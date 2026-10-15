package com.smartqueue.model;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

@IgnoreExtraProperties
public class ChatMessage {
    public String sender; // "Asisten" atau "Mahasiswa"
    public String text;
    public Object timestamp; // Akan diisi oleh Firebase ServerValue.TIMESTAMP

    public ChatMessage() {
        // Konstruktor kosong
    }

    public ChatMessage(String sender, String text) {
        this.sender = sender;
        this.text = text;
        this.timestamp = ServerValue.TIMESTAMP;
    }

    // Getters (PENTING untuk Firebase)
    public String getSender() { return sender; }
    public String getText() { return text; }
    public Object getTimestamp() { return timestamp; }
}