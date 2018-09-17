package jp.ad.wide.sfc.arch.attendancesystem

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.VolleyError
import jp.ad.wide.sfc.arch.attendancesystem.net.PortalClient
import jp.ad.wide.sfc.arch.attendancesystem.util.StudentCardReader
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity(), StudentCardReader.StudentCardListener,
    Response.Listener<JSONObject>, Response.ErrorListener {
    private lateinit var portalClient: PortalClient
    private lateinit var studentReader: StudentCardReader
    private lateinit var audioManager: AudioManager
    private lateinit var soundPool: SoundPool
    private var sucSoundId = 0
    private var errSoundId = 0
    private var lastScannedStudentNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        studentReader = StudentCardReader(this, this)

        if (!studentReader.isNfcInstalled) {
            Toast.makeText(this, getString(R.string.error_nfc_nosupport), Toast.LENGTH_LONG).show()
            return
        }

        if (!studentReader.isNfcEnabled) {
            Toast.makeText(this, getString(R.string.error_nfc_disable), Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }

        inputAccessToken()
    }

    override fun onResume() {
        super.onResume()
        studentReader.enable()
        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        soundPool = SoundPool.Builder()
            .setAudioAttributes(attributes)
            .setMaxStreams(5)
            .build()
        sucSoundId = soundPool.load(this, R.raw.suc, 1)
        errSoundId = soundPool.load(this, R.raw.err, 1)
    }

    override fun onPause() {
        super.onPause()
        studentReader.disable()
        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false)
        soundPool.release()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> inputAccessToken()
            R.id.input_student_number -> inputStudentNumberManually()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onDiscovered(studentNumber: String) {
        if (studentNumber == lastScannedStudentNumber) {
            return
        }
        portalClient.createAttendance(studentNumber)
        lastScannedStudentNumber = studentNumber
    }

    override fun onError(exception: Exception) {
        soundPool.play(errSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    override fun onResponse(response: JSONObject) {
        soundPool.play(sucSoundId, 1.0f, 1.0f, 0, 0, 1.0f)

        try {
            val user = response.getJSONObject("user")
            val name = user.getString("name")
            val nickname = user.getString("nickname")
            val iconUrl = user.getString("icon_url")
            val count = response.getString("attendance_count")
            changeDisplay(name, nickname, iconUrl, count)
        } catch (e: JSONException) {
            Log.e("request", response.toString())
            e.printStackTrace()
        }

    }

    override fun onErrorResponse(error: VolleyError) {
        soundPool.play(errSoundId, 1.0f, 1.0f, 0, 0, 1.0f)

        val newError = VolleyError(String(error.networkResponse.data))
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Error").setMessage(String(error.networkResponse.data)).show()
        Log.e("request", newError.toString())
    }

    private fun changeDisplay(name: String, loginName: String, url: String, count: String) {
        val nameTextView = this.findViewById<TextView>(R.id.name)
        val loginNameTextView = this.findViewById<TextView>(R.id.login_name)
        val countTextView = this.findViewById<TextView>(R.id.attendance_count)
        //TODO 画像を取得する
        nameTextView.text = name
        loginNameTextView.text = loginName
        countTextView.text = "Your attendance: $count"
    }

    private fun inputAccessToken() {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("アクセストークン入力")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val accessToken = editText.text.toString()
                portalClient = PortalClient(application as AppController, this, this, accessToken)
            }
            .show()
    }

    private fun inputStudentNumberManually() {
        val editText = EditText(this)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        AlertDialog.Builder(this)
            .setTitle("Input your student number.")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                portalClient.createAttendance(editText.text.toString())
            }
            .show()
    }
}
