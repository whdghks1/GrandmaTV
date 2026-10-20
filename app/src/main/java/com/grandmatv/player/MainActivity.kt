package com.grandmatv.player

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    companion object {
        private const val CHANNEL_URL = "https://onair.imbc.com/MbicPlay?channelid=43"
        private const val AUTO_LOCK_DELAY_MS = 30_000L
        private const val UNLOCK_HOLD_MS = 4_000L
    }

    private lateinit var root: FrameLayout
    private lateinit var browser: WebView
    private lateinit var shield: View
    private lateinit var hint: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var focusVideo = true
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null
    private var locked = false
    private var downAt = 0L
    private var startedAtTopLeft = false
    private val autoLock = Runnable { lockTouch() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        hideSystemBars()
        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(root)
        browser = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportMultipleWindows(false)
            settings.javaScriptCanOpenWindowsAutomatically = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    installPlayerAssist()
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    return uri.scheme != "https" && uri.scheme != "about"
                }
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        Toast.makeText(this@MainActivity, "연결 오류: 보호자 메뉴에서 새로고침하세요", Toast.LENGTH_LONG).show()
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view == null || customView != null) { callback?.onCustomViewHidden(); return }
                    customView = view
                    customCallback = callback
                    root.addView(view, FrameLayout.LayoutParams(-1, -1))
                    shield.bringToFront()
                    hint.bringToFront()
                    hideSystemBars()
                }
                override fun onHideCustomView() { exitWebFullscreen() }
            }
        }
        root.addView(browser, FrameLayout.LayoutParams(-1, -1))
        shield = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            visibility = View.GONE
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downAt = SystemClock.uptimeMillis()
                        startedAtTopLeft = event.x <= dp(92) && event.y <= dp(92)
                    }
                    MotionEvent.ACTION_UP -> {
                        if (startedAtTopLeft && SystemClock.uptimeMillis() - downAt >= UNLOCK_HOLD_MS) {
                            showGuardianDialog()
                        }
                        startedAtTopLeft = false
                    }
                    MotionEvent.ACTION_CANCEL -> startedAtTopLeft = false
                }
                true // Entire website/player is untouchable while locked
            }
        }
        root.addView(shield, FrameLayout.LayoutParams(-1, -1))
        hint = TextView(this).apply {
            text = "30초 후 터치 잠금 · 보호자: 왼쪽 위를 4초간 누르기"
            setTextColor(Color.WHITE)
            setBackgroundColor(0x99000000.toInt())
            textSize = 12f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { showGuardianDialog() }
        }
        root.addView(hint, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START))
        browser.loadUrl(CHANNEL_URL)
        handler.postDelayed(autoLock, AUTO_LOCK_DELAY_MS)
    }

    private fun installPlayerAssist() {
        try {
            val script = assets.open("player_assist.js").bufferedReader().use { it.readText() }
            browser.evaluateJavascript(script) {
                browser.evaluateJavascript("window.__grandmaSetFocus && window.__grandmaSetFocus(${focusVideo});", null)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "영상 화면 보조 기능 실행 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun lockTouch() {
        locked = true
        shield.visibility = View.VISIBLE
        hint.visibility = View.GONE
        shield.bringToFront()
        hideSystemBars()
    }

    private fun unlockTouch() {
        locked = false
        shield.visibility = View.GONE
        hint.visibility = View.VISIBLE
        handler.removeCallbacks(autoLock)
        handler.postDelayed(autoLock, AUTO_LOCK_DELAY_MS)
    }

    private fun showGuardianDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("보호자 메뉴")
            .setMessage("화면을 조작하거나 방송을 다시 연결할 수 있습니다.")
            .setPositiveButton("30초 터치 해제") { _, _ -> unlockTouch() }
            .setNeutralButton("새로고침") { _, _ ->
                unlockTouch()
                browser.loadUrl(CHANNEL_URL)
            }
            .setNegativeButton("더보기") { _, _ -> showMoreDialog() }
            .create()
        dialog.setOnDismissListener { hideSystemBars() }
        dialog.show()
    }

    private fun showMoreDialog() {
        AlertDialog.Builder(this)
            .setTitle("보호자 기능")
            .setItems(arrayOf(if (focusVideo) "원래 웹페이지 보기" else "영상만 크게 보기", "Chrome으로 열기", "앱 종료", "취소")) { _, which ->
                when (which) {
                    0 -> {
                        focusVideo = !focusVideo
                        browser.evaluateJavascript("window.__grandmaSetFocus && window.__grandmaSetFocus(${focusVideo});", null)
                    }
                    1 -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(CHANNEL_URL))
                        intent.setPackage("com.android.chrome")
                        try { startActivity(intent) } catch (_: Exception) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CHANNEL_URL)))
                        }
                    }
                    2 -> finish()
                }
            }.show()
    }

    private fun exitWebFullscreen() {
        customView?.let { root.removeView(it) }
        customView = null
        customCallback?.onCustomViewHidden()
        customCallback = null
        hideSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }

    @Deprecated("Back is deliberately ignored to protect the viewing page")
    override fun onBackPressed() {
        // Ignore accidental Back; guardian controls are available from top-left long press.
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoLock)
        browser.destroy()
        super.onDestroy()
    }
}
