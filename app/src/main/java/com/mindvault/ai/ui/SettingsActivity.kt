package com.mindvault.ai.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.mindvault.ai.R
import com.mindvault.ai.ml.SettingsStore

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val editServer = findViewById<EditText>(R.id.editServer)
        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // preload
        editServer.setText(SettingsStore.server(this) ?: "")
        editEmail.setText(SettingsStore.email(this) ?: "")
        editPassword.setText(SettingsStore.password(this) ?: "")

        btnSave.setOnClickListener {
            SettingsStore.save(this,
                editServer.text.toString(),
                editEmail.text.toString(),
                editPassword.text.toString())
            finish()
        }
    }
}
