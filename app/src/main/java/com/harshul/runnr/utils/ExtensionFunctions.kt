package com.harshul.runnr.utils

import android.app.Activity
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import com.harshul.runnr.R

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun Activity.setNavStatusColor(color: Int) {
    window.apply {
        statusBarColor = getColor(color)
        navigationBarColor = getColor(color)
    }
}
fun Activity.lightTheme() {
    window.apply {
        statusBarColor = getColor(R.color.white)
        navigationBarColor = getColor(R.color.white)
    }
}

fun TextView.setSpannableTxt(
    text: String,
    textColor: Int,
    start: Int,
    end: Int
) {
    val color = ForegroundColorSpan(context.getColor(textColor))
    val spannableString = SpannableString(text)
    spannableString.setSpan(color, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    setText(spannableString)
}