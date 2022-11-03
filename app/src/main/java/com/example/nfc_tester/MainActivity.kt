package com.example.nfc_tester

import android.content.ContentValues.TAG
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return;
        Log.d(TAG, tag.toString())
//        val result = NfcFReader().read(tag)
        val result = read(tag)
        val resultView = findViewById<TextView>(R.id.result)
        resultView.text = result.joinToString(separator = "\n")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    private fun read(tag: Tag): Array<String> {
        val nfcf: NfcFReader = NfcFReader(tag)

        try {
            nfcf.nfc.connect()

            nfcf.polling()?.let { printByteArray("polling: res\t=", it) }
            nfcf.requestResponse()?.let { printByteArray("requestResponse: res\t=", it) }
            nfcf.readWithoutEncryption(byteArrayOf(0x20, 0x4B), 1)
                ?.let { printByteArray("readWithoutEncryption: res\t=", it) }
            nfcf.requestSystemCode()?.let { printByteArray("requestSystemCode: res\t=", it) }

            for (i: Int in 0..0xffff) {
                val res = nfcf.searchServiceCode(byteArrayOf((i shl 8).toByte(), (i and 0xff).toByte()))
                    ?: return emptyArray()
                Log.d(TAG, "0x%02X(%d), 0x%02X, 0x%02X".format(i, i, i shr 8, i and 0xff))
                printByteArray("searchServiceCode: res\t=", res)
            }

            nfcf.nfc.close()
        } catch (e: Exception) {
            Log.e(TAG, "NfcFReader: ERROR '$e'")
            if (nfcf.nfc.isConnected) {
                nfcf.nfc.close()
            }
        }

        return arrayOf(tag.toString(), "ID  = ${nfcf.idm}", "PMm = ${nfcf.pmm}", "SystemCode  = ${nfcf.systemCode}")
    }

    private fun printByteArray(prefix: String, array: ByteArray) {
        Log.d(
            TAG,
            "$prefix ${array.joinToString(separator = ":") { eachByte -> "%02X".format(eachByte) }}"
        )
    }
}

class NfcFReader(tag: Tag) {
    val idm: ByteArray
    val nfc: NfcF
    val systemCode: ByteArray
    val pmm: ByteArray

    init {
        idm = tag.id
        nfc = NfcF.get(tag)
        systemCode  = nfc.systemCode
        pmm = nfc.manufacturer
    }

    private fun printByteArray(prefix: String, array: ByteArray) {
        Log.d(
            TAG,
            "$prefix ${array.joinToString(separator = ":") { eachByte -> "%02X".format(eachByte) }}"
        )
    }

    fun polling(): ByteArray? {
        val cmdCode     = byteArrayOf(0x00)
        val reqCode     = byteArrayOf(0x00)
        val timeSlot    = byteArrayOf(0x0f)

        val command = cmdCode + systemCode + reqCode + timeSlot
        printByteArray("polling: command\t=", command)
        val msg = byteArrayOf((command.size + 1).toByte()) + command
        printByteArray("polling: msg\t=", msg)
        return nfc.transceive(msg)
    }

    fun requestService(): ByteArray? {
        val cmdCode         = byteArrayOf(0x02)
        val nodeNum         = byteArrayOf(0x01)
        val nodeCodeList    = byteArrayOf(0x08, 0x20)

        val command = cmdCode + idm + nodeNum + nodeCodeList
        printByteArray("requestService: command\t=", command)
        val msg = byteArrayOf((command.size + 1).toByte()) + command
        printByteArray("requestService: msg\t=", msg)
        return nfc.transceive(msg)
    }

    fun requestResponse(): ByteArray? {
        val cmdCode         = byteArrayOf(0x04)

        val command = cmdCode + idm
        printByteArray("requestResponse: command\t=", command)
        val msg = byteArrayOf((command.size + 1).toByte()) + command
        printByteArray("requestResponse: msg\t=", msg)

        return nfc.transceive(msg)
    }

    fun readWithoutEncryption(serviceCode: ByteArray, size: Int): ByteArray? {
        val cmdCode         = byteArrayOf(0x06) // (1-byte)
        val serviceNum      = byteArrayOf(0x01) // 1~16 (1-byte)
        val serviceCodeList = byteArrayOf(serviceCode[1], serviceCode[0])    // 0x000B 0x0009 (2m-byte)
        val blockNum        = byteArrayOf(size.toByte()) // (1-byte)
        var blockList       = byteArrayOf()     // (N-byte, 2n <= N <= 3n)
        for (i in 0 until size) {
            blockList += byteArrayOf(0x80.toByte(), i.toByte())
        }

        val command = cmdCode + idm + serviceNum + serviceCodeList + blockNum + blockList
        printByteArray("readWithoutEncryption: command\t=", command)
        val msg = byteArrayOf((command.size + 1).toByte()) + command
        printByteArray("readWithoutEncryption: msg\t=", msg)

        return nfc.transceive(msg)
    }

    fun searchServiceCode(index: ByteArray): ByteArray? {
        val cmdCode = byteArrayOf(0x0A)
        val idx     = byteArrayOf(index[1], index[0])

        val command = cmdCode + idm + idx
        val msg     = byteArrayOf((command.size + 1).toByte()) + command

        printByteArray("searchServiceCode: msg\t=", msg)
        return nfc.transceive(msg)
    }

    fun requestSystemCode(): ByteArray? {
        val cmdCode = byteArrayOf(0x0C)

        val command = cmdCode + idm
        val msg = byteArrayOf((command.size + 1).toByte()) + command
        return nfc.transceive(msg)
    }
}