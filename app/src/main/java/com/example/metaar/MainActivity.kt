package com.example.metaar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.metaar.databinding.ActivityMainBinding
import com.meta.wearable.mwdat.DeviceAccessClient
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var deviceAccessClient: DeviceAccessClient? = null

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
                val client = DeviceAccessClient.create(applicationContext)
                deviceAccessClient = client
                Log.d(TAG, "DeviceAccessClient created")

                client.connectedDevices.collect { devices ->
                    Log.d(TAG, "Connected devices: $devices")
                    val statusText = if (devices.isEmpty()) {
                        getString(R.string.status_no_devices)
                    } else {
                        val names = devices.joinToString { it.name ?: it.id }
                        getString(R.string.status_connected, names)
                    }
                    binding.statusText.text = statusText
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialise DeviceAccessClient", e)
                binding.statusText.text = getString(R.string.status_error, e.message)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceAccessClient?.close()
        deviceAccessClient = null
    }

    companion object {
        private const val TAG = "MetaAR"
    }
}
