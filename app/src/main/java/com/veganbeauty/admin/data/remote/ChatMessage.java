package com.veganbeauty.admin.data.remote;

public class ChatMessage {
    private String id;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String receiverId;
    private String receiverName;
    private String receiverAvatar;
    private String content;
    private long timestamp;
    private boolean isRead;

    public ChatMessage() {
        this.id = "";
        this.senderId = "";
        this.senderName = "";
        this.senderAvatar = "";
        this.receiverId = "";
        this.receiverName = "";
        this.receiverAvatar = "";
        this.content = "";
        this.timestamp = 0L;
        this.isRead = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverAvatar() { return receiverAvatar; }
    public void setReceiverAvatar(String receiverAvatar) { this.receiverAvatar = receiverAvatar; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
