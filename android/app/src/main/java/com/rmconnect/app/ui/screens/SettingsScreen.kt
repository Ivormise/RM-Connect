package com.rmconnect.app.ui.screens

import com.rmconnect.app.UsbSerialManager
import com.rmconnect.app.ProtocolParser

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    usbManager: UsbSerialManager,
    initialVolume: Int,
    onBack: () -> Unit
) {
    // Local state for UI responsiveness
    var volume by remember { mutableFloatStateOf(initialVolume.toFloat()) }
    
    // De-bouncing write operations
    var lastSentVolume by remember { mutableIntStateOf(initialVolume) }

    LaunchedEffect(volume) {
        val targetVol = volume.toInt()
        if (targetVol != lastSentVolume) {
            // Simple debounce: wait a bit, if still same, send. 
            // Better: just send on change but limit rate?
            // Let's just send immediate for now but don't spam if dragging fast (delay).
            delay(100) 
            if (volume.toInt() == targetVol) {
                 usbManager.write(ProtocolParser.cmdSetVolume(targetVol))
                 lastSentVolume = targetVol
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("Back") }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Detailed Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Volume Control Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("General", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Volume Level: ${volume.toInt()}", fontWeight = FontWeight.Medium)
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0f..100f, // Assuming 0-100 scale based on 1 byte
                    steps = 99,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("- changes are applied immediately -", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}
