/*
 * The MIT License
 *
 * Copyright 2014 Alexander Alexeev.
 *
 */

package thirdparty.gomoku

import org.mumidol.logicgames.examples.Cell
import org.mumidol.logicgames.examples.range
import org.mumidol.logicgames.examples.onLane
import org.mumidol.logicgames.examples.direction
import org.mumidol.logicgames.examples.Gomoku
import org.mumidol.logicgames.IncrementalEvaluator
import org.mumidol.logicgames.examples.NORTH
import org.mumidol.logicgames.examples.SOUTH
import org.mumidol.logicgames.examples.EAST
import org.mumidol.logicgames.examples.WEST
import org.mumidol.logicgames.examples.NORTH_EAST
import org.mumidol.logicgames.examples.SOUTH_EAST
import org.mumidol.logicgames.examples.NORTH_WEST
import org.mumidol.logicgames.examples.SOUTH_WEST
import java.util.LinkedList
import org.mumidol.logicgames.examples.onRange
import kotlin.properties.Delegates
import org.mumidol.logicgames.Player
import org.mumidol.logicgames.Game
import org.mumidol.logicgames.alphaBetaSearch
import org.mumidol.logicgames.examples.generateGomokuTurns
import thirdparty.gomoku.GomokuEvaluator.FeaturedChains
import java.util.Random


/**
 * Created on 26.11.2014.
 */
public class SmartGomokuPlayer : Player<Gomoku> {
    override fun turn(game: Gomoku): Game.Turn<Gomoku> {
        return alphaBetaSearch(game, { generateGomokuTurns(it) }, GomokuEvaluator(game), depth = 5) ?:
                generateGomokuTurns(game).let { it.elementAt(Random().nextInt(it.count())) }
//        return alphaBetaSearch(game, { generateTurns(it) }, { evaluate(it, game.currPlayer)}, depth = 6) ?: generateTurns(game).first();
    }
}

private fun Cell.isAdjacent(cell: Cell) = (range(cell) == 1)

private fun Cell.cellOnRange(direction: Pair<Int, Int>, range: Int): Cell =
        Cell(this.x + direction.first * range, this.y + direction.second * range)
private fun Cell.next(direction: Pair<Int, Int>): Cell = cellOnRange(direction, 1)

private fun Cell.containsIn(begin: Cell, end: Cell): Boolean =
        (this == begin) || (this == end) || onLane(begin, this) && (begin.direction(this) == this.direction(end))

val WIN_POSITION = 1 shl 16

fun GomokuEvaluator(game: Gomoku, player: Int = game.currPlayer): GomokuEvaluator {
    return _GomokuEvaluator(player, game.currPlayer,
            game.markedCells[game.currPlayer], game.markedCells[game.prevPlayer()])
}

fun _GomokuEvaluator(player: Int, currPlayer: Int,
                     currMoves: Stream<Cell>, lastMoves: Stream<Cell>): GomokuEvaluator {
    if (lastMoves.any()) {
        val e = _GomokuEvaluator(player, if (currPlayer == 0) 1 else 0, lastMoves.toList().drop(1).stream(), currMoves)
        return e.increment(currPlayer, currMoves, lastMoves)
    } else {
        return GomokuEvaluator(player, 0, 0, FeaturedChains(), FeaturedChains())
    }
}

