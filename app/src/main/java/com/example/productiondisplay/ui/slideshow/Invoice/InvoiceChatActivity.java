package com.example.productiondisplay.ui.slideshow.Invoice;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import com.example.productiondisplay.ui.slideshow.Production.ChatAdapter;
import com.example.productiondisplay.ui.slideshow.Production.Message;
import com.example.productiondisplay.ui.slideshow.Production.ModuleResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class InvoiceChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private EditText editInput;
    private Button sendButton;
    private Button refreshButton;
    private ImageButton micBtn;

    private boolean awaitingModuleSelection = false;
    private boolean awaitingChartSelection = false;
    private boolean awaitingPlantSelection = false;
    private boolean awaitingInvoiceTypeSelection = false;
    private boolean awaitingDateSelection = false;

    private List<String> currentModules = new ArrayList<>();
    private List<String> currentCharts = new ArrayList<>();
    private List<String> currentPlants = new ArrayList<>();
    private List<String> currentInvoiceTypes = new ArrayList<>();
    private List<String> currentDateFilters = new ArrayList<>();

    private String selectedModule;
    private String selectedChart;
    private String selectedPlant;
    private String selectedInvoiceType;
    private String selectedDate;

    private ApiService apiService;

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private TextToSpeech tts;
    private boolean isTtsInitialized = false;

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 101;
    private final String AUTH_HEADER = "Bearer sb-eba140ab-74bb-44a4-8d92-70a636940def!b1182|it-rt-dev-cri-stjllphr!b68:616d8991-307b-4ab1-be37-7894a8c6db9d$0p0fE2I7w1Ve23-lVSKQF0ka3mKrTVcKPJYELr-i4nE=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_production_chat);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String username = prefs.getString("USERNAME", ""); // default empty string if not found

        chatRecyclerView = findViewById(R.id.recyclerViewChat);
        editInput = findViewById(R.id.userInput);
        sendButton = findViewById(R.id.sendBtn);
        refreshButton = findViewById(R.id.RefreshBtn);
        micBtn = findViewById(R.id.micBtn);

        chatAdapter = new ChatAdapter(this, this::handleUserInput);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pds.iotsignin.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show();
                } else {
                    isTtsInitialized = true;
                    showGreeting(username);
                }
            } else {
                Toast.makeText(this, "TTS Initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        setupSpeechRecognizer();

        String moduleName = getIntent().getStringExtra("MODULE_NAME");

        if (moduleName != null && !moduleName.isEmpty()) {
            selectedModule = moduleName;
            fetchInvoiceCharts_NoSpeak(moduleName);
        }

        sendButton.setOnClickListener(v -> {
            String userInput = editInput.getText().toString().trim();
            if (!userInput.isEmpty()) {
                handleUserInput(userInput);
                editInput.setText("");
            }
        });

        refreshButton.setOnClickListener(v -> {
            if (selectedModule != null) {
                addBotMessage("Refreshing charts...");
                fetchInvoiceCharts_NoSpeak(selectedModule);
            } else {
                addBotMessage("Please select a module first");
            }
        });

        micBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted, listening...", Toast.LENGTH_SHORT).show();
                if (speechRecognizer != null) {
                    speechRecognizer.startListening(speechRecognizerIntent);
                }
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSIONS_REQUEST_RECORD_AUDIO);
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
            String cleanMessage = message.replaceAll("👋", "")
                    .replaceAll("[^\\p{ASCII}]", "")
                    .replaceAll("\\n", ". ")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (!cleanMessage.isEmpty()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(cleanMessage, TextToSpeech.QUEUE_ADD, null, "TTS_ID");
                } else {
                    tts.speak(cleanMessage, TextToSpeech.QUEUE_ADD, null);
                }
            }
        }
    }

    private void stopSpeech() {
        if (tts != null && isTtsInitialized) {
            tts.stop();
        }
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(InvoiceChatActivity.this, "Listening...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spoken = normalizeSpeech(matches.get(0));
                    editInput.setText(spoken);
                    handleUserInput(spoken);
                    editInput.setText("");
                }
            }

            @Override
            public void onError(int error) {
                Toast.makeText(InvoiceChatActivity.this, "Error: " + getErrorMessage(error), Toast.LENGTH_SHORT).show();
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onEndOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error.";
            case SpeechRecognizer.ERROR_CLIENT: return "Client side error.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Insufficient permissions.";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error.";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout.";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No match found.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "RecognitionService is busy.";
            case SpeechRecognizer.ERROR_SERVER: return "Server error.";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech input.";
            default: return "Unknown error occurred.";
        }
    }

    private String preprocessInput(String input) {
        if (input == null) return "";
        return input.replaceAll("\\s+", "");
    }

    private String normalizeSpeech(String input) {
        if (input == null) return "";

        input = input.toLowerCase().trim().replaceAll("[^a-z0-9]", "");

        switch (input) {
            case "1": case "one": case "won": case "on": case "uan": case "uhn": return "1";
            case "2": case "two": case "to": case "too": case "tu": return "2";
            case "3": case "three": case "tree": case "thre": case "thri": return "3";
            case "4": case "four": case "for": case "fo": case "phor": return "4";
            case "5": case "five": case "fie": case "fiv": case "faiv": return "5";
            case "6": case "six": case "siks": case "sics": case "sex": return "6";
            case "7": case "seven": case "sevan": case "sevn": case "saven": return "7";
            case "8": case "eight": case "ate": case "eit": case "aet": return "8";
            case "9": case "nine": case "nain": case "nayn": case "naen": return "9";
            case "10": case "ten": case "tin": case "then": case "tan": return "10";
            case "11": case "eleven": case "elevan": case "elvn": case "aleven": case "elavan": return "11";
            case "12": case "twelve": case "twelv": case "twel": case "tuelve": return "12";
            case "13": case "thirteen": case "thirtn": case "thertin": case "tharteen": return "13";
            case "14": case "fourteen": case "forteen": case "fourtin": case "fourtayn": return "14";
            case "15": case "fifteen": case "fiften": case "fiftn": case "fifftin": return "15";
            case "16": case "sixteen": case "sisteen": case "sixtin": case "sicksteen": return "16";
            case "17": case "seventeen": case "siventeen": case "seventin": case "sevnteen": return "17";
            case "18": case "eighteen": case "ateen": case "eittin": case "aitin": return "18";
            case "19": case "nineteen": case "naintin": case "ninetin": case "naintayn": return "19";
            case "20": case "twenty": case "twanty": case "twant": case "twentee": return "20";
            case "21": case "twentyone": case "twenty1": case "twentywon": return "21";
            case "22": case "twentytwo": case "twenty2": case "twentyto": return "22";
            case "23": case "twentythree": case "twenty3": return "23";
            case "24": case "twentyfour": case "twenty4": return "24";
            case "25": case "twentyfive": case "twenty5": return "25";
            default: return input;
        }
    }

    private void showGreeting(String username) {
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

        userInput = preprocessInput(userInput);
        userInput = normalizeSpeech(userInput);

        try {
            int index = Integer.parseInt(userInput) - 1;

            if (awaitingModuleSelection && isValidSelection(index, currentModules)) {
                selectedModule = currentModules.get(index);
                awaitingModuleSelection = false;
                stopSpeech();
                addBotMessage("Loading charts for " + selectedModule + "...");
                fetchInvoiceCharts_NoSpeak(selectedModule);

            } else if (awaitingChartSelection && isValidSelection(index, currentCharts)) {
                selectedChart = currentCharts.get(index);
                awaitingChartSelection = false;
                stopSpeech();
                addBotMessage("You selected chart: " + selectedChart);
                //addBotMessage("Select the plant");
                fetchPlantList_NoSpeak();

            } else if (awaitingPlantSelection && isValidSelection(index, currentPlants)) {
                selectedPlant = currentPlants.get(index);
                awaitingPlantSelection = false;
                stopSpeech();
                addBotMessage("You selected plant: " + selectedPlant);
                //addBotMessage("Select the invoice type");
                fetchInvoiceTypeList_NoSpeak();

            } else if (awaitingInvoiceTypeSelection && isValidSelection(index, currentInvoiceTypes)) {
                selectedInvoiceType = currentInvoiceTypes.get(index);
                awaitingInvoiceTypeSelection = false;
                stopSpeech();
                addBotMessage("You selected invoice type: " + selectedInvoiceType);
                //addBotMessage("select the date to see the production count");
                fetchDateFiltersForInvoice_NoSpeak(selectedChart);

            } else if (awaitingDateSelection && isValidSelection(index, currentDateFilters)) {
                selectedDate = currentDateFilters.get(index);
                awaitingDateSelection = false;
                stopSpeech();
                addBotMessage("You selected date filter: " + selectedDate);
                if ("Invoice".equalsIgnoreCase(selectedChart)) {
                    fetchProductionCountForInvoice(selectedModule, selectedChart, selectedPlant, selectedInvoiceType, selectedDate);
                } else if ("Invoice Quantity".equalsIgnoreCase(selectedChart)) {
                    fetchInvoiceQuantityChart2(selectedPlant, selectedInvoiceType, selectedDate);
                } else {
                    addBotMessage("Unknown chart selected.");
                }

            } else {
                addBotMessage("Please respond with a valid number.");
            }
        } catch (NumberFormatException e) {
            addBotMessage("Please enter a number to select.");
        }
    }

    private boolean isValidSelection(int index, List<String> list) {
        return list != null && index >= 0 && index < list.size();
    }

    private void fetchInvoiceCharts_NoSpeak(String moduleName) {
        apiService.getInvoiceCharts(AUTH_HEADER, moduleName).enqueue(new Callback<ModuleResponse>() {
            @Override
            public void onResponse(Call<ModuleResponse> call, Response<ModuleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentCharts = response.body().getStatusDescription();
                    awaitingChartSelection = true;
                    //addBotMessage("Select a chart");
                    showSelectableOptions_NoSpeak("Please select a chart:", currentCharts);
                } else {
                    addBotMessage("Failed to fetch charts.");
                }
            }

            @Override
            public void onFailure(Call<ModuleResponse> call, Throwable t) {
                addBotMessage("Network error while fetching charts.");
            }
        });
    }

    private void fetchPlantList_NoSpeak() {
        apiService.getInvoicePlants(AUTH_HEADER, "Plant List").enqueue(new Callback<ModuleResponse>() {
            @Override
            public void onResponse(Call<ModuleResponse> call, Response<ModuleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentPlants = response.body().getStatusDescription();
                    awaitingPlantSelection = true;
                    showSelectableOptions_NoSpeak("Select a plant:", currentPlants);
                } else {
                    addBotMessage("Failed to fetch plants.");
                }
            }

            @Override
            public void onFailure(Call<ModuleResponse> call, Throwable t) {
                addBotMessage("Network error while fetching plants.");
            }
        });
    }

    private void fetchInvoiceTypeList_NoSpeak() {
        apiService.getInvoiceTypes(AUTH_HEADER, "Invoice Type List").enqueue(new Callback<ModuleResponse>() {
            @Override
            public void onResponse(Call<ModuleResponse> call, Response<ModuleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentInvoiceTypes = response.body().getStatusDescription();
                    awaitingInvoiceTypeSelection = true;
                    showSelectableOptions_NoSpeak("Select an invoice type:", currentInvoiceTypes);
                } else {
                    addBotMessage("Failed to fetch invoice types.");
                }
            }

            @Override
            public void onFailure(Call<ModuleResponse> call, Throwable t) {
                addBotMessage("Network error while fetching invoice types.");
            }
        });
    }

    private void fetchDateFiltersForInvoice_NoSpeak(String invoiceName) {
        apiService.getModuleInvoiceFilters(AUTH_HEADER, "Filter List").enqueue(new Callback<ModuleResponse>() {
            @Override
            public void onResponse(Call<ModuleResponse> call, Response<ModuleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentDateFilters = response.body().getStatusDescription();
                    if (!currentDateFilters.isEmpty()) {
                        awaitingDateSelection = true;
                        showSelectableOptions_NoSpeak("Select a date filter:", currentDateFilters);
                    } else {
                        addBotMessage("No date filters available.");
                    }
                } else {
                    addBotMessage("Failed to get date filters.");
                }
            }

            @Override
            public void onFailure(Call<ModuleResponse> call, Throwable t) {
                addBotMessage("Network error while fetching filters.");
            }
        });
    }

    private void showSelectableOptions_NoSpeak(String title, List<String> options) {
        StringBuilder sb = new StringBuilder(title).append("\n");
        for (int i = 0; i < options.size(); i++) {
            sb.append(i + 1).append(". ").append(options.get(i)).append("\n");
        }
        chatAdapter.addMessage(new Message(sb.toString(), true, null));
        scrollToBottom();
    }
    private void fetchProductionCountForInvoice(String module, String chart, String plant, String invoice, String filter) {
       //addBotMessage("Fetching production count...");
        scrollToBottom();

        apiService.getInvoiceProductionCount(AUTH_HEADER, plant, invoice, filter)
                .enqueue(new Callback<InvoiceProductionCountResponse>() {
                    @Override
                    public void onResponse(Call<InvoiceProductionCountResponse> call, Response<InvoiceProductionCountResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            InvoiceProductionCountResponse data = response.body();
                            StringBuilder message = new StringBuilder("📊 Invoice Production Count:\n\n");
                            List<String> labels = data.getLabels();
                            for (InvoiceProductionCountResponse.Dataset set : data.getDatasets()) {
                                message.append("🔹 ").append(set.getLabel()).append(":\n");
                                for (int i = 0; i < labels.size(); i++) {
                                    message.append("   • ").append(labels.get(i)).append(": ")
                                            .append(set.getData().get(i)).append("\n");
                                }
                                message.append("\n");
                            }
                            chatAdapter.addMessage(new Message(message.toString(), true, null));
                            scrollToBottom();

                            Message chartPrompt = new Message(
                                    "✅ Production order count fetched.\n📊 Please select the type of chart you'd like to view",
                                    true,
                                    null
                            );
                            chatAdapter.addMessage(chartPrompt);
                            scrollToBottom();

                        } else {
                            addBotMessage("⚠️ Failed to load production count data.");
                            chatAdapter.addMessage(new Message("⚠️ Failed to load production count data.", true, null));
                            scrollToBottom();
                        }
                        fetchInvoiceCharts_NoSpeak(selectedModule);
                    }

                    @Override
                    public void onFailure(Call<InvoiceProductionCountResponse> call, Throwable t) {
                        chatAdapter.addMessage(new Message("❌ Error: " + t.getMessage(), true, null));
                        scrollToBottom();
                        //resetChatbotState();
                    }
                });
    }

    private void fetchInvoiceQuantityChart2(String plant, String invoiceType, String filter) {
     //addBotMessage("Fetching invoice quantity data...");
        scrollToBottom();

        apiService.getInvoiceQuantityData(AUTH_HEADER, plant, invoiceType, filter).enqueue(new Callback<InvoiceQuantityResponse>() {
            @Override
            public void onResponse(Call<InvoiceQuantityResponse> call, Response<InvoiceQuantityResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    InvoiceQuantityResponse data = response.body();
                    StringBuilder builder = new StringBuilder("📦 Invoice Quantities:\n");

                    List<String> labels = data.getLabels();
                    for (InvoiceQuantityResponse.Dataset set : data.getDatasets()) {
                        builder.append("🔹 ").append(set.getLabel()).append("\n");
                        for (int i = 0; i < labels.size(); i++) {
                            builder.append("  ↳ ").append(labels.get(i)).append(": ")
                                    .append(set.getData().get(i)).append("\n");
                        }
                    }
                    chatAdapter.addMessage(new Message(builder.toString(), true, null));
                    scrollToBottom();

                    Message chartPrompt = new Message(
                            "✅ Invoice quantity data fetched.\n📊 Please select the type of chart you'd like to view",
                            true,
                            null
                    );
                    chatAdapter.addMessage(chartPrompt);
                    scrollToBottom();

                } else {
                    addBotMessage("No data found.");
                    chatAdapter.addMessage(new Message("No data found.", true, null));
                    scrollToBottom();
                }
                fetchInvoiceCharts_NoSpeak(selectedModule);
            }

            @Override
            public void onFailure(Call<InvoiceQuantityResponse> call, Throwable t) {
                chatAdapter.addMessage(new Message("Error getting chart data: " + t.getMessage(), true, null));
                scrollToBottom();
                //resetChatbotState();
            }
        });
    }


    private void scrollToBottom() {
        if (chatAdapter != null && chatRecyclerView != null) {
            chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
