package edu.uw.ischool.hluo5.arewethereyet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManageri
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 1
        private const val READ_PHONE_STATE_PERMISSION_REQUEST_CODE = 2
        private const val TAG = "MainActivity"
    }

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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                isRunning = true
                startStopButton.text = getString(R.string.stop_text)
                runnable = object : Runnable {
                    override fun run() {
                        Log.d(TAG, "Attempting to send SMS to $phone with message: $message")
                        sendSms(phone, message)
                        handler.postDelayed(this, interval * 60 * 1000L)
                    }
                }
                handler.post(runnable)
                Log.d(TAG, "SMS service started with interval $interval minutes")
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_REQUEST_CODE)
                Log.d(TAG, "SMS permission not granted, requesting permission")
            }
        } else {
            showInvalidInputDialog()
            Log.d(TAG, "Invalid input provided, cannot start service")
        }
    }

    private fun stopService() {
        isRunning = false
        startStopButton.text = getString(R.string.start_text)
        handler.removeCallbacks(runnable)
        Log.d(TAG, "SMS service stopped")
    }

    private fun sendSms(phone: String, message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), READ_PHONE_STATE_PERMISSION_REQUEST_CODE)
            Log.d(TAG, "READ_PHONE_STATE permission not granted, requesting permission")
            return
        }

        try {
            val subscriptionManager = getSystemService(SubscriptionManager::class.java)
            val subscriptionId = subscriptionManager.activeSubscriptionInfoList.firstOrNull()?.subscriptionId
                ?: SubscriptionManager.getDefaultSubscriptionId()

            val smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            smsManager.sendTextMessage(phone, null, message, null, null)

            Toast.makeText(this, getString(R.string.message_sent, phone), Toast.LENGTH_SHORT).show()
            Log.d(TAG, "SMS sent successfully to $phone")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.message_failed), Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Failed to send SMS: ${e.message}")
        }
    }

    private fun showInvalidInputDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(getString(R.string.invalid_input_title))
        dialogBuilder.setMessage(getString(R.string.invalid_input_message))
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_PHONE_STATE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSms(phoneEditText.text.toString(), messageEditText.text.toString())
                    Log.d(TAG, "READ_PHONE_STATE permission granted, retrying SMS send")
                } else {
                    Toast.makeText(this, "Permission denied to access phone state", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "READ_PHONE_STATE permission denied")
                }
            }
            SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startService()
                    Log.d(TAG, "SEND_SMS permission granted, starting service")
                } else {
                    Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "SEND_SMS permission denied")
                }
            }
        }
    }
}
