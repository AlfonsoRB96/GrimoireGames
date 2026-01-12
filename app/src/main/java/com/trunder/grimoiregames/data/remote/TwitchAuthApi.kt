package com.trunder.grimoiregames.data.remote

import com.trunder.grimoiregames.data.remote.dto.TwitchTokenDto
import retrofit2.http.POST
import retrofit2.http.Query

interface TwitchAuthApi {
    @POST("oauth2/token")
    suspend fun getAccessToken(
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String,
        @Query("grant_type") grantType: String = "client_credentials"
    ): TwitchTokenDto
}