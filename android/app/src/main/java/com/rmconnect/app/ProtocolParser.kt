package com.rmconnect.app

import java.nio.ByteBuffer
import java.nio.ByteOrder

object ProtocolParser {

    // CRC8 Table from Connection.html
    private val CRC8_TABLE = intArrayOf(
        0x00, 0xD5, 0x7F, 0xAA, 0xFE, 0x2B, 0x81, 0x54, 0x29, 0xFC, 0x56, 0x83, 0xD7, 0x02, 0xA8, 0x7D,
        0x52, 0x87, 0x2D, 0xF8, 0xAC, 0x79, 0xD3, 0x06, 0x7B, 0xAE, 0x04, 0xD1, 0x85, 0x50, 0xFA, 0x2F,
        0xA4, 0x71, 0xDB, 0x0E, 0x5A, 0x8F, 0x25, 0xF0, 0x8D, 0x58, 0xF2, 0x27, 0x73, 0xA6, 0x0C, 0xD9,
        0xF6, 0x23, 0x89, 0x5C, 0x08, 0xDD, 0x77, 0xA2, 0xDF, 0x0A, 0xA0, 0x75, 0x21, 0xF4, 0x5E, 0x8B,
        0x9D, 0x48, 0xE2, 0x37, 0x63, 0xB6, 0x1C, 0xC9, 0xB4, 0x61, 0xCB, 0x1E, 0x4A, 0x9F, 0x35, 0xE0,
        0xCF, 0x1A, 0xB0, 0x65, 0x31, 0xE4, 0x4E, 0x9B, 0xE6, 0x33, 0x99, 0x4C, 0x18, 0xCD, 0x67, 0xB2,
        0x39, 0xEC, 0x46, 0x93, 0xC7, 0x12, 0xB8, 0x6D, 0x10, 0xC5, 0x6F, 0xBA, 0xEE, 0x3B, 0x91, 0x44,
        0x6B, 0xBE, 0x14, 0xC1, 0x95, 0x40, 0xEA, 0x3F, 0x42, 0x97, 0x3D, 0xE8, 0xBC, 0x69, 0xC3, 0x16,
        0xEF, 0x3A, 0x90, 0x45, 0x11, 0xC4, 0x6E, 0xBB, 0xC6, 0x13, 0xB9, 0x6C, 0x38, 0xED, 0x47, 0x92,
        0xBD, 0x68, 0xC2, 0x17, 0x43, 0x96, 0x3C, 0xE9, 0x94, 0x41, 0xEB, 0x3E, 0x6A, 0xBF, 0x15, 0xC0,
        0x4B, 0x9E, 0x34, 0xE1, 0xB5, 0x60, 0xCA, 0x1F, 0x62, 0xB7, 0x1D, 0xC8, 0x9C, 0x49, 0xE3, 0x36,
        0x19, 0xCC, 0x66, 0xB3, 0xE7, 0x32, 0x98, 0x4D, 0x30, 0xE5, 0x4F, 0x9A, 0xCE, 0x1B, 0xB1, 0x64,
        0x72, 0xA7, 0x0D, 0xD8, 0x8C, 0x59, 0xF3, 0x26, 0x5B, 0x8E, 0x24, 0xF1, 0xA5, 0x70, 0xDA, 0x0F,
        0x20, 0xF5, 0x5F, 0x8A, 0xDE, 0x0B, 0xA1, 0x74, 0x09, 0xDC, 0x76, 0xA3, 0xF7, 0x22, 0x88, 0x5D,
        0xD6, 0x03, 0xA9, 0x7C, 0x28, 0xFD, 0x57, 0x82, 0xFF, 0x2A, 0x80, 0x55, 0x01, 0xD4, 0x7E, 0xAB,
        0x84, 0x51, 0xFB, 0x2E, 0x7A, 0xAF, 0x05, 0xD0, 0xAD, 0x78, 0xD2, 0x07, 0x53, 0x86, 0x2C, 0xF9
    )

    fun crc8(data: ByteArray, length: Int): Int {
        var crc = 0
        for (i in 0 until length) {
            // Need to handle Java/Kotlin signed byte issue (0-255 vs -128-127)
            val index = (crc xor (data[i].toInt() and 0xFF))
            crc = CRC8_TABLE[index]
        }
        return crc
    }

    // --- Command Builders ---

    fun cmdRequestDeviceInfo(): ByteArray {
        // 0xA5 0x55 0x1D 0x0D 0x0A
        return byteArrayOf(0xA5.toByte(), 0x55.toByte(), 0x1D.toByte(), 0x0D.toByte(), 0x0A.toByte())
    }

