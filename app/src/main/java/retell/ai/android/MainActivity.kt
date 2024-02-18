package retell.ai.android

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import retell.ai.android.ui.theme.RetellandroiddemoTheme

class MainActivity : ComponentActivity() {

    var webSocketClient: AudioWsClient? = null
    private var receivedMessage: String = ""

    val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startAudioRecording()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RetellandroiddemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RetellDemoApp()
                }
            }
        }

        webSocketClient =
                //AudioWsClient.connect("wss://socketsbay.com/wss/v2/1/demo/") { receivedMessage = it }
        //AudioWsClient.connect("ws://echo.websocket.org") { receivedMessage = it }
        AudioWsClient.connect("wss://api.retellai.com/audio-websocket/3aa35343bc9bbab9bac333efa6e018f8") { receivedMessage = it }
    }

     fun startAudioRecording() {
        Toast.makeText(this, "Start recording", Toast.LENGTH_SHORT).show()
        if (webSocketClient!=null&&webSocketClient!!.isOpen) {
            val audioRecorder = AudioRecorder()
            audioRecorder.startRecording()
        }else{
            Toast.makeText(this, "websocket is not open", Toast.LENGTH_SHORT).show()
        }
    }

    inner class AudioRecorder {
        private val sampleRate = 44100
        private val channelConfig = AudioFormat.CHANNEL_IN_MONO
        private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        private val minBufferSize =
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)


        @SuppressLint("MissingPermission")
        private val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBufferSize
        )

        fun startRecording() {
            val audioData = ByteArray(minBufferSize)
            audioRecord.startRecording()

            val job = lifecycleScope.launch(Dispatchers.IO) {
                while (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val bytesRead = audioRecord.read(audioData, 0, minBufferSize)
                    if (bytesRead > 0) {
                        sendAudioData(audioData.copyOf())
                    }
                }
            }

            lifecycleScope.launch(Dispatchers.Main) {
                job.join()
                audioRecord.stop()
                audioRecord.release()
            }
        }


        private fun sendAudioData(bytes: ByteArray) {
            // Send audio data
            webSocketClient?.send(bytes)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketClient = null
    }

    @Composable
    fun RetellDemoApp() {
        val context = LocalContext.current
        var isRecording by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startAudioRecording()
                    isRecording = true
                } else {
                    (context as MainActivity).audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                }
            }) {
                Text("Start")
            }

            Button(onClick = {
                isRecording = false
            }) {
                Text("Stop")
            }
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        RetellandroiddemoTheme {
            RetellDemoApp()
        }
    }

}



