package com.example.productiondisplay.ui.slideshow.Invoice;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class InvoiceProductionCountResponse {

    @SerializedName("status_code")
    private String statusCode;

    @SerializedName("labels")
    private List<String> labels;

    @SerializedName("datasets")
    private List<Dataset> datasets;

    public String getStatusCode() {
        return statusCode;
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public static class Dataset {
        @SerializedName("label")
        private String label;

        @SerializedName("data")
        private List<Integer> data;

        public String getLabel() {
            return label;
        }

        public List<Integer> getData() {
            return data;
        }
    }
}

