package com.franco.CaminaConmigo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ShakeAlertaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && context != null) {
            val activar = intent.getBooleanExtra("activar", false)
            val app = context.applicationContext as MyApplication
            if (activar) {
                app.iniciarShakeDetector()
            } else {
                app.detenerShakeDetector()
            }
        }
    }
}