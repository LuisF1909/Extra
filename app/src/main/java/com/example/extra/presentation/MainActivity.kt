package com.example.extra.presentation

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.extra.R
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

class MainActivity : AppCompatActivity(),
    SensorEventListener,
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private var activityContext: Context? = null

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private val sensorType = Sensor.TYPE_GYROSCOPE

    lateinit var nodeID: String
    private var sensorReading: String = ""

    private lateinit var sensorTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        activityContext = this
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(sensorType)

        sensorTextView = findViewById(R.id.sensor_text_view)

        val startButton: Button = findViewById(R.id.start_sensor_btn)
        startButton.setOnClickListener {
            startSensor()
        }

        val sendButton: Button = findViewById(R.id.send_data_btn)
        sendButton.setOnClickListener {
            if (::nodeID.isInitialized && sensorReading.isNotEmpty()) {
                sendMessage()
            }
        }
    }

    private fun sendMessage() {
        Wearable.getMessageClient(activityContext!!)
            .sendMessage(nodeID, "/sensor_data", sensorReading.toByteArray())
            .addOnSuccessListener {
                Log.d("sendMessage", "Message sent successfully")
            }
            .addOnFailureListener { exception ->
                Log.d("sendMessage", "Error sending message: ${exception.message}")
            }
    }

    override fun onPause() {
        super.onPause()
        try {
            Wearable.getDataClient(activityContext!!).removeListener(this)
            Wearable.getMessageClient(activityContext!!).removeListener(this)
            Wearable.getCapabilityClient(activityContext!!).removeListener(this)
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            Log.d("onPause", e.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            Wearable.getDataClient(activityContext!!).addListener(this)
            Wearable.getMessageClient(activityContext!!).addListener(this)
            Wearable.getCapabilityClient(activityContext!!)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
        } catch (e: Exception) {
            Log.d("onResume", e.toString())
        }
    }

    override fun onDataChanged(p0: DataEventBuffer) {}

    override fun onMessageReceived(p0: MessageEvent) {}

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        val nodes = capabilityInfo.nodes
        nodes.firstOrNull()?.let { node ->
            nodeID = node.id
            Log.d("onCapabilityChanged", "Found node: ${node.displayName} ($nodeID)")
        }
    }

    private fun startSensor() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.BODY_SENSORS),
                1001
            )
            return
        }
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    override fun onSensorChanged(SE: SensorEvent?) {
        if (SE?.sensor?.type == sensorType) {
            val reading = "X: ${SE.values[0]}, Y: ${SE.values[1]}, Z: ${SE.values[2]}"
            sensorReading = reading
            sensorTextView.text = reading
            Log.d("onSensorChanged", "lectura: $reading")
        }
    }
}
