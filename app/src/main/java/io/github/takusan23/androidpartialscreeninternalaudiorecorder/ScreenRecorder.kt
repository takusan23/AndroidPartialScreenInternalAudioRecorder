package io.github.takusan23.androidpartialscreeninternalaudiorecorder

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import java.io.File

/** 画面録画のためのクラス */
class ScreenRecorder(
    private val context: Context,
    private val resultCode: Int,
    private val resultData: Intent
) {
    private val mediaProjectionManager by lazy { context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }

    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var videoRecordingFile: File? = null
    private var virtualDisplay: VirtualDisplay? = null

    /** 録画を開始する */
    fun startRecord() {
        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()).apply {
            // 呼び出し順が存在します
            // 音声トラックは録画終了時にやります
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(6_000_000)
            setVideoFrameRate(60)
            // 解像度、縦動画の場合は、代わりに回転情報を付与する（縦横の解像度はそのまま）
            setVideoSize(VIDEO_WIDTH, VIDEO_HEIGHT)
            // 保存先。
            // sdcard/Android/data/{アプリケーションID} に保存されますが、後で端末の動画フォルダーに移動します
            videoRecordingFile = context.getExternalFilesDir(null)?.resolve("video_track.mp4")
            setOutputFile(videoRecordingFile!!.path)
            prepare()
        }

        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData).apply {
            // 画面録画中のコールバック
            registerCallback(object : MediaProjection.Callback() {
                override fun onCapturedContentResize(width: Int, height: Int) {
                    super.onCapturedContentResize(width, height)
                    // サイズが変化したら呼び出される
                }

                override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
                    super.onCapturedContentVisibilityChanged(isVisible)
                    // 録画中の画面の表示・非表示が切り替わったら呼び出される
                }

                override fun onStop() {
                    super.onStop()
                    // MediaProjection 終了時
                    // do nothing
                }
            }, null)
        }
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "io.github.takusan23.androidpartialscreeninternalaudiorecorder",
            VIDEO_WIDTH,
            VIDEO_HEIGHT,
            context.resources.configuration.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null,
            null
        )
        // 録画開始
        mediaRecorder?.start()
    }

    /** 録画を終了する */
    fun stopRecord() {
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaProjection?.stop()
        virtualDisplay?.release()
    }

    companion object {
        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720
    }
}