    fun cmdRequestChannels(): ByteArray {
        // 0xA5 0x55 0x02 0x0D 0x0A
        return byteArrayOf(0xA5.toByte(), 0x55.toByte(), 0x02.toByte(), 0x0D.toByte(), 0x0A.toByte())
    }

    fun cmdSetVolume(volume: Int): ByteArray {
        // 0xA5 0x55 0x19 [Vol] 0x0D 0x0A
        // Volume byte: 0-255? Usually 0-100 or 0-31 depending on device.
        // Assuming byte.
        return byteArrayOf(
            0xA5.toByte(), 0x55.toByte(), 0x19.toByte(), 
            volume.toByte(), 
            0x0D.toByte(), 0x0A.toByte()
        )
    }
    
    // --- Parsing Helpers ---
    
    data class ParsedDeviceInfo(
        val serialNumber: String,
        val elrsFirmware: String,
        val deviceFirmware: String,
        val elrsName: String,
        val companyName: String,
        val deviceName: String,
        val volume: Int,
        val channelCount: Int
    )

    fun parseDeviceInfo(packet: ByteArray): ParsedDeviceInfo? {
        // Expected Logic from Connection.html:
        // Header checked outside: 0xFF
        // Offset 2: Volume
        if (packet.size < 130) return null // Packet should be quite large, check safe size
        
        // Basic parsing based on JS reverse engineering (Connection.html:4602)
        var offset = 2
        val devVolume = packet[offset].toInt() and 0xFF
        offset++
        
        val serialNumber = packet.sliceArray(offset until offset+20).decodeToString().trimEnd('\u0000')
        offset += 20
        
        val elrsFirmWare = packet.sliceArray(offset until offset+20).decodeToString().trimEnd('\u0000')
        offset += 20
        
        val deviceFirmWare = packet.sliceArray(offset until offset+10).decodeToString().trimEnd('\u0000')
        offset += 10
        
        val elrsName = packet.sliceArray(offset until offset+50).decodeToString().trimEnd('\u0000')
        offset += 50
        
        val companyName = packet.sliceArray(offset until offset+20).decodeToString().trimEnd('\u0000')
        offset += 20
        
        val devName = packet.sliceArray(offset until offset+20).decodeToString().trimEnd('\u0000')
        offset += 20
        
        // Skip various speeds/flags
        offset += 4 
        val channelNum = packet[offset].toInt() and 0xFF
        
        return ParsedDeviceInfo(
            serialNumber = serialNumber,
            elrsFirmware = elrsFirmWare,
            deviceFirmware = deviceFirmWare,
            elrsName = elrsName,
            companyName = companyName,
            deviceName = devName,
            volume = devVolume,
            channelCount = channelNum
        )
    }

    data class ParsedChannelData(
        val channels: IntArray // 10 channels usually
    )

    fun parseChannelData(packet: ByteArray): ParsedChannelData? {
        // Packet: 0x22 [Len] [Data...] [CRC]
        // Len covers Data+CRC.
        // Data starts at index 2.
        // Each channel is uint16 (2 bytes). 
        // Logic from Connection.html:3168
        
        if (packet.size < 4) return null
        val len = packet[1].toInt() and 0xFF
        if (packet.size < len + 2) return null
        
        // Verify CRC (optional but good practice)
        // const crc = buf[len+1];
        // const calcCrc = crc8tab_js(buf.slice(2,2+len-1), len-1);
        
        val channels = IntArray(10)
        // Data starts at offset 2 (after CMD and LEN). Wait, in JS it was offset 8?
        // Let's re-read Connection.html:3170 -> let off = 8 + i*2;
        // Why 8? 
        // Ah, packet in JS 'buf' passed to tryParseOrigChannelPacketCalib(buf).
        // if (buf[0] !== 0x22) return false;
        // The packet 0x22 is actually embedded? No.
        // JS loop: for(let i=0;i<10;i++){ let off = 8 + i*2; ... }
        // The JS buffer likely contains the full 0xA5 0x55 frame or similar?
        // Actually earlier in JS: "if (buf[0] !== 0x22) return false; const len = buf[1];"
        // If len is around 28 bytes?
        // Let's assume standard offset logic based on observation or try offset 2 if raw payload.
        // BUT JS says offset 8. Let's trust JS logic: 8 + i*2.
        // This suggests bytes 2..7 might be padding or other data?
        
        for (i in 0 until 10) {
            val off = 8 + i * 2 // Using logic from JS
            if (off + 1 >= packet.size) break
            val low = packet[off].toInt() and 0xFF
            val high = packet[off+1].toInt() and 0xFF
            channels[i] = low or (high shl 8)
        }
        
        return ParsedChannelData(channels)
    }
    
