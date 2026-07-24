package com.miniapp.data.remote.services

import com.miniapp.data.remote.models.responses.Response
import retrofit2.http.GET

interface ApiService {

    @GET("users")
    suspend fun getResponses(): List<Response>
}
