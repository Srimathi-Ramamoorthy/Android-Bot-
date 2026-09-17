package com.example.productiondisplay.ui.slideshow.Production;

import java.util.List;

public class Message {
    private String text;
    private boolean isBot;
    private List<String> options;

    public Message(String text, boolean isBot, List<String> options) {
        this.text = text;
        this.isBot = isBot;
        this.options = options;
    }

    public String getText() {
        return text;
    }

    public boolean isBot() {
        return isBot;
    }

    public List<String> getOptions() {
        return options;
    }
}
