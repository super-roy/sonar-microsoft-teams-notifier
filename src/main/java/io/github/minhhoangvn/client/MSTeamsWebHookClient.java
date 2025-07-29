package io.github.minhhoangvn.client;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MSTeamsWebHookClient {

    private static final String APPLICATION_JSON = "application/json";
    private final OkHttpClient client;

    public MSTeamsWebHookClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public Response sendNotify(String webhookUrl, String payload) throws IOException {
        MediaType mediaType = MediaType.parse(APPLICATION_JSON);
        RequestBody requestBody = RequestBody.create(payload, mediaType);
        
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .addHeader("Content-Type", APPLICATION_JSON)
                .addHeader("Accept", APPLICATION_JSON)
                .build();
                
        return client.newCall(request).execute();
    }
}