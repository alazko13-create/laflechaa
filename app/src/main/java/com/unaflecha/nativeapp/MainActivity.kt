package com.unaflecha.nativeapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.unaflecha.nativeapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        updateStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        binding.btnPermissions.setOnClickListener { requestAllPermissions() }
        binding.btnToggleService.setOnClickListener { toggleMonitor() }

        val targetUrl = intent.getStringExtra(EXTRA_TARGET_URL)
            ?: Constants.BASE_URL
        binding.webView.loadUrl(targetUrl)
        updateStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_TARGET_URL)?.let { binding.webView.loadUrl(it) }
        updateStatus()
    }

    private fun setupWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportMultipleWindows(false)
        }
        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                return if (uri.host == Constants.BASE_URL.toUri().host) {
                    false
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                updateStatus()
            }
        }
    }

    private fun toggleMonitor() {
        val enabled = !PrefHelper.isMonitorEnabled(this)
        PrefHelper.setMonitorEnabled(this, enabled)
        if (enabled) {
            requestAllPermissions()
            DriverMonitorService.start(this)
            Toast.makeText(this, "Monitoreo nativo activado", Toast.LENGTH_SHORT).show()
        } else {
            DriverMonitorService.stop(this)
            stopService(Intent(this, OverlayBubbleService::class.java))
            Toast.makeText(this, "Monitoreo nativo detenido", Toast.LENGTH_SHORT).show()
        }
        updateStatus()
    }

    private fun requestAllPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) {
            perms += "android.permission.POST_NOTIFICATIONS"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms += Manifest.permission.ACCESS_BACKGROUND_LOCATION
        }
        permissionLauncher.launch(perms.toTypedArray())

        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
    }

    private fun updateStatus() {
        val running = PrefHelper.isMonitorEnabled(this)
        binding.btnToggleService.text = if (running) "Detener nativo" else "Activar nativo"
        val overlay = Settings.canDrawOverlays(this)
        val locationOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        binding.statusText.text = buildString {
            append("Web cargada: ${binding.webView.url ?: Constants.BASE_URL}\n")
            append("Monitoreo nativo: ${if (running) "activo" else "apagado"}\n")
            append("Ubicación: ${if (locationOk) "ok" else "pendiente"}\n")
            append("Burbuja overlay: ${if (overlay) "ok" else "pendiente"}\n")
            append("Inicia sesión como chofer en la web y luego activa el monitoreo.")
        }
    }

    companion object {
        const val EXTRA_TARGET_URL = "target_url"
    }
}
