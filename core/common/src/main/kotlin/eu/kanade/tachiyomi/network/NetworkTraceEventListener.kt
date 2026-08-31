package eu.kanade.tachiyomi.network

import android.util.Log
import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.Protocol
import okhttp3.Response
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.atomic.AtomicLong

/**
 * Debug-only network lifecycle tracing for Mihon compatibility experiments.
 *
 * This listener is observational only: it does not mutate requests, cookies,
 * connection pooling, TLS configuration, or retry behavior.
 */
class NetworkTraceEventListener(
    private val initialCall: Call,
) : EventListener() {

    private val callId = nextCallId.getAndIncrement()

    private fun log(
        event: String,
        extra: String = "",
    ) {
        val request = initialCall.request()
        val url = request.url

        Log.i(
            TAG,
            buildString {
                append("callId=")
                append(callId)
                append(" event=")
                append(event)
                append(" method=")
                append(request.method)
                append(" host=")
                append(url.host)
                append(" path=")
                append(url.encodedPath)

                if (extra.isNotEmpty()) {
                    append(' ')
                    append(extra)
                }
            },
        )
    }

    override fun callStart(call: Call) {
        log("callStart")
    }

    override fun dnsStart(call: Call, domainName: String) {
        log("dnsStart", "domain=$domainName")
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        log("dnsEnd", "domain=$domainName addresses=${inetAddressList.joinToString()}")
    }

    override fun connectStart(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
    ) {
        log("connectStart", "remote=$inetSocketAddress proxy=$proxy")
    }

    override fun secureConnectStart(call: Call) {
        log("secureConnectStart")
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        log(
            "secureConnectEnd",
            "tlsVersion=${handshake?.tlsVersion} cipherSuite=${handshake?.cipherSuite}",
        )
    }

    override fun connectEnd(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
    ) {
        log("connectEnd", "remote=$inetSocketAddress proxy=$proxy protocol=$protocol")
    }

    override fun connectionAcquired(call: Call, connection: Connection) {
        log(
            "connectionAcquired",
            "connectionId=${System.identityHashCode(connection)} " +
                "protocol=${connection.protocol()} route=${connection.route()}",
        )
    }

    override fun connectionReleased(call: Call, connection: Connection) {
        log(
            "connectionReleased",
            "connectionId=${System.identityHashCode(connection)}",
        )
    }

    override fun requestHeadersStart(call: Call) {
        log("requestHeadersStart")
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        log("responseHeadersEnd", "httpCode=${response.code}")
    }

    override fun callEnd(call: Call) {
        log("callEnd")
    }

    override fun callFailed(call: Call, ioe: IOException) {
        log(
            "callFailed",
            "error=${ioe.javaClass.simpleName}:${ioe.message}",
        )
    }

    companion object {
        const val TAG = "MihonNetTrace"

        private val nextCallId = AtomicLong(1)

        val factory = EventListener.Factory { call ->
            NetworkTraceEventListener(call)
        }
    }
}
