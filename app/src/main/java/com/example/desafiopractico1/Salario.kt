package com.example.desafiopractico1

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class Salario : AppCompatActivity() {

    private lateinit var etNombreEmpleado: EditText
    private lateinit var etSalarioBase: EditText
    private lateinit var tvSalarioBruto: TextView
    private lateinit var tvRenta: TextView
    private lateinit var tvAfp: TextView
    private lateinit var tvIsss: TextView
    private lateinit var tvSalarioNeto: TextView

    //Formato Decimal
    private val formato = DecimalFormat("#.##")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_salario)

        etNombreEmpleado = findViewById(R.id.etNombreEmpleado)
        etSalarioBase = findViewById(R.id.etSalarioBase)
        tvSalarioBruto = findViewById(R.id.tvSalarioBruto)
        tvRenta = findViewById(R.id.tvRenta)
        tvAfp = findViewById(R.id.tvAfp)
        tvIsss = findViewById(R.id.tvIsss)
        tvSalarioNeto = findViewById(R.id.tvSalarioNeto)

        val btnCalcular = findViewById<Button>(R.id.btnCalcularSalario)
        btnCalcular.setOnClickListener {
            procesarCalculo()
        }
    }

    private fun procesarCalculo() {
        val nombre = etNombreEmpleado.text.toString().trim()
        val salarioTexto = etSalarioBase.text.toString().trim()

        if (nombre.isEmpty()) {
            etNombreEmpleado.error = getString(R.string.error_campo_vacio)
            return
        }

        val salario = salarioTexto.toDoubleOrNull()

        // Validar salario positivo (vacío, no numérico o negativo)
        if (salarioTexto.isEmpty() || salario == null || salario <= 0) {
            etSalarioBase.error = getString(R.string.error_salario_invalido)
            vibrarDispositivo()
            return
        }

        val renta = calcularRenta(salario)
        val afp = salario * 0.0725
        val isss = salario * 0.03
        val salarioNeto = salario - renta - afp - isss

        tvSalarioBruto.text = "${getString(R.string.label_salario_bruto)} $${formato.format(salario)}"
        tvRenta.text = "${getString(R.string.label_renta)} $${formato.format(renta)}"
        tvAfp.text = "${getString(R.string.label_afp)} $${formato.format(afp)}"
        tvIsss.text = "${getString(R.string.label_isss)} $${formato.format(isss)}"
        tvSalarioNeto.text = "${getString(R.string.label_salario_neto)} $${formato.format(salarioNeto)}"
    }

    // Función separada para calcular la Renta según tabla de tramos
    private fun calcularRenta(salario: Double): Double {
        return when {
            salario <= 472.00 -> 0.0
            salario <= 895.24 -> {
                val excedente = salario - 472.00
                17.67 + (excedente * 0.10)
            }
            salario <= 2038.10 -> {
                val excedente = salario - 895.24
                60.00 + (excedente * 0.20)
            }
            else -> {
                val excedente = salario - 2038.10
                288.57 + (excedente * 0.30)
            }
        }
    }

    private fun vibrarDispositivo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(400)
        }
    }
}