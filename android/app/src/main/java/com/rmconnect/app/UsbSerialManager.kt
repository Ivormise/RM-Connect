package com.rmconnect.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UsbSerialManager(private val context: Context) : SerialInputOutputManager.Listener {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbSerialPort: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _receivedData = MutableStateFlow<ByteArray?>(null)
    val receivedData = _receivedData.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val ACTION_USB_PERMISSION = "com.rmconnect.app.USB_PERMISSION"
        private const val BAUD_RATE = 460800 // Default for RadioMaster
        private const val TAG = "UsbSerialManager"
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
    
    // Log Flow for UI
    private val _logFlow = kotlinx.coroutines.flow.MutableSharedFlow<String>(replay = 10, extraBufferCapacity = 50)
    val logFlow = _logFlow.asSharedFlow()
    
    private fun log(msg: String) {
        Log.d(TAG, msg)
        scope.launch { _logFlow.emit(msg) }
    }

    fun connect() {
        log("Attempting to connect...")
        _connectionState.value = ConnectionState.Connecting

        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            val msg = "No USB devices found"
            _connectionState.value = ConnectionState.Error(msg)
            log(msg)
            return
        }

        // Just take the first available driver for now
        val driver = availableDrivers[0]
        val connection = usbManager.openDevice(driver.device)

        if (connection == null) {
            // Need to request permission
            if (!usbManager.hasPermission(driver.device)) {
                val permissionIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_IMMUTABLE
                )
                usbManager.requestPermission(driver.device, permissionIntent)
                val msg = "Requesting permission..."
                _connectionState.value = ConnectionState.Error(msg)
                log(msg)
            } else {
                val msg = "Unable to open device"
                _connectionState.value = ConnectionState.Error(msg)
                log(msg)
            }
            return
        }

        try {
            usbSerialPort = driver.ports[0] // Most devices have just one port
            usbSerialPort?.open(connection)
            usbSerialPort?.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            
            // RadioMaster/STM32 usually requires DTR/RTS to be set for serial communication to active
            usbSerialPort?.dtr = true
            usbSerialPort?.rts = true

            ioManager = SerialInputOutputManager(usbSerialPort, this)
            ioManager?.start()
            
            log("Port open. Baud: $BAUD_RATE, DTR: true, RTS: true")

            _connectionState.value = ConnectionState.Connected
            log("Connected successfully")
        } catch (e: Exception) {
            val msg = "Connection failed: ${e.message}"
            Log.e(TAG, msg, e)
            _connectionState.value = ConnectionState.Error(msg)
            log(msg)
            disconnect()
        }
    }

    fun disconnect() {
        ioManager?.stop()
        ioManager = null
        try {
            usbSerialPort?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing port", e)
        }
        usbSerialPort = null
        _connectionState.value = ConnectionState.Disconnected
        log("Disconnected")
    }

    fun write(data: ByteArray) {
        if (usbSerialPort == null) {
            log("Write failed: Port closed")
            return
        }
        scope.launch {
            try {
                log("TX: ${data.toHexString()}")
                usbSerialPort?.write(data, 500) // Increased timeout
            } catch (e: Exception) {
                log("Write error: ${e.message}")
                _connectionState.value = ConnectionState.Error("Write failed: ${e.message}")
            }
        }
    }

    override fun onNewData(data: ByteArray) {
        if (data.isNotEmpty()) {
             log("RX: ${data.toHexString()}")
            _receivedData.value = data
        }
    }

    override fun onRunError(e: Exception) {
        log("IO Error: ${e.message}")
        disconnect()
        _connectionState.value = ConnectionState.Error("IO Error: ${e.message}")
    }
    
    // Quick helper extension
    private fun ByteArray.toHexString(): String {
        return joinToString(" ") { "%02X".format(it) }
    }
}