class GomokuEvaluator (val player: Int, val evaluation: Int, val score: Int,
                       val xFeatureChains: FeaturedChains, val oFeatureChains: FeaturedChains) :
        IncrementalEvaluator<Gomoku> {

    private class object {
        val index = mapOf(NORTH to 0, SOUTH to 0, EAST to 1, WEST to 1,
                NORTH_EAST to 2, SOUTH_EAST to 3, NORTH_WEST to 3, SOUTH_WEST to 2)
    }

    class RawChain(val root: Cell, val direction: Pair<Int, Int>) {
        var beginTerminated = false
        var endTerminated = false

        var bTerminatedLength = 0
        var eTerminatedLength = 0

        var bsl = 0
        var esl = 0
        var btl = 0
        var etl = 0

        var count = 1

        private fun getCell(r: Int): Cell = root.cellOnRange(direction, r)

        public fun add(cell: Cell) {
            val direction = root.direction(cell)
            val r = root.range(cell)
            if (direction == this.direction) {
                if ( ! endTerminated || r < eTerminatedLength) {
                    if (etl == 0) {
                        if (r > esl + 1) {
                            if (r > esl + 2) {
                                etl = esl
                            } else {
                                etl = r
                            }
                        } else {
                            esl++
                        }
                    } else {
                        if (r <= etl + 1) {
                            etl++
                        }
                    }
                    count++
                }
            } else {
                if ( ! beginTerminated || r < bTerminatedLength) {
                    if (btl == 0) {
                        if (r > bsl + 1) {
                            if (r > bsl + 2) {
                                btl = bsl
                            } else {
                                btl = r
                            }
                        } else {
                            bsl++
                        }
                    } else {
                        if (r <= btl + 1) {
                            btl++
                        }
                    }
                    count++
                }
            }
        }

        public fun terminate(cell: Cell) {
            val direction = root.direction(cell)
            if (direction == this.direction) {
                if ( ! endTerminated) {
                    endTerminated = true
                    eTerminatedLength = root.range(cell)
                }
            } else {
                if ( ! beginTerminated) {
                    beginTerminated = true
                    bTerminatedLength = root.range(cell)
                }
            }
        }

        public fun fullLength(): Int {
            return (if (beginTerminated) (bTerminatedLength - 1) else 4) +
                    (if (endTerminated) (eTerminatedLength  - 1) else 4)
        }

        public fun marked(): Int {
            return count
        }

        public fun solidLength(): Int = bsl + esl + 1

        public fun extractChains(): List<Chain> {
            val bl: Int
            val el: Int

            if (btl == 0) btl = bsl
            if (etl == 0) etl = esl

            val list = linkedListOf<Chain>()

            if (bsl != btl && esl != etl) {
                extractChain(bsl, etl, false)?.let{ list.add(it) }
                extractChain(btl, esl, false)?.let{ list.add(it) }
            } else {
                if (bsl + etl > esl + btl) {
                    bl = bsl
                    el = etl
                } else {
                    bl = btl
                    el = esl
                }
                extractChain(bl, el, bsl == btl && esl == etl)?.let{ list.add(it) }
            }
            return list
        }

        private fun extractChain(bl: Int, el: Int, solid: Boolean): Chain? {
            val c = if (solid) bsl + esl + 1 else bl + el

            val bfl = if (beginTerminated) bTerminatedLength - 1 else 4
            val efl = if (endTerminated) eTerminatedLength - 1 else 4

            if (c >= 4) {
                if (solid) {
                    if (efl - el > 0 && bfl - bl > 0) {
                        return ChainOfOpenFour(getCell(-bl - 1), getCell(el + 1))
                    } else if (efl - el > 0) {
                        return ChainOfClosedFour(getCell(-bl), getCell(el + 1))
                    } else if (bfl - bl > 0) {
                        return ChainOfClosedFour(getCell(-bl - 1), getCell(el))
                    } else {
                        return null
                    }
                } else {
                    return ChainOfClosedFour(getCell(-bl), getCell(el))
                }
            } else if (c == 3) {
                if (solid) {
                    if (efl - el > 1 && bfl - bl > 1) {
                        return ChainOfThree(getCell(-bl - 2), getCell(el + 2))
                    } else if (efl - el > 1 && bfl - bl > 0) {
                        return ChainOfThree(getCell(-bl - 1), getCell(el + 2))
                    } else if (efl -el > 0 && bfl - bl > 1) {
                        return ChainOfThree(getCell(-bl - 2), getCell(el + 1))
                    } else {
                        return null
                    }
                } else {
                    if (efl - el > 0 && bfl - bl > 0) {
                        return ChainOfThree(getCell(-bl - 1), getCell(el + 1))
                    } else {
                        return null
                    }
                }
            } else {
                return null
            }
        }
    }

    fun buildRawChains(last: Cell, lastCells: List<Cell>, currCells: List<Cell>): Array<RawChain> {
        //        val map = hashMapOf(NORTH to RawChain(last, NORTH),
        //                EAST to RawChain(last, EAST),
        //                NORTH_EAST to RawChain(last, NORTH_EAST),
        //                SOUTH_EAST to RawChain(last, SOUTH_EAST))
        val arr = array(RawChain(last, NORTH), RawChain(last, EAST), RawChain(last, NORTH_EAST), RawChain(last, SOUTH_EAST))

        for (cell in currCells) {
            val direction = last.direction(cell)
            arr[index[direction]!!].terminate(cell)
        }
        for (cell in lastCells) {
            val direction = last.direction(cell)
            arr[index[direction]!!].add(cell)
        }
        return arr
    }

    abstract class Chain(val begin: Cell, val end: Cell) {
        fun contains(cell: Cell): Boolean = cell.containsIn(begin, end)

        fun length() = begin.range(end) + 1

        abstract fun isBrokenBy(cell: Cell): Boolean
        abstract fun broke(cell: Cell): Chain?
        abstract fun isImprovedBy(cell: Cell): Boolean
        abstract fun improve(cell: Cell): Chain
    }

    class ChainOfThree(begin: Cell, end: Cell) : Chain(begin, end) {
        override fun isImprovedBy(cell: Cell) = contains(cell)

        override fun improve(cell: Cell): Chain {
            if (cell == begin) {
                return ChainOfClosedFour(begin, begin.cellOnRange(begin.direction(end), 4))
            } else if (cell == end) {
                return ChainOfClosedFour(end.cellOnRange(end.direction(begin), 4), end)
            } else {
                if (length() == 6) {
                    return ChainOfOpenFour(begin, end)
                } else {
                    if (cell.isAdjacent(begin)) {
                        return ChainOfOpenFour(begin, end.next(end.direction(begin)))
                    } else {
                        return ChainOfOpenFour(begin.next(begin.direction(end)), end)
                    }
                }
            }
        }

        override fun broke(cell: Cell): Chain? {
            if (length() == 7) {
                if (cell == begin) {
                    return ChainOfThree(begin.next(begin.direction(end)), end)
                }
                if (cell == end) {
                    return ChainOfThree(begin, end.next(end.direction(begin)))
                }
            }
            return null
        }

        override fun isBrokenBy(cell: Cell): Boolean {
            return contains(cell)
        }
    }

    abstract class ChainOfFour(begin: Cell, end: Cell) : Chain(begin, end) {
        override fun isImprovedBy(cell: Cell): Boolean = contains(cell)
        override fun improve(cell: Cell): Chain {
            assert(false)
            return this
        }
        override fun isBrokenBy(cell: Cell) = contains(cell)
    }

    class ChainOfOpenFour(begin: Cell, end: Cell) : ChainOfFour(begin, end) {
        override fun broke(cell: Cell): Chain? {
            if (cell == begin) {
                return ChainOfClosedFour(cell, end)
            } else {
                return ChainOfClosedFour(begin, cell)
            }
        }
    }

    class ChainOfClosedFour(begin: Cell, end: Cell) : ChainOfFour(begin, end) {
        override fun broke(cell: Cell): Chain? = null
    }

    class FeaturedChains(val chains: List<Chain> = listOf()) {
        val openFourCount: Int by Delegates.lazy { chains.count { it is ChainOfOpenFour } }
        val closedFourCount: Int by Delegates.lazy { chains.count { it is ChainOfClosedFour } }
        val wideOpenThreeCount: Int by Delegates.lazy { chains.count { it is ChainOfThree } }

        fun isBrokenBy(cell: Cell) = chains.any { it.isBrokenBy(cell) }
        fun isImprovedBy(cell: Cell) = chains.any { it.isImprovedBy(cell) }

        fun add(chs: List<Chain>): FeaturedChains {
            if (chs.any()) {
                val chains = this.chains.toLinkedList()
                chains.addAll(chs)
                return FeaturedChains(chains)
            } else {
                return this
            }
        }

        fun broke(cell: Cell): FeaturedChains {
            if (isBrokenBy(cell)) {
                val chains = linkedListOf<Chain>()
                for (chain in this.chains) {
                    if (chain.isBrokenBy(cell)) {
                        chain.broke(cell)?.let { chains.add(it) }
                    } else {
                        chains.add(chain)
                    }
                }
                return FeaturedChains(chains)
            } else {
                return this
            }
        }

        fun improve(cell: Cell): FeaturedChains {
            if (isImprovedBy(cell)) {
                val chains = linkedListOf<Chain>()
                for (chain in this.chains) {
                    if (chain.isImprovedBy(cell)) {
                        chains.add(chain.improve(cell))
                    } else {
                        chains.add(chain)
                    }
                }
                return FeaturedChains(chains)
            } else {
                return this
            }
        }

        fun anyOpenFour() = openFourCount > 0
        fun anyClosedFour() = closedFourCount > 0
        fun anyWideOpenThree() = wideOpenThreeCount > 0
    }

    fun increment(currPlayer: Int, currMoves: Stream<Cell>, lastMoves: Stream<Cell>): GomokuEvaluator {
        var value = 0
        var score = this.score
        val lastPlayer = if (currPlayer == 0) 1 else 0

        val last = lastMoves.first()
        var lastFeatureChains = if (lastPlayer == 0) xFeatureChains else oFeatureChains
        var currFeatureChains = if (currPlayer == 0) xFeatureChains else oFeatureChains

        currFeatureChains = currFeatureChains.broke(last)

        if (currFeatureChains.anyOpenFour() || currFeatureChains.anyClosedFour()) { // current player win in current turn
            value = - WIN_POSITION * 2
        } else {
            lastFeatureChains = lastFeatureChains.improve(last)

            if (lastFeatureChains.anyOpenFour()) { // most probably win in next turn
                value = WIN_POSITION + WIN_POSITION + 2
            } else {
                val lastCells = lastMoves.drop(1).filter{ cell ->
                    onRange(last, cell, 4) && onLane(last, cell) &&
                            lastFeatureChains.chains.all { chain ->
                                ! (chain.contains(cell) && chain.contains(last))}}.toSortedListBy { last.range(it) }
                val currCells = currMoves.filter{ cell ->
                    onRange(last, cell, 4) && onLane(last, cell)}.toSortedListBy { last.range(it) }

                val rawChains = buildRawChains(last, lastCells, currCells) // update last player list of chains
                for (rawChain in rawChains) {
                    lastFeatureChains = lastFeatureChains.add(rawChain.extractChains())
                }

                if (lastFeatureChains.anyOpenFour() || lastFeatureChains.closedFourCount > 1) {
                    value = WIN_POSITION + WIN_POSITION / 2 // most probably win in next turn
                } else if (lastFeatureChains.anyClosedFour() && lastFeatureChains.anyWideOpenThree()) {
                    value = WIN_POSITION  // most probably win in several turns
                } else if (currFeatureChains.anyWideOpenThree() && ! lastFeatureChains.anyClosedFour() ||
                        currFeatureChains.wideOpenThreeCount > 1) {
                    value = - WIN_POSITION  // most probably loose in several turns
                } else if (lastFeatureChains.wideOpenThreeCount > 1) {
                    value = WIN_POSITION // most probably win in several turns
                } else {
                    if (lastFeatureChains.anyClosedFour()) {
                        value = WIN_POSITION / 2
                    } else if (lastFeatureChains.anyWideOpenThree()) {
                        value = WIN_POSITION / 2
                    }
                    // try to evaluate position
                    val currChains = buildRawChains(last, currCells, lastCells)
                    val s = currChains.fold(0, {(s, c) -> c.extractChains().size})

                    score = s * WIN_POSITION / 4 + rawChains.fold(0, {(c, chain) ->
                        if (chain.fullLength() >= 5 && chain.marked() >= 2) chain.marked() else 0 })
//                    +
//                            currChains.fold(0, {(c, chain) ->
//                                if (chain.fullLength() >= 5 && chain.marked() > 2) (chain.solidLength() - 1) else 0 })
                    -
                            score
                    value += if (player != currPlayer) score else -score
                }
            }
        }

        value = if (player != currPlayer) value else -value
        if (currPlayer == 0) {
            return GomokuEvaluator(player, value, score, currFeatureChains, lastFeatureChains)
        } else {
            return GomokuEvaluator(player, value, score, lastFeatureChains, currFeatureChains)
        }
    }

    override public fun increment(game: Gomoku): GomokuEvaluator {
        if (game.isOver) {
            return GomokuEvaluator(player, if (player == game.winner) Integer.MAX_VALUE else Integer.MIN_VALUE, score,
                    xFeatureChains, oFeatureChains)
        } else {
            return increment(game.currPlayer, game.markedCells[game.currPlayer], game.markedCells[game.prevPlayer()])
        }
    }

    override public fun evaluate(): Int {
        return evaluation
    }

}
