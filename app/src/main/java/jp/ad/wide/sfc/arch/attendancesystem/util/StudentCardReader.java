package jp.ad.wide.sfc.arch.attendancesystem.util;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class StudentCardReader implements NfcAdapter.ReaderCallback {
    protected Activity mActivity;
    protected NfcAdapter mNfcAdapter;
    protected StudentCardListener mListener;

    public StudentCardReader(Activity activity, StudentCardListener listener) {
        mActivity = activity;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mActivity);
        mListener = listener;
    }

    public void enable() {
        if (mActivity == null) return;
        mNfcAdapter.enableReaderMode(mActivity, this, NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
    }

    public void disable() {
        if (mActivity == null) return;
        mNfcAdapter.disableReaderMode(mActivity);
    }

    public boolean isNfcInstalled() {
        return mActivity != null;
    }

    public boolean isNfcEnabled() {
        return mActivity != null && mNfcAdapter.isEnabled();
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        NfcF nfcF = NfcF.get(tag);
        byte[] IDm = tag.getId();
        try {
            String studentNumber = getStudentNumber(nfcF, IDm);
            mListener.onDiscovered(studentNumber);
        } catch (IOException e) {
            mListener.onError(e);
        }
    }

    String getStudentNumber(NfcF nfcF, byte[] IDm) throws IOException {
        byte[] res = new byte[0];
        try {
            res = readWithoutEncryption(nfcF, IDm);
        } catch (IOException e) {
            Log.e("nfc", e.getMessage(), e);
            e.printStackTrace();
        }
        if (res == null) return null;
        byte[] numberCodes = new byte[]{res[13], res[14], res[15], res[16], res[17], res[18], res[19], res[20]};
        return new String(numberCodes, "US-ASCII");
    }

    byte[] readWithoutEncryption(NfcF nfcF, byte[] IDm) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(100);
        byte[] res;

        //size of send data
        bout.write(0x10);

        /**********************
         * command packet data
         **********************
         * command code         0x06(Read Without Encryption)
         * idm
         * number of services   0x01
         * service code list    0x0B11
         * number of blocks     0x01
         */
        bout.write(0x06);
        bout.write(IDm);
        bout.write(0x01);
        bout.write(0x0B);
        bout.write(0x11);
        bout.write(0x01);

        /***********************
         *  block list element
         ***********************
         * b1       2 bytes block list element
         * b000     access mode
         * b1001    service code list order
         * -> b1000 1001 = 0x8001
         */
        bout.write(0x80);
        bout.write(0x01);

        byte[] msg = bout.toByteArray();
        nfcF.connect();
        res = nfcF.transceive(msg);
        nfcF.close();

        return res;
    }

    public interface StudentCardListener {
        void onDiscovered(String studentNumber);
        void onError(Exception exception);
    }
}
