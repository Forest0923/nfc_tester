package com.example.nfc_tester

import android.content.ContentValues.TAG
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.*
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var mNfcAdapter: NfcAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mNfcAdapter = getDefaultAdapter(applicationContext)

        findViewById<LinearLayout>(R.id.home).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.result).visibility = View.GONE
        findViewById<TextView>(R.id.mem_dump).movementMethod = ScrollingMovementMethod()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")

        val tag = intent.getParcelableExtra<Tag>(EXTRA_TAG) ?: return
        Log.d(TAG, tag.toString())
        val nfcf = NfcFReader(tag)
        val tagInfo = nfcf.getTagInfo(tag)
        val memInfo = nfcf.getMemoryContent(tag)

        findViewById<LinearLayout>(R.id.home).visibility = View.GONE
        findViewById<LinearLayout>(R.id.result).visibility = View.VISIBLE
        findViewById<TextView>(R.id.tag_info).text = tagInfo.joinToString(separator = "\n")
        findViewById<TextView>(R.id.mem_dump).text = memInfo.joinToString(separator = "\n")
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        mNfcAdapter.enableReaderMode(
            this,
            MyReaderCallback(this),
            FLAG_READER_NFC_F or FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        mNfcAdapter.disableReaderMode(this)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    class MyReaderCallback(private val activity: MainActivity) : ReaderCallback {
        override fun onTagDiscovered(tag: Tag) {
            Log.d(TAG, "onTagDiscovered: $tag")

            val nfcf = NfcFReader(tag)
            val tagInfo = nfcf.getTagInfo(tag)
            val memInfo = nfcf.getMemoryContent(tag)

            activity.runOnUiThread {
                activity.findViewById<LinearLayout>(R.id.home)?.visibility = View.GONE
                activity.findViewById<LinearLayout>(R.id.result)?.visibility = View.VISIBLE
                activity.findViewById<TextView>(R.id.tag_info)?.text =
                    tagInfo.joinToString(separator = "\n")
                activity.findViewById<TextView>(R.id.mem_dump)?.text =
                    memInfo.joinToString(separator = "\n")
            }
        }
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
        systemCode = nfc.systemCode
        pmm = nfc.manufacturer
    }

    /* For debug */
    private fun printByteArray(prefix: String, array: List<Byte>) =
        printByteArray(prefix, array.toByteArray())

    private fun printByteArray(prefix: String, array: ByteArray) {
        Log.d(
            TAG,
            "$prefix ${array.joinToString(separator = ":") { eachByte -> "%02X".format(eachByte) }}"
        )
    }

    /**
     * Implementation of `Polling`
     */
    fun polling(): ByteArray? {
        val cmdCode = byteArrayOf(0x00)
        val reqCode = byteArrayOf(0x00)
        val timeSlot = byteArrayOf(0x0f)

        val command = cmdCode + systemCode + reqCode + timeSlot
        val msg = byteArrayOf((command.size + 1).toByte()) + command
        return nfc.transceive(msg)
    }

    /**
     * Implementation of `Request Service`
     */
    fun requestService(serviceCode: ByteArray): ByteArray? {
        val cmdCode = byteArrayOf(0x02)
        val nodeNum = byteArrayOf(0x01)
        val nodeCodeList = byteArrayOf(serviceCode[1], serviceCode[0])

        val command = cmdCode + idm + nodeNum + nodeCodeList
        val msg = byteArrayOf((command.size + 1).toByte()) + command
        return nfc.transceive(msg)
    }

    /**
     * Implementation of `Request Response`
     */
    fun requestResponse(): ByteArray? {
        val cmdCode = byteArrayOf(0x04)

        val command = cmdCode + idm
        val msg = byteArrayOf((command.size + 1).toByte()) + command

        return nfc.transceive(msg)
    }

    /**
     * Implementation of `Read Without Encryption`
     *
     * FIXME:
     * There is some kind of bug that emits an error when the block size increases.
     * Error code is A2 and it means "illegal command packet".
     */
    fun readWithoutEncryption(serviceCode: ByteArray, size: Int): ByteArray? {
        val cmdCode = byteArrayOf(0x06) // (1-byte)
        val serviceNum = byteArrayOf(0x01) // 1~16 (1-byte)
        val serviceCodeList =
            byteArrayOf(serviceCode[1], serviceCode[0])    // 0x000B 0x0009 (2m-byte)
        val blockNum = byteArrayOf(size.toByte()) // (1-byte)
        var blockList = byteArrayOf()     // (N-byte, 2n <= N <= 3n)
        for (i in 0 until size) {
            blockList += byteArrayOf(0x80.toByte(), i.toByte())
        }

        val command = cmdCode + idm + serviceNum + serviceCodeList + blockNum + blockList
        val msg = byteArrayOf((command.size + 1).toByte()) + command

        return nfc.transceive(msg)
    }

    /**
     * Implementation of `Search Service Code`
     */
    fun searchServiceCode(index: ByteArray): ByteArray? {
        val cmdCode = byteArrayOf(0x0A)
        val idx = byteArrayOf(index[1], index[0])

        val command = cmdCode + idm + idx
        val msg = byteArrayOf((command.size + 1).toByte()) + command

        return nfc.transceive(msg)
    }

    /**
     * Implementation of `Request System Code`
     */
    fun requestSystemCode(): ByteArray? {
        val cmdCode = byteArrayOf(0x0C)

        val command = cmdCode + idm
        val msg = byteArrayOf((command.size + 1).toByte()) + command
        return nfc.transceive(msg)
    }

    /**
     * Get Array<String> of TAG, IDm, PMm, and System Code
     */
    fun getTagInfo(tag: Tag): Array<String> {
        val nfcf = NfcFReader(tag)
        var res: ByteArray = emptyArray<Byte>().toByteArray()

        try {
            nfcf.nfc.connect()
            res = nfcf.requestSystemCode()!!.drop(10).toByteArray()
            nfcf.nfc.close()
        } catch (e: Exception) {
            nfcf.nfc.close()
        }
        var systemCodeVec = ""
        for (i in 0 until res[0]) {
            systemCodeVec += "%02X:%02X".format(res[i * 2 + 1], res[i * 2 + 2])
            if (i < res[0] - 1)
                systemCodeVec += ", "
        }
        return arrayOf(
            tag.toString(),
            "IDm = ${nfcf.idm.joinToString(separator = ":") { eachByte -> "%02X".format(eachByte) }}",
            "PMm = ${nfcf.pmm.joinToString(separator = ":") { eachByte -> "%02X".format(eachByte) }}",
            "SystemNumber   = %d".format(res[0]),
            "SystemCode     = $systemCodeVec"
        )
    }

    private fun parseServiceAttr(attribute: Int): String {
        when (attribute) {
            0b001000 -> return "Random R/W (w/ auth)"
            0b001001 -> return "Random R/W (w/o auth)"
            0b001010 -> return "Random R/O (w/ auth)"
            0b001011 -> return "Random R/O (w/o auth)"
            0b001100 -> return "Cyclic R/W (w/ auth)"
            0b001101 -> return "Cyclic R/W (w/o auth)"
            0b001110 -> return "Cyclic R/O (w/ auth)"
            0b001111 -> return "Cyclic R/O (w/o auth)"
            0b010000 -> return "Purse (direct, w/ auth)"
            0b010001 -> return "Purse (direct, w/o auth)"
            0b010010 -> return "Purse (cashback, w/ auth)"
            0b010011 -> return "Purse (cashback, w/o auth)"
            0b010100 -> return "Purse (decrement, w/ auth)"
            0b010101 -> return "Purse (decrement, w/o auth)"
            0b010110 -> return "Purse R/O (w/ auth)"
            0b010111 -> return "Purse R/O (w/o auth)"
            else -> return ""
        }
    }

    private fun parseBlockData(size: Int, blockData: ByteArray): Array<String> {
        var ret = emptyArray<String>()
        var data = blockData
        for (i in 0 until size) {
            for (j in 0..1) {
                ret += if (j == 0) {
                    data.joinToString(
                        separator = " ",
                        limit = 8,
                        prefix = "  %2d: ".format(i)
                    ) { eachByte ->
                        "%02X".format(eachByte)
                    }
                } else {
                    data.joinToString(separator = " ", limit = 8, prefix = "      ") { eachByte ->
                        "%02X".format(eachByte)
                    }
                }
                data = data.drop(8).toByteArray()
            }
        }
        return ret
    }

    private fun getMemoryContentOfService(nfcf: NfcFReader, serviceCode: ByteArray): Array<String> {
        var ret = emptyArray<String>()
        var size = 1
        while (true) {
            val res = nfcf.readWithoutEncryption(
                byteArrayOf(
                    serviceCode[0],
                    serviceCode[1]
                ), size
            ) ?: return arrayOf("ERROR: readWithoutEncryption")

            when (res[11]) {
                0x00.toByte() -> {
                    // Success
                    ret = parseBlockData(res[12].toInt(), res.drop(13).toByteArray())
                    size++
                    continue
                }
                0xA5.toByte() -> {
                    return arrayOf("   Access not allowed")
                }
                0xA8.toByte() -> {
                    // Illegal block number
                    return ret
                }
                else -> {
                    return ret
                }
            }
        }
    }

    fun getMemoryContent(tag: Tag): Array<String> {
        val nfcf = NfcFReader(tag)
        var areaInfo: Array<String> = emptyArray()
        var serviceInfo: Array<String> = emptyArray()
        try {
            nfcf.nfc.connect()
            var areaNum = 0
            for (i: Int in 0x0..0xffff) {
                val resSearchServiceCode =
                    nfcf.searchServiceCode(byteArrayOf((i shr 8).toByte(), (i and 0xff).toByte()))
                        ?: break
                if (resSearchServiceCode[10] == 0xFF.toByte() && resSearchServiceCode[11] == 0xFF.toByte())
                    break
                when (resSearchServiceCode[0]) {
                    0x0C.toByte() -> {
                        serviceInfo += "Service 0x%02X%02X (#%d): %s".format(
                            resSearchServiceCode[11],
                            resSearchServiceCode[10],
                            (resSearchServiceCode[11].toInt() shl 2) + ((resSearchServiceCode[10].toInt() shr 6) and 0x3),
                            parseServiceAttr(resSearchServiceCode[10].toInt() and 0x3f)
                        )
                        serviceInfo += getMemoryContentOfService(
                            nfcf, byteArrayOf(resSearchServiceCode[11], resSearchServiceCode[10])
                        )
                    }
                    0x0E.toByte() -> {
                        areaInfo += "Area%d: 0x%02X%02X-0x%02X%02X".format(
                            areaNum,
                            resSearchServiceCode[11],
                            resSearchServiceCode[10],
                            resSearchServiceCode[13],
                            resSearchServiceCode[12]
                        )
                        areaNum++
                    }
                    else -> continue
                }

            }
            nfcf.nfc.close()
        } catch (e: Exception) {
            nfcf.nfc.close()
        }
        return areaInfo + "----------------" + serviceInfo
    }
}