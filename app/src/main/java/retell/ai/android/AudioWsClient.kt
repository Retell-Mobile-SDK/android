package retell.ai.android

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class AudioWsClient(uri: URI, private val messageReceived: (String) -> Unit) : WebSocketClient(uri) {

    override fun onOpen(handshakedata: ServerHandshake?) {
        println("WebSocket connection opened")
    }

    override fun onMessage(message: String?) {
        message?.let { messageReceived(it) }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        println("WebSocket connection closed")
    }

    override fun onError(ex: Exception?) {
        ex?.printStackTrace()
    }

    companion object {
        fun connect(uri: String,messageReceived: (String) -> Unit): AudioWsClient {
            val client = AudioWsClient(URI(uri), messageReceived)
            client.connect()
            return client
        }
    }
}