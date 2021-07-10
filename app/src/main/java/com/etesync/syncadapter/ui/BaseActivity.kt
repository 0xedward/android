package com.etesync.syncadapter.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager


open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val flags = getWindow().getAttributes().flags
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        if ((flags and WindowManager.LayoutParams.FLAG_SECURE) != 0) {
            Log.d("FLAG_SECURE", "SET")
        } else {
            Log.d("FLAG_SECURE", "NOT SET")
        }
//        val sharedPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext())
//        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
//        val secureTest = prefs.getBoolean("block_screenshots", false)
//        Log.d("secureTest", secureTest.toString())
//        if (secureTest) {
//            // TODO maybe a workaround is to inform the user they need to restart the application for this effect to kick in
//            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
//        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (!supportFragmentManager.popBackStackImmediate()) {
                finish()
            }
            return true
        }
        return false
    }
}