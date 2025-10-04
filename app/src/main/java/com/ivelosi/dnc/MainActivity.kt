package com.ivelosi.dnc

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.ivelosi.dnc.data.local.NodeIdentity
import com.ivelosi.dnc.domain.repository.OwnAccountRepository
import com.ivelosi.dnc.domain.repository.OwnProfileRepository
import com.ivelosi.dnc.network.BackupApi
import com.ivelosi.dnc.network.NetworkManager
import com.ivelosi.dnc.security.E2EEManager
import com.ivelosi.dnc.ui.DNCProtocol
import com.ivelosi.dnc.ui.theme.Theme
import com.ivelosi.dnc.utils.NidGenerator
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private lateinit var ownAccountRepository: OwnAccountRepository
    private lateinit var ownProfileRepository: OwnProfileRepository
    private lateinit var networkManager: NetworkManager
    private lateinit var e2eeManager: E2EEManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (application as App).initializeContainer(this)

        requestPermissions()
        checkServices()

        ownAccountRepository = (application as App).container.ownAccountRepository
        ownProfileRepository = (application as App).container.ownProfileRepository
        networkManager = (application as App).container.networkManager

        // Initialize E2EE Manager
        e2eeManager = E2EEManager(this)
        e2eeManager.initialize()

        // Store E2EE manager in container for access throughout the app
        (application as App).container.e2eeManager = e2eeManager

        lifecycleScope.launch {
            if(ownAccountRepository.getAccount().Nid == 0L) {
                val id = NodeIdentity.nid

                ownAccountRepository.setNid(id)
                ownProfileRepository.setNid(id)
            }

            networkManager.startConnections()
            networkManager.startDiscoverPeersHandler()
            networkManager.startSendKeepaliveHandler()
            networkManager.startUpdateConnectedDevicesHandler()

            // Start periodic cleanup of expired sessions
            startSessionCleanup()
        }

        setContent {
            Theme {
                DNCProtocol()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume")

        registerReceiver(networkManager.receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause")

        unregisterReceiver(networkManager.receiver)
    }

    override fun onStop() {
        super.onStop()
        Log.d("MainActivity", "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy")

        // Clean up sessions on destroy
        lifecycleScope.launch {
            e2eeManager.clearAllSessions()
        }
    }

    private fun startSessionCleanup() {
        lifecycleScope.launch {
            // Clean up expired sessions every hour
            kotlinx.coroutines.delay(60 * 60 * 1000L)
            e2eeManager.cleanupExpiredSessions()
            startSessionCleanup() // Reschedule
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {

            }

            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_DENIED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.NEARBY_WIFI_DEVICES)) {

                }

                permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {

            }

            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissions.isNotEmpty()) {
            val permissionsResultLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

            }

            permissionsResultLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun checkServices() {
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        if(!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "Please activate Wi-Fi", Toast.LENGTH_LONG).show()
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please activate location", Toast.LENGTH_LONG).show()
        }
    }
}