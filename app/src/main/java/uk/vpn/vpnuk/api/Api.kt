package com.syject.scout.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import uk.vpn.vpnuk.model.*
import uk.vpn.vpnuk.model.subscriptionModel.SubscriptionsModel

interface Api {

    @Headers("Content-Type: application/json")
    @POST("wp-json/vpnuk/v1/customers")
    fun registerNewCustomer(@Body body: RegisterModel): Call<ErrorModel>


    @FormUrlEncoded
    @POST("wp-json/vpnuk/v1/token")
    fun getToken(
        @Field("grant_type") grant: String,
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<TokenModelResp>


    @Headers("Content-Type: application/json")
    @POST("wp-json/vpnuk/v1/subscriptions")
    fun createSubscription(@Header("Authorization") token: String, @Body body: CreateSubscriptionModel): Call<CreatedSubscriptionResp>


    @GET("wp-json/vpnuk/v1/subscriptions/{productId}")
    fun getSubInfo(@Header("Authorization") token: String, @Path("productId") productId: String): Call<SubscriptionsModel>


    @Headers("Content-Type: application/json")
    @POST("wp-json/vpnuk/v1/amzinapp/purchase/order/{productId}")
    fun renewSubscription(
        @Header("Authorization") token: String,
        @Body body: String,
        @Path("productId") productId: String
    ): Call<String>
}