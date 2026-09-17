package com.example.productiondisplay.ui.slideshow.Production;

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
    Call<ModuleResponse> getCharts(
            @Header("Authorization") String authHeader,
            @Header("module-name") String moduleName
    );

    @GET("api/get/moduleplant-name/data")
    Call<ModuleResponse> getPlants(


            @Header("Authorization") String authHeader,
            @Header("plant-name") String plantName // use "Plant List"
    );

    @GET("api/get/module-plantline-name/data")
    Call<ModuleResponse> getLines(
            @Header("Authorization") String authHeader,
            @Header("plant-name") String plantName,
            @Header("chart-name") String chartName
    );

    @GET("api/get/module-line-filter-name/data")
    Call<ModuleResponse> getDateFilters(
            @Header("Authorization") String authToken,
            @Header("line-name") String lineName
    );

    @GET("api/get/module-filter-value/data")
    Call<ProductionCountResponse> getProductionCount(
            @Header("Authorization") String authHeader,
            @Header("module-name") String moduleName,
            @Header("chart-name") String chartName,
            @Header("plant-name") String plantName,
            @Header("line-name") String lineName,
            @Header("filter-name") String filterName
    );

    // chart-2 hourly count
    @GET("api/get/module-filter-value/data")
    Call<HourlyProductionResponse> getHourlyProductionCount(
            @Header("authorization") String token,
            @Header("module-name") String moduleName,
            @Header("chart-name") String chartName,
            @Header("plant-name") String plantName,
            @Header("line-name") String lineName,
            @Header("filter-name") String filterName
    );

    // chart 3 production line stop count
    @GET("api/get/module-production-linestop/data")
    Call<LineStopResponse> getLineStopCount(
            @Header("authorization") String token,
            @Header("module-name") String moduleName,
            @Header("chart-name") String chartName,
            @Header("plant-name") String plantName,
            @Header("line-name") String lineName,
            @Header("filter-name") String filterName);

    // chart 4 production order count
    @GET("api/get/module-production-order/data")
    Call<POCResponse> getProductionOrderCount(
            @Header("authorization") String token,
            @Header("module-name") String moduleName,
            @Header("chart-name") String chartName,
            @Header("plant-name") String plantName,
            @Header("line-name") String lineName,
            @Header("production-order") String productionOrder,
            @Header("filter-name") String filterName
    );


}
