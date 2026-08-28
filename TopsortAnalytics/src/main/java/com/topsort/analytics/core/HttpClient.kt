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

// LIBRARY_VERSION is generated from VERSION_NAME - see the generateLibraryVersion task.

internal data class HttpResponse(
    val code : Int,
    val message : String,
    val body: String? = null,
){
    fun isSuccessful() : Boolean {
        @Suppress("detekt:MagicNumber")
        return code in 200..299
    }
}

internal class HttpClient (
    private val apiHost: String,
    private val requestFactory: RequestFactory = RequestFactory()
) {

    fun post(body: String, bearerToken: String?): HttpResponse {
        val connection: HttpURLConnection = requestFactory.upload(apiHost, bearerToken)
        val postConnection = connection.createPostConnection()
        try {
            val writeStream = postConnection.outputStream!!.bufferedWriter()

            writeStream.write(body)
            writeStream.flush()
            postConnection.outputStream.close()

            // On a non-2xx the body is on errorStream. Read it so the API's rejection reason
            // reaches the caller's log, but never let that read turn a definite rejection into an
            // exception - the code is the decision, the body is a courtesy.
            val code = connection.responseCode
            @Suppress("detekt:MagicNumber")
            val responseBody = if (code in 200..299) {
                connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                runCatching { connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) }
                    .getOrNull()
            }
            return HttpResponse(code, connection.responseMessage, responseBody)
        } finally {
            postConnection.close()
        }
    }
}

/**
 * Wraps an HTTP connection. Callers can either read from the connection via the [ ] or write to the connection via [OutputStream].
 */
internal abstract class Connection(
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


internal class RequestFactory {
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
        connection.connectTimeout = CONNECTION_TIMEOUT
        connection.readTimeout = READ_TIMEOUT
        return connection
    }

    companion object {
        const val CONNECTION_TIMEOUT = 15_000 // 15 seconds
        const val READ_TIMEOUT = 20_000 // 20 seconds
    }
}
