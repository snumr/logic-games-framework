/*
 * The MIT License
 *
 * Copyright 2014 Alexander Alexeev.
 *
 */

/**
 * Created on 06.11.2014.
 */
package org.mumidol.logicgames.examples

import org.mumidol.logicgames.SequentialGame
import org.mumidol.logicgames.Game
import org.mumidol.logicgames.IllegalTurnException
import kotlin.properties.Delegates
import org.mumidol.logicgames.Player
import java.util.Random
import org.mumidol.logicgames.alphaBetaSearch
import org.mumidol.logicgames.IncrementalEvaluator
import java.util.LinkedList
import java.util.Scanner
import org.mumidol.logicgames.minimaxSearch
import java.util.HashMap

data public class Cell(val x: Int, val y: Int) {
    override public fun toString(): String = "($x, $y)"
}

val NORTH = Pair(0, 1)
val SOUTH = Pair(0, -1)
val EAST = Pair(1, 0)
val WEST = Pair(-1, 0)
val NORTH_EAST = Pair(1, 1)
val SOUTH_EAST = Pair(1, -1)
val NORTH_WEST = Pair(-1, 1)
val SOUTH_WEST = Pair(-1, -1)

public fun Gomoku(): Gomoku = Gomoku(streamOf(), streamOf())

public fun Gomoku(x: Array<Pair<Int, Int>>, o: Array<Pair<Int, Int>>): Gomoku {
    val itx = x.iterator()
    val ito = o.iterator()
    var g: Gomoku = Gomoku()
    while (itx.hasNext() && ito.hasNext()) {
        val _x = itx.next()
        val _o = ito.next()
        g = g.mark(_x.first, _x.second).move()
        g = g.mark(_o.first, _o.second).move()
    }
    if (itx.hasNext()) {
        val _x = itx.next()
        g = g.mark(_x.first, _x.second).move()
    }
    return g
}

public class Gomoku (val xCells: Stream<Cell>, val oCells: Stream<Cell>) : SequentialGame() {
    override val playersCount: Int = 2

    override val currPlayer: Int = xCells.count() - oCells.count()

    val markedCells = array(xCells, oCells)

    private class object {
        val initial_lengths = mapOf(NORTH to 0, SOUTH to 0, EAST to 0, WEST to 0,
                NORTH_EAST to 0, SOUTH_EAST to 0, NORTH_WEST to 0, SOUTH_WEST to 0)
    }

    private val isWon: Boolean
    {
        if (markedCells[prevPlayer()].any()) {
            val last = markedCells[prevPlayer()].first()
            val cells = markedCells[prevPlayer()].drop(1).filter{ onRange(last, it, 4) && onLane(last, it) }.
                    toSortedListBy { last.range(it) }
            val lengths = HashMap(initial_lengths)
            for (cell in cells) {
                val direction = last.direction(cell)
                val r = last.range(cell)
                if (lengths[direction] == r - 1) {
                    lengths[direction]++
                }
            }
            isWon = (lengths[NORTH] + lengths[SOUTH] >= 4) ||
                    (lengths[EAST] + lengths[WEST] >= 4) ||
                    (lengths[NORTH_EAST] + lengths[SOUTH_WEST] >= 4) ||
                    (lengths[NORTH_WEST] + lengths[SOUTH_EAST] >= 4)
        } else {
            isWon = false
        }
    }

    override val winner: Int? =
        if (isWon) {
            prevPlayer()
        } else {
            null
        }

    override val isOver: Boolean = isWon

    fun mark(x: Int, y: Int): GomokuTurn =
        if (isOver) {
            throw IllegalTurnException("Game is over!")
        } else if ( ! xCells.any()) {
            if ((x != 0) || (y != 0)) {
                throw IllegalTurnException()
            } else {
                GomokuTurn(0, 0)
            }
        } else if ( ! xCells.contains(Pair(x, y)) && ! oCells.contains(Pair(x, y))) {
            GomokuTurn(x, y)
        } else {
            throw IllegalTurnException("Wrong move: $x $y")
        }

    override fun toString(): String {
        val xPlayer = xCells.joinToString()
        val oPlayer = oCells.joinToString()
        return "X: [$xPlayer] 0: [$oPlayer]"
    }

    inner class GomokuTurn(val x: Int, val y: Int) : Game.Turn<Gomoku>((this : Game).TurnDefender()) {
        override fun move() =
            if (currPlayer == 0) {
                Gomoku(streamOf(Cell(x, y)).plus(xCells), oCells)
            } else {
                Gomoku(xCells, streamOf(Cell(x, y)).plus(oCells))
            }
    }
}

public fun Int.abs() : Int = if (this >= 0) this else -this
public fun Int.sign(): Int = if (this > 0) 1 else if (this < 0) -1 else 0
public fun Cell.direction(cell: Cell): Pair<Int, Int> = Pair((cell.x - this.x).sign(), (cell.y - this.y).sign())

public fun Cell.range(cell: Cell): Int =
        Integer.max((cell.x - this.x).abs(), (cell.y - this.y).abs())

public fun onRange(root: Cell, cell: Cell, range: Int): Boolean =
        (root.range(cell) <= range)

public fun onLane(root: Cell, cell: Cell): Boolean =
        ((cell.x - root.x).abs() == (cell.y - root.y).abs()) ||
                ((cell.x - root.x) == 0) || (cell.y - root.y == 0)

private fun getAllCellsInRangeTwo(cell: Cell): Set<Cell> {
    val set = hashSetOf<Cell>()
    for (i in -2..2) {
        for (j in -2..2) {
            if (i != 0 || j != 0) {
                set.add(Cell(cell.x + i, cell.y + j))
            }
        }
    }
    return set
}

private fun getAvailableCells(game: Gomoku): Set<Cell> {
    val available = hashSetOf<Cell>()
    if (game.xCells.any()) {
        for (i in 0..1) {
            game.markedCells[i].forEach{ available.addAll(getAllCellsInRangeTwo(it)) }
        }
        available.removeAll(game.xCells)
        available.removeAll(game.oCells)
    } else {
        available.add(Cell(0, 0))
    }
    return available
}

public fun generateGomokuTurns(game: Gomoku): Iterable<Gomoku.GomokuTurn> {
    return getAvailableCells(game).map{ game.mark(it.x, it.y) }
}

public class DummyGomokuPlayer : Player<Gomoku> {
    override fun turn(game: Gomoku): Game.Turn<Gomoku> {
        val it = generateGomokuTurns(game)
        return it.elementAt(Random().nextInt(it.count()))
    }
}

class ConsoleHumanGomokuPlayer : Player<Gomoku> {
    override fun turn(game: Gomoku): Game.Turn<Gomoku> {
        println(game)

        val scanner = Scanner(readLine())
        val x = scanner.nextInt()
        val y = scanner.nextInt()
        return game.mark(x, y)
    }
}