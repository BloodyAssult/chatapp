package org.danial.chatapp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatSession {
    private String id = UUID.randomUUID().toString();
    private String name = "مکالمه جدید";
    private List<String> messages = new ArrayList<>();

    public ChatSession() {}
    public ChatSession(String name) { this.name = name;}
    public String getId() { return id;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public List<String> getMessages() {return messages;}
    public void addMessage(String message) {messages.add(message);}
}
