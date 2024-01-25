package io.github.takusan23.androidpartialscreeninternalaudiorecorder

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.contentValuesOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaStoreTool {

    /** 端末の動画フォルダーにコピーする */
    suspend fun copyToVideoFolder(
        context: Context,
        file: File,
        fileName: String
    ) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val contentValues = contentValuesOf(
            MediaStore.MediaColumns.DISPLAY_NAME to fileName,
            MediaStore.MediaColumns.RELATIVE_PATH to "${Environment.DIRECTORY_MOVIES}/AndroidPartialScreenInternalAudioRecorder",
        )
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            file.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

}