package com.TOTOMOFYP.VTOAPP.repositories

import android.util.Base64
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object KlingAIVirtualTryOn {

    private const val BASE_URL = "https://api.klingai.com"
    private const val CREATE_ENDPOINT = "/v1/images/kolors-virtual-try-on"
    private const val STATUS_ENDPOINT = "/v1/images/kolors-virtual-try-on/"
    private val ACCESS_KEY = BuildConfig.KLING_ACCESS_KEY
    private val SECRET_KEY = BuildConfig.KLING_SECRET_KEY

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun generateJwtToken(): String {
        val header = JSONObject().apply {
            put("alg", "HS256")
            put("typ", "JWT")
        }

        val currentTime = System.currentTimeMillis() / 1000
        val payload = JSONObject().apply {
            put("iss", ACCESS_KEY)
            put("exp", currentTime + 1800) // Token valid for 30 minutes
            put("nbf", currentTime - 5)    // Token valid 5 seconds before current time
        }

        val headerBase64 = Base64.encodeToString(header.toString().toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
        val payloadBase64 = Base64.encodeToString(payload.toString().toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
        val signatureInput = "$headerBase64.$payloadBase64"

        val hmacSha256 = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(), "HmacSHA256")
        hmacSha256.init(secretKeySpec)
        val signatureBytes = hmacSha256.doFinal(signatureInput.toByteArray())
        val signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP or Base64.URL_SAFE)

        return "$headerBase64.$payloadBase64.$signatureBase64"
    }

    fun imageFileToBase64(file: File): String {
        val bytes = file.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun inputStreamToBase64(inputStream: InputStream): String {
        val bytes = inputStream.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    suspend fun downloadImageToBase64(imageUrl: String): String = withContext(Dispatchers.IO) {
        val url = URL(imageUrl)
        val connection = url.openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.connect()
        val inputStream = connection.getInputStream()
        val bytes = inputStream.readBytes()
        inputStream.close()
        return@withContext Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    suspend fun createVirtualTryOnTask(
        humanImageBase64: String,
        clothImageBase64: String? = null,
        modelName: String = "kolors-virtual-try-on-v1",
        callbackUrl: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val jwtToken = generateJwtToken()

            val requestBodyJson = JSONObject().apply {
                put("human_image", humanImageBase64)
                put("model_name", modelName)
                clothImageBase64?.let { put("cloth_image", it) }
                callbackUrl?.let { put("callback_url", it) }
            }

            val mediaType = "application/json".toMediaType()
            val body = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(BASE_URL + CREATE_ENDPOINT)
                .addHeader("Authorization", "Bearer $jwtToken")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    println("Request failed with status code: ${response.code}")
                    println("Response body: $responseBody")
                    return@withContext null
                }

                val jsonResponse = JSONObject(responseBody)

                // Check if the response contains an error
                if (jsonResponse.has("code") && jsonResponse.getInt("code") != 0) {
                    val errorMessage = jsonResponse.optString("message", "Unknown error")
                    println("API Error: $errorMessage")
                    return@withContext null
                }

                // Correctly extract task_id from the response according to the API documentation
                return@withContext jsonResponse.getJSONObject("data").getString("task_id")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun pollTaskStatus(taskId: String): String? = withContext(Dispatchers.IO) {
        val jwtToken = generateJwtToken()
        val request = Request.Builder()
            .url(BASE_URL + STATUS_ENDPOINT + taskId)
            .addHeader("Authorization", "Bearer $jwtToken")
            .get()
            .build()

        var result: String? = null

        while (true) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Status request failed with status code: ${response.code}")
                    result = null
                    return@withContext null
                }

                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody)

                // Get data object first, then task_status from it, according to API documentation
                val data = jsonResponse.getJSONObject("data")
                val status = data.getString("task_status")
                println("Current status: $status")

                if (status.equals("succeed", ignoreCase = true)) {
                    // Extract the image URL from the response
                    try {
                        val taskResult = data.getJSONObject("task_result")
                        val images = taskResult.getJSONArray("images")
                        if (images.length() > 0) {
                            val firstImage = images.getJSONObject(0)
                            val imageUrl = firstImage.getString("url")
                            result = imageUrl
                            return@withContext imageUrl
                        }
                    } catch (e: Exception) {
                        println("Error extracting image URL: ${e.message}")
                    }
                    result = null
                    return@withContext null
                } else if (status.equals("failed", ignoreCase = true)) {
                    val statusMsg = data.optString("task_status_msg", "Task failed with no specific reason.")
                    println("Task failed: $statusMsg")
                    result = null
                    return@withContext null
                } else {
                    println("Task still in progress. Current status: $status")
                }
            }
            delay(5000) // Wait for 5 seconds before polling again
        }

        // This line is needed to satisfy the compiler, but it will never be reached
        return@withContext result
    }
}
