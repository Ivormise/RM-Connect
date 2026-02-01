package com.rmconnect.app.ui.screens

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
import com.rmconnect.app.ProtocolParser
import com.rmconnect.app.UsbSerialManager
import com.rmconnect.app.ui.components.GradientButton
import com.rmconnect.app.ui.components.PremiumCard
import com.rmconnect.app.ui.components.SectionHeader
import com.rmconnect.app.ui.theme.SuccessGreen
import com.rmconnect.app.ui.theme.ErrorRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    usbManager: UsbSerialManager,
    onNavigateToCalibration: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToElrs: () -> Unit
) {
    val connectionState by usbManager.connectionState.collectAsState()
    val receivedData by usbManager.receivedData.collectAsState()
    
    // Logs State
    var logs by remember { mutableStateOf(listOf<String>()) }
    fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs = (logs + "[$time] $msg").takeLast(50)
    }
    
    // ELRS Buffer State (Local to MainScreen for now, or moved to ViewModel later)
    var elrsBuffer by remember { mutableStateOf(byteArrayOf()) }
    
    // Device Info State
    var deviceInfo by remember { mutableStateOf<ProtocolParser.ParsedDeviceInfo?>(null) }
    
    // Process Data
    LaunchedEffect(receivedData) {
        receivedData?.let { data ->
            if (data.isNotEmpty() && (data[0].toInt() and 0xFF) == 0xFF) {
                 val info = ProtocolParser.parseDeviceInfo(data)
                 if (info != null) {
                     deviceInfo = info
                     addLog("Device Info Updated: ${info.deviceName}")
                 }
            } else {
                // ELRS Accumulation
                if (elrsBuffer.size > 8192) elrsBuffer = byteArrayOf()
                elrsBuffer += data
                
                // Scan for ELRS packets just to log or verify connection?
                // Actually parsing happens in ElrsScreen usually, but here we detect it?
                // For now, let's just log if we see ELRS headers
                if (data.any { it == 0x56.toByte() }) {
                    // addLog("ELRS Data detected")
                }
            }
        }
    }
    
    // Connection State Logging
    LaunchedEffect(Unit) {
        usbManager.logFlow.collect { msg ->
            addLog(msg)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("RM Connect", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("RadioMaster Configurator", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
            }
            
            PremiumCard(
                modifier = Modifier.wrapContentSize(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Dot Indicator
                    Box(modifier = Modifier.size(8.dp).background(
                         if (connectionState is UsbSerialManager.ConnectionState.Connected) SuccessGreen else ErrorRed,
                         shape = androidx.compose.foundation.shape.CircleShape
                    ))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (connectionState is UsbSerialManager.ConnectionState.Connected) "CONNECTED" else "DISCONNECTED",
                        color = if (connectionState is UsbSerialManager.ConnectionState.Connected) SuccessGreen else ErrorRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
         // CONTROL PANEL
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            GradientButton(
                text = "Connect USB",
                onClick = { usbManager.connect() }, 
                modifier = Modifier.weight(1f).fillMaxWidth(),
                enabled = connectionState !is UsbSerialManager.ConnectionState.Connected
            )
            
            OutlinedButton(
                onClick = { usbManager.disconnect() }, 
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                enabled = connectionState is UsbSerialManager.ConnectionState.Connected,
                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha=0.5f))
            ) { Text("Disconnect") }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (deviceInfo != null) {
            SectionHeader("Device Information")
            DashboardCard(deviceInfo!!)
            Spacer(modifier = Modifier.height(24.dp))
            
            SectionHeader("Actions")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onNavigateToCalibration, modifier = Modifier.weight(1f)) { Text("Calibration") }
                Button(onClick = onNavigateToSettings, modifier = Modifier.weight(1f)) { Text("Settings") }
            }
            Spacer(modifier = Modifier.height(8.dp))
             GradientButton(
                text = "ELRS Configuration",
                onClick = onNavigateToElrs,
                modifier = Modifier.fillMaxWidth(),
                colorStart = MaterialTheme.colorScheme.tertiary,
                colorEnd = MaterialTheme.colorScheme.primary
            )
            
        } else if (connectionState is UsbSerialManager.ConnectionState.Connected) {
             Button(
                onClick = { usbManager.write(ProtocolParser.cmdRequestDeviceInfo()) },
                modifier = Modifier.fillMaxWidth()
             ) { Text("Load Device Info") }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Logs
        SectionHeader("Debug Console")
        Card(
            modifier = Modifier.height(150.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            LazyColumn(modifier = Modifier.padding(8.dp), reverseLayout = true) {
                items(logs.reversed()) { log ->
                    Text(log, fontSize = 10.sp, color = SuccessGreen, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun DashboardCard(info: ProtocolParser.ParsedDeviceInfo) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(info.companyName.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Text(info.deviceName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            // maybe an icon or image here?
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoColumn("Serial Number", info.serialNumber, Modifier.weight(1f))
            InfoColumn("Firmware", info.deviceFirmware, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoColumn("ELRS Ver", info.elrsFirmware, Modifier.weight(1f))
            InfoColumn("ELRS Name", info.elrsName, Modifier.weight(1f))
        }
         Spacer(modifier = Modifier.height(12.dp))
         Row(modifier = Modifier.fillMaxWidth()) {
            InfoColumn("Volume", "${info.volume}", Modifier.weight(1f))
            InfoColumn("Channels", "${info.channelCount}", Modifier.weight(1f))
        }
    }
}

@Composable
fun InfoColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
