package com.example.productiondisplay.ui.slideshow.Production;

import java.util.List;

public class POCResponse {
    public String status_code;
    public List<POCDataset> datasets;
    public List<String> labels; // Only present for weekly/monthly views
}