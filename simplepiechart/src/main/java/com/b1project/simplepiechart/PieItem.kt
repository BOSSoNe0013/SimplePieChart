package com.b1project.simplepiechart

import android.graphics.Color

data class PieItem (
        var value: Float = 0.0F,
        var label: String? = null,
        var color: Int = 0,
        var texture: Int = 0
) {
    companion object {
        val dummyData = listOf(
                PieItem(3F, "One", Color.RED, 0),
                PieItem(3F, "Two", Color.GREEN, 0),
                PieItem(3F, "Three", Color.BLUE, 0))
    }
}

