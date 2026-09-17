package com.example.productiondisplay.ui.slideshow.Production;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LineStopResponse {
    @SerializedName("status_code")
    public String status_code;

    @SerializedName("labels")
    public List<String> labels;

    @SerializedName("data")
    public List<Double> data;
}
