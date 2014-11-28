/*
 * The MIT License
 *
 * Copyright 2014 Alexander Alexeev.
 *
 */

package org.mumidol.logicgames

/**
 * Created on 04.11.2014.
 */
public abstract class Game {
    inner class TurnDefender {
        val game = this@Game
    }
    public abstract class Turn<G : Game> (val defender: TurnDefender) {
        public val game: Game = defender.game
        public abstract fun move(): G
    }
//    inner abstract class Turn<G : Game> () {
//        val game = this@Game
//        abstract fun move(): G
//    }

//    abstract class Initial<G : Game> {
//        abstract val initial: G
//    }

    abstract val isOver: Boolean
}

public trait Player<G : Game> {
    fun turn(game: G): Game.Turn<G>
}

class IllegalTurnException(msg: String? = null) : Exception(msg)

public abstract class Puzzle : Game() {
    public abstract val isCompleted: Boolean
}

public abstract class MultiPlayerGame : Game() {
    public abstract val playersCount: Int
    public abstract val currPlayer: Int
    public abstract val winner: Int?
}

public abstract class SequentialGame() : MultiPlayerGame() {
    public fun prevPlayer(): Int = (playersCount + currPlayer - 1) % playersCount
    public fun nextPlayer(): Int = (currPlayer + 1) % playersCount
}

public fun play<G : Puzzle>(start: G, solver: Player<G>): G { //where class object G : Game.Initial<G> {
    var game = start

    while ( ! game.isOver) {
        val turn  = solver.turn(game)
        if (turn.game == game) {
            game = turn.move()
        } else {
            throw IllegalTurnException()
        }
    }
    return game;
}

public fun play<G : MultiPlayerGame>(start: G, vararg players: Player<G>): G {
    var game = start

    while ( ! game.isOver) {
        val turn = players[game.currPlayer].turn(game)
        if (turn.game == game) {
            game = turn.move()
        } else {
            throw IllegalTurnException()
        }
    }
    return game
}
