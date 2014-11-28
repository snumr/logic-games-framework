/*
 * The MIT License
 *
 * Copyright 2014 Alexander Alexeev.
 *
 */

package org.mumidol.logicgames.examples

import org.mumidol.logicgames.SequentialGame
import org.mumidol.logicgames.Game
import org.mumidol.logicgames.Player

/**
 * Created on 02.11.2014.
 */
class DummyGame(val count: Int = 0, override val playersCount: Int = 2, override val currPlayer: Int = 0) :
        SequentialGame() {
    //    class object : Game.Initial<DummyGame>() {
    //        override val initial = DummyGame()
    //    }
    override val isOver = (count == 10)

    override val winner: Int?
        get() {
            return if (isOver) {
                prevPlayer()
            } else {
                null
            }
        }

    fun increase() = DummyGameTurn()

    inner class DummyGameTurn : Game.Turn<DummyGame>((this : Game).TurnDefender()) {
        override fun move() =
            DummyGame(count + 1, playersCount, nextPlayer())
    }
}

trait DummyPlayer : Player<DummyGame>

class DummyPlayerImpl() : DummyPlayer {
    override fun turn(game: DummyGame) = //{ (t: DummyGameTurn) -> t.increase()}
            game.increase()
}