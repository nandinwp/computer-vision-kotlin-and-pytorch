package com.demo.loginapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.demo.loginapp.ui.theme.LoginAppTheme

class MainActivity : ComponentActivity() {

    lateinit var usernameEditText: EditText
    lateinit var passwordEditText: EditText
    lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        usernameEditText = this.findViewById(R.id.username_edit_text)
        passwordEditText = this.findViewById(R.id.password_input)
        loginButton = this.findViewById(R.id.login_btn)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            Log.i("Test Credentials", "Username: $username, Password: $password")

            if (username == "ai.teste" && password == "1234") {
                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Credenciais inválidas", Toast.LENGTH_LONG).show()
            }
        }
    }
}