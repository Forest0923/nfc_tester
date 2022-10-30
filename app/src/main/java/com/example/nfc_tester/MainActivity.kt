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
        Log.d(TAG,"onNewIntent")

        if (intent == null) {
            return
        }

        Log.d(TAG,"Action" + intent.action)

        if(NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.action)) {

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return

            val ndef = Ndef.get(tag) ?: return;

            val raws = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) ?: return;

            var msgs = arrayOfNulls<NdefMessage>(raws.size)

            for(i in raws.indices) {
                msgs[i] = raws.get(i) as NdefMessage?
                for(records in msgs) {
                    for(record in records?.records!!){
                        Log.d(TAG,"TNF=" + record.tnf)
                        Log.d(TAG,"mime=" + record.toMimeType())
                        Log.d(TAG,"payload=" + String(record.payload));
                    }
                }
            }
        }
    }
}