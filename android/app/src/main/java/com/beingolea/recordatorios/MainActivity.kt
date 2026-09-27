package com.beingolea.recordatorios

import androidx.activity.ComponentActivity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.onesignal.OneSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class FamilyMember(val id: String, val name: String)

class MainActivity : ComponentActivity() {
    private val green = Color.rgb(20, 111, 101)
    private val dark = Color.rgb(27, 54, 51)
    private val prefs by lazy { getSharedPreferences("family", MODE_PRIVATE) }
    private lateinit var codeInput: EditText
    private lateinit var memberSpinner: Spinner
    private lateinit var status: TextView
    private lateinit var members: List<FamilyMember>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(239, 247, 244)
        window.navigationBarColor = Color.rgb(239, 247, 244)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        showSetup()
        val savedId = prefs.getString("member_id", null)
        val savedCode = prefs.getString("join_code", null)
        if (savedId != null && !savedCode.isNullOrBlank()) registerExistingMember(savedId)
        handleBirthdayLink(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleBirthdayLink(intent)
    }

    private fun handleBirthdayLink(source: android.content.Intent?) {
        val birthdayId = source?.data?.takeIf { it.scheme == "beingolea" && it.host == "birthday" }
            ?.lastPathSegment ?: source?.getStringExtra("birthday_id") ?: return
        val code = prefs.getString("join_code", null)
        if (code.isNullOrBlank()) {
            status.text = "Abre la app, ingresa el código familiar y vuelve a tocar el aviso para ver el saludo."
            return
        }
        lifecycleScope.launch {
            try {
                val birthday = withContext(Dispatchers.IO) { getBirthday(birthdayId, code) }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Hoy saludamos a ${birthday.first} 🎂")
                    .setMessage(birthday.second)
                    .setPositiveButton("Listo", null)
                    .setNeutralButton("Abrir grupo de WhatsApp") { _, _ ->
                        val groupUrl = birthday.third
                        if (groupUrl.isNotBlank()) startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(groupUrl)))
                    }
                    .show()
            } catch (e: Exception) {
                status.text = e.message ?: "No pudimos abrir el saludo. Comprueba tu conexión."
            }
        }
    }

    private fun showSetup() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(32), dp(24), dp(28))
            setBackgroundColor(Color.rgb(239, 247, 244))
        }
        page.addView(label("FAMILIA BEINGOLEA", 14, green, true))
        page.addView(label("Que no se nos pase saludar 🎂", 27, dark, true), margin(0, 5, 0, 8))
        page.addView(label("Activa el aviso de cumpleaños en este teléfono. No necesitamos correo ni número.", 16, Color.DKGRAY, false), margin(0, 0, 0, 24))
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(Color.WHITE)
        }
        card.addView(label("Código familiar", 16, dark, true))
        codeInput = EditText(this).apply {
            hint = "Pide el código al administrador"
            setSingleLine(true)
            setText(prefs.getString("join_code", ""))
        }
        card.addView(codeInput, margin(0, 6, 0, 12))
        val load = Button(this).apply { text = "Continuar"; setTextColor(Color.WHITE); setBackgroundColor(green) }
        card.addView(load, matchWrap())
        memberSpinner = Spinner(this).apply { visibility = View.GONE }
        card.addView(memberSpinner, margin(0, 14, 0, 4))
        val activate = Button(this).apply {
            text = "Activar notificaciones"
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            visibility = View.GONE
        }
        card.addView(activate, matchWrap())
        status = label("El aviso se envía automáticamente el día del cumpleaños.", 14, Color.DKGRAY, false)
        card.addView(status, margin(0, 16, 0, 0))
        page.addView(card)
        page.addView(label("Los recordatorios llegan como una notificación de Android. Puedes cambiar el sonido en Ajustes > Notificaciones > Cumpleaños de la familia.", 14, Color.DKGRAY, false), margin(2, 22, 2, 0))
        val scroll = ScrollView(this).apply { addView(page) }
        setContentView(scroll)

        load.setOnClickListener {
            hideKeyboard()
            fetchMembers(codeInput.text.toString().trim(), memberSpinner, activate)
        }
        activate.setOnClickListener { activatePush() }
    }

    private fun fetchMembers(code: String, spinner: Spinner, activate: Button) {
        if (code.isBlank()) { status.text = "Escribe el código familiar que te dio el administrador."; return }
        status.text = "Buscando a la familia…"
        lifecycleScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { getMembers(code) }
                if (loaded.isEmpty()) throw IllegalStateException("Todavía no hay familiares en la lista.")
                members = loaded
                prefs.edit().putString("join_code", code).apply()
                spinner.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, loaded.map { it.name })
                spinner.visibility = View.VISIBLE
                activate.visibility = View.VISIBLE
                status.text = "Elige tu nombre y activa el permiso de notificaciones."
            } catch (e: Exception) {
                status.text = e.message ?: "No se pudo conectar. Revisa el código y tu conexión."
            }
        }
    }

    private suspend fun getMembers(code: String): List<FamilyMember> {
        val url = URL(BuildConfig.API_BASE_URL + "/api/members?code=" + java.net.URLEncoder.encode(code, "UTF-8"))
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
        }
        try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val result = JSONObject(text)
            if (responseCode !in 200..299) throw IllegalStateException(result.optString("error", "Código no válido."))
            val array = result.optJSONArray("members") ?: return emptyList()
            return (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                FamilyMember(item.getString("id"), item.getString("name"))
            }
        } finally { connection.disconnect() }
    }

    private fun activatePush() {
        if (!::members.isInitialized || memberSpinner.selectedItemPosition !in members.indices) {
            status.text = "Elige tu nombre para continuar."
            return
        }
        val member = members[memberSpinner.selectedItemPosition]
        status.text = "Activando notificaciones…"
        OneSignal.login(member.id)
        lifecycleScope.launch {
            try {
                val granted = OneSignal.Notifications.requestPermission(false)
                if (!granted) {
                    status.text = "Permite las notificaciones de Cumpleaños Beingolea en Ajustes del teléfono."
                    return@launch
                }
                prefs.edit().putString("member_id", member.id).putString("member_name", member.name).apply()
                status.text = "¡Listo, ${member.name}! Este teléfono recibirá un aviso visible el día del cumpleaños."
            } catch (_: Exception) {
                status.text = "No se pudo activar el permiso. Revisa Ajustes > Notificaciones."
            }
        }
    }

    private fun getBirthday(id: String, code: String): Triple<String, String, String> {
        val query = "id=" + java.net.URLEncoder.encode(id, "UTF-8") + "&code=" + java.net.URLEncoder.encode(code, "UTF-8")
        val connection = (URL(BuildConfig.API_BASE_URL + "/api/birthday?" + query).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
        }
        try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val result = JSONObject(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            if (responseCode !in 200..299) throw IllegalStateException(result.optString("error", "No encontramos el saludo."))
            return Triple(result.optString("name"), result.optString("message"), result.optString("groupUrl"))
        } finally { connection.disconnect() }
    }

    private fun registerExistingMember(id: String) {
        OneSignal.login(id)
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        gravity = Gravity.START
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun margin(left: Int, top: Int, right: Int, bottom: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(left), dp(top), dp(right), dp(bottom))
        }
    private fun matchWrap() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(codeInput.windowToken, 0)
    }
}
