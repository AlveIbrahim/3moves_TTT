package com.example.threemovetictactoe

import android.os.Handler
import android.os.Looper
import kotlin.random.Random

class TicTacToeBot(
    private val difficulty: Difficulty,
    private val botMarker: String, // "X" or "O"
    private val onMoveMade: (position: Int) -> Unit
) {
    enum class Difficulty {
        EASY, MEDIUM, HARD
    }

    // Make a move based on the current board state
    fun makeMove(
        gameState: Array<String>,
        moveHistory: Map<String, MutableList<Int>>
    ) {
        // Add a small delay to simulate "thinking"
        Handler(Looper.getMainLooper()).postDelayed({
            val position = when (difficulty) {
                Difficulty.EASY -> calculateEasyMove(gameState)
                Difficulty.MEDIUM -> calculateMediumMove(gameState, moveHistory)
                Difficulty.HARD -> calculateHardMove(gameState, moveHistory)
            }

            // Pass the chosen position back to the game
            onMoveMade(position)
        }, 500) // 500ms delay
    }

    // EASY: Make a random move
    private fun calculateEasyMove(gameState: Array<String>): Int {
        val emptyPositions = getEmptyPositions(gameState)
        return if (emptyPositions.isNotEmpty()) {
            emptyPositions.random()
        } else -1
    }

    // MEDIUM: Block wins and try to win
    private fun calculateMediumMove(
        gameState: Array<String>,
        moveHistory: Map<String, MutableList<Int>>
    ): Int {
        val emptyPositions = getEmptyPositions(gameState)
        if (emptyPositions.isEmpty()) return -1

        val playerMarker = if (botMarker == "X") "O" else "X"

        // Clone the game state for testing moves
        val testBoard = gameState.clone()

        // Try to win
        for (pos in emptyPositions) {
            testBoard[pos] = botMarker
            if (checkWin(testBoard)) {
                return pos
            }
            testBoard[pos] = "" // Reset
        }

        // Block opponent's win
        for (pos in emptyPositions) {
            testBoard[pos] = playerMarker
            if (checkWin(testBoard)) {
                return pos
            }
            testBoard[pos] = "" // Reset
        }

        // Prefer center
        if (4 in emptyPositions) return 4

        // Prefer corners
        val corners = listOf(0, 2, 6, 8).filter { it in emptyPositions }
        if (corners.isNotEmpty()) return corners.random()

        // Take any available position
        return emptyPositions.random()
    }

    // HARD: More strategic play considering the 3-move limit
    private fun calculateHardMove(
        gameState: Array<String>,
        moveHistory: Map<String, MutableList<Int>>
    ): Int {
        val emptyPositions = getEmptyPositions(gameState)
        if (emptyPositions.isEmpty()) return -1

        val playerMarker = if (botMarker == "X") "O" else "X"
        val botPieces = moveHistory[botMarker] ?: mutableListOf()

        // Clone the game state for testing moves
        val testBoard = gameState.clone()

        // Check for immediate win
        for (pos in emptyPositions) {
            testBoard[pos] = botMarker
            if (checkWin(testBoard)) {
                return pos
            }
            testBoard[pos] = "" // Reset
        }

        // Block immediate loss
        for (pos in emptyPositions) {
            testBoard[pos] = playerMarker
            if (checkWin(testBoard)) {
                return pos
            }
            testBoard[pos] = "" // Reset
        }

        // If we already have 3 pieces, strategically replace oldest
        if (botPieces.size >= 3) {
            val oldestPos = botPieces.first()

            // Find best move considering the removal of oldest piece
            var bestScore = -1000
            var bestPosition = emptyPositions.random()

            for (pos in emptyPositions) {
                // Simulate removing oldest and placing new
                testBoard[oldestPos] = ""
                testBoard[pos] = botMarker

                val score = evaluateBoard(testBoard, botMarker, playerMarker)

                // Reset
                testBoard[oldestPos] = botMarker
                testBoard[pos] = ""

                if (score > bestScore) {
                    bestScore = score
                    bestPosition = pos
                }
            }

            return bestPosition
        }

        // Strategic placement
        if (4 in emptyPositions) return 4 // Center

        // Corners
        val corners = listOf(0, 2, 6, 8).filter { it in emptyPositions }
        if (corners.isNotEmpty()) return corners.random()

        // Other positions
        return emptyPositions.random()
    }

    // Helper method to evaluate board positions
    private fun evaluateBoard(board: Array<String>, botMarker: String, playerMarker: String): Int {
        val winCombinations = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8), // Rows
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8), // Columns
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)                       // Diagonals
        )

        var score = 0

        // Check each winning line
        for (combo in winCombinations) {
            var botCount = 0
            var playerCount = 0
            var emptyCount = 0

            for (pos in combo) {
                when (board[pos]) {
                    botMarker -> botCount++
                    playerMarker -> playerCount++
                    else -> emptyCount++
                }
            }

            // Bot has 2 in a row with an empty cell
            if (botCount == 2 && emptyCount == 1) score += 10

            // Bot has 1 in a row with 2 empty cells
            else if (botCount == 1 && emptyCount == 2) score += 1

            // Player has 2 in a row with an empty cell (block this)
            else if (playerCount == 2 && emptyCount == 1) score += 8
        }

        // Prefer center
        if (board[4] == botMarker) score += 5

        // Prefer corners
        for (corner in listOf(0, 2, 6, 8)) {
            if (board[corner] == botMarker) score += 3
        }

        return score
    }

    // Helper method to check for a win
    private fun checkWin(board: Array<String>): Boolean {
        val winCombinations = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8), // Rows
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8), // Columns
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)                       // Diagonals
        )

        for (combo in winCombinations) {
            val (a, b, c) = combo
            if (board[a].isNotEmpty() && board[a] == board[b] && board[a] == board[c]) {
                return true
            }
        }

        return false
    }

    // Helper method to get empty positions
    private fun getEmptyPositions(board: Array<String>): List<Int> {
        return board.indices.filter { board[it].isEmpty() }
    }
}