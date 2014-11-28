/*
 * The MIT License
 *
 * Copyright 2014 Alexander Alexeev.
 *
 */

package org.mumidol.logicgames.test

import org.junit.Test as test
import org.junit.Assert
import org.mumidol.logicgames.examples.DummyPlayerImpl
import org.mumidol.logicgames.examples.DummyGame
import org.mumidol.logicgames.examples.DummyPlayer
import org.mumidol.logicgames.IllegalTurnException
import org.mumidol.logicgames.Game
import org.mumidol.logicgames.play
import org.mumidol.logicgames.alphaBetaSearch
import kotlin.util.measureTimeMillis
import org.mumidol.logicgames.minimaxSearch
import org.mumidol.logicgames.examples.TicTacToe
import org.mumidol.logicgames.examples.TicTacToePlayer
import org.mumidol.logicgames.examples.DummyTicTacToePlayer
import org.mumidol.logicgames.examples.SmartTicTacToePlayer
import org.mumidol.logicgames.examples.Gomoku
import org.mumidol.logicgames.examples.generateTicTacToeTurns
import org.mumidol.logicgames.examples.generateGomokuTurns
import org.mumidol.logicgames.examples.GomokuEvaluator
import org.mumidol.logicgames.examples.WIN_POSITION

/**
 * Created on 02.11.2014.
 */
class GameTest {
    test fun testTurn() {
        val player = DummyPlayerImpl()
        val game = DummyGame(2, 3, 2)
        val newState = player.turn(game).move()
        Assert.assertTrue(game.count + 1 == newState.count)
        Assert.assertTrue(game.playersCount == newState.playersCount)
        Assert.assertTrue(newState.currPlayer == 0)
    }

    test fun testGameOver() {
        val game = DummyGame(10)
        Assert.assertTrue(game.isOver)
    }

    test fun testPlay() {
        Assert.assertTrue(
                play(DummyGame(playersCount = 3), DummyPlayerImpl(), DummyPlayerImpl(), DummyPlayerImpl()).isOver)
    }

    test fun testCheatPlayer() {
        val cheatPlayer = object : DummyPlayer {
            override fun turn(game: DummyGame): Game.Turn<DummyGame> {
                class CrackGame : Game() {
                    override val isOver: Boolean = false
                    fun defender() = (this : Game).TurnDefender()
                }
                return object : Game.Turn<DummyGame>(CrackGame().defender()) {
                    override fun move(): DummyGame = DummyGame(10)
                }
            }
        }
        try {
            play(DummyGame(), DummyPlayerImpl(), cheatPlayer)
            Assert.assertFalse(true);
        } catch (e: IllegalTurnException) {
            Assert.assertTrue(true);
        }
    }

//    test fun testPartlyWrongPlay() {
//        val partlyWrongPlayer = object : DummyPlayer {
//            var tries = 0
//            override fun turn(game: DummyGame): DummyGame {
//                return if (++tries > 1) {
//                    tries = 0;
//                    DummyPlayerImpl().turn(game)
//                } else {
//                    game
//                }
//            }
//        }
//        var game = DummyGame()
//        val runner = MultiPlayerGameRunner(DummyJudge(), DummyPlayerImpl(), partlyWrongPlayer)
//        while ( ! game.isOver) {
//            try {
//                game = runner.play(game)
//            } catch (e: IllegalTurnException<DummyGame>){
//                game = e.game
//            }
//        }
//    }

    test fun testWinner() {
        Assert.assertTrue(play(DummyGame(7), DummyPlayerImpl(), DummyPlayerImpl()).winner == 0)
    }

    test fun testTicTacToeEndGame() {
        Assert.assertFalse(TicTacToe().isOver)
        Assert.assertFalse(TicTacToe(charArray(
                'X', '_', '_',
                '_', 'X', '_',
                '_', '_', '0')).isOver)
        Assert.assertTrue(TicTacToe(charArray(
                'X', 'X', '0',
                '0', 'X', 'X',
                'X', '0', '0')).isOver)
        Assert.assertTrue(TicTacToe(charArray(
                '_', 'X', '0',
                '0', 'X', 'X',
                '0', 'X', '_')).isOver)
        Assert.assertTrue(TicTacToe(charArray(
                '_', 'X', '0',
                '0', '0', 'X',
                '0', 'X', 'X')).isOver)
    }

