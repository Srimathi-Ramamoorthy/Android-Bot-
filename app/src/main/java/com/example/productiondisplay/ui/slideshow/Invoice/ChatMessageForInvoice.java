package com.example.productiondisplay.ui.slideshow.Invoice;

public class ChatMessageForInvoice {

    public static final int SENDER_USER = 0;
    public static final int SENDER_BOT = 1;

    private String message;
    private int sender;

    public ChatMessageForInvoice(String message, int sender) {
        this.message = message;
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public int getSender() {
        return sender;
    }
}
