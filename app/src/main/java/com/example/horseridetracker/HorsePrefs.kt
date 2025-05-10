package com.example.horseridetracker

import android.content.Context

object HorsePrefs {
    private const val PREF = "horses"
    private const val KEY  = "pony"

    fun load(context: Context): MutableList<String> =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())!!
            .sorted()
            .toMutableList()

    fun save(context: Context, horses: Set<String>) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY, horses)
            .apply()
    }
}
