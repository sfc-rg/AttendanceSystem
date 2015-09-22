package jp.ad.wide.sfc.arch.attendancesystem;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    protected NfcAdapter mNfcAdapter;
    private SoundPool soundPool;
    private int sucSoundId, errSoundId;
    private AudioManager audioManager;
    private static final Object TAG_REQUEST_QUEUE = MainActivity.class.getName();
    String tag_json_obj = "json_obj_req";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        //NFCが搭載されているかチェック
        if (mNfcAdapter == null) {
            //NFC非搭載の場合は通知
            Toast.makeText(getApplicationContext(), getString(R.string.error_nfc_nosupport), Toast.LENGTH_SHORT).show();
            return;
        }

        //NFCが有効かどうかチェック
        if (!mNfcAdapter.isEnabled()) {
            //NFCが無効の場合通知
            Toast.makeText(getApplicationContext(), getString(R.string.error_nfc_disable), Toast.LENGTH_LONG).show();
            //設定画面へ飛ばす
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //起動中のアクティビティが優先的にNFCを受け取れるように設定
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            return;
        }
        mNfcAdapter.enableReaderMode(this, new CustomReaderCallback(), NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);

        AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        soundPool = new SoundPool.Builder().setAudioAttributes(attributes).setMaxStreams(5).build();
        sucSoundId = soundPool.load(this, R.raw.suc, 1);
        errSoundId = soundPool.load(this, R.raw.err, 1);
    }

    private class CustomReaderCallback implements NfcAdapter.ReaderCallback {
        NfcF nfcF;
        byte[] IDm;
        @Override
        public void onTagDiscovered(Tag tag) {
            nfcF = NfcF.get(tag);
            IDm = tag.getId();
            String res = null;
            try {
                res = getStudentNumber(nfcF, IDm);
                Log.d("nfc", res);
                request(res);
            } catch (IOException e) {
                soundPool.stop(sucSoundId);
                soundPool.play(errSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
                e.printStackTrace();
            }

            Log.d("nfc", res);
        }
    }

    private void request(String studentNumber) {
        String URL_API = "http://192.168.1.72:3000/api/v1/attendances";
        JSONObject json = new JSONObject();
        try {
            json.put("access_token", "86a1ceba597a80b2369bff0c60ad10b8597a139360f11eb5048f165e0d734c9a");
            json.put("student_id", studentNumber);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST, URL_API, json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject user = response.getJSONObject("user");
                            Log.d("json", user.toString());
                            soundPool.play(sucSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
        new Response.ErrorListener() {
            @Override
        public void onErrorResponse(VolleyError error) {
               error.printStackTrace();
            }
        });

        AppController.getInstance().addToRequestQueue(jsonObjReq, tag_json_obj);
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
        byte[] res = new byte[]{0};

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

        try {
            nfcF.connect();
            res = nfcF.transceive(msg);
            nfcF.close();
        } catch (IOException e) {
            Log.e("nfc", e.getMessage(), e);
            e.printStackTrace();
            return null;
        }

        return res;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        soundPool.release();
        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        super.onPause();
    }
}
