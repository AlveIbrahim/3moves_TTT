package com.example.threemovetictactoe

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // Game state
    private var currentPlayer = "X"
    private var gameActive = true
    private val gameState = Array(9) { "" }

    // Bot settings
    private var playWithBot = false
    private var botDifficulty = TicTacToeBot.Difficulty.MEDIUM
    private var botMarker = "O" // Default bot plays as O
    private lateinit var bot: TicTacToeBot

    // Move history for each player
    private val moveHistory = mutableMapOf(
        "X" to mutableListOf<Int>(),
        "O" to mutableListOf<Int>()
    )

    // Cell views on the board
    private val cells = Array<View?>(9) { null }

    // UI Elements
    private lateinit var statusTextView: TextView
    private lateinit var countXTextView: TextView
    private lateinit var countOTextView: TextView

    // Winning combinations
    private val winningCombinations = arrayOf(
        intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8), // Rows
        intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8), // Columns
        intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)                        // Diagonals
    )

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        statusTextView = findViewById(R.id.statusTextView)
        countXTextView = findViewById(R.id.countXTextView)
        countOTextView = findViewById(R.id.countOTextView)
        val boardGridLayout = findViewById<android.widget.GridLayout>(R.id.boardGridLayout)
        val resetButton = findViewById<android.widget.Button>(R.id.resetButton)

        // Show game mode selection dialog
        showGameModeDialog()

        // Create the game board
        createBoard(boardGridLayout)

        // Set up reset button
        resetButton.setOnClickListener {
            showGameModeDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showGameModeDialog() {
        val options = arrayOf("2 Players", "Play vs Easy Bot", "Play vs Medium Bot", "Play vs Hard Bot")

        AlertDialog.Builder(this)
            .setTitle("Select Game Mode")
            .setItems(options) { dialog, which ->
                gameActive = true
                resetGame()

                when (which) {
                    0 -> {
                        // 2 Players mode
                        playWithBot = false
                    }
                    1, 2, 3 -> {
                        // Bot mode with different difficulty levels
                        playWithBot = true
                        botDifficulty = when (which) {
                            1 -> TicTacToeBot.Difficulty.EASY
                            2 -> TicTacToeBot.Difficulty.MEDIUM
                            else -> TicTacToeBot.Difficulty.HARD
                        }

                        // Let player choose X or O
                        showMarkerSelectionDialog()
                    }
                }
            }
            .setCancelable(false)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showMarkerSelectionDialog() {
        val options = arrayOf("Play as X (First Move)", "Play as O (Second Move)")

        AlertDialog.Builder(this)
            .setTitle("Choose Your Marker")
            .setItems(options) { dialog, which ->
                botMarker = if (which == 0) "O" else "X"

                // Initialize the bot with selected settings
                bot = TicTacToeBot(
                    difficulty = botDifficulty,
                    botMarker = botMarker
                ) { position ->
                    // This lambda is called when the bot makes a move
                    handleCellClick(cells[position]!!)
                }

                // If bot goes first (player chose O), let bot make first move
                if (botMarker == "X") {
                    bot.makeMove(gameState, moveHistory)
                }

                updateStatus()
                updateCounts()
            }
            .setCancelable(false)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createBoard(boardGridLayout: android.widget.GridLayout) {
        val inflater = LayoutInflater.from(this)

        // Create 9 cells for the 3x3 grid
        for (i in 0 until 9) {
            val cellView = inflater.inflate(R.layout.cell_layout, boardGridLayout, false)

            // Set cell tag for identification
            cellView.tag = i

            // Set click listener
            cellView.setOnClickListener { view ->
                if (gameActive && (!playWithBot || currentPlayer != botMarker)) {
                    handleCellClick(view)

                    // If it's now the bot's turn, let it make a move
                    if (gameActive && playWithBot && currentPlayer == botMarker) {
                        bot.makeMove(gameState, moveHistory)
                    }
                }
            }

            // Add to the grid
            boardGridLayout.addView(cellView)

            // Store reference to the cell
            cells[i] = cellView
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun handleCellClick(view: View) {
        val index = view.tag as Int

        // Exit if game over or cell not empty
        if (!gameActive || gameState[index].isNotEmpty()) return

        // If the current player already has 3 pieces, remove the oldest
        if ((moveHistory[currentPlayer]?.size ?: 0) >= 3) {
            val oldestMoveIndex = moveHistory[currentPlayer]?.removeAt(0) ?: 0
            val oldestCell = cells[oldestMoveIndex]

            // Clear the oldest cell
            gameState[oldestMoveIndex] = ""
            oldestCell?.findViewById<TextView>(R.id.cellTextView)?.text = ""
        }

        // Place the new piece
        gameState[index] = currentPlayer
        view.findViewById<TextView>(R.id.cellTextView).apply {
            text = currentPlayer

            // Apply neon styling
            if (currentPlayer == "X") {
                setTextColor(resources.getColor(R.color.playerX, theme))
                setShadowLayer(15f, 0f, 0f, resources.getColor(R.color.playerX, theme))
            } else {
                setTextColor(resources.getColor(R.color.playerO, theme))
                setShadowLayer(15f, 0f, 0f, resources.getColor(R.color.playerO, theme))
            }
        }

        // Add this move to the player's history
        moveHistory[currentPlayer]?.add(index)

        // Update the counters
        updateCounts()

        // Check for a win
        if (checkWin()) {
            gameActive = false

            // Show winner
            val winnerText = if (playWithBot && currentPlayer == botMarker) {
                "Bot wins!"
            } else {
                "Player ${currentPlayer} wins!"
            }

            statusTextView.text = winnerText

            // Make the status text flash for winner
            statusTextView.setTextColor(
                if (currentPlayer == "X")
                    resources.getColor(R.color.playerX, theme)
                else
                    resources.getColor(R.color.playerO, theme)
            )
            statusTextView.setShadowLayer(15f, 0f, 0f,
                if (currentPlayer == "X")
                    resources.getColor(R.color.playerX, theme)
                else
                    resources.getColor(R.color.playerO, theme)
            )
            return
        }

        // Switch player
        currentPlayer = if (currentPlayer == "X") "O" else "X"
        updateStatus()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkWin(): Boolean {
        for (combination in winningCombinations) {
            val (a, b, c) = combination
            if (gameState[a].isNotEmpty() && gameState[a] == gameState[b] && gameState[a] == gameState[c]) {
                // Highlight winning cells
                highlightWinningCells(combination)
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun highlightWinningCells(winningCells: IntArray) {
        for (index in winningCells) {
            cells[index]?.findViewById<TextView>(R.id.cellTextView)?.apply {
                // Increase the glow effect for winning cells
                if (text == "X") {
                    setShadowLayer(25f, 0f, 0f, resources.getColor(R.color.playerX, theme))
                } else {
                    setShadowLayer(25f, 0f, 0f, resources.getColor(R.color.playerO, theme))
                }
                textSize = 56f  // Make winning cells slightly larger
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateStatus() {
        // Update status text with appropriate style
        statusTextView.apply {
            text = when {
                // If playing with bot, show different message
                playWithBot && currentPlayer == botMarker -> "Bot is thinking..."
                currentPlayer == "X" && (moveHistory["X"]?.size ?: 0) < 3 -> getString(R.string.player_x_turn_place)
                currentPlayer == "X" -> getString(R.string.player_x_turn_oldest)
                (moveHistory["O"]?.size ?: 0) < 3 -> getString(R.string.player_o_turn_place)
                else -> getString(R.string.player_o_turn_oldest)
            }

            // Set the text color and glow based on current player
            setTextColor(
                resources.getColor(
                    if (currentPlayer == "X") R.color.playerX else R.color.playerO,
                    theme
                )
            )

            setShadowLayer(15f, 0f, 0f,
                resources.getColor(
                    if (currentPlayer == "X") R.color.playerX else R.color.playerO,
                    theme
                )
            )
        }
    }

    private fun updateCounts() {
        countXTextView.text = getString(R.string.player_x_count, moveHistory["X"]?.size ?: 0)
        countOTextView.text = getString(R.string.player_o_count, moveHistory["O"]?.size ?: 0)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun resetGame() {
        currentPlayer = "X"
        gameActive = true

        // Clear game state
        for (i in gameState.indices) {
            gameState[i] = ""
        }

        // Clear cells
        for (cell in cells) {
            cell?.findViewById<TextView>(R.id.cellTextView)?.text = ""
        }

        // Clear move history
        moveHistory["X"]?.clear()
        moveHistory["O"]?.clear()

        // Update UI
        updateStatus()
        updateCounts()

        // Reset status text to neutral color
        statusTextView.apply {
            setTextColor(resources.getColor(R.color.neonYellow, theme))
            setShadowLayer(8f, 0f, 0f, resources.getColor(R.color.neonYellow, theme))
        }
    }
}