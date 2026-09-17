package com.example.productiondisplay.ui.slideshow.Invoice;

import com.example.productiondisplay.ui.slideshow.Production.ModuleResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface ApiService {

    @GET("api/get/module-name/data")
    Call<ModuleResponse> getModules(
            @Header("Authorization") String authHeader,
            @Header("module-name") String moduleName
    );

    @GET("api/get/modulechart-name/data")
    Call<ModuleResponse> getInvoiceCharts(
            @Header("Authorization") String authHeader,
            @Header("module-name") String moduleName
    );

    @GET("api/get/moduleplant-name/data")
    Call<ModuleResponse> getInvoicePlants(
            @Header("Authorization") String authHeader,
            @Header("plant-name") String plantHeader
    );

    @GET("api/get/module-invoice-type/data")
    Call<ModuleResponse> getInvoiceTypes(
            @Header("Authorization") String authHeader,
            @Header("invoice-type-list") String typeHeader
    );

    //date filter
    @GET("api/get/module-invoice-filter/data")
    Call<ModuleResponse> getModuleInvoiceFilters(
            @Header("Authorization") String authToken,
            @Header("filter-name") String filterName
    );

    @GET("api/get/module-invoice-count/data")
    Call<InvoiceProductionCountResponse> getInvoiceProductionCount(
            @Header("Authorization") String authHeader,
            @Header("plant-name") String plantName,
            @Header("invoice-name") String invoiceName,
            @Header("filter-name") String filterName
    );

    @GET("api/get/module-invoice-count/data")
    Call<InvoiceQuantityResponse> getInvoiceQuantityData(
            @Header("Authorization") String authHeader,
            @Header("plant-name") String plantName,
            @Header("invoice-name") String invoiceName,
            @Header("filter-name") String filterName
    );

}
