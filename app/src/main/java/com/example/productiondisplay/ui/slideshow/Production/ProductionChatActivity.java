package com.example.productiondisplay.ui.slideshow.Production;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.productiondisplay.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProductionChatActivity extends AppCompatActivity {

    private RecyclerView chatRecycler;
    private ChatAdapter chatAdapter;
    private EditText editInput;
    private Button sendButton;
    private Button refreshButton;
    private ImageButton micBtn;

    private List<String> currentModules;
    private List<String> currentCharts;
    private List<String> currentPlants;
    private List<String> currentLines;
    private List<String> currentDateFilters;

    private boolean awaitingModuleSelection = false;
    private boolean awaitingChartSelection = false;
    private boolean awaitingPlantSelection = false;
    private boolean awaitingLineSelection = false;
    private boolean awaitingDateSelection = false;
    private boolean awaitingProductionOrder = false;

    private String selectedProductionOrder = null;
    private String selectedModule;
    private String selectedChart;
    private String selectedPlant;
    private String selectedLine;
    private String selectedDate;
    private ApiService apiService;

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 100;
    private TextToSpeech tts;
    private boolean isTtsInitialized = false;
    private final String AUTH_HEADER = "Bearer sb-eba140ab-74bb-44a4-8d92-70a636940def!b1182|it-rt-dev-cri-stjllphr!b68:616d8991-307b-4ab1-be37-7894a8c6db9d$0p0fE2I7w1Ve23-lVSKQF0ka3mKrTVcKPJYELr-i4nE=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_production_chat);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String username = prefs.getString("USERNAME", ""); // default empty string if not found

        chatRecycler = findViewById(R.id.recyclerViewChat);
        editInput = findViewById(R.id.userInput);
        sendButton = findViewById(R.id.sendBtn);
        refreshButton = findViewById(R.id.RefreshBtn);
        micBtn = findViewById(R.id.micBtn);

        chatAdapter = new ChatAdapter(this, new ChatAdapter.OnOptionClickListener() {
            @Override
            public void onOptionClicked(String input) {
                handleUserInput(input);
            }
        });

        chatRecycler.setLayoutManager(new LinearLayoutManager(this));
        chatRecycler.setAdapter(chatAdapter);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pds.iotsignin.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // Initialize TTS properly
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show();
                } else {
                    isTtsInitialized = true;
                    displayGreeting(username);
                }
            } else {
                Toast.makeText(this, "TTS Initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup speech recognizer with multiple English accents
        String[] languageTags = {"en_US", "en_GB", "en_IN", "en_AU"};
        for (String lang : languageTags) {
            setupSpeechRecognizer(lang);
        }

        String moduleName = getIntent().getStringExtra("MODULE_NAME");

        if (moduleName != null && !moduleName.isEmpty()) {
            selectedModule = moduleName;
            fetchChartsForModule(moduleName);
        } else {
        }

        // Send button click listener
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userInput = editInput.getText().toString().trim();
                if (!userInput.isEmpty()) {
                    handleUserInput(userInput);
                    editInput.setText("");
                }
            }
        });

        refreshButton.setOnClickListener(v -> {
            if (selectedModule != null) {
                Log.d("RefreshBtn", "Refreshing charts for module: " + selectedModule);
                addBotMessage("Refreshing charts...");
                fetchChartsForModule(selectedModule);
            } else {
                Log.d("RefreshBtn", "selectedModule is null!");
                addBotMessage("Please select a chart first");
            }
        });


        micBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "permission granted", Toast.LENGTH_SHORT).show();
                speechRecognizer.startListening(speechRecognizerIntent);
                editInput.setText("");

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSIONS_REQUEST_RECORD_AUDIO);
                editInput.setText("");
            }
        });
    }

    private void addBotMessage(String message) {
        chatAdapter.addMessage(new Message(message, true, null));
        scrollToBottom();
        speakBotMessage(message);
    }

    private void speakBotMessage(String message) {
        if (isTtsInitialized && tts != null) {
            // Clean up message for TTS
            String cleanMessage = message.replaceAll("👋", "")  // Remove emojis
                    .replaceAll("[^\\p{ASCII}]", "")  // Remove non-ASCII characters
                    .replaceAll("\\n", ", ")  // Replace newlines with commas
                    .replaceAll("\\s+", " ")  // Collapse multiple spaces
                    .trim();

            // Skip speaking if message is too long or contains lists
            if (!cleanMessage.isEmpty() && cleanMessage.length() < 100 && !cleanMessage.contains(":")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(cleanMessage, TextToSpeech.QUEUE_ADD, null, "tts1");
                } else {
                    tts.speak(cleanMessage, TextToSpeech.QUEUE_ADD, null);
                }
            }
        }
    }

    private void setupSpeechRecognizer(String lang) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            private Bundle partialResults;

            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(ProductionChatActivity.this, "Listening...", Toast.LENGTH_SHORT).show();
            }

            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spoken = normalizeSpeech(matches.get(0));
                    editInput.setText(spoken);
                    handleUserInput(spoken);
                    editInput.setText("");
                }
            }

            @Override public void onError(int error) {
                Toast.makeText(ProductionChatActivity.this, "Error: " + getErrorMessage(error), Toast.LENGTH_SHORT).show();
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onEndOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}

            public void onPartialResults() {
                onPartialResults((Bundle) null);
            }

            @Override public void onPartialResults(Bundle partialResults) {
                this.partialResults = partialResults;
            }
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        if (speechRecognizer != null) {
            speechRecognizer.startListening(speechRecognizerIntent);
        }
    }

    private void speak(String text) {
        if (tts != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1");
            } else {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        } else {
            Toast.makeText(this, "TTS not initialized", Toast.LENGTH_SHORT).show();
        }
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error.";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions.";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error.";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout.";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No match found.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "RecognitionService is busy.";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error.";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input.";
            default:
                return "Unknown error occurred.";
        }
    }

    private String  preprocessInput(String input) {
        // Remove spaces inside letters in case of spelled out words like "o n e"
        return input.replaceAll("\\s+", "");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted! You can use voice input now.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied. Voice input disabled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void displayGreeting(String username) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour < 12) greeting = "Good morning";
        else if (hour >= 12 && hour < 16) greeting = "Good afternoon";
        else greeting = "Good evening";
        addBotMessage(greeting + ", " + username + " 👋");
    }
    private void handleUserInput(String userInput) {
        chatAdapter.addMessage(new Message(userInput, false, null));
        scrollToBottom();

        if (userInput == null || userInput.trim().isEmpty()) {
            addBotMessage("Input cannot be empty. Please try again.");
            return;
        }

        // Preprocess and normalize input
        userInput = preprocessInput(userInput);
        userInput = normalizeSpeech(userInput);

        // 1. Module selection
        if (awaitingModuleSelection && currentModules != null) {
            try {
                int index = Integer.parseInt(userInput) - 1;
                if (index >= 0 && index < currentModules.size()) {
                    selectedModule = currentModules.get(index);
                    awaitingModuleSelection = false;
                    awaitingChartSelection = true;
                    addBotMessage("You selected module: " + selectedModule);
                    fetchChartsForModule(selectedModule);

                } else {
                    addBotMessage("Invalid selection. Use the numbers shown above.");
                }
            } catch (NumberFormatException e) {
                addBotMessage("Please enter a number for module selection.");
            }
            return;
        }

        // 2. Chart selection
        if (awaitingChartSelection && currentCharts != null) {
            try {
                int index = Integer.parseInt(userInput) - 1;
                if (index >= 0 && index < currentCharts.size()) {
                    selectedChart = currentCharts.get(index);
                    awaitingChartSelection = false;
                    awaitingPlantSelection = true;
                    addBotMessage("You selected chart: " + selectedChart);
                    fetchPlantList();

                } else {
                    addBotMessage("Invalid chart selection. Use the numbers shown above.");
                }
            } catch (NumberFormatException e) {
                addBotMessage("Please enter a number for chart selection.");
            }
            return;
        }

            // 3. Plant selection
            if (awaitingPlantSelection && currentPlants != null) {
                try {
                    int index = Integer.parseInt(userInput) - 1;
                    if (index >= 0 && index < currentPlants.size()) {
                        selectedPlant = currentPlants.get(index);
                        awaitingPlantSelection = false;
                        awaitingLineSelection = true;
                        addBotMessage("You selected plant: " + selectedPlant);
                        fetchLineList(selectedPlant, selectedChart);

                } else {
                    addBotMessage("Invalid plant selection.");
                }
            } catch (NumberFormatException e) {
                addBotMessage("Please enter a number for plant selection.");
            }
            return;
        }

        // 4. Line selection
        if (awaitingLineSelection && currentLines != null) {
            try {
                int index = Integer.parseInt(userInput) - 1;
                if (index >= 0 && index < currentLines.size()) {
                    selectedLine = currentLines.get(index);
                    awaitingLineSelection = false;
                    addBotMessage("You selected line: " + selectedLine);

                    if ("Production Order Count".equalsIgnoreCase(normalizeSpaces(selectedChart))) {
                        requestProductionOrderNumber();
                    } else {
                        fetchDateFilters(selectedLine);
                    }

                } else {
                    addBotMessage("Invalid line selection.");
                }
            } catch (NumberFormatException e) {
                addBotMessage("Please enter a number for line selection.");
            }
            return;
        }

        // 4.1 Production Order entry (only for "Production Order Count")
        if (awaitingProductionOrder) {
            if (!userInput.isEmpty()) {
                selectedProductionOrder = userInput;
                awaitingProductionOrder = false;

                addBotMessage("Production Order Number you entered: " + selectedProductionOrder);
                fetchDateFilters(selectedLine);

            } else {
                addBotMessage("Production Order number cannot be empty. Please enter a valid number.");
            }
            return;
        }

        // 5. Date filter selection (for all charts)
        if (awaitingDateSelection && currentDateFilters != null) {
            try {
                int index = Integer.parseInt(userInput) - 1;
                if (index >= 0 && index < currentDateFilters.size()) {
                    selectedDate = currentDateFilters.get(index);
                    awaitingDateSelection = false;
                    addBotMessage("You Selected date: " + selectedDate);

                    if ("Production Hourly Count".equalsIgnoreCase(selectedChart)) {
                        fetchProductionHourlyCount(selectedModule, selectedChart, selectedPlant, selectedLine, selectedDate);
                    } else if ("Production Line Stop Count".equalsIgnoreCase(selectedChart)
                            || "Production Line Stop".equalsIgnoreCase(selectedChart)) {
                        fetchProductionLineStopCount( selectedModule,selectedChart,selectedPlant, selectedLine, selectedDate);
                    } else if ("Production Order Count".equalsIgnoreCase(normalizeSpaces(selectedChart))) {
                        if (selectedProductionOrder != null && !selectedProductionOrder.trim().isEmpty()) {
                            fetchProductionOrderCount(
                                    selectedModule,
                                    selectedChart,
                                    selectedPlant,
                                    selectedLine,
                                    selectedProductionOrder,
                                    selectedDate
                            );
                        } else {
                            addBotMessage("❗ Production Order number is missing. Please enter it to continue.");
                            awaitingProductionOrder = true;
                            addBotMessage("Enter Production Order Number:");
                        }
                    } else {
                        fetchProductionCount(selectedModule, selectedChart, selectedPlant, selectedLine, selectedDate);
                    }

                } else {
                    addBotMessage("Invalid selection for date filter.");
                }
            } catch (NumberFormatException e) {
                addBotMessage("Please enter a number for date filter selection.");
            }
            return;
        }
    }

    private void scrollToBottom() {
        if (chatAdapter != null && chatRecycler != null) {
            chatRecycler.scrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    private void fetchChartsForModule(String moduleName) {
        apiService.getCharts(AUTH_HEADER, moduleName).enqueue(new Callback<ModuleResponse>() {
            @Override
            public void onResponse(Call<ModuleResponse> call, Response<ModuleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentCharts = response.body().getStatusDescription();
                    awaitingChartSelection = true;

                    StringBuilder sb = new StringBuilder("Select a chart:\n");
                    for (int i = 0; i < currentCharts.size(); i++) {
                        sb.append(i + 1).append(" - ").append(currentCharts.get(i)).append("\n");
                    }
                    addBotMessage(sb.toString());
                } else {
                    addBotMessage("Chart fetching failed.");
                }
            }

            @Override
            public void onFailure(Call<ModuleResponse> call, Throwable t) {
                addBotMessage("Network error during chart fetch.");
                addBotMessage("Select a chart to see the production count.");
            }
        });
    }

    private void fetchPlantList() {
        addBotMessage("Choose plants");

        apiService.getPlants(AUTH_HEADER, "Plant List").enqueue(new Callback<ModuleResponse>() {
            @Override
            public void onResponse(Call<ModuleResponse> call, Response<ModuleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentPlants = response.body().getStatusDescription();
                    awaitingPlantSelection = true;

                    StringBuilder sb = new StringBuilder("Select a plant:\n");
                    for (int i = 0; i < currentPlants.size(); i++) {
                        sb.append(i + 1).append(" - ").append(currentPlants.get(i)).append("\n");
                    }
                    addBotMessage(sb.toString());
                } else {
                    addBotMessage("Could not fetch plants.");
                }
            }

            @Override
            public void onFailure(Call<ModuleResponse> call, Throwable t) {
                addBotMessage("Network error fetching plants.");
            }
        });
    }

    private void fetchLineList(String plant, String chart) {
        addBotMessage("Choose line for plant");

        apiService.getLines(AUTH_HEADER, plant, chart).enqueue(new Callback<ModuleResponse>() {
            @Override
            public void onResponse(Call<ModuleResponse> call, Response<ModuleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentLines = response.body().getStatusDescription();
                    awaitingLineSelection = true;

                    StringBuilder sb = new StringBuilder("Select a line:\n");
                    for (int i = 0; i < currentLines.size(); i++) {
                        sb.append(i + 1).append(" - ").append(currentLines.get(i)).append("\n");
                    }
                    addBotMessage(sb.toString());
                } else {
                    addBotMessage("No Lines found");
                }
            }

            @Override
            public void onFailure(Call<ModuleResponse> call, Throwable t) {
                addBotMessage("Network failure while fetching lines.");
            }
        });
    }

    private void fetchDateFilters(String lineName) {
        addBotMessage("Choose the date filter");

        apiService.getDateFilters(AUTH_HEADER, lineName).enqueue(new Callback<ModuleResponse>() {
            @Override
            public void onResponse(Call<ModuleResponse> call, Response<ModuleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentDateFilters = response.body().getStatusDescription();
                    awaitingDateSelection = true;

                    StringBuilder sb = new StringBuilder("Choose a date filter:\n");
                    for (int i = 0; i < currentDateFilters.size(); i++) {
                        sb.append(i + 1).append(" - ").append(currentDateFilters.get(i)).append("\n");
                    }
                    addBotMessage(sb.toString());
                } else {
                    addBotMessage("No date filters found");
                }
            }

            @Override
            public void onFailure(Call<ModuleResponse> call, Throwable t) {
                addBotMessage("Network error fetching date filters.");
            }
        });
    }

    private void fetchProductionCount(String module, String chart, String plant, String line, String date) {
        //addBotMessage("Fetching production count...");

        apiService.getProductionCount(AUTH_HEADER, module, chart, plant, line, date)
                .enqueue(new Callback<ProductionCountResponse>() {
                    @Override
                    public void onResponse(Call<ProductionCountResponse> call, Response<ProductionCountResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Object result = response.body().getStatus_description();
                            StringBuilder sb = new StringBuilder();

                            if (result instanceof Map) {
                                sb.append("Production Counts:\n");
                                Map<String, Double> results = (Map<String, Double>) result;
                                for (Map.Entry<String, Double> entry : results.entrySet()) {
                                    sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                                }
                            } else {
                                sb.append("Production Count: ").append(result.toString());
                            }

                            addBotMessage(sb.toString());

                            // Show prompt message after production count is fetched
                            addBotMessage("✅ Production count fetched.\n📊 Please select the type of chart you'd like to view");
                        } else {
                            addBotMessage("Error fetching production count.");
                        }
                        // Now load and display charts for user to select next
                        fetchChartsForModule(selectedModule);
                    }

                    @Override
                    public void onFailure(Call<ProductionCountResponse> call, Throwable t) {
                        addBotMessage("Network error while fetching production count.");
                        resetChatbotState();
                    }
                });
    }

    private void fetchProductionHourlyCount(
            String module, String chart, String plant, String line, String filter) {

        apiService.getHourlyProductionCount(
                AUTH_HEADER,
                module,
                chart,
                plant,
                line,
                filter
        ).enqueue(new Callback<HourlyProductionResponse>() {
            @Override
            public void onResponse(Call<HourlyProductionResponse> call, Response<HourlyProductionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    HourlyProductionResponse hr = response.body();
                    StringBuilder sb = new StringBuilder();
                    sb.append("Chart Name: ").append(chart).append("\n");
                    sb.append("Filter: ").append(filter).append("\n\n");

                    if (hr.datasets != null && !hr.datasets.isEmpty()) {
                        HourlyDataset d = hr.datasets.get(0);
                        sb.append(d.label).append(":\n\n");

                        List<String> labels = hr.labels;
                        List<Integer> data = d.data;

                        for (int i = 0; i < labels.size(); i++) {
                            sb.append(labels.get(i)).append(" : ")
                                    .append(i < data.size() ? data.get(i) : "-")
                                    .append("\n");
                        }
                    } else {
                        sb.append("No data available.");
                    }

                    addBotMessage(sb.toString());

                    // Add your new prompt message here:
                    addBotMessage("✅ Hourly production data fetched.\n📊 Please select the type of chart you'd like to view");

                } else {
                    addBotMessage("Failed to fetch hourly production data.");
                }

                // Automatically load charts for same module instead of resetting
                fetchChartsForModule(selectedModule);
            }

            @Override
            public void onFailure(Call<HourlyProductionResponse> call, Throwable t) {
                addBotMessage("Network error while fetching hourly production.");
                resetChatbotState();
            }
        });
    }

    // chart 3 production line stop count
    private void fetchProductionLineStopCount(
            String module, String chart, String plant, String line, String filter) {
        Log.d("fetchProductionLineStopCount", "Params - module: " + module + ", chart: " + chart
                + ", plant: " + plant + ", line: " + line + ", filter: " + filter);

        apiService.getLineStopCount(
                AUTH_HEADER,
                module,
                chart,
                plant,
                line,
                filter
        ).enqueue(new Callback<LineStopResponse>() {
            @Override
            public void onResponse(Call<LineStopResponse> call, Response<LineStopResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LineStopResponse result = response.body();
                    StringBuilder sb = new StringBuilder();

                    sb.append("📊 Chart Name: ").append(chart).append("\n");
                    sb.append("🗓 Filter: ").append(filter).append("\n\n");

                    if (result.labels != null && result.data != null && result.labels.size() == result.data.size()) {
                        for (int i = 0; i < result.labels.size(); i++) {
                            sb.append("• ").append(result.labels.get(i))
                                    .append(" : ").append(result.data.get(i)).append(" hrs\n");
                        }

                    } else {
                        sb.append("No line stop data available.");
                        fetchChartsForModule(selectedModule);
                    }

                    addBotMessage(sb.toString());

                    addBotMessage("✅ Line stop data fetched.\n📊 Please select the type of chart you'd like to view");


                } else {
                    String errorMsg = "Failed to fetch line stop data. Response: "
                            + response.code() + " - " + response.message();
                    Log.e("LineStopAPI", errorMsg);
                    addBotMessage(errorMsg);
                }

                fetchChartsForModule(selectedModule);
            }

            @Override
            public void onFailure(Call<LineStopResponse> call, Throwable t) {
                Log.e("LineStopAPI", "Network failure: " + t.getMessage(), t);
                addBotMessage("🌐 Network error: " + t.getMessage());
                resetChatbotState();
                fetchChartsForModule(selectedModule);
            }
        });
    }

    public void onResults(Bundle results) {
        List<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String recognizedText = matches.get(0);
            recognizedText = preprocessInput(recognizedText);
            recognizedText = normalizeSpeech(recognizedText);
            editInput.setText(recognizedText);
            handleUserInput(recognizedText);
            editInput.setText("");
        }
    }

    private String normalizeSpeech(String input) {
        if (input == null) return "";

        input = input.toLowerCase().trim().replaceAll("[^a-z0-9]", "");

        switch (input) {
            case "1": case "one": case "won": case "on": case "uan": case "uhn":
                return "1";

            case "2": case "two": case "to": case "too": case "tu":
                return "2";

            case "3": case "three": case "tree": case "thre": case "thri":
                return "3";

            case "4": case "four": case "for": case "fo": case "phor":
                return "4";

            case "5": case "five": case "fie": case "fiv": case "faiv":
                return "5";

            case "6": case "six": case "siks": case "sics": case "sex":
                return "6";

            case "7": case "seven": case "sevan": case "sevn": case "saven":
                return "7";

            case "8": case "eight": case "ate": case "eit": case "aet":
                return "8";

            case "9": case "nine": case "nain": case "nayn": case "naen":
                return "9";

            case "10": case "ten": case "tin": case "then": case "tan":
                return "10";

            case "11": case "eleven": case "elevan": case "elvn": case "aleven": case "elavan":
                return "11";

            case "12": case "twelve": case "twelv": case "twel": case "tuelve":
                return "12";

            case "13": case "thirteen": case "thirtn": case "thertin": case "tharteen":
                return "13";

            case "14": case "fourteen": case "forteen": case "fourtin": case "fourtayn":
                return "14";

            case "15": case "fifteen": case "fiften": case "fiftn": case "fifftin":
                return "15";

            case "16": case "sixteen": case "sisteen": case "sixtin": case "sicksteen":
                return "16";

            case "17": case "seventeen": case "siventeen": case "seventin": case "sevnteen":
                return "17";

            case "18": case "eighteen": case "ateen": case "eittin": case "aitin":
                return "18";

            case "19": case "nineteen": case "naintin": case "ninetin": case "naintayn":
                return "19";

            case "20": case "twenty": case "twanty": case "twant": case "twentee":
                return "20";

            case "21": case "twentyone": case "twenty1": case "twentywon":
                return "21";

            case "22": case "twentytwo": case "twenty2": case "twentyto":
                return "22";

            case "23": case "twentythree": case "twenty3":
                return "23";

            case "24": case "twentyfour": case "twenty4":
                return "24";

            case "25": case "twentyfive": case "twenty5":
                return "25";

            default:
                return input;
        }
    }

    private void requestProductionOrderNumber() {
        awaitingProductionOrder = true;
        addBotMessage("Please enter the Production Order Number:");
    }

    public static String normalizeSpaces(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }

    // chart -4 production order count
    private void fetchProductionOrderCount(
            String module, String chart, String plant, String line, String productionOrder, String filter) {

        apiService.getProductionOrderCount(
                AUTH_HEADER, module, chart, plant, line, productionOrder, filter
        ).enqueue(new Callback<POCResponse>() {
            @Override
            public void onResponse(Call<POCResponse> call, Response<POCResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    POCResponse res = response.body();
                    StringBuilder sb = new StringBuilder();

                    if (res.datasets != null && !res.datasets.isEmpty()) {
                        POCDataset ds = res.datasets.get(0);
                        JsonElement element = ds.data;

                        if (element != null) {
                            if (element.isJsonObject()) {
                                JsonObject obj = element.getAsJsonObject();
                                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                                    sb.append(" ").append(entry.getKey())
                                            .append(" : ").append(entry.getValue().getAsInt()).append("\n");
                                }
                            } else if (element.isJsonArray()) {
                                JsonArray arr = element.getAsJsonArray();
                                if (res.labels != null && res.labels.size() == arr.size()) {
                                    for (int i = 0; i < arr.size(); i++) {
                                        sb.append(" ").append(res.labels.get(i))
                                                .append(" : ").append(arr.get(i).getAsInt()).append("\n");
                                    }
                                } else {
                                    for (int i = 0; i < arr.size(); i++) {
                                        sb.append("📦Data ").append(i + 1)
                                                .append(" : ").append(arr.get(i).getAsInt()).append("\n");
                                    }
                                }
                            } else if (element.isJsonPrimitive()) {
                                sb.append("📦 Count: ").append(element.getAsInt()).append("\n");
                            } else {
                                sb.append("Unknown data format");
                            }
                        } else {
                            sb.append("No data found");
                        }
                    } else {
                        sb.append("No dataset available");
                    }

                    // Add result as bot message
                    addBotMessage(sb.toString());

                    // Now reset state and fetch chart
                    resetChatbotState();
                    fetchChartsForModule(selectedModule);

                } else {
                    addBotMessage("Server error: " + response.code());
                    resetChatbotState();
                    fetchChartsForModule(selectedModule);
                }
            }

            @Override
            public void onFailure(Call<POCResponse> call, Throwable t) {
                addBotMessage("Network error: " + t.getMessage());
                resetChatbotState();
                fetchChartsForModule(selectedModule);
            }
        });
    }

    private void resetChatbotState() {
        currentCharts = null;
        currentPlants = null;
        currentLines = null;
        currentDateFilters = null;

        selectedChart = null;
        selectedPlant = null;
        selectedLine = null;
        selectedDate = null;

        awaitingChartSelection = false;
        awaitingPlantSelection = false;
        awaitingLineSelection = false;
        awaitingDateSelection = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Shutdown TextToSpeech
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        // Shutdown SpeechRecognizer
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}