package com.example.productiondisplay.ui.slideshow;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.productiondisplay.R;
import com.example.productiondisplay.ui.slideshow.Invoice.InvoiceChatActivity;
import com.example.productiondisplay.ui.slideshow.Production.ApiService;
import com.example.productiondisplay.ui.slideshow.Production.ModuleAdapter;
import com.example.productiondisplay.ui.slideshow.Production.ModuleResponse;
import com.example.productiondisplay.ui.slideshow.Production.ProductionChatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class SlideshowFragment extends Fragment {

    private RecyclerView moduleRecycler;
    private ModuleAdapter adapter;
    private ApiService apiService;
    private TextToSpeech textToSpeech;

    private final String AUTH_HEADER = "Bearer sb-eba140ab-74bb-44a4-8d92-70a636940def!b1182|it-rt-dev-cri-stjllphr!b68:616d8991-307b-4ab1-be37-7894a8c6db9d$0p0fE2I7w1Ve23-lVSKQF0ka3mKrTVcKPJYELr-i4nE=";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_slideshow, container, false);

        moduleRecycler = root.findViewById(R.id.recyclerViewModules);
        moduleRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize TTS
        textToSpeech = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        // Initialize adapter
        adapter = new ModuleAdapter(moduleName -> {
            Intent intent;
            String normalizedModuleName = moduleName.trim().toLowerCase();

            if (normalizedModuleName.contains("production")) {
                intent = new Intent(getContext(), ProductionChatActivity.class);
            } else if (normalizedModuleName.contains("invoice")) {
                intent = new Intent(getContext(), InvoiceChatActivity.class);
            } else {
                Toast.makeText(getContext(), "Unknown module: " + moduleName, Toast.LENGTH_SHORT).show();
                return;
            }

            // Speak module selection
            if (textToSpeech != null) {
                String msg = "You have chosen " + moduleName;
                textToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "MODULE_SELECTED_MSG");
            }

            intent.putExtra("MODULE_NAME", moduleName);
            startActivity(intent);
        });

        moduleRecycler.setAdapter(adapter);

        // Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pds.iotsignin.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();


        apiService = retrofit.create(ApiService.class);

        fetchModules();

        return root;
    }

    private void fetchModules() {
        apiService.getModules(AUTH_HEADER, "Module List").enqueue(new Callback<ModuleResponse>() {
            @Override
            public void onResponse(Call<ModuleResponse> call, Response<ModuleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Set API response as buttons
                    adapter.setModules(response.body().getStatusDescription());

                    // Welcome message
                    String userMessage = "Hello user! Welcome to C.R.I Pumps. Click on the dashboard name to see the production count";
                    if (textToSpeech != null) {
                        textToSpeech.speak(userMessage, TextToSpeech.QUEUE_FLUSH, null, "WELCOME_MSG");
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load modules", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ModuleResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error loading modules", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        String welcomeMessage = "Hello user! Welcome to C.R.I Pumps. Click on the dashboard name to see the production count.";
        if (textToSpeech != null) {
            textToSpeech.speak(welcomeMessage, TextToSpeech.QUEUE_FLUSH, null, "WELCOME_MSG");
        }
        Toast.makeText(getContext(), welcomeMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroyView();
    }
}
