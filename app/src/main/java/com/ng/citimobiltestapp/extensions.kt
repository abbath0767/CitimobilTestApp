package com.ng.citimobiltestapp

import android.support.v4.content.ContextCompat
import android.util.Log

inline fun log(message: String) = Log.d("TAG", message)

inline fun Int.asColor() = ContextCompat.getColor(App.instance.applicationContext, this)
inline fun Int.dimen(): Float = App.instance.applicationContext.resources.getDimension(this)