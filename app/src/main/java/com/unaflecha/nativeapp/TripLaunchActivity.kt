package com.unaflecha.nativeapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TripLaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetUrl = intent.getStringExtra(MainActivity.EXTRA_TARGET_URL) ?: Constants.BASE_URL
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_TARGET_URL, targetUrl)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        )
        finish()
    }
}