    test fun testTicTacToeToString() {
        Assert.assertEquals(TicTacToe(charArray(
                '_', 'X', '0',
                '0', '0', 'X',
                '0', 'X', 'X')).toString(),
                "_X0\n00X\n0XX\n")
    }

    test fun testWrongPlay() {
        val wrongPlayer = object : TicTacToePlayer {
            override fun turn(game: TicTacToe) = game.mark(0, 0)
        }
        try {
            play(TicTacToe(), DummyTicTacToePlayer(), wrongPlayer)
            Assert.assertFalse(true);
        } catch (e: IllegalTurnException) {
            Assert.assertTrue(true);
        }
    }

    test fun testTicTacToePlay() {
        Assert.assertTrue(play(TicTacToe(), DummyTicTacToePlayer(), DummyTicTacToePlayer()).isOver)
        Assert.assertEquals(0, play(TicTacToe(), SmartTicTacToePlayer(), DummyTicTacToePlayer()).winner)
        val game = play(TicTacToe(), DummyTicTacToePlayer(), SmartTicTacToePlayer())
        print(game)
        Assert.assertEquals(1, game.winner)
    }

    test fun testSearch() {
        Assert.assertNotNull(minimaxSearch(TicTacToe(), { generateTicTacToeTurns(it) }))
        Assert.assertNotNull(alphaBetaSearch(TicTacToe(), { generateTicTacToeTurns(it) }))

        Assert.assertNotNull(alphaBetaSearch(TicTacToe(charArray(
                '_', '0', '_',
                '_', 'X', '_',
                '_', '_', '_')), { generateTicTacToeTurns(it) }))
    }

    test fun testGomokuIsOver() {
        Assert.assertFalse(Gomoku().isOver)
        Assert.assertFalse(Gomoku(array(Pair(0, 0)), array()).isOver)
        Assert.assertTrue(Gomoku(array(Pair(0, 0), Pair(-2, -2), Pair(-1, -1), Pair(1, 1), Pair(2, 2)),
                array(Pair(0, 1), Pair(1, -1), Pair(0, -1), Pair(-1, 0))).isOver)
    }

    test fun testGomokuTurnGenerator() {
        Assert.assertEquals(1, generateGomokuTurns(Gomoku()).count())
        Assert.assertEquals(24, generateGomokuTurns(Gomoku(array(Pair(0, 0)), array())).count())
        Assert.assertEquals(39, generateGomokuTurns(Gomoku(array(Pair(0, 0)), array(Pair(-2, -2)))).count())
    }

