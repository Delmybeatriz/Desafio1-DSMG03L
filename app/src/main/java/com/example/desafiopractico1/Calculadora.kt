package com.example.desafiopractico1

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.sqrt

class Calculadora : AppCompatActivity() {

    private lateinit var etNum1: EditText
    private lateinit var etNum2: EditText
    private lateinit var tvResultadoCalc: TextView

    private val formato = DecimalFormat("#.####")
    private val NOMBRE_ARCHIVO = "historial.txt"
    private val PERMISO_STORAGE_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculadora)

        etNum1 = findViewById(R.id.etNum1)
        etNum2 = findViewById(R.id.etNum2)
        tvResultadoCalc = findViewById(R.id.tvResultadoCalc)

        findViewById<Button>(R.id.btnSuma).setOnClickListener { operar("suma") }
        findViewById<Button>(R.id.btnResta).setOnClickListener { operar("resta") }
        findViewById<Button>(R.id.btnMultiplicacion).setOnClickListener { operar("multiplicacion") }
        findViewById<Button>(R.id.btnDivision).setOnClickListener { operar("division") }
        findViewById<Button>(R.id.btnPotencia).setOnClickListener { operar("potencia") }
        findViewById<Button>(R.id.btnRaiz).setOnClickListener { operar("raiz") }

        findViewById<Button>(R.id.btnVerHistorial).setOnClickListener { verHistorial() }
        findViewById<Button>(R.id.btnExportarHistorial).setOnClickListener { exportarHistorial() }
    }

    private fun operar(tipo: String) {
        val num1Texto = etNum1.text.toString().trim()
        if (num1Texto.isEmpty()) {
            etNum1.error = getString(R.string.error_num_invalido)
            return
        }
        val num1 = num1Texto.toDoubleOrNull()
        if (num1 == null) {
            etNum1.error = getString(R.string.error_num_invalido)
            return
        }

        // La raíz cuadrada solo necesita el primer número
        if (tipo == "raiz") {
            if (num1 < 0) {
                Toast.makeText(this, getString(R.string.error_raiz_negativa), Toast.LENGTH_LONG).show()
                return
            }
            val resultado = raizCuadrada(num1)
            mostrarResultado("√${formato.format(num1)} = ${formato.format(resultado)}")
            return
        }

        // El resto de operaciones necesita el segundo número
        val num2Texto = etNum2.text.toString().trim()
        if (num2Texto.isEmpty()) {
            etNum2.error = getString(R.string.error_num_invalido)
            return
        }
        val num2 = num2Texto.toDoubleOrNull()
        if (num2 == null) {
            etNum2.error = getString(R.string.error_num_invalido)
            return
        }

        val (simbolo, resultado) = when (tipo) {
            "suma" -> "+" to sumar(num1, num2)
            "resta" -> "-" to restar(num1, num2)
            "multiplicacion" -> "×" to multiplicar(num1, num2)
            "division" -> {
                if (num2 == 0.0) {
                    Toast.makeText(this, getString(R.string.error_division_cero), Toast.LENGTH_LONG).show()
                    return
                }
                "÷" to dividir(num1, num2)
            }
            "potencia" -> "^" to potencia(num1, num2)
            else -> return
        }

        val operacionTexto = "${formato.format(num1)} $simbolo ${formato.format(num2)} = ${formato.format(resultado)}"
        mostrarResultado(operacionTexto)
    }

    private fun mostrarResultado(texto: String) {
        tvResultadoCalc.text = "${getString(R.string.label_resultado)} $texto"
        guardarEnHistorial(texto)
    }

    // ----- Funciones matemáticas separadas -----
    private fun sumar(a: Double, b: Double) = a + b
    private fun restar(a: Double, b: Double) = a - b
    private fun multiplicar(a: Double, b: Double) = a * b
    private fun dividir(a: Double, b: Double) = a / b
    private fun potencia(a: Double, b: Double) = a.pow(b)
    private fun raizCuadrada(a: Double) = sqrt(a)

    // ----- Historial: almacenamiento interno -----
    private fun guardarEnHistorial(operacion: String) {
        try {
            openFileOutput(NOMBRE_ARCHIVO, MODE_APPEND).use { fos ->
                fos.write("$operacion\n".toByteArray())
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar historial: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verHistorial() {
        val contenido = try {
            openFileInput(NOMBRE_ARCHIVO).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }

        val mensaje = if (contenido.isBlank()) getString(R.string.historial_vacio) else contenido

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.historial_titulo))
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }

    // ----- Exportar historial a Descargas -----
    private fun exportarHistorial() {
        val contenido = try {
            openFileInput(NOMBRE_ARCHIVO).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }

        if (contenido.isBlank()) {
            Toast.makeText(this, getString(R.string.historial_vacio), Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ : usar MediaStore, no requiere permiso especial
            exportarConMediaStore(contenido)
        } else {
            // Android 9 o menor: requiere permiso WRITE_EXTERNAL_STORAGE en tiempo de ejecución
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISO_STORAGE_CODE
                )
            } else {
                exportarLegacy(contenido)
            }
        }
    }

    private fun exportarConMediaStore(contenido: String) {
        try {
            val resolver = contentResolver
            val valores = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "historial_calculadora.txt")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(contenido.toByteArray()) }
                Toast.makeText(this, getString(R.string.historial_exportado_ok), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, getString(R.string.historial_exportado_error), Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.historial_exportado_error), Toast.LENGTH_LONG).show()
        }
    }

    private fun exportarLegacy(contenido: String) {
        try {
            @Suppress("DEPRECATION")
            val carpetaDescargas = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val archivo = File(carpetaDescargas, "historial_calculadora.txt")
            archivo.writeText(contenido)
            Toast.makeText(this, getString(R.string.historial_exportado_ok), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.historial_exportado_error), Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISO_STORAGE_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportarHistorial()
            } else {
                Toast.makeText(this, getString(R.string.permiso_denegado_storage), Toast.LENGTH_LONG).show()
            }
        }
    }
}