package com.topsort.analytics.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import kotlin.concurrent.thread

/**
 * The one layer that talks to production, exercised against a real socket. A hand-rolled server
 * rather than a test dependency: com.sun.net.httpserver does not resolve on the Android unit-test
 * classpath (verified - it is a compile error). Bodies are ASCII, which is what lets the
 * ISO-8859-1 reader treat byte counts and char counts as the same thing.
 */
class HttpClientTest {

    private class Request(val line: String, val headers: Map<String, String>, val body: String)

    private lateinit var socket: ServerSocket
    private lateinit var server: Thread
    @Volatile private var responseCode = 200
    @Volatile private var responseBody = ""
    @Volatile private var request: Request? = null
    @Volatile private var serverFailure: Throwable? = null

    @Before
    fun startServer() {
        socket = ServerSocket(0)
        server = thread(isDaemon = true) {
            runCatching { serve() }.onFailure { serverFailure = it }
        }
    }

    private fun serve() {
        socket.accept().use { client ->
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.ISO_8859_1))
            val line = reader.readLine()
            val headers = generateSequence { reader.readLine().takeIf { it.isNotEmpty() } }
                .associate { it.substringBefore(":").trim() to it.substringAfter(":").trim() }
            request = Request(line, headers, readChunkedBody(reader))
            val reason = if (responseCode == 200) "OK" else "Rejected"
            client.getOutputStream().write(
                ("HTTP/1.1 $responseCode $reason\r\nContent-Length: ${responseBody.length}\r\n" +
                    "Connection: close\r\n\r\n$responseBody").toByteArray()
            )
        }
    }

    /** HttpClient streams its body chunked (setChunkedStreamingMode), so decode that framing. */
    private fun readChunkedBody(reader: BufferedReader): String = buildString {
        while (true) {
            val size = reader.readLine().trim().toInt(16)
            if (size == 0) break
            val chunk = CharArray(size)
            var read = 0
            while (read < size) read += reader.read(chunk, read, size - read)
            append(chunk)
            reader.readLine()
        }
    }

    @After
    fun stopServer() {
        socket.close()
        server.join(5_000)
        serverFailure?.let { throw AssertionError("server thread failed", it) }
    }

    private fun client() = HttpClient("http://127.0.0.1:${socket.localPort}/v2/events")

    /** The request the server recorded; joining first makes the read structural, not lucky. */
    private fun recorded(): Request {
        server.join(5_000)
        return request ?: throw AssertionError("server recorded no request", serverFailure)
    }

    @Test
    fun `posts the body as json with bearer token and user agent`() {
        client().post("""{"clicks":[]}""", "token-123")

        val request = recorded()
        assertThat(request.line).isEqualTo("POST /v2/events HTTP/1.1")
        assertThat(request.body).isEqualTo("""{"clicks":[]}""")
        assertThat(request.headers["Authorization"]).isEqualTo("Bearer token-123")
        assertThat(request.headers["Content-Type"]).isEqualTo("application/json; charset=utf-8")
        assertThat(request.headers["Accept"]).isEqualTo("application/json")
        assertThat(request.headers["User-Agent"]).isEqualTo("topsort.kt/$LIBRARY_VERSION")
    }

    @Test
    fun `sends no Authorization header without a token`() {
        client().post("{}", null)

        assertThat(recorded().headers).doesNotContainKey("Authorization")
    }

    @Test
    fun `returns the code and body of a success`() {
        responseBody = """{"ok":true}"""

        val response = client().post("{}", null)

        assertThat(response.code).isEqualTo(200)
        assertThat(response.body).isEqualTo("""{"ok":true}""")
    }

    @Test
    fun `returns the code, message and body of a rejection`() {
        responseCode = 422
        responseBody = """{"error":"opaqueUserId is required"}"""

        val response = client().post("{}", null)

        assertThat(response.code).isEqualTo(422)
        assertThat(response.message).isEqualTo("Rejected")
        assertThat(response.body).isEqualTo("""{"error":"opaqueUserId is required"}""")
    }
}
