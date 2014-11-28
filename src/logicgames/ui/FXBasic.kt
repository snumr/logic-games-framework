/*
 * The MIT License
 *
 * Copyright 2014 Alexander Alexeev.
 *
 */

package org.mumidol.logicgames.ui

import javafx.application.Application
import java.util.concurrent.Executors
import org.mumidol.logicgames.MultiPlayerGame
import org.mumidol.logicgames.Player
import org.mumidol.logicgames.IllegalTurnException
import javafx.stage.Stage
import org.mumidol.logicgames.Game
import javafx.concurrent.Task
import kotlin.util.measureTimeMillis
import java.util.concurrent.locks.ReentrantLock
import kotlin.properties.Delegates

/**
 * Created on 27.11.2014.
 */
class PlayerTurnTask<G : MultiPlayerGame>(val game: G, val player: Player<G>, val onSuccess: (Game.Turn<G>) -> Unit):
        Task<Game.Turn<G>>() {
    override fun call(): Game.Turn<G>? {
        var t: Game.Turn<G>? = null
        val time = measureTimeMillis { t = player.turn(game) }
        println("Time: $time")
        return t
    }

    override fun failed() {
        super<Task>.failed()
        println("Failure :-(. ${ getException()?.toString() ?: ""}")
        getException()?.printStackTrace()
    }

    override fun succeeded() {
        super<Task>.succeeded()
        onSuccess(get())
    }
}

trait GameObserver<G : MultiPlayerGame> {
    public fun started(game: G)
    public fun turn(game: G)
    public fun gameOver(game: G)
}

public fun play<G : MultiPlayerGame>(start: G, o: GameObserver<G>, vararg players: Player<G>) {
    //        var game = start
//    val pool = Executors.newFixedThreadPool(1)

    fun submit(game: G) {
        Thread(PlayerTurnTask(game, players[game.currPlayer], { turn ->
            if (turn.game == game) {
                val g = turn.move()
                if (g.isOver) {
//                    pool.shutdown()
                    o.gameOver(g)
                } else {
                    o.turn(g)
                    submit(turn.move())
                }
            } else {
                throw IllegalTurnException()
            }
        })).start()
    }

    o.started(start)

    submit(start)
}

public abstract class AsyncHumanPlayer<G : MultiPlayerGame> : Player<G> {
    val lock = ReentrantLock()
    val waiting = lock.newCondition()
    var turn: Game.Turn<G> by Delegates.notNull()

    public override fun turn(game: G): Game.Turn<G> {
        lock.lock()
        try {
            startWaiting(game)

            waiting.await()

            return turn
        } finally {
            lock.unlock()
        }
    }

    public fun moved(turn: Game.Turn<G>) {
        lock.lock()
        try {
            this.turn = turn
            waiting.signal()
        } finally {
            lock.unlock()
        }
    }

    public fun interrupt() {
        lock.lock()
        try {
            waiting.signal()
        } finally {
            lock.unlock()
        }
    }

    public abstract fun startWaiting(game: G)
}
