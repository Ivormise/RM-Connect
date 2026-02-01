package com.rmconnect.app.ui.screens

import com.rmconnect.app.UsbSerialManager
import com.rmconnect.app.ProtocolParser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rmconnect.app.ProtocolParser.ElrsParameter

@Composable
fun ElrsScreen(
    usbManager: UsbSerialManager,
    parameters: List<ElrsParameter>,
    onBack: () -> Unit
) {
    var statusText by remember { mutableStateOf("Ready") }

    // Initial Request if list is empty?
    // Usually Logic: Main Screen -> Click "ELRS" -> Send Request -> Navigate here -> Data loads.
    // Or: Screen opens empty -> triggers request.
    
    LaunchedEffect(Unit) {
        if (parameters.isEmpty()) {
            statusText = "Requesting ELRS Config..."
            usbManager.write(ProtocolParser.cmdRequestElrsTx()) // Request TX by default? Or RX?
            // Maybe add tabs for TX / RX later.
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("Back") }
            Spacer(modifier = Modifier.width(16.dp))
            Text("ELRS Configuration", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Text(statusText, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        
        // Buttons to refresh
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { usbManager.write(ProtocolParser.cmdRequestElrsTx()); statusText="Requesting TX..." }) {
                 Text("Load TX") 
            }
            Button(onClick = { usbManager.write(ProtocolParser.cmdRequestElrsRx()); statusText="Requesting RX..." }) {
                 Text("Load RX") 
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp), 
            modifier = Modifier.fillMaxSize()
        ) {
            items(parameters) { param ->
                if (!param.hidden) {
                    ParamItem(param)
                }
            }
        }
    }
}

@Composable
fun ParamItem(param: ElrsParameter) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(param.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            
            when (param) {
                is ElrsParameter.TypeUint8 -> {
                    Text("Value: ${param.value} ${param.unit}")
                    // Slider or Input
                }
                is ElrsParameter.TypeSelection -> {
                    val label = param.options.getOrNull(param.value) ?: "Unknown"
                    Text("Selected: $label")
                    // Dropdown
                }
                is ElrsParameter.TypeFloat -> {
                    Text("Value: ${param.value} ${param.unit}")
                }
                 is ElrsParameter.TypeFolder -> {
                    Text("[Folder] (Click to enter - Todo)")
                }
                is ElrsParameter.TypeInfo -> {
                    Text(param.value, color = Color.Gray)
                }
                is ElrsParameter.TypeString -> {
                    Text(param.value)
                }
                is ElrsParameter.TypeCommand -> {
                    Button(onClick = {}) { Text(param.info) }
                }
            }
        }
    }
}
