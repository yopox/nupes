package com.yopox.ui

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.yopox.HEIGHT
import com.yopox.TILE

object GUI {
    private val alphabet = "abcdefghijklmnopqrsuvwxyz".map { Letter(it) }.apply {
        this[4].color = Colors.RED
    }

    fun draw(batch: SpriteBatch) {
        Text.draw(alphabet, HEIGHT - TILE * 3, batch)
    }
}