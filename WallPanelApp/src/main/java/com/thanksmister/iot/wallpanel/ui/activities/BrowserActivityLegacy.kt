/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.iot.wallpanel.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.preference.PreferenceManager
import android.text.TextUtils
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebSettings
import com.thanksmister.iot.wallpanel.R
import com.thanksmister.iot.wallpanel.persistence.Configuration

import org.xwalk.core.XWalkResourceClient
import org.xwalk.core.XWalkView
import org.xwalk.core.XWalkCookieManager

import timber.log.Timber
import javax.inject.Inject
import android.content.DialogInterface
import android.net.http.SslError
import android.support.v7.app.AlertDialog
import android.webkit.SslErrorHandler
import android.webkit.WebView
import kotlinx.android.synthetic.main.activity_browser.*


class BrowserActivityLegacy : BrowserActivity() {

    private var xWebView: XWalkView? = null

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        try {
            setContentView(R.layout.activity_browser)
         } catch (e: Exception) {
            Timber.e(e.message)
            //dialogUtils.showAlertDialog(this@BrowserActivityLegacy, getString(R.string.dialog_missing_webview_warning))
            AlertDialog.Builder(this@BrowserActivityLegacy)
                    .setMessage(getString(R.string.dialog_missing_webview_warning))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            return
        }

        swipeContainer.setOnRefreshListener { loadUrl(configuration.appLaunchUrl)}

        xWebView = findViewById<View>(R.id.activity_browser_webview_legacy) as XWalkView
        xWebView!!.visibility = View.VISIBLE
        clearCache()

        xWebView!!.setResourceClient(object : XWalkResourceClient(xWebView) {

            var snackbar: Snackbar

            init {
                snackbar = Snackbar.make(xWebView!!, "", Snackbar.LENGTH_INDEFINITE)
            }

            override fun onProgressChanged(view: XWalkView, progressInPercent: Int) {
                if (!displayProgress) return

                if (progressInPercent == 100) {
                    snackbar.dismiss()
                    pageLoadComplete(view.url)
                } else {
                    //val text = "Loading " + progressInPercent + "% " + view.url
                    val text = getString(R.string.text_loading_percent, progressInPercent.toString(), view.url)
                    snackbar.setText(text)
                    snackbar.show()
                }
            }
        })

        xWebView!!.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    resetScreen()
                    if (!v.hasFocus()) {
                        v.requestFocus()
                    }
                }
                MotionEvent.ACTION_UP -> if (!v.hasFocus()) {
                    v.requestFocus()
                }
            }
            false
        }

        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        swipeContainer.viewTreeObserver.addOnScrollChangedListener {
            swipeContainer.isEnabled = xWebView!!.scrollY == 0
        }
    }

    override fun onStop() {
        super.onStop()
        swipeContainer.viewTreeObserver.removeOnScrollChangedListener(mOnScrollChangedListener)
    }

    override fun complete() {
        if(swipeContainer.isRefreshing) {
            swipeContainer.isRefreshing = false
        }
    }

    override fun configureWebSettings(userAgent: String) {
        val webSettings = xWebView!!.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true

        if(!TextUtils.isEmpty(userAgent)) {
            webSettings.userAgentString = userAgent
        }
        Timber.d(webSettings.userAgentString)
    }

    override fun loadUrl(url: String) {
        if (zoomLevel.toDouble() != 1.0) {
            xWebView!!.setInitialScale((zoomLevel * 100).toInt())
        }
        xWebView!!.loadUrl(url)
    }

    override fun evaluateJavascript(js: String) {
        xWebView!!.evaluateJavascript(js, null)
    }

    override fun clearCache() {
        xWebView!!.clearCache(true)
        val manager = XWalkCookieManager()
        manager.removeAllCookie()
    }

    override fun reload() {
        xWebView!!.reload(XWalkView.RELOAD_NORMAL)
    }
}