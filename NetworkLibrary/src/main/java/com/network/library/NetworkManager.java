package com.network.library;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NetworkManager extends AsyncTask<String, Integer, Boolean> {
    private static final String TAG = "NetworkManager";
    private static final String CHARSET = "UTF-8";

    private WeakReference<Context> contextWeakReference = new WeakReference<>(null);
    private final List<RequestCreator> requestCreatorList = new ArrayList<>();
    private NetworkManagerCallbacks networkManagerCallbacks = null;
    private HttpURLConnection httpURLConnection;

    private ProgressDialog progressDialog;

    private String errorMassage = "Connection error...";
    private String dialogTitle;

    private int dialogTheme = ProgressDialog.STYLE_SPINNER;
    private int delay = 0;

    private boolean cancelOnDestroy = true;
    private boolean showDialog = false;

    public NetworkManager(Context context, boolean cancelOnDestroy) {
        this.contextWeakReference = new WeakReference<>(context);
        this.cancelOnDestroy = cancelOnDestroy;
    }

    @SuppressWarnings("unused")
    public void init(String defaultErrorMessage, int delay) {
        setErrorMassage(defaultErrorMessage);
        setDelay(delay);
    }

    @Override
    protected void onPreExecute() {
        if (this.networkManagerCallbacks != null) {
            try {
                this.networkManagerCallbacks.onStart();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (this.showDialog) {
            onCreateProgressDialog();
        }
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            Thread.sleep(this.delay);
            if (this.requestCreatorList.size() < 1) {
                Log.e(TAG, "You must have at least one \"RequestCreator\"");
                return false;
            }

            for (int i = 0; i < this.requestCreatorList.size(); i++) {
                if (isCancelled()) {
                    return false;
                }

                Context context = this.contextWeakReference.get();
                if (context == null) {
                    return false;
                }

                if (!isInternetConnection(context)) {
                    throw new Exception();
                }

                if (this.cancelOnDestroy && context instanceof Activity) {
                    if (!isActivityRunning()) {
                        return false;
                    }
                }

                RequestCreator requestCreator = this.requestCreatorList.get(i);
                onCreateRequest(requestCreator);
                publishProgress(i + 1);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        onUpdateProgress(values[0], requestCreatorList.size());
    }

    @SuppressWarnings({"EmptyMethod", "UnusedParameters", "WeakerAccess"})
    protected void onUpdateProgress(int progress, int size) {

    }

    @Override
    protected void onPostExecute(Boolean result) {
        try {
            Context context = this.contextWeakReference.get();
            if (context == null) {
                return;
            }

            if (context instanceof Activity && !isActivityRunning()) {
                return;
            }

            onDismissDialog();
            if (result) {
                if (this.networkManagerCallbacks != null)
                    this.networkManagerCallbacks.onSuccess();
            } else {
                if (this.networkManagerCallbacks != null) {
                    this.networkManagerCallbacks.onError(getErrorMassage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public void addRequest(RequestCreator requestCreator) {
        this.requestCreatorList.add(requestCreator);
    }

    @SuppressWarnings("unchecked")
    private void onCreateRequest(final RequestCreator requestCreator) throws Exception {
        requestCreator.setNetworkManager(this);

        String requestMethod = requestCreator.onCreateRequestMethod();
        RequestParams requestParams = new RequestParams();
        requestCreator.onCreateRequestParams(requestParams);

        int retryCount = requestCreator.onCreateRetryCount();
        retryCount = retryCount < 1 ? 1 : retryCount;

        requestMethod = requestMethod == null ? RequestMethod.POST : requestMethod;
        String requestUrl = requestCreator.onCreateUrl();
        HashMap<String, String> urlParams = requestParams.getUrlParams();

        Uri.Builder uriBuilder = new Uri.Builder();
        for (String key : urlParams.keySet()) {
            String value = urlParams.get(key);
            uriBuilder.appendQueryParameter(key, value);
        }
        String params = uriBuilder.build().getEncodedQuery();

        RequestHeaders requestHeaders = new RequestHeaders();
        requestCreator.onCreateRequestHeaders(requestHeaders);

        MultipartRequestParams multipartRequestParams = new MultipartRequestParams();
        requestCreator.onCreateMultipartRequestParams(multipartRequestParams);

        for (int i = retryCount; i >= 0; i--) {
            if (isCancelled()) {
                break;
            }

            InputStream inputStream = onHttpConnect(requestUrl, params, requestMethod, requestHeaders, multipartRequestParams);
            if (inputStream != null) {
                Context context = this.contextWeakReference.get();
                if (context == null) {
                    return;
                }

                final Object result = requestCreator.onDownloadSuccess(inputStream);
                Handler handler = new Handler(context.getMainLooper());
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        /*
                        * Błąd w "onResult" nie anuluje pobierania
                        * */
                        try {
                            if (!isCancelled()) requestCreator.onResult(result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                handler.post(runnable);
                inputStream.close();
                break;
            }

            if (!isCancelled()) {
                if (i == 0) {
                    throw new Exception("RETRY == 0");
                }

                Log.e(TAG, "******RETRY COUNT: " + (i - 1) + "*******");
                Thread.sleep(1000);
            }
        }
    }

    private InputStream onHttpConnect(String requestUrl, String params, String requestMethod,
                                      RequestHeaders requestHeaders, MultipartRequestParams multipartRequestParams) {
        InputStream inputStream;
        try {
            Log.w(TAG, "REQUEST_URL: " + requestUrl);
            Log.w(TAG, "REQUEST_PARAMS: " + params);
            Log.w(TAG, "REQUEST_METHOD: " + requestMethod);

            HashMap<String, String> requestHeadersHeaders = requestHeaders.getHeaders();
            for (String key : requestHeadersHeaders.keySet()) {
                String value = requestHeadersHeaders.get(key);
                Log.w(TAG, "REQUEST_HEADERS: " + key + ":" + value);
            }

            if (!TextUtils.isEmpty(params) && multipartRequestParams.getFileRequestParams().size() > 0)
                throw new Exception("Wysyłanie zablokowane: nie można używać MultipartRequestParams oraz RequestParams jednocześnie");

            switch (requestMethod) {
                case RequestMethod.POST:
                case RequestMethod.PUT:
                case RequestMethod.DELETE:
                    URL postUrl = new URL(requestUrl);

                    httpURLConnection = (HttpURLConnection) postUrl.openConnection();
                    httpURLConnection.setRequestMethod(requestMethod);
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setDoOutput(true);

                    for (String key : requestHeadersHeaders.keySet()) {
                        String value = requestHeadersHeaders.get(key);
                        httpURLConnection.setRequestProperty(key, value);
                    }

                    /*
                    * Default request params
                    * */
                    if (!TextUtils.isEmpty(params)) {
                        OutputStream outputStream = httpURLConnection.getOutputStream();
                        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, CHARSET));
                        bufferedWriter.write(params);
                        bufferedWriter.flush();
                        bufferedWriter.close();
                        outputStream.close();
                    }

                    /*
                    * Multipart request params
                    * */
                    HashMap<String, Object> multipartRequestParamsHashMap = multipartRequestParams.getFileRequestParams();
                    if (multipartRequestParamsHashMap.size() > 0) {
                        String boundary = "---------------------------" + System.currentTimeMillis();
                        httpURLConnection.setRequestProperty("Accept-Charset", CHARSET);
                        httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                        httpURLConnection.setUseCaches(false);

                        OutputStream outputStream = httpURLConnection.getOutputStream();
                        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, CHARSET), true);

                        String CRLF = "\r\n";

                        for (String key : multipartRequestParamsHashMap.keySet()) {
                            Object object = multipartRequestParamsHashMap.get(key);

                            if (object instanceof String) {
                                String value = (String) object;
                                printWriter.append("--").append(boundary).append(CRLF)
                                        .append("Content-Disposition: form-data; name=").append(key)
                                        .append("").append(CRLF)
                                        .append("Content-Type: text/plain; charset=").append(CHARSET)
                                        .append(CRLF).append(CRLF).append(value).append(CRLF);
                            } else {
                                /*
                                * File
                                * */
                                File uploadFile = (File) object;
                                String fileName = uploadFile.getName();

                                printWriter.append("--").append(boundary).append(CRLF)
                                        .append("Content-Disposition: form-data; name=").append(key).append("; filename=").append(fileName)
                                        .append("").append(CRLF).append("Content-Type: ")
                                        .append(URLConnection.guessContentTypeFromName(fileName)).append(CRLF)
                                        .append(CRLF);

                                printWriter.flush();

                                FileInputStream fileInputStream = new FileInputStream(uploadFile);
                                final byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, bytesRead);
                                }
                                printWriter.append(CRLF).append("--").append(boundary).append("--").append(CRLF);
                            }
                            printWriter.flush();
                        }

                        printWriter.close();
                        outputStream.close();
                    }
                    break;
                case RequestMethod.GET:
                    URL getUrl;
                    if (TextUtils.isEmpty(params)) {
                        getUrl = new URL(requestUrl);
                    } else {
                        getUrl = new URL(requestUrl + "?" + params);
                    }
                    httpURLConnection = (HttpURLConnection) getUrl.openConnection();
                    httpURLConnection.setRequestMethod(RequestMethod.GET);

                    for (String key : requestHeadersHeaders.keySet()) {
                        String value = requestHeadersHeaders.get(key);
                        httpURLConnection.setRequestProperty(key, value);
                    }
                    break;
                default:
                    throw new Exception();
            }

            int responseCode = httpURLConnection.getResponseCode();
            Log.w(TAG, "RESPONSE_CODE: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = httpURLConnection.getInputStream();
            } else {
                inputStream = httpURLConnection.getErrorStream();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return inputStream;
    }

    @SuppressWarnings("unused")
    public String convertInputStreamToString(InputStream inputStream) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
            if (isCancelled()) {
                break;
            }
        }
        return stringBuilder.toString();
    }

    @SuppressWarnings({"unused", "TryFinallyCanBeTryWithResources"})
    public void convertInputStreamToFile(InputStream inputStream, String filePath, String fileName) throws Exception {
        File file = new File(filePath, fileName);
        OutputStream outputStream = new FileOutputStream(file);

        long startTimeMillis = System.currentTimeMillis();

        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
        } finally {
            outputStream.close();
        }

        long endTimeMillis = System.currentTimeMillis();
        Log.e("Czas zapisywania pliku:", "" + (endTimeMillis - startTimeMillis) + "ms");
    }

    public void setErrorMassage(String errorMassage) {
        this.errorMassage = errorMassage;
    }

    @SuppressWarnings("WeakerAccess")
    public void setDelay(int delay) {
        this.delay = delay;
    }

    @SuppressWarnings("unused")
    public void setDialog(String dialogTitle, int dialogTheme) {
        this.showDialog = true;
        this.dialogTitle = dialogTitle;
        this.dialogTheme = dialogTheme;
    }

    @SuppressWarnings("unused")
    public void setNetworkManagerCallbacks(NetworkManagerCallbacks networkManagerCallbacks) {
        this.networkManagerCallbacks = networkManagerCallbacks;
    }

    @SuppressWarnings("WeakerAccess")
    public String getErrorMassage() {
        return this.errorMassage;
    }

    public Context getContext() {
        return this.contextWeakReference.get();
    }

    private void onCreateProgressDialog() {
        try {
            Context context = this.contextWeakReference.get();
            if (context == null || context instanceof Application) {
                return;
            }

            if (!isActivityRunning()) {
                return;
            }

            this.progressDialog = new ProgressDialog(context, dialogTheme);
            this.progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
            this.progressDialog.setMessage(dialogTitle);
            this.progressDialog.setCanceledOnTouchOutside(false);
            this.progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                boolean onKeyPressed = false;

                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK && !onKeyPressed) {
                        this.onKeyPressed = true;
                        onDismissDialog();
                        cancelRequests();
                    }
                    return true;
                }
            });
            this.progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onDismissDialog() {
        try {
            if (this.progressDialog != null)
                this.progressDialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public void cancelRequests() {
        try {
            if (this.networkManagerCallbacks != null) {
                this.networkManagerCallbacks.onCancelled();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.networkManagerCallbacks = null;
        cancel(true);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        try {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
                httpURLConnection = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        onDismissDialog();
    }

    @SuppressWarnings("unused")
    public boolean isFinished() {
        return getStatus() == Status.FINISHED;
    }

    public static boolean isInternetConnection(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    private boolean isActivityRunning() {
        Context context = this.contextWeakReference.get();
        return context != null && !((Activity) context).isFinishing();
    }
}
