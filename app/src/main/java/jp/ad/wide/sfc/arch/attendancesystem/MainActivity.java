package jp.ad.wide.sfc.arch.attendancesystem;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import jp.ad.wide.sfc.arch.attendancesystem.net.PortalClient;
import jp.ad.wide.sfc.arch.attendancesystem.util.StudentCardReader;

public class MainActivity
        extends AppCompatActivity
        implements StudentCardReader.StudentCardListener, Response.Listener<JSONObject>, Response.ErrorListener {
    private PortalClient mPortalClient;
    private StudentCardReader mStudentReader;
    private AudioManager mAudioManager;
    private SoundPool soundPool;
    private int sucSoundId, errSoundId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mStudentReader = new StudentCardReader(this, this);

        if (!mStudentReader.isNfcInstalled()) {
            Toast.makeText(getApplicationContext(), getString(R.string.error_nfc_nosupport), Toast.LENGTH_LONG).show();
            return;
        }

        if (!mStudentReader.isNfcEnabled()) {
            Toast.makeText(getApplicationContext(), getString(R.string.error_nfc_disable), Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }

        inputAccessToken();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStudentReader.enable();
        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder().setAudioAttributes(attributes).setMaxStreams(5).build();
        sucSoundId = soundPool.load(this, R.raw.suc, 1);
        errSoundId = soundPool.load(this, R.raw.err, 1);
    }

    @Override
    public void onPause() {
        super.onPause();
        mStudentReader.disable();
        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        soundPool.release();
    }

    @Override
    public void onDiscovered(String studentNumber) {
        mPortalClient.createAttendance(studentNumber);
    }

    @Override
    public void onError(Exception exception) {
        soundPool.play(errSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
    }

    @Override
    public void onResponse(JSONObject response) {
        soundPool.play(sucSoundId, 1.0f, 1.0f, 0, 0, 1.0f);

        try {
            JSONObject user = response.getJSONObject("user");
            String name = user.getString("name");
            String nickname = user.getString("nickname");
            String iconUrl = user.getString("icon_url");
            String count = response.getString("attendance_count");
            changeDisplay(name, nickname, iconUrl, count);
        } catch (JSONException e) {
            Log.e("request", response.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        soundPool.play(errSoundId, 1.0f, 1.0f, 0, 0, 1.0f);

        VolleyError newError = new VolleyError(new String(error.networkResponse.data));
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setTitle("Error").setMessage(new String(error.networkResponse.data)).show();
        Log.e("request", newError.toString());
    }

    protected void changeDisplay(String name, String loginName, String url, String count) {
        TextView nameTextView = (TextView)this.findViewById(R.id.name);
        TextView loginNameTextView = (TextView)this.findViewById(R.id.login_name);
        TextView countTextView = (TextView)this.findViewById(R.id.attendance_count);
        //TODO 画像を取得する
        nameTextView.setText(name);
        loginNameTextView.setText(loginName);
        countTextView.setText("Your attendance: " + count);
    }

    protected void inputAccessToken() {
        final EditText editText = new EditText(MainActivity.this);
        new AlertDialog
            .Builder(this)
            .setTitle("アクセストークン入力")
            .setView(editText)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String accessToken = editText.getText().toString();
                    mPortalClient = new PortalClient(MainActivity.this, MainActivity.this, accessToken);
                }
            })
            .show();
    }

    protected void inputStudentNumberManually() {
        final EditText editText = new EditText(MainActivity.this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog
            .Builder(MainActivity.this)
            .setTitle("Input your student number.")
            .setView(editText)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mPortalClient.createAttendance(editText.getText().toString());
                }
            })
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                inputAccessToken();
                return true;
            case R.id.input_student_number:
                inputStudentNumberManually();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