    test fun testGomokuEvaluator() {
        Assert.assertEquals(0, GomokuEvaluator(Gomoku(array(Pair(0,0)), array())).evaluate())
        Assert.assertEquals(0, GomokuEvaluator(Gomoku(array(Pair(0,0)), array(Pair(0, 1)))).evaluate())
        Assert.assertEquals(WIN_POSITION / 2 + 3, GomokuEvaluator(Gomoku(array(Pair(0, 0), Pair(-1, 1), Pair(1, -1)),
                array(Pair(2, 1), Pair(1, 2))), 0).evaluate())
        //         0
        // 0X_XXX_X0
        //         0
        Assert.assertEquals(WIN_POSITION + WIN_POSITION / 2, GomokuEvaluator(Gomoku(
                array(Pair(0, 0), Pair(-1, 0), Pair(-3, 0), Pair(3, 0), Pair(1, 0)),
                array(Pair(-4, 0), Pair(4, 0), Pair(4, 1), Pair(4, -1))), 0).evaluate())
        // 0X0
        // XXX
        // _X00
        Assert.assertEquals(WIN_POSITION, GomokuEvaluator(Gomoku(
                array(Pair(0, 0), Pair(1, 1), Pair(2, 0), Pair(1, -1), Pair(1, 0)),
                array(Pair(0, 1), Pair(2, 1), Pair(2, -1), Pair(3, -1))), 0).evaluate())
        Assert.assertEquals(-WIN_POSITION / 2 + 4, GomokuEvaluator(Gomoku(
                array(Pair(0, 0), Pair(2, -2), Pair(1, 0), Pair(-2, 0), Pair(4, -3), Pair(4, -2)),
                array(Pair(1, 1), Pair(1, -1), Pair(2, 0), Pair(-1, 0), Pair(3, -1), Pair(0, 2))), 0).evaluate())
        //   0_0_0
        // _XX_X_X
        Assert.assertEquals(WIN_POSITION / 2, GomokuEvaluator(Gomoku(
                array(Pair(0, 0), Pair(-1, 0), Pair(4, 0), Pair(2, 0)),
                array(Pair(0, 1), Pair(2, 1), Pair(4, 1))), 0).evaluate())
        Assert.assertEquals(WIN_POSITION / 2, GomokuEvaluator(Gomoku(
                array(Pair(0, 0), Pair(1, -1), Pair(0, -2)),
                array(Pair(-1, -1), Pair(-1, 1), Pair(-1, 0))), 1).evaluate())
        Assert.assertEquals(WIN_POSITION / 2 + 2, GomokuEvaluator(Gomoku(
                array(Pair(0, 0), Pair(1, -1), Pair(0, -2), Pair(-3, -3), Pair(-4, -4)),
                array(Pair(-1, -1), Pair(-1, 1), Pair(-1, 0), Pair(2, 0), Pair(-1, 3))), 1).evaluate())
//        Assert.assertEquals(3, GomokuEvaluator(
//                Gomoku(array(Pair(0,0), Pair(1, 0), Pair(0, 1)), array(Pair(-1, 0), Pair(-3, 0), Pair(-5, 0)))).evaluate())
//        Assert.assertEquals(-1, GomokuEvaluator(
//                Gomoku(array(Pair(0,0), Pair(-2, 1), Pair(-1, 0)), array(Pair(2, 1), Pair(2, 0), Pair(3, 1)))).evaluate())
//        val g = Gomoku(array(Pair(0,0), Pair(-2, 1), Pair(-1, 0)), array(Pair(2, 1), Pair(2, 0)))
//        val ev = GomokuEvaluator(g, 0)
//        Assert.assertEquals(-1, ev.increment(g.mark(3, 1).move()).evaluate())

        var game = Gomoku()
        var eval = GomokuEvaluator(game)
        val turns = listOf(0 to 0, 2 to -1, 2 to -3, 1 to -2, -1 to -4, 1 to -1)
        for ((x, y) in turns) {
            game = game.mark(x, y).move()
            eval = eval.increment(game)
            Assert.assertEquals(GomokuEvaluator(game, 0).evaluate(), eval.evaluate())
        }
    }

    test fun testGomoku() {
//        Assert.assertEquals(0, play(Gomoku(), SmartGomokuPlayer(), DummyGomokuPlayer()).winner)
        // X0X0
        // _X_X
        // ___00
        val game = Gomoku(
                array(Pair(0, 0), Pair(1, 1), Pair(2, 0), Pair(-1, 1)),
                array(Pair(0, 1), Pair(2, 1), Pair(2, -1), Pair(3, -1)))
        val turn = (alphaBetaSearch(game, { generateGomokuTurns(it) }, GomokuEvaluator(game), depth = 5) as Gomoku.GomokuTurn)
        Assert.assertEquals(1, turn.x)
        Assert.assertTrue(turn.y == -1 || turn.y == 0)
        // ___X
        // 0___
        // _0_X
        // __0X
        val game2 = Gomoku(
                array(Pair(0, 0), Pair(0, 2), Pair(0, -1)),
                array(Pair(-3, 1), Pair(-1, -1), Pair(-2, 0)))
        val turn2 = (alphaBetaSearch(game2, { generateGomokuTurns(it) }, GomokuEvaluator(game2), depth = 5) as Gomoku.GomokuTurn)
        Assert.assertEquals(0, turn2.x)
        Assert.assertEquals(1, turn2.y)
        val game3 = Gomoku(
                array(Pair(0, 0), Pair(-1, 2), Pair(3, -2), Pair(3, 2), Pair(-1, 3)),
                array(Pair(2, 1), Pair(4, -1), Pair(2, -1), Pair(3, 0)))
        val turn3 = (alphaBetaSearch(game3, { generateGomokuTurns(it) }, GomokuEvaluator(game3), depth = 5) as Gomoku.GomokuTurn)
        Assert.assertEquals(1, turn3.x)
        Assert.assertEquals(2, turn3.y)
    }
}
