package com.example.productiondisplay.ui.slideshow.Production;

public class ProductionCountResponse {
    private String status_code;
    private Object status_description; // Can be Integer OR Map<String, Integer>

    public String getStatus_code() { return status_code; }
    public Object getStatus_description() { return status_description; }

}
