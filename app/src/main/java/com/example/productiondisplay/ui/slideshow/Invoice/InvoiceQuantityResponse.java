package com.example.productiondisplay.ui.slideshow.Invoice;

import java.util.List;

public class InvoiceQuantityResponse {

    private String status_code;
    private List<String> labels;
    private List<Dataset> datasets;

    public String getStatusCode() {
        return status_code;
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public static class Dataset {
        private String label;
        private List<Integer> data;

        public String getLabel() {
            return label;
        }

        public List<Integer> getData() {
            return data;
        }
    }
}
