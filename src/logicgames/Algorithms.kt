/*
 * The MIT License
 *
 * Copyright 2014 Alexander Alexeev.
 *
 */

package org.mumidol.logicgames

import kotlin.properties.Delegates

fun evaluateWin<G: SequentialGame>(player: Int, node: G): Int {
    if (node.winner != null) {
        if (node.winner == player) {
            return Integer.MAX_VALUE
        } else {
            return Integer.MIN_VALUE
        }
    } else {
        return 0
    }
}

public fun _minimaxSearch<G: SequentialGame>
        (game: G, turnsGenerator: (G) -> Iterable<Game.Turn<G>>,
         evaluator: (G) -> Int, isOr: (G) -> Boolean, depth: Int): Pair<Int, Game.Turn<G>?> {
    if (depth == 0 || game.isOver) {
        return Pair(evaluator(game), null)
    }
    var bestEval = if (isOr(game)) { Integer.MIN_VALUE } else { Integer.MAX_VALUE }
    var bestTurn: Game.Turn<G>? = null
    val turns = turnsGenerator(game)
    for (turn in turns) {
        val node = turn.move()
        val (e, t)  = _minimaxSearch(node, turnsGenerator, evaluator, isOr, depth - 1)
        if (isOr(game) && e > bestEval || ! isOr(game) && e < bestEval) {
            bestEval = e
            bestTurn = turn
        }
        if (isOr(game) && bestEval == Integer.MAX_VALUE || ! isOr(game) && bestEval == Integer.MIN_VALUE) {
            break
        }
    }
    return Pair(bestEval, bestTurn)
}

/**
 * Created on 05.11.2014.
 */

public fun minimaxSearch<G: SequentialGame>
        (game: G, turnsGenerator: (G) -> Iterable<Game.Turn<G>>,
         evaluator: (G) -> Int = { evaluateWin(game.currPlayer, it) }, // simple win evaluation by default
         isOr: (G) -> Boolean = { (game.currPlayer == it.currPlayer) }, // assume root is or's node by default
         depth: Int = Integer.MAX_VALUE
        ): Game.Turn<G>? {
    if (game.isOver) {
        throw IllegalArgumentException()
    }

    return _minimaxSearch(game, turnsGenerator, evaluator, isOr, depth).second
}

public fun alphaBetaSearch<G : SequentialGame>
        (game: G, turnsGenerator: (G) -> Iterable<Game.Turn<G>>,
         evaluator: (G) -> Int = { evaluateWin(game.currPlayer, it) }, // simple win evaluation by default
         isOr: (G) -> Boolean = { (game.currPlayer == it.currPlayer) }, // assume root is or's node by default
        depth: Int = Integer.MAX_VALUE
        ): Game.Turn<G>? {

    class Node(val parent: Node?, val turn: Game.Turn<G>?, val state: G = turn!!.move()) {
        val depth: Int  by Delegates.lazy { (parent?.depth ?: -1) + 1 }

        val isAnd = ! isOr(state)

        private val rating = evaluator(state);

        {
            if (state.isOver || this.depth == depth) {
                evaluation = rating
                evaluated()
            }
        }

        private var evaluated = false
        private var evaluation: Int = if (isAnd) Integer.MAX_VALUE else Integer.MIN_VALUE

        var bestChild: Node? = null
        var evaluatedChildCount = 0
        var childCount = 0

        public fun rating(): Int = rating

        private fun evaluated() {
            evaluated = true
            parent?.childEvaluated(this)
        }

        public fun isEvaluated(): Boolean {
            if ( ! evaluated && parent != null && parent.evaluated) {
                evaluated = true
            }
            return evaluated
        }

        private fun childEvaluated(child: Node) {
            evaluatedChildCount++
            if ( ! isAnd && (child.evaluation > evaluation) ||
                    isAnd && (child.evaluation < evaluation)) {
                evaluation = child.evaluation
                bestChild = child
            }
            if ( ! isEvaluated()) {
                if ( ! isAnd && (evaluation == Integer.MAX_VALUE) ||
                        isAnd && (evaluation == Integer.MIN_VALUE) ||
                        (childCount == evaluatedChildCount)) {
                    evaluated()
                } else if (parent != null &&
                        (isAnd && evaluation <= parent.evaluation || ! isAnd && evaluation >= parent.evaluation)) {
                    evaluated() // alpha-beta cut
                }
            }
        }
    }

    if (game.isOver) {
        throw IllegalArgumentException()
    }

    var count = 0
    val root = Node(null, null, game)
    val queue = linkedListOf(root)
//    val processed = hashSetOf<Node>()
    while (queue.any() && ( ! root.isEvaluated())) {
        val node = queue.remove(0)
//        processed.add(node)

        if ( ! node.isEvaluated()) {
            val turns = turnsGenerator(node.state)
            node.childCount = turns.count()
            val q = linkedListOf<Node>()
            for (turn in turns) {
                if ( ! node.isEvaluated()) {
                    val child = Node(node, turn)
                    count++
                    if ( ! child.isEvaluated()) {
                        q.add(child)
                    }
                } else {
                    break
                }
            }
            q.sortBy { if (it.isAnd) it.rating() else -it.rating() }.forEach { queue.add(0, it) }
        }
    }

//    println(processed.size)
    println("Count: $count")

    if (root.isEvaluated() && root.bestChild != null) {
        return root.bestChild!!.turn
    } else {
        return null
    }
}

