package io.github.takusan23.androidpartialscreeninternalaudiorecorder

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.Q)
class InternalAudioRecorder {
    private var audioRecord: AudioRecord? = null
    private var audioEncoder: AudioEncoder? = null
    private var mediaMuxer: MediaMuxer? = null

    var audioRecordingFile: File? = null
        private set

    /**
     * 内部音声収録の初期化をする
     *
     * @param samplingRate サンプリングレート
     * @param channelCount チャンネル数
     */
    fun prepareRecorder(
        context: Context,
        mediaProjection: MediaProjection,
        samplingRate: Int,
        channelCount: Int
    ) {
        // 内部音声取るのに使う
        val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection).apply {
            addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            addMatchingUsage(AudioAttributes.USAGE_GAME)
            addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
        }.build()
        val audioFormat = AudioFormat.Builder().apply {
            setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            setSampleRate(samplingRate)
            setChannelMask(if (channelCount == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO)
        }.build()
        audioRecord = AudioRecord.Builder().apply {
            setAudioPlaybackCaptureConfig(playbackConfig)
            setAudioFormat(audioFormat)
        }.build()
        // エンコーダーの初期化
        audioEncoder = AudioEncoder().apply {
            prepareEncoder(
                samplingRate = samplingRate,
                channelCount = channelCount
            )
        }
        // コンテナフォーマットに書き込むやつ
        audioRecordingFile = context.getExternalFilesDir(null)?.resolve("audio_track.mp4")
        mediaMuxer = MediaMuxer(audioRecordingFile!!.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    /** 録音中はコルーチンが一時停止します */
    suspend fun startRecord() = withContext(Dispatchers.Default) {
        val audioRecord = audioRecord ?: return@withContext
        val audioEncoder = audioEncoder ?: return@withContext
        val mediaMuxer = mediaMuxer ?: return@withContext

        try {
            // 録音とエンコーダーを開始する
            audioRecord.startRecording()
            var trackIndex = -1
            audioEncoder.startAudioEncode(
                onRecordInput = { byteArray ->
                    // PCM音声を取り出しエンコする
                    audioRecord.read(byteArray, 0, byteArray.size)
                },
                onOutputBufferAvailable = { byteBuffer, bufferInfo ->
                    // エンコードされたデータが来る
                    mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo)
                },
                onOutputFormatAvailable = { mediaFormat ->
                    // onOutputBufferAvailable よりも先にこちらが呼ばれるはずです
                    trackIndex = mediaMuxer.addTrack(mediaFormat)
                    mediaMuxer.start()
                }
            )
        } finally {
            // リソース開放
            audioRecord.stop()
            audioRecord.release()
            mediaMuxer.stop()
            mediaMuxer.release()
        }
    }
}