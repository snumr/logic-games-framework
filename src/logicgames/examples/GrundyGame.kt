/*
 * The MIT License
 *
 * Copyright 2014 Alexander Alexeev.
 *
 */

package org.mumidol.logicgames.examples

import org.mumidol.logicgames.SequentialGame
import org.mumidol.logicgames.Game

class GrundyGame(val heaps: List<Int>, override val currPlayer: Int = 0) : SequentialGame() {
    override val playersCount = 2

    override val isOver: Boolean = false
    override val winner: Int? = null

    fun split(heap: Int, count: Int) = object : Game.Turn<GrundyGame>((this : Game).TurnDefender()) {
        override fun move(): GrundyGame {
            val newHeaps = heaps.toArrayList()
            val n = newHeaps.remove(heap)
            newHeaps.add(count)
            newHeaps.add(n - count)
            return GrundyGame(newHeaps.toList())
        }
    }
}