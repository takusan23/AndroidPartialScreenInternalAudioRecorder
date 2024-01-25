package io.github.takusan23.androidpartialscreeninternalaudiorecorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val mediaProjectionManager = remember { context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }

    // 内部音声収録のためにマイク権限を貰う
    val permissionResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGrant ->
            if (isGrant) {
                Toast.makeText(context, "許可されました", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // 画面共有の合否
    val mediaProjectionResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            // 許可がもらえたら
            if (result.resultCode == ComponentActivity.RESULT_OK && result.data != null) {
                ScreenRecordService.startService(context, result.resultCode, result.data!!)
            }
        }
    )

    // マイク権限無ければ貰う
    LaunchedEffect(key1 = Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionResult.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column {
        Button(onClick = {
            // 許可をリクエスト
            mediaProjectionResult.launch(mediaProjectionManager.createScreenCaptureIntent())
        }) {
            Text(text = "録画サービス起動")
        }

        Button(onClick = {
            ScreenRecordService.stopService(context)
        }) {
            Text(text = "録画サービス終了")
        }
    }
}