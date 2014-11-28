/*
 * The MIT License
 *
 * Copyright 2014 Alexander Alexeev.
 *
 */

package org.mumidol.logicgames.ui.examples

import javafx.application.Application
import javafx.stage.Stage
import javafx.scene.control.Button
import javafx.scene.layout.StackPane
import javafx.scene.Scene
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.shape.ArcType
import javafx.scene.input.MouseEvent
import javafx.concurrent.Service
import javafx.beans.property.StringProperty
import javafx.concurrent.Task
import javafx.beans.property.SimpleStringProperty
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javafx.concurrent.WorkerStateEvent
import kotlin.properties.Delegates
import org.mumidol.logicgames.Game
import org.mumidol.logicgames.Player
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.util.measureTimeMillis
import org.mumidol.logicgames.examples.Gomoku
import org.mumidol.logicgames.MultiPlayerGame
import org.mumidol.logicgames.IllegalTurnException
import org.mumidol.logicgames.examples.DummyGomokuPlayer
import org.mumidol.logicgames.ui.play
import org.mumidol.logicgames.examples.SmartGomokuPlayer
import org.mumidol.logicgames.ui.GameObserver
import org.mumidol.logicgames.ui.AsyncHumanPlayer

fun main(args: Array<String>) {
    Application.launch(javaClass<GomokuRunner>(), args.joinToString(separator = " "))
}

fun convertToGame(x: Int, y: Int): Pair<Int, Int> {
    return Pair(x - 9, 9 - y)
}
fun convertFromGame(x: Int, y: Int): Pair<Int, Int> {
    return Pair(x + 9, 9 - y)
}

class UIController(val runner: GomokuRunner) {
    val canvas: Canvas by Delegates.lazy { Canvas(405.0, 405.0) }
    val gc: GraphicsContext by Delegates.lazy { canvas.getGraphicsContext2D() }

    fun start(primaryStage: Stage) {
        val root = Group();

        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, { mouseClicked(it) });

        drawBoard()

        root.getChildren().add(canvas);
        primaryStage.setTitle("Gomoku");
        primaryStage.setScene(Scene(root));
        primaryStage.show();
    }

    fun mouseClicked(ev: MouseEvent) {
        if (ev.getClickCount() == 1) {
            runner.humanTurn((ev.getX().toLong() / 20).toInt(), (ev.getY().toLong() / 20).toInt())
        }
    }

    private fun drawBoard() {
        val size = 20.0
        gc.setStroke(Color.BLACK)
        gc.setLineWidth(2.0)
        for (i in 0..19) {
            gc.strokeLine(0.0, i * size, 19 * size, i * size)
        }
        for (i in 0..19) {
            gc.strokeLine(i * size, 0.0, i * size, 19 * size)
        }
    }

    fun drawCell(x: Int, y: Int, player: Int, color: Color) {
        val size = 20.0
        gc.clearRect(x * size + 3, y * size + 3, size - 6, size - 6)
        gc.setStroke(color)
        if (player == 0) {
            gc.strokeLine(x * size + 3, y * size + 3, (x + 1) * size - 3, (y + 1) * size - 3)
            gc.strokeLine(x * size + 3, (y + 1) * size - 3, (x + 1) * size - 3, y * size + 3)
        } else {
            gc.strokeOval(x * size + 3, y * size + 3, size - 6, size - 6);
        }
    }
}

/**
 * Created on 16.11.2014.
 */
class GomokuRunner() : Application() {

    val ui = UIController(this)

    val human = HumanGomokuPlayer()
    volatile var game: Gomoku? = null
//    var game = Gomoku().mark(0, 0).move()

    override fun start(primaryStage: Stage) {
        ui.start(primaryStage)

        play(Gomoku(), GomokuObserver(), DummyGomokuPlayer(), SmartGomokuPlayer())
    }

    inner class GomokuObserver : GameObserver<Gomoku> {
        public override fun started(game: Gomoku) {
        }

        public override fun turn(game: Gomoku) {
            drawLast(game)
        }

        public override fun gameOver(game: Gomoku) {
            drawLast(game)
            println("Winner is ${game.winner}")
        }

        private fun drawLast(game: Gomoku) {
            if (game.markedCells[game.currPlayer].any()) {
                val (x, y) = game.markedCells[game.currPlayer].first()
                val p = convertFromGame(x, y)
                ui.drawCell(p.first, p.second, game.currPlayer, Color.BLACK)
            }

            val (x, y) = game.markedCells[game.prevPlayer()].first()

            println("${if (game.prevPlayer() == 0) "X" else "0"}: $x $y")

            val p = convertFromGame(x, y)
            ui.drawCell(p.first, p.second, game.prevPlayer(), Color.BLUE)
        }
    }

    inner class HumanGomokuPlayer : AsyncHumanPlayer<Gomoku>() {
        override fun startWaiting(game: Gomoku) {
            this@GomokuRunner.game = game
        }
    }

    fun humanTurn(x: Int, y: Int) {
        if (game != null) {
            val (gx, gy) = convertToGame(x, y)
            if (gx <= 9 && gx >= -9 && gy <= 9 && gy >= -9) {
                val t = game!!.mark(gx, gy)
                game = null

                human.moved(t)
            }
        }
    }

    override fun stop() {
        super<Application>.stop()
        human.interrupt()
    }
}