package com.rmconnect.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.rmconnect.app.ui.RMNavHost
import com.rmconnect.app.ui.theme.RMConnectTheme

class MainActivity : ComponentActivity() {
    private lateinit var usbManager: UsbSerialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usbManager = UsbSerialManager(this)

        setContent {
            RMConnectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    RMNavHost(usbManager = usbManager)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        usbManager.disconnect()
    }
}
