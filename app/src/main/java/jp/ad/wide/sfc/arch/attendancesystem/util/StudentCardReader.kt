package jp.ad.wide.sfc.arch.attendancesystem.util

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException

class StudentCardReader(
    private val activity: Activity,
    private val listener: StudentCardListener
) : NfcAdapter.ReaderCallback {
    private val nfcAdapter: NfcAdapter = NfcAdapter.getDefaultAdapter(activity)

    val isNfcInstalled = true
    val isNfcEnabled = nfcAdapter.isEnabled

    fun enable() {
        nfcAdapter.enableReaderMode(
            activity,
            this,
            NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    fun disable() {
        nfcAdapter.disableReaderMode(activity)
    }

    override fun onTagDiscovered(tag: Tag) {
        val nfcF = NfcF.get(tag)
        val iDm = tag.id
        try {
            val studentNumber = getStudentNumber(nfcF, iDm)
            if (studentNumber != null) {
                listener.onDiscovered(studentNumber)
            } else {
                val exception = NullPointerException("Student number is null")
                listener.onError(exception)
            }
        } catch (e: IOException) {
            listener.onError(e)
        }
    }

    @Throws(IOException::class)
    private fun getStudentNumber(nfcF: NfcF, IDm: ByteArray): String? {
        val res: ByteArray
        try {
            res = readWithoutEncryption(nfcF, IDm)
        } catch (e: IOException) {
            Log.e("nfc", e.message, e)
            listener.onError(e)
            return null
        }

        val numberCodes = res.copyOfRange(13, 21)
        return String(numberCodes, Charsets.US_ASCII)
    }

    @Throws(IOException::class)
    private fun readWithoutEncryption(nfcF: NfcF, iDm: ByteArray): ByteArray {
        val bout = ByteArrayOutputStream(100)
        val res: ByteArray

        bout.apply {
            //size of send data
            write(0x10)

            /**********************
             * command packet data
             *
             * command code         0x06(Read Without Encryption)
             * idm
             * number of services   0x01
             * service code list    0x0B11
             * number of blocks     0x01
             */
            write(0x06)
            write(iDm)
            write(0x01)
            write(0x0B)
            write(0x11)
            write(0x01)

            /***********************
             * block list element
             *
             * b1       2 bytes block list element
             * b000     access mode
             * b1001    service code list order
             * -> b1000 1001 = 0x8001
             */
            write(0x80)
            write(0x01)
        }

        val msg = bout.toByteArray()
        nfcF.connect()
        res = nfcF.transceive(msg)
        nfcF.close()

        return res
    }

    interface StudentCardListener {
        fun onDiscovered(studentNumber: String)
        fun onError(exception: Exception)
    }
}
