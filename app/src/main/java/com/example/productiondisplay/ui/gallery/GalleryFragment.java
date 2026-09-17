package com.example.productiondisplay.ui.gallery;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.*;
import android.webkit.*;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.productiondisplay.R;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.*;
import java.io.IOException;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends Fragment {

    private LinearLayout headerLayout, headerContainer;
    private ImageView dragHandle;
    private ViewFlipper tabFlipper;
    private Spinner tabSelector;

    private final List<WebView> webViewTabs = new ArrayList<>();
    private final List<String> tabTitles = new ArrayList<>();
    private ActivityResultLauncher<Intent> saveFileLauncher;
    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private int currentTabIndex = 0;
    private ValueCallback<Uri[]> filePathCallback;
    private Uri cameraImageUri;
    private byte[] pendingFileData;
    private String pendingFileName;

    private String pendingDownloadUrl;
    private String pendingDownloadFilename;
    private DownloadManager downloadManager;
    private long currentDownloadId = 0;

    private Context context;
    private String userEmail;
    private String userPassword;

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == currentDownloadId) {
                Toast.makeText(context, "Download finished", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get credentials from arguments passed from LoginActivity
        Bundle args = getArguments();
        if (args != null) {
            userEmail = args.getString("email");
            userPassword = args.getString("password");
            Log.d("GalleryFragment", "Received credentials - Email: " + userEmail + ", Password: " + userPassword);
        }

        // Initialize the launchers
        saveFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            if (pendingFileData != null) {
                                saveFileToUri(uri, pendingFileData);
                                pendingFileData = null;
                            } else if (pendingDownloadUrl != null) {
                                downloadFileToUri(uri, pendingDownloadUrl);
                                pendingDownloadUrl = null;
                            }
                        }
                    }
                });

        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (filePathCallback == null) return;

                    Uri[] results = null;
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String dataString = result.getData().getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                    filePathCallback.onReceiveValue(results);
                    filePathCallback = null;
                });

        // Register download receiver
        ContextCompat.registerReceiver(requireContext(), onDownloadComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        headerLayout = view.findViewById(R.id.headerLayout);
        headerContainer = view.findViewById(R.id.headerContainer);
        dragHandle = view.findViewById(R.id.dragHandle);
        tabFlipper = view.findViewById(R.id.tabFlipper);
        tabSelector = view.findViewById(R.id.tabSelector);
        ImageView myImageView = view.findViewById(R.id.mycrilogo);

        if (myImageView != null) {
            myImageView.setVisibility(View.VISIBLE);
        } else {
            Log.e("GalleryFragment", "ImageView not found in layout");
        }

        setupDragHandle();
        setupButtons(view);
        setupTabSelector();

        // Create initial tab with auto-login
        createInitialTab();
    }

    private void setupDragHandle() {
        dragHandle.setVisibility(View.VISIBLE);
        dragHandle.bringToFront();

        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            float initialY = 0f;
            float rawStartY = 0f;
            final float SWIPE_THRESHOLD = 50f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialY = event.getY();
                        rawStartY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float rawCurrentY = event.getRawY();
                        float deltaY = rawCurrentY - rawStartY;
                        if (deltaY < -SWIPE_THRESHOLD && headerContainer.getVisibility() == View.VISIBLE) {
                            hideHeader();
                        } else if (deltaY > SWIPE_THRESHOLD && headerContainer.getVisibility() == View.GONE) {
                            showHeader();
                        }
                        break;
                }
                return true;
            }
        });
    }

    private void setupButtons(View view) {
        view.findViewById(R.id.btnFront).setOnClickListener(v -> navigateBack());
        view.findViewById(R.id.btnBack).setOnClickListener(v -> navigateForward());
        view.findViewById(R.id.btnRefresh).setOnClickListener(v -> refreshPage());
        view.findViewById(R.id.btnNew).setOnClickListener(v -> createNewTab());
        view.findViewById(R.id.btnDeleteTab).setOnClickListener(v -> deleteCurrentTab());
    }

    private void setupTabSelector() {
        tabSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentTabIndex = position;
                tabFlipper.setDisplayedChild(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void createInitialTab() {
        WebView webView = new WebView(context);
        setupWebView(webView);
        webView.loadUrl("https://pds.iotsignin.com/admin/login");

        tabTitles.add("Loading...");
        webViewTabs.add(webView);
        tabFlipper.addView(webView);
        currentTabIndex = webViewTabs.size() - 1;
        tabFlipper.setDisplayedChild(currentTabIndex);
        refreshSpinner();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void createNewTab() {
        WebView webView = new WebView(context);
        setupWebView(webView);
        webView.loadUrl("https://pds.iotsignin.com/admin/login");

        tabTitles.add("New Tab");
        webViewTabs.add(webView);
        tabFlipper.addView(webView);
        currentTabIndex = webViewTabs.size() - 1;
        tabFlipper.setDisplayedChild(currentTabIndex);
        refreshSpinner();
    }

    private void setupWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.addJavascriptInterface(new WebAppInterface(context), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Fill credentials but don't auto-submit when login page is loaded
                if (url.contains("login") && userEmail != null && userPassword != null) {
                    fillCredentials(view);
                }

                injectDownloadInterceptor(view);
                updateTabTitle(view);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (url.endsWith(".csv") || url.endsWith(".xlsx") || url.contains("/download")) {
                    String fn = url.substring(url.lastIndexOf('/') + 1);
                    handleDirectDownload(url, fn);
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> cb, FileChooserParams params) {
                filePathCallback = cb;
                showUploadDialog();
                return true;
            }
        });
    }

    private void fillCredentials(WebView webView) {
        String fillScript = String.format(
                "javascript:(function() {" +
                        "   try {" +
                        "       if (document.readyState !== 'complete') return;" +
                        "       " +
                        "       var emailField = document.querySelector('input[type=\"email\"], input[name=\"email\"], #email, [id*=\"email\"], [name*=\"email\"]');" +
                        "       var passwordField = document.querySelector('input[type=\"password\"], input[name=\"password\"], #password, [id*=\"password\"], [name*=\"pass\"]');" +
                        "       " +
                        "       if (!emailField || !passwordField) return;" +
                        "       " +
                        "       emailField.value = '%s';" +
                        "       emailField.dispatchEvent(new Event('input', {bubbles: true}));" +
                        "       emailField.dispatchEvent(new Event('change', {bubbles: true}));" +
                        "       " +
                        "       passwordField.value = '%s';" +
                        "       passwordField.dispatchEvent(new Event('input', {bubbles: true}));" +
                        "       passwordField.dispatchEvent(new Event('change', {bubbles: true}));" +
                        "       " +
                        "       return 'Credentials filled - please click login manually';" +
                        "   } catch (e) {" +
                        "       return 'Error: ' + e.message;" +
                        "   }" +
                        "})();",
                userEmail,
                userPassword
        );

        webView.postDelayed(() -> {
            webView.evaluateJavascript(fillScript, result -> {
                Log.d("FillCredentials", "Result: " + result);
            });
        }, 1000);
    }


    private void updateTabTitle(WebView webView) {
        String title = webView.getTitle();
        if (currentTabIndex >= 0 && currentTabIndex < tabTitles.size()) {
            tabTitles.set(currentTabIndex, title != null && !title.isEmpty() ? title : "Untitled");
            refreshSpinner();
        }
    }

    private void hideHeader() {
        headerContainer.animate().translationY(-headerContainer.getHeight()).setDuration(300).withEndAction(() -> {
            headerContainer.setVisibility(View.GONE);
        });
        dragHandle.animate().translationY(0).setDuration(300);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) tabFlipper.getLayoutParams();
        lp.addRule(RelativeLayout.BELOW, R.id.dragHandle);
        lp.topMargin = 0;
        tabFlipper.setLayoutParams(lp);
    }

    private void showHeader() {
        headerContainer.setVisibility(View.VISIBLE);
        headerContainer.setTranslationY(-headerContainer.getHeight());
        headerContainer.animate().translationY(0).setDuration(300);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) tabFlipper.getLayoutParams();
        lp.addRule(RelativeLayout.BELOW, R.id.dragHandle);
        lp.topMargin = 4;
        tabFlipper.setLayoutParams(lp);
    }

    private void injectDownloadInterceptor(WebView w) {
        String js = ""
                + "(function() {"
                + "  if (window._dl) return; window._dl = 1;"
                + "  const origFetch = window.fetch;"
                + "  window.fetch = function() {"
                + "    return origFetch.apply(this, arguments).then(response => {"
                + "      let disposition = response.headers.get('content-disposition');"
                + "      if (disposition && disposition.includes('attachment')) {"
                + "        response.clone().blob().then(blob => {"
                + "          const reader = new FileReader();"
                + "          reader.onloadend = function() {"
                + "            const base64 = reader.result.split(',')[1];"
                + "            let m = /filename\\*=UTF-8''(.+)/.exec(disposition) ||"
                + "                    /filename=\"?([^\";]+)/.exec(disposition);"
                + "            let filename = m ? decodeURIComponent(m[1]) : 'file.csv';"
                + "            window.Android.saveBlob(base64, filename);"
                + "          };"
                + "          reader.readAsDataURL(blob);"
                + "        });"
                + "      }"
                + "      return response;"
                + "    });"
                + "  };"
                + "  const origCreateObjectURL = URL.createObjectURL;"
                + "  URL.createObjectURL = function(blob) {"
                + "    try {"
                + "      const reader = new FileReader();"
                + "      reader.onloadend = function() {"
                + "        const base64 = reader.result.split(',')[1];"
                + "        window.Android.saveBlob(base64, 'imported_file.csv');"
                + "      };"
                + "      reader.readAsDataURL(blob);"
                + "    } catch (e) { console.error('Blob hook failed', e); }"
                + "    return origCreateObjectURL(blob);"
                + "  };"
                + "})();";
        w.evaluateJavascript(js, null);
    }

    private void handleDirectDownload(String url, String filename) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(filename);
        request.setDescription("Downloading file...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

        String cookie = CookieManager.getInstance().getCookie(url);
        if (cookie != null) {
            request.addRequestHeader("Cookie", cookie);
        }

        currentDownloadId = downloadManager.enqueue(request);
        Toast.makeText(context, "Export download started...", Toast.LENGTH_SHORT).show();

        // Clear any pending data as direct download handled immediately
        pendingDownloadUrl = null;
        pendingDownloadFilename = null;
    }


    private void showUploadDialog() {
        String[] opts = {"Camera", "Gallery", "File Manager"};
        new AlertDialog.Builder(context)
                .setTitle("Upload File")
                .setItems(opts, (d, which) -> {
                    Intent i;
                    if (which == 0) {
                        ContentValues v = new ContentValues();
                        v.put(MediaStore.Images.Media.TITLE, "New Picture");
                        cameraImageUri = context.getContentResolver()
                                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                        i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                .putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                    } else if (which == 1) {
                        i = new Intent(Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                .setType("image/*");
                    } else {
                        i = new Intent(Intent.ACTION_GET_CONTENT)
                                .setType("*/*")
                                .addCategory(Intent.CATEGORY_OPENABLE);
                    }
                    fileChooserLauncher.launch(Intent.createChooser(i, "Choose File"));
                })
                .setNegativeButton("Cancel", (d, w) -> {
                    if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                    filePathCallback = null;
                })
                .show();
    }

    private void saveFileToUri(Uri uri, byte[] data) {
        try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                os.write(data);
                Toast.makeText(context, "File saved", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(context, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void downloadFileToUri(Uri uri, String url) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(uri)
                .setTitle(pendingDownloadFilename);
        currentDownloadId = downloadManager.enqueue(request);
    }

    public class WebAppInterface {
        Context ctx;
        WebAppInterface(Context c) { ctx = c; }

        @JavascriptInterface
        public void saveBlob(String base64, String filename) {
            pendingFileData = Base64.decode(base64, Base64.DEFAULT);
            pendingFileName = filename;
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/octet-stream")
                    .putExtra(Intent.EXTRA_TITLE, filename);
            saveFileLauncher.launch(i);
        }
    }

    private void refreshSpinner() {
        ArrayAdapter<String> a = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, tabTitles);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tabSelector.setAdapter(a);
        tabSelector.setSelection(currentTabIndex);
    }

    private WebView getCurrentWebView() {
        return webViewTabs.get(currentTabIndex);
    }

    private void navigateBack() {
        WebView w = getCurrentWebView();
        if (w.canGoBack()) w.goBack();
        else Toast.makeText(context, "No previous page found", Toast.LENGTH_SHORT).show();
    }

    private void navigateForward() {
        WebView w = getCurrentWebView();
        if (w.canGoForward()) w.goForward();
        else Toast.makeText(context, "No next page found", Toast.LENGTH_SHORT).show();
    }

    private void refreshPage() {
        getCurrentWebView().reload();
        Toast.makeText(context, "Page refreshed", Toast.LENGTH_SHORT).show();
    }

    private void deleteCurrentTab() {
        if (webViewTabs.size() > 1) {
            tabFlipper.removeViewAt(currentTabIndex);
            webViewTabs.remove(currentTabIndex);
            tabTitles.remove(currentTabIndex);
            currentTabIndex = Math.max(currentTabIndex - 1, 0);
            tabFlipper.setDisplayedChild(currentTabIndex);
            refreshSpinner();
        } else {
            Toast.makeText(context, "Cannot delete last tab", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            context.unregisterReceiver(onDownloadComplete);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }
    }
}