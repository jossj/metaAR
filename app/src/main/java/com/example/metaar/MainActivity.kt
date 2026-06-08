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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
            // TODO: initialise the MWDAT DeviceAccessClient here once you have
            // registered your application on the Meta Wearables Developer Portal
            // and replaced YOUR_MWDAT_APPLICATION_ID in AndroidManifest.xml.
            //
            // Example (requires mwdat-core API):
            //   val client = DeviceAccessClient.create(applicationContext)
            //   client.connectedDevices.collect { devices -> ... }
            Log.d(TAG, "Ready to initialise Meta Wearables Device Access Toolkit")
        }
    }

    companion object {
        private const val TAG = "MetaAR"
    }
}
