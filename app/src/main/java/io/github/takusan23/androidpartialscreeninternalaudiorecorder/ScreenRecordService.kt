package io.github.takusan23.androidpartialscreeninternalaudiorecorder

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/** 画面録画サービス */
class ScreenRecordService : Service() {
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }

    private var screenRecorder: ScreenRecorder? = null

    override fun onBind(intent: Intent): IBinder? = null

override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // 通知を出す
    notifyForegroundServiceNotification()

    // Intent から値を取り出す
    if (intent?.getBooleanExtra(INTENT_KEY_START_OR_STOP, false) == true) {
        // 開始命令
        screenRecorder = ScreenRecorder(
            context = this,
            resultCode = intent.getIntExtra(INTENT_KEY_MEDIA_PROJECTION_RESULT_CODE, -1),
            resultData = intent.getParcelableExtra(INTENT_KEY_MEDIA_PROJECTION_RESULT_DATA)!!
        )
        screenRecorder?.startRecord()
    } else {
        // 終了命令
        screenRecorder?.stopRecord()
        stopSelf()
    }

    return START_NOT_STICKY
}

    private fun notifyForegroundServiceNotification() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW).apply {
                setName("画面録画サービス起動中通知")
            }.build()
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentTitle("画面録画")
            setContentText("録画中です")
            setSmallIcon(R.drawable.ic_launcher_foreground)
        }.build()

        // ForegroundServiceType は必須です
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, 0)
        }
    }

    companion object {
        // 通知関連
        private const val CHANNEL_ID = "screen_recorder_service"
        private const val NOTIFICATION_ID = 4545

        // Intent に MediaProjection の結果を入れるのでそのキー
        private const val INTENT_KEY_MEDIA_PROJECTION_RESULT_CODE = "result_code"
        private const val INTENT_KEY_MEDIA_PROJECTION_RESULT_DATA = "result_code"

        // サービス終了時は、終了しろというフラグを Intent に入れて、startService することにする
        // 多分 onDestroy でファイル操作とかやっちゃいけない気がする
        // true だったら start 、false だったら stop
        private const val INTENT_KEY_START_OR_STOP = "start_or_stop"

        fun startService(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenRecordService::class.java).apply {
                putExtra(INTENT_KEY_MEDIA_PROJECTION_RESULT_CODE, resultCode)
                putExtra(INTENT_KEY_MEDIA_PROJECTION_RESULT_DATA, data)
                putExtra(INTENT_KEY_START_OR_STOP, true)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ScreenRecordService::class.java).apply {
                putExtra(INTENT_KEY_START_OR_STOP, false)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}