    // --- ELRS / CRSF Parsing ---

    sealed class ElrsParameter(
        val paramNum: Int,
        val name: String,
        val parentFolder: Int,
        val hidden: Boolean
    ) {
        data class TypeUint8(
            val id: Int, val label: String, val parent: Int, val isHidden: Boolean,
            val value: Int, val min: Int, val max: Int, val def: Int, val unit: String
        ) : ElrsParameter(id, label, parent, isHidden)

        data class TypeFloat(
            val id: Int, val label: String, val parent: Int, val isHidden: Boolean,
            val value: Float, val min: Float, val max: Float, val def: Float, 
            val decimal: Int, val step: Float, val unit: String
        ) : ElrsParameter(id, label, parent, isHidden)

        data class TypeSelection(
            val id: Int, val label: String, val parent: Int, val isHidden: Boolean,
            val value: Int, val options: List<String>, val unit: String
        ) : ElrsParameter(id, label, parent, isHidden)
        
        data class TypeString(
            val id: Int, val label: String, val parent: Int, val isHidden: Boolean,
            val value: String
        ) : ElrsParameter(id, label, parent, isHidden)
        
        data class TypeFolder(
             val id: Int, val label: String, val parent: Int, val isHidden: Boolean
        ) : ElrsParameter(id, label, parent, isHidden)
        
        data class TypeInfo(
             val id: Int, val label: String, val parent: Int, val isHidden: Boolean,
             val value: String
        ) : ElrsParameter(id, label, parent, isHidden)
        
        data class TypeCommand(
             val id: Int, val label: String, val parent: Int, val isHidden: Boolean,
             val status: Int, val timeout: Int, val info: String
        ) : ElrsParameter(id, label, parent, isHidden)
    }

    fun cmdRequestElrsTx(): ByteArray = byteArrayOf(0xA5.toByte(), 0x11.toByte(), 0x00.toByte(), 0x0D.toByte(), 0x0A.toByte())
    fun cmdRequestElrsRx(): ByteArray = byteArrayOf(0xA5.toByte(), 0x22.toByte(), 0x00.toByte(), 0x0D.toByte(), 0x0A.toByte())

    fun parseElrsPacket(packet: ByteArray): List<ElrsParameter> {
        // Needs a robust reassembly logic usually, but let's try to parse if we have a full "BigPacket"
        // Packet accumulated in memory.
        // Logic from Connection.html parseCRSFPacket
        
        val params = mutableListOf<ElrsParameter>()
        val chunkCache = mutableMapOf<Int, MutableMap<Int, ByteArray>>() 
        
        var offset = 0
        // Check for 0x56 header usually stripped
        if (packet.isNotEmpty() && packet[0] == 0x56.toByte()) offset = 1
        
        while (offset + 2 < packet.size) {
            val len = packet[offset+1].toInt() and 0xFF
            val type = packet[offset+2].toInt() and 0xFF
            
            if (len == 0) { offset++; continue }
            val packetTotalLen = 2 + len
            if (offset + packetTotalLen > packet.size) break
            
            val payloadStart = offset + 2 // include type? JS says pkt = slice(offset+2, ...). pkt[0] is type.
            // Actually JS: const pkt = bigPacket.slice(offset + 2, offset + 2 + len - 1 ); // pkt[0]=type
            // So pkt contains Type at 0.
            
            if (type == 0x2B) { // Parameter
                // pkt index: 0=Type, 1=Src, 2=Dst, 3=ParamNum, 4=ChunkNum, 5...Data
                var p = offset + 2 + 1 // Start of pkt + 1 (Skip Type)
                val src = packet[p++]
                val dst = packet[p++]
                val paramNum = packet[p++].toInt() and 0xFF
                val chunkNum = packet[p++].toInt() and 0xFF
                
                val dataLen = (offset + 2 + len - 1) - p // Remaining data
                if (dataLen > 0) {
                    val chunkData = packet.sliceArray(p until p + dataLen)
                    
                    val entry = chunkCache.getOrPut(paramNum) { mutableMapOf() }
                    entry[chunkNum] = chunkData
                }
            }
            // Ignore other packets for now (Ping 0x29 etc)
            
            offset += packetTotalLen
        }
        
        // Reassemble
        chunkCache.keys.sorted().forEach { paramNum ->
            val chunks = chunkCache[paramNum]!!
            // Sort chunks by index
            val sortedChunks = chunks.keys.sorted().map { chunks[it]!! }
            
            // Concatenate
            val totalSize = sortedChunks.sumOf { it.size }
            val fullData = ByteArray(totalSize)
            var pos = 0
            sortedChunks.forEach { 
                System.arraycopy(it, 0, fullData, pos, it.size)
                pos += it.size
            }
            
            // Parse Param
            try {
                val param = parseParamData(paramNum, fullData)
                if (param != null) params.add(param)
            } catch (e: Exception) {
                // Log error
            }
        }
        
        return params
    }

