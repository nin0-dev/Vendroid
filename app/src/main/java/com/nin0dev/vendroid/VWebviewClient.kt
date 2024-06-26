package com.nin0dev.vendroid

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.nin0dev.vendroid.Logger.e
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class VWebviewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url
        if ("discord.com" == url.authority || "about:blank" == url.toString()) {
            return false
        }
        val intent = Intent(Intent.ACTION_VIEW, url)
        view.context.startActivity(intent)
        return true
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        view.evaluateJavascript(HttpClient.VencordRuntime!!, null)
        view.evaluateJavascript(HttpClient.VencordMobileRuntime!!, null)
    }

    override fun onPageFinished(view: WebView, url: String) {
        view.visibility = View.VISIBLE
        super.onPageFinished(view, url)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldInterceptRequest(view: WebView, req: WebResourceRequest): WebResourceResponse? {
        val uri = req.url
        if (req.isForMainFrame || req.url.path!!.endsWith(".css")) {
            try {
                return doFetch(req)
            } catch (ex: IOException) {
                //e("Error during shouldInterceptRequest", ex)
            }
        }
        return null
    }

        @RequiresApi(Build.VERSION_CODES.N)
        private fun doFetch(req: WebResourceRequest): WebResourceResponse {
            val url = req.url.toString()
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestMethod(req.method)
            for ((key, value) in req.requestHeaders) {
                conn.setRequestProperty(key, value)
            }
            val code = conn.getResponseCode()
            val msg = conn.getResponseMessage()
            val headers = conn.headerFields
            val modifiedHeaders = HashMap<String, String>(headers.size)
            for ((key, valueList) in headers) {
                if (key == null) {
                    continue
                }
                if (!"Content-Security-Policy".equals(key, ignoreCase = true)) {
                    if (valueList != null && valueList.isNotEmpty()) {
                        val value = valueList[0]
                        modifiedHeaders[key] = value
                    }
                }
            }
            if (url.endsWith(".css")) modifiedHeaders["Content-Type"] = "text/css"
            val wong = modifiedHeaders.getOrDefault("Content-Type", "application/octet-stream")
            return WebResourceResponse(wong, "utf-8", code, msg, modifiedHeaders, conn.inputStream)
        }
}
