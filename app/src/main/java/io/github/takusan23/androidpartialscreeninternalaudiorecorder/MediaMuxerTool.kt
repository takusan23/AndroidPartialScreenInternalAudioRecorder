package io.github.takusan23.androidpartialscreeninternalaudiorecorder

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object MediaMuxerTool {

    /** それぞれ音声トラック、映像トラックを取り出して、2つのトラックを一つの mp4 にする */
    @SuppressLint("WrongConstant")
    suspend fun mixAvTrack(
        audioTrackFile: File,
        videoTrackFile: File,
        resultFile: File
    ) = withContext(Dispatchers.IO) {
        // 出力先
        val mediaMuxer = MediaMuxer(resultFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val (
            audioPair,
            videoPair
        ) = listOf(
            // mimeType と ファイル
            audioTrackFile to "audio/",
            videoTrackFile to "video/"
        ).map { (file, mimeType) ->
            // MediaExtractor を作る
            val mediaExtractor = MediaExtractor().apply {
                setDataSource(file.path)
            }
            // トラックを探す
            // 今回の場合、TrackFile には 1 トラックしか無いはずだが、一応探す処理を入れる
            // 本当は音声と映像で 2 トラックあるので、どっちのデータが欲しいかをこれで切り替える
            val trackIndex = (0 until mediaExtractor.trackCount)
                .map { index -> mediaExtractor.getTrackFormat(index) }
                .indexOfFirst { mediaFormat -> mediaFormat.getString(MediaFormat.KEY_MIME)?.startsWith(mimeType) == true }
            mediaExtractor.selectTrack(trackIndex)
            // MediaExtractor と MediaFormat の返り値を返す
            mediaExtractor to mediaExtractor.getTrackFormat(trackIndex)
        }

        val (audioExtractor, audioFormat) = audioPair
        val (videoExtractor, videoFormat) = videoPair

        // MediaMuxer に追加して開始
        val audioTrackIndex = mediaMuxer.addTrack(audioFormat)
        val videoTrackIndex = mediaMuxer.addTrack(videoFormat)
        mediaMuxer.start()

        // MediaExtractor からトラックを取り出して、MediaMuxer に入れ直す処理
        listOf(
            audioExtractor to audioTrackIndex,
            videoExtractor to videoTrackIndex,
        ).forEach { (extractor, trackIndex) ->
            val byteBuffer = ByteBuffer.allocate(1024 * 4096)
            val bufferInfo = MediaCodec.BufferInfo()
            // データが無くなるまで回す
            while (isActive) {
                // データを読み出す
                val offset = byteBuffer.arrayOffset()
                bufferInfo.size = extractor.readSampleData(byteBuffer, offset)
                // もう無い場合
                if (bufferInfo.size < 0) break
                // 書き込む
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags // Lintがキレるけど黙らせる
                mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo)
                // 次のデータに進める
                extractor.advance()
            }
            // あとしまつ
            extractor.release()
        }

        // 後始末
        mediaMuxer.stop()
        mediaMuxer.release()
    }
}