    private fun parseParamData(paramNum: Int, data: ByteArray): ElrsParameter? {
        if (data.size < 3) return null
        var p = 0
        val parentFolder = data[p++].toInt() and 0xFF
        val dataType = data[p++].toInt() and 0xFF
        val baseType = dataType and 0x7F
        val hidden = (dataType and 0x80) != 0
        
        // Name
        val nameEnd = findNullTerminator(data, p)
        val name = String(data, p, nameEnd - p)
        p = nameEnd + 1
        
        when (baseType) {
            0, 1 -> { // UINT8, INT8
                val value = data[p++].toInt() and 0xFF
                val min = data[p++].toInt() and 0xFF
                val max = data[p++].toInt() and 0xFF
                val def = data[p++].toInt() and 0xFF
                val unitEnd = findNullTerminator(data, p)
                val unit = String(data, p, unitEnd - p)
                
                return ElrsParameter.TypeUint8(paramNum, name, parentFolder, hidden, value, min, max, def, unit)
            }
            2, 3 -> { // UINT16, INT16
                val value = getUint16(data, p); p+=2
                val min = getUint16(data, p); p+=2
                val max = getUint16(data, p); p+=2
                val def = getUint16(data, p); p+=2
                val unitEnd = findNullTerminator(data, p)
                val unit = String(data, p, unitEnd - p)
                
                 return ElrsParameter.TypeUint8(paramNum, name, parentFolder, hidden, value, min, max, def, unit) // Reuse TypeUint8 for now or make generic
            }
            8 -> { // FLOAT
                 val value = getFloat(data, p); p+=4
                 val min = getFloat(data, p); p+=4
                 val max = getFloat(data, p); p+=4
                 val def = getFloat(data, p); p+=4
                 val dec = data[p++].toInt() and 0xFF
                 val step = getFloat(data, p); p+=4
                 val unitEnd = findNullTerminator(data, p)
                 val unit = String(data, p, unitEnd - p)

                 return ElrsParameter.TypeFloat(paramNum, name, parentFolder, hidden, value, min, max, def, dec, step, unit)
            }
            9 -> { // SELECTION
                val optsEnd = findNullTerminator(data, p)
                val optsStr = String(data, p, optsEnd - p)
                p = optsEnd + 1
                val value = data[p++].toInt() and 0xFF
                val unitEnd = findNullTerminator(data, p)
                val unit = String(data, p, unitEnd - p)
                
                return ElrsParameter.TypeSelection(paramNum, name, parentFolder, hidden, value, optsStr.split(";"), unit)
            }
            10 -> { // STRING
                // len, value, len, def
                val maxLen = data[p++].toInt() and 0xFF
                val strEnd = findNullTerminator(data, p)
                val value = String(data, p, strEnd - p)
                
                return ElrsParameter.TypeString(paramNum, name, parentFolder, hidden, value)
            }
            11 -> return ElrsParameter.TypeFolder(paramNum, name, parentFolder, hidden)
            12 -> { // INFO
                val strEnd = findNullTerminator(data, p)
                val value = String(data, p, strEnd - p)
                return ElrsParameter.TypeInfo(paramNum, name, parentFolder, hidden, value)
            }
            13 -> { // COMMAND
                 val status = data[p++].toInt() and 0xFF
                 val timeout = data[p++].toInt() and 0xFF
                 val infoEnd = findNullTerminator(data, p)
                 val info = String(data, p, infoEnd - p)
                 return ElrsParameter.TypeCommand(paramNum, name, parentFolder, hidden, status, timeout, info)
            }
        }
        
        return null
    }

    private fun findNullTerminator(data: ByteArray, start: Int): Int {
        var i = start
        while (i < data.size && data[i] != 0.toByte()) i++
        return i
    }
    
    private fun getUint16(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or ((data[offset+1].toInt() and 0xFF) shl 8)
    }
    
    // Float is Little Endian 4 bytes?
    private fun getFloat(data: ByteArray, offset: Int): Float {
        val i = (data[offset].toInt() and 0xFF) or
                ((data[offset+1].toInt() and 0xFF) shl 8) or
                ((data[offset+2].toInt() and 0xFF) shl 16) or
                ((data[offset+3].toInt() and 0xFF) shl 24)
        return Float.fromBits(i)
    }

    // Extension to decode byte array to string cleanly
    private fun ByteArray.decodeToString(): String {
        return String(this, Charsets.UTF_8)
    }
}
