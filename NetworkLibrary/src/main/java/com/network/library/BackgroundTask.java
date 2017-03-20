package com.network.library;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
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

public class BackgroundTask extends AsyncTask<String, Integer, Boolean> {
    private static final String TAG = "NetworkManager";
    private static final String CHARSET = "UTF-8";
    private static final String CRLF = "\r\n";

    private final List<RequestCreator> requestCreatorList = new ArrayList<>();
    private final List<BackgroundTask> mBackgroundTaskList;


    private WeakReference<Context> mContextWeakReference = new WeakReference<>(null);
    private NetworkManagerCallbacks mNetworkManagerCallbacks = null;
    private UpdateListener mUpdateListener = null;

    private HttpURLConnection mHttpURLConnection;

    private ProgressDialog mProgressDialog;
    private RequestCreator mCurrentRequest;

    private String mErrorMassage = "";
    private String mDialogTitle;

    private int mDialogTheme = ProgressDialog.STYLE_SPINNER;
    private int mDelay = 0;

    private boolean mCancelOnDestroy = false;
    private boolean mShowDialog = false;
    private boolean mCancelableDialog = true;

    BackgroundTask(List<BackgroundTask> mBackgroundTaskList, Context pContext, boolean pCancelOnDestroy) {
        this.mBackgroundTaskList = mBackgroundTaskList;
        this.mContextWeakReference = new WeakReference<>(pContext);
        this.mCancelOnDestroy = pCancelOnDestroy;
    }

