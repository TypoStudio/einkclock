package com.typostudio.einkclock

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val htmlFile get() = File(filesDir, "index.html")
    private val localesDir get() = File(filesDir, "locales")
    private val updateUrl = "https://raw.githubusercontent.com/TypoStudio/einkclock/main/index.html"
    private val localeBaseUrl = "https://raw.githubusercontent.com/TypoStudio/einkclock/main/locales"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 잠금화면 위에 앱 표시 (시계는 잠금 없이 보임, 다른 앱 전환 시 잠금 해제 요청)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, true, false)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView, request: WebResourceRequest
            ): WebResourceResponse? {
                val path = request.url.path ?: return null
                if (request.url.scheme == "file" && path.endsWith(".json")) {
                    val file = File(path)
                    if (file.exists()) {
                        return WebResourceResponse("application/json", "UTF-8", file.inputStream())
                    }
                }
                return null
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme ?: return false
                if (scheme == "http" || scheme == "https") {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    return true
                }
                return false
            }
        }

        // 최초 실행 시 assets → 내부 저장소로 복사
        if (!htmlFile.exists()) {
            assets.open("index.html").use { it.copyTo(htmlFile.outputStream()) }
        }
        if (!localesDir.exists()) {
            localesDir.mkdirs()
            assets.list("locales")?.forEach { name ->
                assets.open("locales/$name").use { it.copyTo(File(localesDir, name).outputStream()) }
            }
        }

        webView.loadUrl("file://${htmlFile.absolutePath}")

        // 백그라운드에서 최신 파일 다운로드 (다음 실행부터 적용)
        fetchLatestHtml()
        fetchLatestLocales()

        onBackPressedDispatcher.addCallback(this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) webView.goBack()
                    else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            })
    }

    private fun fetchLatestHtml() {
        Thread {
            try {
                val content = URL(updateUrl).readText(Charsets.UTF_8)
                htmlFile.writeText(content)
            } catch (_: Exception) { }
        }.start()
    }

    private fun fetchLatestLocales() {
        Thread {
            localesDir.mkdirs()
            val names = assets.list("locales") ?: return@Thread
            names.forEach { name ->
                try {
                    val content = URL("$localeBaseUrl/$name").readText(Charsets.UTF_8)
                    File(localesDir, name).writeText(content)
                } catch (_: Exception) { }
            }
        }.start()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }
}
