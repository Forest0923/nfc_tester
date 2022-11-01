package com.example.nfc_tester

import android.content.ContentValues.TAG
import android.content.Intent
import android.nfc.*
import android.nfc.tech.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)

        button.setOnClickListener {
            val intent = Intent(this, ReadActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG,"onNewIntent: Action = ${intent.action}")
        Log.d(TAG,"onNewIntent: Data = ${intent.dataString}")
        Log.d(TAG,"onNewIntent: extras = ${intent.extras}")
        Log.d(TAG,"onNewIntent: ID = ${intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)}")
        Log.d(TAG,"onNewIntent: Messages = ${intent.getByteArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)}")

        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return;

        Log.d(TAG,"onNewIntent: tag = $tag")
        NfcFReader().read(tag)
    }
}

class NfcFReader() {
    fun read(tag: Tag) {
        val nfc = NfcF.get(tag)
        Log.d(TAG,"NfcFReader.read: $nfc")
        Log.d(TAG,"id from tag: ${tag.id}")

        try {
            nfc.connect()

            // systemcode
            Log.d(TAG,"systemcode: ${nfc.systemCode.iterator()}")
            for (i in 0 until nfc.systemCode.size)
                Log.d(TAG,String.format("%02X",nfc.systemCode[i]))

            // manufacturer PMm
            Log.d(TAG,"manufacturer: ")
            for (i in 0 until nfc.manufacturer.size)
                Log.d(TAG,String.format("%02X",nfc.manufacturer[i]))

            Log.d(TAG,"maxTransceiveLength = ${nfc.maxTransceiveLength}")
            Log.d(TAG,"isConnected = ${nfc.isConnected}")
            val command = byteArrayOf(0x0a.toByte(), 0x0b.toByte())
            val msg = byteArrayOf(command.size.toByte(), command[0], command[1])
            Log.d(TAG,"${msg[0]}, ${msg[1]}, ${msg[2]}")
            val res = nfc.transceive(msg)
            Log.d(TAG, "res = $res")

            nfc.close()
        } catch (e: Exception) {
            Log.e(TAG,"NfcFReader: ERROR '$e'")
            if (nfc.isConnected) {
                nfc.close()
            }
        }
    }
}
