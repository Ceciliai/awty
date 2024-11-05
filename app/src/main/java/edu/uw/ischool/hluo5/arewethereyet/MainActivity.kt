package edu.uw.ischool.hluo5.arewethereyet

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var messageEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var intervalEditText: EditText
    private lateinit var startStopButton: Button
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        messageEditText = findViewById(R.id.messageEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        intervalEditText = findViewById(R.id.intervalEditText)
        startStopButton = findViewById(R.id.startStopButton)


        startStopButton.setOnClickListener {
            if (isRunning) {
                stopService()
            } else {
                startService()
            }
        }
    }

    private fun startService() {
        val message = messageEditText.text.toString()
        val phone = phoneEditText.text.toString()
        val interval = intervalEditText.text.toString().toIntOrNull()

        if (message.isNotBlank() && phone.isNotBlank() && interval != null && interval > 0) {
            isRunning = true
            startStopButton.text = "Stop"
            runnable = object : Runnable {
                override fun run() {
                    showCustomToast(phone, message)
                    handler.postDelayed(this, interval * 60 * 1000L)
                }
            }
            handler.post(runnable)
        } else {

            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setTitle("Invalid Input")
            dialogBuilder.setMessage("Please enter a valid message, phone number, and interval (minutes).")
            dialogBuilder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            val alertDialog = dialogBuilder.create()
            alertDialog.show()
        }
    }

    private fun stopService() {
        isRunning = false
        startStopButton.text = "Start"
        handler.removeCallbacks(runnable)
    }

    private fun showCustomToast(phone: String, message: String) {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.custom_toast, null)


        layout.findViewById<TextView>(R.id.toastTitle).text = "Texting $phone"
        layout.findViewById<TextView>(R.id.toastMessage).text = message


        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }


}
