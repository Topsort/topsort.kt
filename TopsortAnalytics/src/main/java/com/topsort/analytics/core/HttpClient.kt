package com.topsort.analytics.core

import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.zip.GZIPOutputStream

const val LIBRARY_VERSION = 1.0

data class HttpResponse(
    val code : Int,
    val message : String,
    val body: String? = null,
){
    fun isSuccessful() : Boolean {
        return code in 200..299
    }
}

class HttpClient (
    private val apiHost: String,
    private val requestFactory: RequestFactory = RequestFactory()
) {

    fun post(body: String, bearerToken: String?): HttpResponse {
        val connection: HttpURLConnection = requestFactory.upload(apiHost, bearerToken)
        val postConnection = connection.createPostConnection()
        val writeStream = postConnection.outputStream!!.bufferedWriter()

        writeStream.write(body)
        writeStream.flush()
        postConnection.outputStream.close()

        if (connection.responseCode !in 200..299) {
            postConnection.close()
            return HttpResponse(connection.responseCode, connection.responseMessage)
        }

        val inputStream =
            try {
                connection.inputStream
            } catch (ignored: IOException) {
                connection.errorStream
            }

        val responseBody = inputStream?.bufferedReader()?.use(BufferedReader::readText)
        return HttpResponse(connection.responseCode, connection.responseMessage, responseBody)
    }
}

/**
 * Wraps an HTTP connection. Callers can either read from the connection via the [ ] or write to the connection via [OutputStream].
 */
abstract class Connection(
    private val connection: HttpURLConnection,
    val outputStream: OutputStream? = null,
) : Closeable {
    @Throws(IOException::class)
    override fun close() {
        connection.disconnect()
    }
}

internal fun HttpURLConnection.createPostConnection(): Connection {
    val encoding = getRequestProperty("Content-Encoding") ?: ""
    val outputStream: OutputStream =
        if (encoding.contains("gzip")) {
            GZIPOutputStream(this.outputStream)
        } else {
            this.outputStream
        }

    return object : Connection(this, outputStream) {
        @Throws(IOException::class)
        override fun close() {
            super.close()
            this.outputStream?.close()
        }
    }
}

class RequestFactory {

    fun upload(apiHost: String, bearerToken : String?): HttpURLConnection {
        val connection: HttpURLConnection = openConnection(apiHost)
        connection.requestMethod = "POST"
        bearerToken?.let {
            connection.setRequestProperty("Authorization", "Bearer $bearerToken")
        }
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty(
            "User-Agent",
            "topsort.kt/$LIBRARY_VERSION"
        )
        connection.doOutput = true
        connection.setChunkedStreamingMode(0)
        return connection
    }

    private fun openConnection(url: String): HttpURLConnection {
        val requestedURL: URL = try {
            URL(url)
        } catch (e: MalformedURLException) {
            val error = IOException("Attempted to use malformed url: $url", e)
            throw error
        }
        val connection = requestedURL.openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000 // 15s
        connection.readTimeout = 20_1000 // 20s
        return connection
    }
}
