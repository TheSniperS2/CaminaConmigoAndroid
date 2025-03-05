package com.franco.CaminaConmigo

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.franco.CaminaConmigo.model_mvvm.mapa.view.EmergenciaDialogFragment

class MyApplication : Application(), SensorEventListener {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeThreshold = 12.0f
    private var lastShakeTime: Long = 0
    private lateinit var shakeAlertaReceiver: ShakeAlertaReceiver
    private var currentActivity: Activity? = null
    private var isAlarmActive: Boolean = false

    override fun onCreate() {
        super.onCreate()

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("configuraciones", MODE_PRIVATE)

        // Aplicar modo oscuro si está activado
        aplicarModoOscuro(sharedPreferences.getBoolean("modo_oscuro", false))

        // Inicializar el sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Iniciar el detector de shake si está activado
        if (sharedPreferences.getBoolean("shake_alerta", false)) {
            iniciarShakeDetector()
        }

        // Registrar el BroadcastReceiver
        shakeAlertaReceiver = ShakeAlertaReceiver()
        val filter = IntentFilter("com.franco.CaminaConmigo.SHAKE_ALERTA")
        registerReceiver(shakeAlertaReceiver, filter, RECEIVER_NOT_EXPORTED)

        // Registrar un callback para detectar la actividad actual
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                currentActivity = activity
            }

            override fun onActivityStarted(activity: Activity) {
                currentActivity = activity
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
                if (sharedPreferences.getBoolean("shake_alerta", false)) {
                    iniciarShakeDetector()
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (currentActivity === activity) {
                    detenerShakeDetector()
                    currentActivity = null
                }
            }

            override fun onActivityStopped(activity: Activity) {
                if (currentActivity === activity) {
                    currentActivity = null
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity === activity) {
                    currentActivity = null
                }
            }
        })
    }

    private fun aplicarModoOscuro(activar: Boolean) {
        val modo = if (activar) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(modo)
    }

    fun iniciarShakeDetector() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun detenerShakeDetector() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH
            val currentTime = System.currentTimeMillis()

            if (acceleration > shakeThreshold && currentTime - lastShakeTime > 1000) {
                lastShakeTime = currentTime
                if (sharedPreferences.getBoolean("shake_alerta", false)) {
                    activarEmergencia()
                }
            }
        }
    }

    private fun activarEmergencia() {
        if (isAlarmActive) {
            // Si la alarma ya está activa, no hacer nada
            return
        }

        try {
            Toast.makeText(this, "¡Emergencia activada!", Toast.LENGTH_SHORT).show()

            // Mostrar el diálogo de emergencia
            val emergenciaDialog = EmergenciaDialogFragment()

            // Necesitamos un contexto de actividad para mostrar el diálogo
            val activity = currentActivity
            if (activity is FragmentActivity) {
                val fragmentManager: FragmentManager = activity.supportFragmentManager
                if (fragmentManager.findFragmentByTag("EmergenciaDialogFragment") == null) {
                    emergenciaDialog.show(fragmentManager, "EmergenciaDialogFragment")
                    isAlarmActive = true
                    emergenciaDialog.setOnDismissListener(object : EmergenciaDialogFragment.OnDismissListener {
                        override fun onDismiss() {
                            isAlarmActive = false
                            if (sharedPreferences.getBoolean("shake_alerta", false)) {
                                iniciarShakeDetector()
                            }
                        }
                    })
                }
            }
        } catch (e: IllegalStateException) {
            // Manejar la excepción y mostrar un mensaje de error
            Toast.makeText(this, "Error al activar la emergencia: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se requiere implementación para este ejemplo
    }

    override fun onTerminate() {
        super.onTerminate()
        detenerShakeDetector()
        unregisterReceiver(shakeAlertaReceiver)
    }

    inner class ShakeAlertaReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null && context != null) {
                val activar = intent.getBooleanExtra("activar", false)
                if (activar) {
                    iniciarShakeDetector()
                } else {
                    detenerShakeDetector()
                }
            }
        }
    }
}