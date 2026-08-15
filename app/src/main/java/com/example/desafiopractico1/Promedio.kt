package com.example.desafiopractico1

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.DecimalFormat

class Promedio : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etNota: List<EditText>
    private lateinit var etPonderacion: List<EditText>
    private lateinit var tvResultado: TextView

    private val CANAL_ID = "canal_resultados"
    private val PERMISO_NOTIFICACION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_promedio)

        etNombre = findViewById(R.id.etNombre)
        etNota = listOf(
            findViewById(R.id.etNota1),
            findViewById(R.id.etNota2),
            findViewById(R.id.etNota3),
            findViewById(R.id.etNota4),
            findViewById(R.id.etNota5)
        )
        etPonderacion = listOf(
            findViewById(R.id.etPonderacion1),
            findViewById(R.id.etPonderacion2),
            findViewById(R.id.etPonderacion3),
            findViewById(R.id.etPonderacion4),
            findViewById(R.id.etPonderacion5)
        )
        tvResultado = findViewById(R.id.tvResultado)

        crearCanalNotificacion()

        val btnCalcular = findViewById<Button>(R.id.btnCalcularPromedio)
        btnCalcular.setOnClickListener {
            calcularYMostrarResultado()
        }
    }

    private fun calcularYMostrarResultado() {
        // Validar nombre
        val nombre = etNombre.text.toString().trim()
        if (nombre.isEmpty()) {
            etNombre.error = getString(R.string.error_campo_vacio)
            return
        }

        val notas = DoubleArray(5)
        val ponderaciones = DoubleArray(5)

        // Validar cada nota y ponderación
        for (i in 0 until 5) {
            val notaTexto = etNota[i].text.toString().trim()
            val ponderacionTexto = etPonderacion[i].text.toString().trim()

            if (notaTexto.isEmpty()) {
                etNota[i].error = getString(R.string.error_campo_vacio)
                return
            }
            if (ponderacionTexto.isEmpty()) {
                etPonderacion[i].error = getString(R.string.error_campo_vacio)
                return
            }

            val nota = notaTexto.toDoubleOrNull()
            val ponderacion = ponderacionTexto.toDoubleOrNull()

            if (nota == null || nota < 0 || nota > 10) {
                etNota[i].error = getString(R.string.error_nota_invalida)
                return
            }
            if (ponderacion == null || ponderacion < 0 || ponderacion > 100) {
                etPonderacion[i].error = getString(R.string.error_ponderacion_invalida)
                return
            }

            notas[i] = nota
            ponderaciones[i] = ponderacion
        }

        // Validar que las ponderaciones sumen 100%
        val sumaPonderaciones = ponderaciones.sum()
        if (sumaPonderaciones != 100.0) {
            Toast.makeText(this, getString(R.string.error_suma_ponderaciones), Toast.LENGTH_LONG).show()
            return
        }

        val promedio = calcularPromedio(notas, ponderaciones)
        val aprobado = promedio >= 7.0

        val formato = DecimalFormat("#.##")
        val promedioFormateado = formato.format(promedio)

        val estado = if (aprobado) getString(R.string.resultado_aprobado) else getString(R.string.resultado_reprobado)
        val mensaje = "$nombre - Promedio: $promedioFormateado\n$estado"

        tvResultado.text = mensaje

        enviarNotificacion(promedioFormateado, aprobado)
    }

    // Función separada para calcular el promedio ponderado
    private fun esNotaValida(nota: Double?): Boolean {
        return nota != null && nota in 0.0..10.0
    }

    private fun calcularPromedio(notas: DoubleArray, ponderaciones: DoubleArray): Double {
        var sumaPonderada = 0.0
        for (i in notas.indices) {
            sumaPonderada += notas[i] * (ponderaciones[i] / 100.0)
        }
        return sumaPonderada
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                getString(R.string.notif_canal_nombre),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
    }

    private fun enviarNotificacion(promedio: String, aprobado: Boolean) {
        // En Android 13+ se debe pedir permiso en tiempo de ejecución
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISO_NOTIFICACION_CODE
                )
                return
            }
        }

        val estado = if (aprobado) getString(R.string.resultado_aprobado) else getString(R.string.resultado_reprobado)

        val notificacion = NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.notif_titulo))
            .setContentText("Promedio: $promedio - $estado")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(1, notificacion)
    }
}