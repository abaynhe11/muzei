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

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

interface FiveHundredPxService {
    @GET("v1/photos?feature=popular&sort=rating&image_size=5&rpp=40")
    Call<PhotosResponse> getPopularPhotos();

    class PhotosResponse {
        List<Photo> photos;
    }

    class Photo {
        int id;
        List<Image> images;
        String name;
        User user;
    }

    class Image {
        String https_url;
    }

    class User {
        String fullname;
    }
}