    @SuppressWarnings("unused")
    public void init(String defaultErrorMessage, int delay) {
        this.setErrorMassage(defaultErrorMessage);
        this.setDelay(delay);
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isCancelOnDestroy() {
        return this.mCancelOnDestroy;
    }

    @Override
    protected void onPreExecute() {
        if (this.mNetworkManagerCallbacks != null) {
            try {
                this.mNetworkManagerCallbacks.onStart();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (this.mShowDialog) {
            this.onCreateProgressDialog();
        }
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            Thread.sleep(this.mDelay);
            if (this.requestCreatorList.size() < 1) {
                Log.e(TAG, "You must have at least one \"RequestCreator\"");
                return false;
            }

            for (int i = 0; i < this.requestCreatorList.size(); i++) {
                if (this.isCancelled()) {
                    return false;
                }

                final Context context = this.mContextWeakReference.get();
                if (context == null) {
                    Log.w(TAG, "doInBackground: context == null");
                    return false;
                }

                if (!NetworkManager.isInternetConnection(context)) {
                    throw new Exception();
                }

                /*
                * Cancel on destroy
                * */
                if (this.mCancelOnDestroy && context instanceof Activity) {
                    if (!isActivityRunning()) {
                        return false;
                    }
                }

                final RequestCreator requestCreator = this.requestCreatorList.get(i);
                this.onCreateRequest(requestCreator);
                this.publishProgress(i + 1);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Log.e(TAG, "Finally: disconnect()");
            if (this.mHttpURLConnection != null) {
                this.mHttpURLConnection.disconnect();
                this.mHttpURLConnection = null;
            }
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (values.length > 1) {
            if (this.mCurrentRequest != null)
                this.onUpdateProgress(values[0], 100, this.mCurrentRequest.getClass().getSimpleName());
        } else {
            this.onUpdateProgress(values[0], this.requestCreatorList.size(), TAG);
            if (this.mUpdateListener != null) {
                this.mUpdateListener.onUpdate(values[0], this.requestCreatorList.size());
            }
        }
    }

    @SuppressWarnings({"EmptyMethod", "UnusedParameters", "WeakerAccess"})
    protected void onUpdateProgress(int progress, int size, String simpleName) {
        //Log.e(simpleName, "onUpdateProgress: " + progress + "/" + size);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        this.mBackgroundTaskList.remove(this);
        try {
            final Context context = this.mContextWeakReference.get();
            if (context == null) {
                return;
            }

            if (context instanceof Activity && !this.isActivityRunning()) {
                return;
            }

            this.onDismissDialog();
            if (result) {
                if (this.mNetworkManagerCallbacks != null)
                    this.mNetworkManagerCallbacks.onSuccess();
            } else {
                if (this.mNetworkManagerCallbacks != null) {
                    this.mNetworkManagerCallbacks.onError(this.getErrorMassage());
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
        this.setCurrentRequest(requestCreator);
        requestCreator.setBackgroundTask(this);

        String requestMethod = requestCreator.onCreateRequestMethod();
        final RequestParams requestParams = new RequestParams();
        requestCreator.onCreateRequestParams(requestParams);

        int retryCount = requestCreator.onCreateRetryCount();
        retryCount = retryCount < 1 ? 1 : retryCount;

        requestMethod = requestMethod == null ? RequestMethod.POST : requestMethod;
        final String requestUrl = requestCreator.onCreateUrl();
        final HashMap<String, String> urlParams = requestParams.getUrlParams();

        final Uri.Builder uriBuilder = new Uri.Builder();
        for (String key : urlParams.keySet()) {
            String value = urlParams.get(key);
            uriBuilder.appendQueryParameter(key, value);
        }

        final String params = uriBuilder.build().getEncodedQuery();
        final RequestHeaders requestHeaders = new RequestHeaders();
        requestCreator.onCreateRequestHeaders(requestHeaders);

        final MultipartRequestParams multipartRequestParams = new MultipartRequestParams();
        requestCreator.onCreateMultipartRequestParams(multipartRequestParams);

        for (int i = retryCount; i >= 0; i--) {
            if (this.isCancelled()) {
                break;
            }

            final InputStream inputStream = onHttpConnect(requestUrl, params, requestMethod, requestHeaders, multipartRequestParams);
            if (inputStream != null) {
                final Context context = this.mContextWeakReference.get();
                if (context == null) {
                    return;
                }

                final Object result = requestCreator.onDownloadSuccess(inputStream);
                final Handler handler = new Handler(context.getMainLooper());
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        /*
                        * Błąd w "onResult ,onSuccess" nie anuluje pobierania
                        * */
                        try {
                            if (!BackgroundTask.this.isCancelled()) {
                                requestCreator.onResult(result);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                handler.post(runnable);
                inputStream.close();
                break;
            }

            if (!this.isCancelled()) {
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
            Log.w(TAG, "REQUEST_PARAMS: " + (TextUtils.isEmpty(params) ? " " : params));
            Log.w(TAG, "REQUEST_METHOD: " + requestMethod);

            final HashMap<String, String> requestHeadersHeaders = requestHeaders.getHeaders();
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
                    final URL postUrl = new URL(requestUrl);

                    this.mHttpURLConnection = (HttpURLConnection) postUrl.openConnection();
                    this.mHttpURLConnection.setRequestMethod(requestMethod);
                    this.mHttpURLConnection.setDoInput(true);
                    this.mHttpURLConnection.setDoOutput(true);

                    for (String key : requestHeadersHeaders.keySet()) {
                        String value = requestHeadersHeaders.get(key);
                        this.mHttpURLConnection.setRequestProperty(key, value);
                    }

                    /*
                    * Default request params
                    * */
                    if (!TextUtils.isEmpty(params)) {
                        final OutputStream outputStream = this.mHttpURLConnection.getOutputStream();
                        final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, CHARSET));
                        bufferedWriter.write(params);
                        bufferedWriter.flush();
                        bufferedWriter.close();
                        outputStream.close();
                    }

                    /*
                    * Multipart request params
                    * */
                    final HashMap<String, Object> multipartRequestParamsHashMap = multipartRequestParams.getFileRequestParams();
                    if (multipartRequestParamsHashMap.size() > 0) {
                        final String boundary = "---------------------------" + System.currentTimeMillis();
                        this.mHttpURLConnection.setRequestProperty("Accept-Charset", CHARSET);
                        this.mHttpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                        this.mHttpURLConnection.setUseCaches(false);

                        final OutputStream outputStream = this.mHttpURLConnection.getOutputStream();
                        final PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, CHARSET), true);

                        for (String key : multipartRequestParamsHashMap.keySet()) {
                            final Object object = multipartRequestParamsHashMap.get(key);

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
                    final URL getUrl;
                    if (TextUtils.isEmpty(params)) {
                        getUrl = new URL(requestUrl);
                    } else {
                        getUrl = new URL(requestUrl + "?" + params);
                    }
                    this.mHttpURLConnection = (HttpURLConnection) getUrl.openConnection();
                    this.mHttpURLConnection.setRequestMethod(RequestMethod.GET);
                    for (String key : requestHeadersHeaders.keySet()) {
                        final String value = requestHeadersHeaders.get(key);
                        this.mHttpURLConnection.setRequestProperty(key, value);
                    }
                    break;
                default:
                    throw new Exception();
            }

            final int responseCode = this.mHttpURLConnection.getResponseCode();
            Log.w(TAG, "RESPONSE_CODE: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK
                    || responseCode == HttpURLConnection.HTTP_CREATED
                    || responseCode == HttpURLConnection.HTTP_ACCEPTED
                    || responseCode == 204) {

                inputStream = this.mHttpURLConnection.getInputStream();
            } else {
                inputStream = this.mHttpURLConnection.getErrorStream();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return inputStream;
    }

    @SuppressWarnings("unused")
    public String convertInputStreamToString(InputStream inputStream) throws Exception {
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        final StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
            if (isCancelled()) {
                break;
            }
        }
        return stringBuilder.toString();
    }

    @SuppressWarnings({"unused", "TryFinallyCanBeTryWithResources", "ResultOfMethodCallIgnored"})
    public void convertInputStreamToFile(InputStream inputStream, String filePath, String fileName) throws Exception {
        Log.e(TAG, "convertInputStreamToFile");

        final File file = new File(filePath, fileName);
        final long startTimeMillis = System.currentTimeMillis();

        final OutputStream outputStream = new FileOutputStream(file);
        final byte[] buffer = new byte[1024];

        int bufferLength;
        long total = 0;

        final int mConnectionLength = this.mHttpURLConnection.getContentLength();

        while ((bufferLength = inputStream.read(buffer)) > 0) {
            total += bufferLength;
            outputStream.write(buffer, 0, bufferLength);
            this.publishProgress((int) (total * 100 / mConnectionLength), mConnectionLength);
            if (this.isCancelled()) {
                break;
            }
        }

        outputStream.flush();
        outputStream.close();

        if (this.isCancelled()) {
            file.delete();
        } else {
            final long endTimeMillis = System.currentTimeMillis();
            Log.e("Czas zapisywania pliku:", "" + (endTimeMillis - startTimeMillis) + "ms");
        }
    }

    private void setCurrentRequest(RequestCreator pRequest) {
        this.mCurrentRequest = pRequest;
    }

    @SuppressWarnings("WeakerAccess")
    public void setErrorMassage(String errorMassage) {
        this.mErrorMassage = errorMassage;
    }

    @SuppressWarnings("WeakerAccess")
    public void setDelay(int delay) {
        this.mDelay = delay;
    }

    @SuppressWarnings("unused")
    public void setDialog(String dialogTitle, int dialogTheme) {
        this.mShowDialog = true;
        this.mDialogTitle = dialogTitle;
        this.mDialogTheme = dialogTheme;
    }

    @SuppressWarnings("unused")
    public void setNetworkManagerCallbacks(NetworkManagerCallbacks networkManagerCallbacks) {
        this.mNetworkManagerCallbacks = networkManagerCallbacks;
    }

    @SuppressWarnings("unused")
    public void setUpdateListener(UpdateListener pUpdateLister) {
        this.mUpdateListener = pUpdateLister;
    }

    @SuppressWarnings("WeakerAccess")
    public String getErrorMassage() {
        return this.mErrorMassage;
    }

    @SuppressWarnings("WeakerAccess")
    public Context getContext() {
        return this.mContextWeakReference.get();
    }

    @SuppressWarnings("unused")
    public void setDialogCancelable(boolean pCancelableDialog) {
        this.mCancelableDialog = pCancelableDialog;
    }

    private void onCreateProgressDialog() {
        try {
            final Context context = this.mContextWeakReference.get();
            if (context == null || context instanceof Application) {
                return;
            }

            if (!this.isActivityRunning()) {
                return;
            }

            this.mProgressDialog = new ProgressDialog(context, mDialogTheme);

            final Window mWindow = this.mProgressDialog.getWindow();
            if (mWindow == null) {
                return;
            }

            mWindow.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

            this.mProgressDialog.setMessage(mDialogTitle);
            this.mProgressDialog.setCanceledOnTouchOutside(false);
            this.mProgressDialog.setCancelable(this.mCancelableDialog);
            this.mProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                boolean onKeyPressed = false;

                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (!mCancelableDialog) {
                        return true;
                    }

                    if (keyCode == KeyEvent.KEYCODE_BACK && !onKeyPressed) {
                        this.onKeyPressed = true;
                        onDismissDialog();
                        cancelRequests();
                    }
                    return true;
                }
            });
            this.mProgressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onDismissDialog() {
        try {
            final Context context = this.mContextWeakReference.get();
            if (context == null) {
                return;
            }

            if (!this.isActivityRunning()) {
                return;
            }

            if (this.mProgressDialog != null && this.mProgressDialog.isShowing())
                this.mProgressDialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("[unused, WeakerAccess]")
    public void cancelRequests() {
        try {
            if (this.mNetworkManagerCallbacks != null) {
                this.mNetworkManagerCallbacks.onCancelled();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.mNetworkManagerCallbacks = null;
        this.cancel(false);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        this.mBackgroundTaskList.remove(this);
        this.onDismissDialog();

        if (this.mHttpURLConnection != null) {
            Log.e(TAG, "onCancelled: " + "Force disconnect");
            this.mHttpURLConnection.disconnect();
            this.mHttpURLConnection = null;
        }
    }

    @SuppressWarnings("[unused, WeakerAccess]")
    public boolean isFinished() {
        return getStatus() == Status.FINISHED;
    }

    private boolean isActivityRunning() {
        final Context context = this.mContextWeakReference.get();
        if (Build.VERSION.SDK_INT >= 17) {
            return context != null && !((Activity) context).isFinishing() && !((Activity) context).isDestroyed();
        } else {
            return context != null && !((Activity) context).isFinishing();
        }
    }
}