public trait IncrementalEvaluator<G : SequentialGame> {
    fun increment(game: G): IncrementalEvaluator<G>
    fun evaluate(): Int
}

private class DelegatingTurn<G : SequentialGame>(val turn: Game.Turn<G>, d: Game.TurnDefender) :
        Game.Turn<GameProxy<G>>(d) {
    override fun move(): GameProxy<G> {
        val g = turn.move()
        return GameProxy(g, (game as GameProxy<G>).evaluator.increment(g))//, game.generator.next(g))
    }
}

private class GameProxy<G : SequentialGame>(val game: G, val evaluator: IncrementalEvaluator<G>): //,
                                                    //val generator: TurnGenerator<G>) :
        SequentialGame() {
    override val playersCount: Int = game.playersCount
    override val isOver: Boolean = game.isOver
    override val winner: Int? = game.winner
    override val currPlayer: Int = game.currPlayer

//    inner class Turn(val turn: Game.Turn<G>) : Game.Turn<GameProxy<G>>(this) {
//        override fun move(): GameProxy<G> {
//            val game = turn.move()
//            return GameProxy(game, evaluator.calculate(game))
//        }
//    }

    fun turn(turn: Game.Turn<G>) = DelegatingTurn(turn, (this : Game).TurnDefender())
}

//fun delegateGeneration<P : SequentialGame>(turnsGenerator: (P) -> Iterable<Game.Turn<P>>):
//        (StateGame<P>) -> Iterable<Game.Turn<StateGame<P>>> = {sg -> turnsGenerator(sg.game).map { it -> sg.turn(it) }}

public fun minimaxSearch<G : SequentialGame>
        (game: G,
//         turnsGenerator: TurnGenerator<G>,
         turnsGenerator: (G) -> Iterable<Game.Turn<G>>,
         evaluator: IncrementalEvaluator<G>,
         isOr: (G) -> Boolean = { (game.currPlayer == it.currPlayer) },
         depth: Int = Integer.MAX_VALUE): Game.Turn<G>? {
    return (minimaxSearch(GameProxy(game, evaluator),// turnsGenerator),
            { sg -> turnsGenerator(sg.game).map { it -> sg.turn(it) } },
//            { sg -> sg.generator.generate().map { it -> sg.turn(it) } },
            { sg -> sg.evaluator.evaluate() },
            { sg -> isOr(sg.game)},
            depth) as DelegatingTurn?)?.turn ?: null
}

public fun alphaBetaSearch<G : SequentialGame>
        (game: G,
         turnsGenerator: (G) -> Iterable<Game.Turn<G>>,
//         turnsGenerator: TurnGenerator<G>,
         evaluator: IncrementalEvaluator<G>,
         isOr: (G) -> Boolean = { (game.currPlayer == it.currPlayer) },
         depth: Int = Integer.MAX_VALUE): Game.Turn<G>? {
    return (alphaBetaSearch(GameProxy(game, evaluator),// turnsGenerator),
            { sg -> turnsGenerator(sg.game).map { it -> sg.turn(it) } },
//            { sg -> sg.generator.generate().map { it -> sg.turn(it) } },
            { sg -> sg.evaluator.evaluate() },
            { sg -> isOr(sg.game)},
            depth) as DelegatingTurn?)?.turn ?: null
}

