package com.network.manager;

import com.network.library.MultipartRequestParams;
import com.network.library.RequestCreator;
import com.network.library.RequestHeaders;
import com.network.library.RequestParams;

public class InitRequestCreator extends RequestCreator<InitModel> {

    @Override
    public String onCreateUrl() {
        return "http://mraport.witchcraftstudios.com/api/init.json";
    }

    @Override
    public void onCreateRequestParams(RequestParams requestParams) {

    }

    @Override
    public void onCreateRequestHeaders(RequestHeaders requestHeaders) {

    }

    @Override
    public void onCreateMultipartRequestParams(MultipartRequestParams multipartRequestParams) {

    }

    @Override
    public int onCreateRetryCount() {
        return 10;
    }

    @Override
    public String onCreateRequestMethod() {
        return null;
    }

    @Override
    public InitModel onDownloadSuccess(String response) throws Exception {
        return new InitModel();
    }

    @Override
    public void onResult(InitModel result) throws Exception {

    }
}
