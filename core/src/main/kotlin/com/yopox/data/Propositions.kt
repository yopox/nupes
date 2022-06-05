package com.yopox.data

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.yopox.TILE
import com.yopox.WIDTH
import com.yopox.ui.*
import ktx.math.vec2
import kotlin.math.max

private const val UNKNOWN = '~'

class Proposition(before: String, answer: String, after: String) {
    private val before = before.toLetterList(Colors.WHITE, Colors.WHITE)
    private var guess = answer.map {
        when (it.lowercaseChar()) {
            in GIVEN_CHARS -> Letter(it, Colors.WHITE, Colors.WHITE)
            else -> Letter(UNKNOWN, Colors.WHITE, Colors.WHITE)
        }
    }.toMutableList()
    private val after = after.toLetterList(Colors.WHITE, Colors.WHITE)

    private val answer = answer.lowercase()
    private val found = mutableListOf<Int>()
    private val answerOffset: Float = (WIDTH - answer.length * TILE) / 2

    private var lastGuess = ""
    private var counter = 0
    private var charCounter = 0

    val over: Boolean
        get() = guess.filter { it.char in VALID_CHARS || it.char == UNKNOWN }.all { it.color == Colors.GREEN }

    fun draw(currentGuess: String, posY: Float, batch: SpriteBatch) {
        val h1 = before.height
        val h2 = guess.height
        val h3 = after.height

        val gap1 = if (before.any()) TILE.toInt() else 0
        val gap2 = if (after.any()) TILE.toInt() else 0
        val totalHeight = h1 + h2 + h3 + gap1 + gap2

        // Draw proposition
        if (before.any()) before.draw(posY + totalHeight / 2 - h1 / 2, batch)
        guess.draw(posY + totalHeight / 2 - h1 - gap1 - h2 / 2, batch)
        if (after.any()) after.draw(posY + totalHeight / 2 - h1 - gap1 - h2 - gap2 - h3 / 2, batch)

        // Draw current guess
        currentGuess.toLetterList(Colors.BLUE, Colors.WHITE).draw(vec2(answerOffset, posY + totalHeight / 2 - h1 - gap1 - h2 / 2), batch, true)
    }

    fun completeGuess(currentGuess: String, char: Char): String {
        if (char.lowercaseChar() !in VALID_CHARS) return currentGuess
        if (currentGuess.length == answer.length) return currentGuess
        val nextChar = answer[currentGuess.length]
        var newGuess = currentGuess + char
        if (nextChar in GIVEN_CHARS) newGuess = "$currentGuess$nextChar$char"
        return newGuess.substring(0, minOf(newGuess.length, answer.length))
    }

    fun guess(text: String) {
        lastGuess = text
        text.mapIndexed { index, c -> guess[index] = Letter(c, Colors.BLUE, Colors.WHITE) }
    }

    private fun updateGuessChar(i: Int) {
        if (i >= answer.length) return
        val char = sanitize(lastGuess[i])
        if (char !in VALID_CHARS) return
        when (char) {
            sanitize(answer[i]) -> {
                guess[i].apply { this.char = char; color = Colors.GREEN }
                found.add(i)
            }
            !in answer -> {
                guess[i].apply { this.char = char; color = Colors.RED }
            }
            else -> {
                val sanitizedAnswer = answer.map { sanitize(it) }
                val nChar = sanitizedAnswer.count { it == char }
                val rightChars = lastGuess.filterIndexed { index, c -> c == char && sanitizedAnswer[index] == c }
                val alreadyFound = answer.filterIndexed { index, c -> index in found && c == char }
                val wrongChars = lastGuess.filterIndexed { index, c -> index < i && c == char && sanitizedAnswer[index] != char }

                val color = when {
                    nChar > max(rightChars.length, alreadyFound.length) + wrongChars.length -> Colors.ORANGE
                    else -> Colors.BLUE
                }
                guess[i].apply { this.char = lastGuess[i]; this.color = color }
            }
        }
        if (i in found) guess[i].apply {
            this.char = answer[i]
            color = Colors.GREEN
        }
    }

    private fun sanitize(c: Char): Char = when (c) {
        in "àâ" -> 'a'
        in "ç" -> 'c'
        in "èéêë" -> 'e'
        in "ô" -> 'o'
        in "ùû" -> 'u'
        else -> c
    }

    fun update(): Boolean {
        if (charCounter == lastGuess.length) {
            for (c in VALID_CHARS) {
                val color = when {
                    c in answer && answer.withIndex().filter { it.value == c }.map { it.index }.all { it in found } -> Colors.GREEN
                    c in answer && c in lastGuess && !answer.withIndex().filter { it.value == c }.map { it.index }.all { it in found } -> Colors.ORANGE
                    c in lastGuess && c !in answer -> Colors.RED
                    else -> null
                }
                color?.let { GUI.update(c, it) }
            }
            charCounter = 0
            counter = 0
            return true
        }
        if (counter == 0) {
            updateGuessChar(charCounter)
            counter = when (guess[charCounter].color) {
                Colors.GREEN -> 6
                Colors.ORANGE -> 8
                else -> 4
            }
            charCounter += 1
        } else {
            counter -= 1
        }
        return false
    }

    fun reveal(): Boolean {
        val colorCondition = { x: Letter -> x.color == Colors.WHITE }
        if (counter > 0) {
            counter -= 1
            return false
        }
        if (before.any(colorCondition)) {
            val letter = before.first(colorCondition)
            letter.color = Colors.BLUE
            counter = if (letter.char in VALID_CHARS) 1 else 2
        } else if (guess.any(colorCondition)) {
            val letter = guess.first(colorCondition)
            letter.color = Colors.BLUE
            counter = if (letter.char in VALID_CHARS) 2 else 4
        } else if (after.any(colorCondition)) {
            val letter = after.first(colorCondition)
            letter.color = Colors.BLUE
            counter = if (letter.char in VALID_CHARS) 1 else 2
        } else {
            counter = 0
            return true
        }
        return false
    }

    fun hide(): Boolean {
        val colorCondition = { x: Letter -> x.color != Colors.WHITE }
        if (counter > 0) {
            counter -= 1
            return false
        }
        if (before.any(colorCondition)) {
            val letter = before.first(colorCondition)
            letter.color = Colors.WHITE
            counter = 0
        } else if (guess.any(colorCondition)) {
            val letter = guess.first(colorCondition)
            letter.color = Colors.WHITE
            counter = 0
        } else if (after.any(colorCondition)) {
            val letter = after.first(colorCondition)
            letter.color = Colors.WHITE
            counter = 0
        } else {
            counter = 0
            return true
        }
        return false
    }
}

val propositions = listOf(
    Proposition("Inscrire dans la Constitution\nla règle verte, qui impose de", "ne pas prendre plus à la nature", "que ce qu'elle peut reconstituer"),
    Proposition("Rendre le droit à l'eau\net à l'assainissement par", "la gratuité des mètres cubes", "indispensables à la vie digne"),
    Proposition("", "Créer 300 000 emplois agricoles", "pour instaurer une agriculture\nrelocalisée, diversifiée\net écologique"),
    Proposition("Porter immédiatement le", "SMIC mensuel à 1 500 euros net", "et accompagner les TPE/PME"),
    Proposition("Restaurer le droit à la", "retraite à 60 ans avec 40 annuités", "avec une attention particulière\npour les métiers pénibles"),
)

fun randomPropositions(): MutableList<Proposition> {
    val list = mutableListOf<Proposition>()
    while (list.size < 5) {
        val p = propositions.random()
        if (p !in list) list.add(p)
    }
    return list
}