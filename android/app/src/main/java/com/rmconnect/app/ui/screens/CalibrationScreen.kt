package com.rmconnect.app.ui.screens

import com.rmconnect.app.UsbSerialManager
import com.rmconnect.app.ProtocolParser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun CalibrationScreen(
    usbManager: UsbSerialManager,
    onBack: () -> Unit
) {
    val receivedData by usbManager.receivedData.collectAsState()
    var channelData by remember { mutableStateOf(IntArray(10) { 0 }) }
    var packetCount by remember { mutableLongStateOf(0L) }

    // Start requesting channels when screen opens
    LaunchedEffect(Unit) {
        while(true) {
            // "Keep Alive" or request loop if needed. 
            // In the JS code, it might be a single request that starts a stream, 
            // or a polling loop. Connection.html uses a readLoop, but likely sends a start command.
            // The plan says: 0xA5 0x55 0x02 ... is "Request Channels".
            // Let's send it once. If it needs polling, we can add loop here.
            usbManager.write(ProtocolParser.cmdRequestChannels())
            delay(100) // Poll every 100ms for now
        }
    }

    LaunchedEffect(receivedData) {
        receivedData?.let { data ->
            // Check for Channel Packet (0x22)
            if (data.size > 4 && (data[0].toInt() and 0xFF) == 0x22) {
                val parsed = ProtocolParser.parseChannelData(data)
                if (parsed != null) {
                    channelData = parsed.channels
                    packetCount++
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("Back") }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Calibration / Monitor", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Text("Packet Count: $packetCount", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        // Auto-generated channel bars
        channelData.forEachIndexed { index, value ->
            ChannelBar(index + 1, value)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ChannelBar(id: Int, value: Int) {
    // Value for RadioMaster usually 0..4095 or 1000..2000? 
    // JS: "val / 40.96"? or similar mapping.
    // Let's assume standard RC range 1000-2000 or 0-4096. 
    // Protocol usually 10-12 bits. Let's normalize to 0.0 - 1.0 safely.
    // Assuming 12-bit (0-4095) with center around 2048.
    
    val normalized = (value / 4096f).coerceIn(0f, 1f)
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("CH $id", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("$value", fontSize = 14.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = normalized,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
