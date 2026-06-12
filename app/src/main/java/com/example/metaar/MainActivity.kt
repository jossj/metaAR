package com.example.metaar

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.metaar.databinding.ActivityMainBinding
import com.meta.wearable.mwdat.DeviceAccessClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mjpegServer: MjpegServer
    private var deviceAccessClient: DeviceAccessClient? = null
    private var cameraJob: Job? = null

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeWearableSession()
        } else {
            Log.w(TAG, "One or more Bluetooth permissions denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mjpegServer = MjpegServer()
        try {
            mjpegServer.start()
            Log.d(TAG, "MJPEG server started on port ${MjpegServer.PORT}")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start MJPEG server", e)
        }

        ensureBluetoothPermissions()
    }

    private fun ensureBluetoothPermissions() {
        val required = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            initializeWearableSession()
        } else {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    private fun initializeWearableSession() {
        lifecycleScope.launch {
            try {
                Wearables.initialize(applicationContext)
                Log.d(TAG, "Wearables initialized")

                Wearables.devices.collect { devices ->
                    Log.d(TAG, "Connected devices: $devices")

                    cameraJob?.cancel()
                    cameraJob = null

                    if (devices.isEmpty()) {
                        binding.statusText.text = getString(R.string.status_no_devices)
                        return@collect
                    }

                    val ip = getLocalIpAddress() ?: "device-ip"
                    val url = "http://$ip:${MjpegServer.PORT}"
                    binding.statusText.text = getString(R.string.status_streaming, url)
                    Log.d(TAG, "Stream available at $url")

                    cameraJob = lifecycleScope.launch(Dispatchers.Default) {
                        // openCameraStream() returns a Flow of Bitmap frames from the glasses camera.
                        // Requires the mwdat-camera artifact and a device with camera capability.
                        devices.first().openCameraStream().frames.collect { bitmap ->
                            mjpegServer.pushFrame(bitmap.toJpegBytes())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Wearables", e)
                binding.statusText.text = getString(R.string.status_error, e.message)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraJob?.cancel()
        deviceAccessClient?.close()
        deviceAccessClient = null
        mjpegServer.stop()
    }

    private fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.asSequence()
                ?.flatMap { it.inetAddresses.asSequence() }
                ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "MetaAR"
    }
}

private fun Bitmap.toJpegBytes(quality: Int = 80): ByteArray =
    ByteArrayOutputStream().also { compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
