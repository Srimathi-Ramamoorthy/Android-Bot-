package com.example.productiondisplay.ui.slideshow.Invoice;


import com.google.gson.annotations.SerializedName;

public class ChartData {

    @SerializedName("chart_title")
    private String chartTitle;

    @SerializedName("chart_value")
    private String chartValue;

    // Constructor
    public ChartData(String chartTitle, String chartValue) {
        this.chartTitle = chartTitle;
        this.chartValue = chartValue;
    }

    // Getter for chart title
    public String getChartTitle() {
        return chartTitle;
    }

    // Setter for chart title
    public void setChartTitle(String chartTitle) {
        this.chartTitle = chartTitle;
    }

    // Getter for chart value
    public String getChartValue() {
        return chartValue;
    }

    // Setter for chart value
    public void setChartValue(String chartValue) {
        this.chartValue = chartValue;
    }
}
