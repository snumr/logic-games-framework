/*
 * The MIT License
 *
 * Copyright 2014 Alexander Alexeev.
 *
 */

package org.mumidol.logicgames.examples

import org.mumidol.logicgames.MultiPlayerGame
import org.mumidol.logicgames.Game
import org.mumidol.logicgames.IllegalTurnException
import org.mumidol.logicgames.Player
import org.mumidol.logicgames.SequentialGame
import org.mumidol.logicgames.alphaBetaSearch
import org.mumidol.logicgames.examples.TicTacToe.TicTacToeTurn

/**
 * Created on 03.11.2014.
 */
class TicTacToe(_field: CharArray = charArray(
        '_', '_', '_',
        '_', '_', '_',
        '_', '_', '_')) : SequentialGame() {
    override val playersCount = 2
    val E = '_'

//    class object : Game.Initial<TicTacToe>() {
//        override val initial: TicTacToe = TicTacToe()
//    }

    val field = String(_field)
    val turnCount: Int
    {
        turnCount = field.fold(0, {(n, c) -> if (c != E) n + 1 else n})
    }

    override val currPlayer = turnCount % 2

    private val isWon =
        (field[0] == field[1]) && (field[1] == field[2]) && (field[0] != E) ||
        (field[1 * 3] == field[1 * 3 + 1]) && (field[1 * 3 +1] == field[1 * 3 +2]) && (field[1 * 3] != E) ||
        (field[2 * 3] == field[2 * 3 + 1]) && (field[2 * 3 +1] == field[2 * 3 +2]) && (field[2 * 3] != E) ||
        (field[0] == field[1 * 3]) && (field[1 * 3] == field[2 * 3]) && (field[0] != E) ||
        (field[1] == field[1 * 3 + 1]) && (field[1 * 3 + 1] == field[2 * 3 + 1]) && (field[1] != E) ||
        (field[2] == field[1 * 3 + 2]) && (field[1 * 3 + 2] == field[2 * 3 + 2]) && (field[2] != E) ||
        (field[0] == field[1 * 3 + 1]) && (field[1 * 3 + 1] == field[2 * 3 + 2]) && (field[0] != E) ||
        (field[2] == field[1 * 3 + 1]) && (field[1 * 3 + 1] == field[2 * 3]) && (field[2] != E)

    override val isOver =
            (turnCount == 9) || isWon

    override val winner: Int? =
        if (isWon) {
            prevPlayer()
        } else {
            null
        }

    override fun toString(): String {
        val sb = StringBuilder()
        for (i in 0..2) {
            for (j in 0..2) {
                sb.append(field[i * 3 + j])
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    fun mark(x: Int, y: Int) =
        if (x > 3 || y > 3) {
            throw IllegalTurnException()
        } else if (field[x * 3 + y] == E) {
            TicTacToeTurn(x * 3 + y)
        } else {
            throw IllegalTurnException()
        }

    inner class TicTacToeTurn(val i: Int) : Game.Turn<TicTacToe>((this : Game).TurnDefender()) {
        override fun move(): TicTacToe {
            val newField = field.toCharArray()
            newField[i] = if (currPlayer == 0) 'X' else '0'
            return TicTacToe(newField)
        }
    }
}

trait TicTacToePlayer : Player<TicTacToe>

class DummyTicTacToePlayer : TicTacToePlayer {
    override fun turn(game: TicTacToe): TicTacToeTurn {
        val i = game.field.indexOf(game.E)
        return game.mark(i / 3, i % 3)
    }
}

fun generateTicTacToeTurns(node: TicTacToe) =
    object : Iterable<TicTacToeTurn> {
        override fun iterator() = object : Iterator<TicTacToeTurn> {
            var i = -1;

            override fun next(): TicTacToeTurn {
                i = node.field.indexOf(node.E, i + 1)
                return node.mark(i / 3, i % 3)
            }

            override fun hasNext() = node.field.indexOf(node.E, i + 1) != -1
        }
    }

class SmartTicTacToePlayer: TicTacToePlayer {
    override fun turn(game: TicTacToe) =
        alphaBetaSearch(game,
                { node -> generateTicTacToeTurns(node) }
        )!!
}
