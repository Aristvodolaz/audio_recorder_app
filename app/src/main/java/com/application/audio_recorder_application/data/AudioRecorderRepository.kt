import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

class AudioRecorderRepository @Inject constructor(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private val _isRecordingFlow = MutableStateFlow(false)
    val isRecordingFlow = _isRecordingFlow.asStateFlow()
    private var mediaPlayer: MediaPlayer? = null

    // Флаг для отслеживания состояния паузы
    private var isPaused = false

    fun startRecording(outputFileName: String) {
        val outputFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "$outputFileName.3gp")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
            _isRecordingFlow.value = true
        }
        isPaused = false
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null && !isPaused) {
            mediaRecorder?.pause()
            isPaused = true
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null && isPaused) {
            mediaRecorder?.resume()
            isPaused = false
        }
    }

    fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        _isRecordingFlow.value = false
        isPaused = false
    }

    fun playRecording(file: File) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
        }
    }

    fun saveTranscription(transcription: String, fileName: String) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "$fileName.txt")
        file.writeText(transcription)
    }

    fun getRecordingsList(): List<File> {
        return context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.listFiles()?.toList() ?: emptyList()
    }
}
