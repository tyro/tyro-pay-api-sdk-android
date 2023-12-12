package com.tyro.payapi.util

object ThreeDSUtils {
    private val PAY_3DS_WEB_VIEW_URL = "https://pay.3ds.connect.tyro.com/"

    fun isWebViewFinished(url: String?): Boolean {
        return url !== null && url.contains("#result=done")
    }

    fun getWebViewUrl(paySecret: String): String {
        var queryString = java.net.URLEncoder.encode(paySecret, "utf-8")
        return "$PAY_3DS_WEB_VIEW_URL?paysecret=$queryString"
    }
}
