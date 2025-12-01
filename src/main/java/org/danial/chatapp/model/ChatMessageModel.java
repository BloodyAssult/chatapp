package org.danial.chatapp.model;


public class ChatMessageModel {
    private String sender;
    private String content;

    public ChatMessageModel(String sender, String conetnt){
        this.sender = sender;
        this.content = conetnt;
    }
    public String getSender() { return sender; }
    public String getContent() {return content; }
}
