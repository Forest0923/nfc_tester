package com.example.nfc_tester

import android.content.ContentValues.TAG
import android.content.Intent
import android.nfc.*
import android.nfc.tech.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return;
        Log.d(TAG, tag.toString())
        val result  = NfcFReader().read(tag)
        val resultView  = findViewById<TextView>(R.id.result)
        resultView.text = result.joinToString(separator = "\n")
    }
}

class NfcFReader() {
    fun read(tag: Tag): Array<String> {
        val nfc = NfcF.get(tag)
        val id = tag.id.joinToString(separator = ":") { eachByte -> "%02X".format(eachByte) }
        val systemcode =
            nfc.systemCode.joinToString(separator = ":") { eachByte -> "%02X".format(eachByte) }
        val pmm =
            nfc.manufacturer.joinToString(separator = ":") { eachByte -> "%02X".format(eachByte) }
        try {
            nfc.connect()
//            val command = byteArrayOf(0x00.toByte())
//            val IDm     = tag.id
//            val msg = byteArrayOf(command.size.toByte(), command[0], command[1])
//            Log.d(TAG, "${msg[0]}, ${msg[1]}, ${msg[2]}")
//            val res = nfc.transceive(msg)
//            Log.d(TAG, "res = $res")

            nfc.close()
        } catch (e: Exception) {
            Log.e(TAG, "NfcFReader: ERROR '$e'")
            if (nfc.isConnected) {
                nfc.close()
            }
        }

        return arrayOf(tag.toString(), "ID  = $id", "PMm = $pmm", "SystemCode  = $systemcode")
    }
}
