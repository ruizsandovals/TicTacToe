package com.sesaras.tictactoe

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.sesaras.tictactoe.BoardGame.Companion.NONE
import com.sesaras.tictactoe.BoardGame.Companion.PLAYER_O
import com.sesaras.tictactoe.BoardGame.Companion.PLAYER_X
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var boardGame = BoardGame() // Main board game



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startUp()

    }

    // App initialization
    private fun startUp() {
        var buttons    = arrayOf( button00,button01,button02,button10,button11,button12,button20,button21,button22)
        var cell         : Array <Array < Button >> = Array(3) {Array(3){Button(this)} }


        for (i in buttons.indices)
            cell [i/3][i.rem(3)] = buttons [i]


        // Set click listener for TicTacToe buttons
        for (r in cell.indices)
            for (c in cell[r].indices)
                cell[r][c].setOnClickListener { cellSelected(r,c) }


        // switches functions
             switchXSelected.setOnClickListener { whoIsHumanPlayer() }
             switchWhoStarts.setOnClickListener { whoStarts() }

        // Reset button
            buttonReset.setOnClickListener { reset() }

        // Play button
            buttonPlay.setOnClickListener { play() }

        // start playing
            play()

    }

    // Rest all
    fun reset () {
        whoStarts()
        boardGame.clearAll()
        showBoard()
        play()
    }

    // Show move based on user selection
    fun cellSelected (row : Int = 0, col : Int = 0) {
        var message = ""
        var status : Int

        if (boardGame.mainBoard.cell [row][col] == NONE && !boardGame.gameEnd) {

            // Select clicked button by Human Player
            boardGame.makeMove (row,col,boardGame.humanPlayer)

            // Computer's turn
            boardGame.bestMove()

            // Show board
            showBoard()

            // Set message
            when (boardGame.lastStatus) {
                BoardStatus.WIN.value -> if (boardGame.winner == boardGame.computerPlayer)
                                             message = "Computer Player Wins!, press play to continue..."
                                         else
                                             message = "Human Player Wins!, press play to continue..."
                BoardStatus.DRAW.value -> message = "It is a TIE, press play to continue ..."
                else -> message = ""
            }

            if (boardGame.gameEnd) {
                val toast = Toast.makeText(applicationContext,message,Toast.LENGTH_SHORT)
                toast.show()
            }


        }
    }


    fun whoStarts() {
        // Check who wants to start
        if (switchWhoStarts.isChecked)
            boardGame.lastPlayer = boardGame.humanPlayer
        else
            boardGame.lastPlayer = boardGame.computerPlayer
        play ()
    }

    fun whoIsHumanPlayer() {
        // Select icon for each player
        if (!switchXSelected.isChecked) {
            boardGame.humanPlayer = PLAYER_O
            boardGame.computerPlayer = PLAYER_X
        }
        else {
            boardGame.humanPlayer = PLAYER_X
            boardGame.computerPlayer = PLAYER_O
        }
        play ()
    }

    // Start a new game
    fun play () {

        // Initialize board game variables
        boardGame.clearBoard()

        // Show board
        showBoard()

        // If computer starts play a random move
        if (!switchWhoStarts.isChecked) {
            boardGame.randomMove()
            showBoard()
        }
    }

    // Show board and game stats
    fun showBoard () {

        var buttons    = arrayOf( button00,button01,button02,button10,button11,button12,button20,button21,button22)
        var cell         : Array <Array < Button >> = Array(3) {Array(3){Button(this)} }

        // Initialize 2d Array
        for (i in buttons.indices)
            cell [i/3][i.rem(3)] = buttons [i]

        // Shows game stats
            textViewTie.setText(boardGame.draws.toString())
            textViewHPwins.setText(boardGame.gamesHuman.toString())
            textViewUCWins.setText(boardGame.gamesComputer.toString())

        // Show board moves
        for (r in cell.indices) {
            for (c in cell[r].indices) {
                when (boardGame.mainBoard.cell[r][c]) {
                    PLAYER_X -> cell[r][c].setText(R.string.X)
                    PLAYER_O -> cell[r][c].setText(R.string.O)
                    NONE -> cell[r][c].setText(R.string.Empty)
                }
                cell[r][c].setBackgroundColor(Color.WHITE)
            }
        }

        // If game end and have winner highlight cells
        if (boardGame.gameEnd && boardGame.lastStatus == BoardStatus.WIN.value) {
            if (boardGame.winnerLine.cellList.size > 0) {
                for (i in boardGame.winnerLine.cellList) {
                    cell[i.row][i.col].setBackgroundColor(Color.BLUE)
                }
            }
        }


    }

}
