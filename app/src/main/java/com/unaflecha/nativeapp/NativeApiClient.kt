package com.unaflecha.nativeapp

import android.webkit.CookieManager
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NativeApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun cookieHeader(): String = CookieManager.getInstance().getCookie(Constants.BASE_URL).orEmpty()

    fun fetchApiToken(): String? {
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/api/token.php")
            .header("Cookie", cookieHeader())
            .header("X-Requested-With", "com.unaflecha.nativeapp")
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            val body = res.body.string()
            val json = JSONObject(body)
            if (!json.optBoolean("ok")) return null
            return json.optString("token").takeIf { it.isNotBlank() }
        }
    }

    fun pollInbox(apiToken: String): TripPayload? {
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/api/driver_inbox.php?api_token=$apiToken")
            .header("Cookie", cookieHeader())
            .build()
        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return null
            val json = JSONObject(res.body.string())
            if (!json.optBoolean("ok")) return null
            val trip = json.optJSONObject("trip") ?: return null
            return TripPayload(
                id = trip.optInt("id"),
                originText = trip.optString("origin_text"),
                destText = trip.optString("dest_text"),
                fareCup = trip.optDouble("fare_cup", 0.0),
                originLat = trip.optDouble("origin_lat", 0.0),
                originLng = trip.optDouble("origin_lng", 0.0),
                destLat = trip.optDouble("dest_lat", 0.0),
                destLng = trip.optDouble("dest_lng", 0.0)
            ).takeIf { it.id > 0 }
        }
    }

    fun ackTrip(apiToken: String, tripId: Int) {
        val body = FormBody.Builder()
            .add("api_token", apiToken)
            .add("trip_id", tripId.toString())
            .build()
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/api/driver_ack.php")
            .post(body)
            .header("Cookie", cookieHeader())
            .build()
        client.newCall(req).execute().close()
    }

    fun pingLocation(apiToken: String, lat: Double, lng: Double) {
        val body = FormBody.Builder()
            .add("api_token", apiToken)
            .add("lat", lat.toString())
            .add("lng", lng.toString())
            .build()
        val req = Request.Builder()
            .url("${Constants.BASE_URL}/api/driver_location_ping.php")
            .post(body)
            .header("Cookie", cookieHeader())
            .build()
        client.newCall(req).execute().close()
    }
}

data class TripPayload(
    val id: Int,
    val originText: String,
    val destText: String,
    val fareCup: Double,
    val originLat: Double,
    val originLng: Double,
    val destLat: Double,
    val destLng: Double
)
