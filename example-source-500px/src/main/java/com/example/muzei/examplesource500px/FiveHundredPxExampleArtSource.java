/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.muzei.examplesource500px;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.example.muzei.examplesource500px.FiveHundredPxService.Photo;
import static com.example.muzei.examplesource500px.FiveHundredPxService.PhotosResponse;

public class FiveHundredPxExampleArtSource extends RemoteMuzeiArtSource {
    private static final String TAG = "500pxExample";
    private static final String SOURCE_NAME = "FiveHundredPxExampleArtSource";

    private static final int ROTATE_TIME_MILLIS = 3 * 60 * 60 * 1000; // rotate every 3 hours

    public FiveHundredPxExampleArtSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    @Override
    protected void onTryUpdate(@UpdateReason int reason) throws RetryException {
        String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(final Chain chain) throws IOException {
                        Request request = chain.request();
                        HttpUrl url = request.url().newBuilder()
                                .addQueryParameter("consumer_key", Config.CONSUMER_KEY).build();
                        request = request.newBuilder().url(url).build();
                        return chain.proceed(request);
                    }
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.500px.com/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        FiveHundredPxService service = retrofit.create(FiveHundredPxService.class);
        PhotosResponse response;
        try {
            response = service.getPopularPhotos().execute().body();
        } catch (IOException e) {
            Log.w(TAG, "Error reading 500px response", e);
            throw new RetryException();
        }

        if (response == null || response.photos == null) {
            Log.w(TAG, "Response " + (response == null ? "was null" : "had null photos"));
            throw new RetryException();
        }

        List<Photo> photos = response.photos;
        ListIterator<Photo> iterator = photos.listIterator();
        while (iterator.hasNext()) {
            List<FiveHundredPxService.Image> images = iterator.next().images;
            if (images.isEmpty() || TextUtils.isEmpty(images.get(0).https_url)) {
                iterator.remove();
            }
        }

        if (photos.size() == 0) {
            Log.w(TAG, "No photos returned from API.");
            scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
            return;
        }

        Random random = new Random();
        Photo photo;
        String token;
        while (true) {
            photo = photos.get(random.nextInt(photos.size()));
            token = Integer.toString(photo.id);
            if (photos.size() <= 1 || !TextUtils.equals(token, currentToken)) {
                break;
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Loaded " + photo.id + " with uri: " +
                    photo.images.get(0).https_url);
        }
        publishArtwork(new Artwork.Builder()
                .title(photo.name)
                .byline(photo.user.fullname)
                .imageUri(Uri.parse(photo.images.get(0).https_url))
                .token(token)
                .viewIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://500px.com/photo/" + photo.id)))
                .build());

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
}

