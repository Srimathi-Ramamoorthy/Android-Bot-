package com.example.productiondisplay.ui.slideshow.Invoice;

import java.util.List;

public class ModuleResponse {
    private String status_code;
    private List<String> status_description;

    public String getStatusCode() {
        return status_code;
    }

    public List<String> getStatusDescription() {
        return status_description;
    }
}
