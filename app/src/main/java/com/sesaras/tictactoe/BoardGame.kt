package com.sesaras.tictactoe

import java.util.*
import com.sesaras.tictactoe.BoardGame.Companion.NUM_COLS
import com.sesaras.tictactoe.BoardGame.Companion.NUM_ROWS
import java.lang.Math.max
import java.lang.Math.min
import kotlin.system.exitProcess

enum class BoardStatus (var value: Int){
    WIN (10),
    LOSE (-10),
    DRAW (0),
    UNKNOWN (-20)
}

class BoardGame () {
    companion object {
        const val PLAYER_X = 1                     // Internal representation for player O
        const val PLAYER_O = 2                     // Internal representation for player O
        const val NONE = 0                     // Internal representation for non-played cells
        const val NUM_ROWS = 3                     // Number of rows in the board
        const val NUM_COLS = 3                     // number of cols in the board
        const val ADJ_CELLS =
            3                     // Number of adjacent cells to win (must be equal or less than min (NUM_ROWS, NUM_COLS)
        const val MAX_PLAYS = NUM_COLS * NUM_ROWS   // Num max of plays
        const val NUM_MAX_I = 10000000               // Max number of iterations
    }

    var cellFigure = mapOf<Int, String>(0 to " ", 1 to "X", 2 to "O")
    var strPlayer =
        { player: Int -> if (player == computerPlayer) "Computer Player" else "Human Player" }
    var mainBoard = Board()                              // Main board
    var winningLines = ListOfLines()                         // List of winning lines
    var winner = PLAYER_X                              // Winner player
    var winLinesByCell = WinLinesByCell()                      // List of winning lines by cell
    var scoreBoard = Board()                              // Stores the results of last move
    var winnerLine = ListOfCells()                         // Cells of the winner line
    var suggestedMove = BoardCell()                          // Suggested next move
    var lastMove = BoardCell()                          // Last player move
    var lastPlayer = PLAYER_X                              // Who was the last player?
    var lastStatus = BoardStatus.UNKNOWN.value               // Game last status
    var candidateMove = ListOfCells()                        // List of possible moves
    var computerPlayer = PLAYER_X                              // Computer player = X by default
    var humanPlayer = PLAYER_O                              // Human player = 0 by default
    var iterations: Long =
        0                              // Number of times the minMax function is called
    var maxDepthReached: Int =
        0                              // Max depth reached to calculate a solution
    var numPlays: Int = 0                              // Current number of plays
    var maxDepth: Int = 15                             // Max depth level for analysis
    var limitMaxDepth = false                                 // Limit max depth analysis ?
    var limitAnalysis = false                                 // Limit cell analysis ?
    var AnalysisRatio =
        ADJ_CELLS + 1                         // Limit ratio of analysis (cells around the last move)
    val DEFAULT_SCORE =
        BoardStatus.LOSE.value                // Default score then MAX_DEPTH is reached without solution
    var showScores = false                                  // Show scores ?
    var detailDepth = -1                                     // Details depth
    var showDetails = false                                 // Show Details
    var progress =
        0                                     // To show number of iterations every 1,000,000
    var gamesHuman = 0                                     // Number of Games won by human player
    var gamesComputer = 0                                     // Number of Games won by computer
    var draws = 0                                     // Number of draws
    var gameEnd = false                               // End of game ?

    init {
        initWinningLines()
        clearAll()
    }

    // Ask for gaming parameters
    private fun configureGame() {
        val reader = Scanner(System.`in`)
        var strFigure: String
        var strStart: String

        // Let user select the figure (X or O)
        do {
            print("Select figure (X or O): ")
            strFigure = reader.next().toUpperCase(Locale.ROOT)
            if (strFigure == "X" || strFigure == "O")
                break
            else
                println("No valid selection try again")
        } while (true)

        // Let the user decide if he wants to start
        do {
            print("Do you want to start? (Y or N): ")
            strStart = reader.next().toUpperCase(Locale.ROOT)
            if (strStart == "Y" || strStart == "N")
                break
            else
                println("No valid selection try again")
        } while (true)

        // Change board game parameters based on user selection
        if (strFigure == "X") {
            computerPlayer = PLAYER_O
            humanPlayer = PLAYER_X
        } else {
            computerPlayer = PLAYER_X
            humanPlayer = PLAYER_O
        }
        if (strStart == "N") lastPlayer = humanPlayer else lastPlayer = computerPlayer
    }

    // Play routine
    fun play() {
        val reader = Scanner(System.`in`)
        var row: Int    // Input row
        var col: Int    // Input col
        var strAgain: String // Play again ?

        do {
            // Initialize variables
            clearAll()

            // game configuration
            configureGame()

            if (lastPlayer == humanPlayer) {
                // play random position
                makeMove((0 until NUM_ROWS).random(), (0 until NUM_COLS).random(), computerPlayer)
            }

            // Shows the board
            showBoard(mainBoard)

            do {

                if (lastPlayer == computerPlayer) {
                    // Ask next move
                    do {

                        try {
                            print("Select a row (0-${NUM_ROWS - 1}):")
                            row = reader.nextInt()
                            print("Select a col (0-${NUM_COLS - 1}):")
                            col = reader.nextInt()

                            if (col !in (0..NUM_COLS - 1) || row !in (0..NUM_ROWS - 1)) {
                                println("Selection out of range, please try again.")
                            } else {
                                if (mainBoard.cell[row][col] != NONE)
                                    println("The selected position [${row},${col}] is not available, please try again")
                                else
                                    break
                            }
                        } catch (e: InputMismatchException) {
                            println("Please enter a number")
                            exitProcess(0)
                        }
                    } while (true)
                    makeMove(row, col, humanPlayer)
                } else
                    bestMove() // Calculate next move for computer player

                // show selected cell
                showBoard(mainBoard)

                // End of game? break loop
                if (getBoardStatus() != BoardStatus.UNKNOWN.value) break

            } while (true)

            // Let the user decide if he wants to start
            do {
                print("Do you want to PLAY AGAIN? (Y or N): ")
                strAgain = reader.next().toUpperCase(Locale.ROOT)
                if (strAgain == "Y" || strAgain == "N")
                    break
                else
                    println("No valid selection try again")
            } while (true)
        } while (strAgain == "Y")
    }

    // Gets the winner line, return the main board status
    fun getBoardStatus(): Int {
        var status = BoardStatus.DRAW.value
        var haveWinner: Boolean
        var prev: Int
        var curr: Int
        val row =
            { l: Int, c: Int -> winningLines.linesList[l].cellList[c].row }      // Get the row value of a particular winning line index and board cell index
        val col =
            { l: Int, c: Int -> winningLines.linesList[l].cellList[c].col }      // Get the column value of a particular winning line index and board cell index

        // If we already have the winner then return
        if (lastStatus != BoardStatus.UNKNOWN.value)
            return lastStatus

        // Check if there is a winning line
        for (l in winningLines.linesList.indices) {
            haveWinner = true
            for (c in 1 until ADJ_CELLS) {
                curr = mainBoard.cell[row(l, c)][col(l, c)]                          // Get the current board cell value of the winning line
                prev = mainBoard.cell[row(l, c - 1)][col(l,c - 1)]                          // Get the previous board cell value of the winning line
                if ((prev != curr) || (curr == NONE) || (prev == NONE)) {                  // Check if previous cell and current are different, then we do not have a winner.
                    haveWinner = false
                    break
                }
            }
            if (haveWinner) {                                                              // If we have a winner, we need to identify the winning line and the winner
                winner = mainBoard.cell[row(l, 0)][col(l, 0)]
                winnerLine.cellList.clear()
                for (c in winningLines.linesList[l].cellList.indices)
                    winnerLine.cellList.add(BoardCell(row(l, c),col(l, c)))
                status = BoardStatus.WIN.value
                break
            }
        }

        if (status == BoardStatus.DRAW.value)                                             // Review if there are more cells to play, if so the status is Unknown
        {
            for (y in mainBoard.cell)
                if (y.contains(NONE)) {
                    status = BoardStatus.UNKNOWN.value
                    break
                }
        }

        if (showDetails) {
            if (status == BoardStatus.WIN.value) {
                println("*** ${strPlayer(winner)} WON!!!")
                print("*** Winner line : ")
                for (i in winnerLine.cellList) {
                    print("[${i.row},${i.col}] ")
                }
                println()
            } else {
                if (status == BoardStatus.DRAW.value) {
                    println("It's a TIE!")
                }
            }
        }

        // Increase game counters
        if (status != BoardStatus.UNKNOWN.value) {
            if (status == BoardStatus.DRAW.value) {
                draws++
            } else {
                if (winner == computerPlayer)
                    gamesComputer++
                else
                    gamesHuman++
            }
            gameEnd = true
        }

        lastStatus = status
        return status
    }


    // * Clears board and game variables
    fun clearAll() {
        // Clear board
            clearBoard()

        // Game stats
            gamesComputer = 0
            gamesHuman = 0
            draws = 0
    }

    // Clear all board movements, number of plays and internal scores
    fun clearBoard () {
        // Clear Board
        for (row in 0 until NUM_ROWS)
            for (col in 0 until NUM_COLS)
                mainBoard.cell[row][col] = NONE

        // Clear other variables
            clearScores()
            iterations = 0
            maxDepthReached = 0
            numPlays = 0
            maxDepth = 15
            gameEnd = false
            lastStatus = BoardStatus.UNKNOWN.value
    }

    // Make the move in the main board
    fun makeMove(row: Int, col: Int, player: Int) {
        mainBoard.cell[row][col] = player
        lastMove.row = row
        lastMove.col = col
        lastPlayer = player
        numPlays++

        // Get the board status
            getBoardStatus()
    }

    // Calculates max depth allowed
    private fun calculateMaxDepth() {
        var playsLeft = MAX_PLAYS - numPlays    // Movements left
        val limit = NUM_MAX_I                   // Max number of iterations allowed
        var depth = 0
        var total = playsLeft

        // Calculate Max depth based on Max number of iterations
        do {
            depth++
            playsLeft--
            total *= playsLeft
            if (total > limit || playsLeft == 0) break
        } while (true)

        maxDepth = depth
    }

    // Determines which is the next best move for computer player
    fun bestMove() {
        var bestScore = BoardStatus.LOSE.value
        var score: Int
        var initialRow = 0
        var finalRow = NUM_ROWS
        var initialCol = 0
        var finalCol = NUM_COLS

        // Initialize iterations counter
        iterations = 0

        // Initialize max depth level reached
        maxDepthReached = 0

        //  Limit Max Depth analysis if required
        if (limitMaxDepth)
            calculateMaxDepth()

        // Limit area of analysis ?
        if (limitAnalysis) {
            initialRow = 0.coerceAtLeast(lastMove.row - AnalysisRatio)
            finalRow = NUM_ROWS.coerceAtMost(lastMove.row + AnalysisRatio + 1)
            initialCol = 0.coerceAtLeast(lastMove.col - AnalysisRatio)
            finalCol = NUM_COLS.coerceAtMost(lastMove.col + AnalysisRatio + 1)
        }

        // Clear scores
        clearScores()

        // If there are no more movements then return
        if (numPlays >= NUM_COLS * NUM_ROWS || lastStatus != BoardStatus.UNKNOWN.value)
            return

        // Establish ratio of analysis based of the main variable

        // Create local copies of the board
        for (row in initialRow until finalRow) {
            for (col in initialCol until finalCol) {
                if (mainBoard.cell[row][col] == NONE) {

                    // Selecting a particular cell to be evaluated
                    mainBoard.cell[row][col] = computerPlayer

                    // Evaluate the move
                    score = minMax(mainBoard, 1, false, row, col)

                    // Store score
                    scoreBoard.cell[row][col] = score

                    // Get best score
                    if (score >= bestScore) {
                        bestScore = score
                        suggestedMove.row = row
                        suggestedMove.col = col
                    }

                    // Restore original board
                    mainBoard.cell[row][col] = NONE
                }
            }
        }

        // Show scores if necessary
        if (showScores) showScores()

        // Select best move from possible options
        randomBestMove(bestScore)

        //Show number of iterations
        if (showDetails) {
            println("Iterations              = $iterations")
            println("Max Depth Level Reached = $maxDepthReached")
            println()
        }
    }

    // Executes a random move using available cells
    fun randomMove () {
        var i = 0 // Selected Move
        // Clear list of candidate moves
        candidateMove.cellList.clear()

        // Create list of best moves
        for (row in 0 until NUM_ROWS) {
            for (col in 0 until NUM_COLS) {
                if (mainBoard.cell[row][col] == NONE) {
                    candidateMove.cellList.add(BoardCell(row, col))
                }
            }
        }

        // Randomize selection
        if (candidateMove.cellList.size >= 0)
            i = (0..candidateMove.cellList.lastIndex).random()

        // Make the best move
        makeMove(candidateMove.cellList[i].row, candidateMove.cellList[i].col, computerPlayer)

        // Print out the selection
        if (showDetails)
            println("Computer played (row,col): [${candidateMove.cellList[i].row},${candidateMove.cellList[i].col}]")
    }

    // Select from best move randomly
    private fun randomBestMove(bestScore: Int = 0) {
        var i = 0 // Selected Move

        // Clear list of candidate moves
        candidateMove.cellList.clear()

        // Create list of best moves
        for (row in 0 until NUM_ROWS) {
            for (col in 0 until NUM_COLS) {
                if (scoreBoard.cell[row][col] == bestScore) {
                    candidateMove.cellList.add(BoardCell(row, col))
                }
            }
        }

        // Randomize selection
        if (candidateMove.cellList.size >= 0)
            i = (0..candidateMove.cellList.lastIndex).random()

        // Make the best move
        makeMove(candidateMove.cellList[i].row, candidateMove.cellList[i].col, computerPlayer)

        // Print out the selection
        if (showDetails)
            println("Computer played (row,col): [${candidateMove.cellList[i].row},${candidateMove.cellList[i].col}]")

    }

    // Calculate score for computer's play based on the minimax algorithm
    private fun minMax(
        board: Board,
        depth: Int,
        isMaximizing: Boolean,
        evaluatedRow: Int = -1,
        evaluatedCol: Int = -1
    ): Int {
        val boardCpy = Board()                          // Local copy of the board
        val eval: Int                              // To determine if the game ends (WIN, LOSE, DRAW)
        var bestScore: Int = BoardStatus.LOSE.value     // To store best score when maximizing
        var worstScore: Int = BoardStatus.WIN.value      // To store worst score when minimizing
        var score: Int
        val result: Int                              // Final result (worst or best score base on the minimizing or maximizing parameter)
        val strPad = "".padStart(depth * 10, ' ')

        // if we have reached the maximum depth allowed then return
        if (depth > maxDepth) return DEFAULT_SCORE

        progress++
        if (showDetails) {
            if (progress.rem(1000000) == 0)
                println(progress)
        }

        // Create local copy of the board
        boardCpy.cell = board.cell.copyOf()

        // Check if the game is over to return the result
        eval = evaluateCell(boardCpy, computerPlayer, evaluatedRow, evaluatedCol)
        if (eval != BoardStatus.UNKNOWN.value) return eval

        // Update max depth level
        maxDepthReached = depth.coerceAtLeast(maxDepthReached)

        // Increase number of iterations
        iterations++

        // Show information based on verbose level
        if (showDetails && depth <= detailDepth) {
            println("${strPad}Evaluating move (row,col) [${evaluatedRow},${evaluatedCol}]")
            println("for player ${cellFigure.getValue(if (isMaximizing) humanPlayer else computerPlayer)} (Minimizing)")
            showBoard(boardCpy, depth)
        }

        // Evaluate all available moves
        for (row in 0 until NUM_ROWS) {
            for (col in 0 until NUM_COLS) {
                if (boardCpy.cell[row][col] == NONE) {

                    // Selecting a particular cell to be evaluated
                    boardCpy.cell[row][col] = if (isMaximizing) computerPlayer else humanPlayer

                    // Evaluate this position
                    score = minMax(boardCpy, depth + 1, !isMaximizing, row, col)

                    // Get best score
                    bestScore = max(score, bestScore)
                    worstScore = min(score, worstScore)

                    // Restore original board
                    boardCpy.cell[row][col] = NONE
                }
            }
        }

        // Calculate result
        result = if (isMaximizing) bestScore else worstScore

        // Show score for this position if required
        if (showDetails && depth <= detailDepth) {
            println("${strPad}Score for the move (above) = ${result}")
            println()
            println()
        }

        // Return result
        return result
    }

    // Clear all scores
    private fun clearScores() {
        // Clear Board
        for (row in 0 until NUM_ROWS)
            for (col in 0 until NUM_COLS)
                scoreBoard.cell[row][col] = BoardStatus.UNKNOWN.value
    }

    // *Shows the specific board game
    private fun showBoard(board: Board, depth: Int = 0) {
        var strMain: String
        var strLine: String
        val strPadd = "".padStart(depth * 10, ' ')  // Left padding

        // Header
        strMain = "$strPadd   "
        strLine = "$strPadd  +"
        for (col in 0 until NUM_COLS) {
            strMain += "  ${col.toString().padStart(2, ' ')}  "
            strLine += "-----+"
        }
        println(strMain)
        println(strLine)

        // Body
        for (row in 0 until NUM_ROWS) {
            strMain = "$strPadd$row |"
            for (col in 0 until NUM_COLS) {
                strMain += "  ${cellFigure.getValue(board.cell[row][col])}  |"
            }
            println(strMain)
            println(strLine)
        }

        // Footer
        println()
        println("Number of plays $numPlays out of $MAX_PLAYS")
        println("MAX DEPTH = $maxDepth")
        println()
    }

    // *Shows scores of last move
    private fun showScores() {
        var strMain = " ".repeat(7)
        var strLine = " ".repeat(6) + "+"

        if (showDetails) {
            // Header
            println("*** Scores ***")
            for (col in 0 until NUM_COLS) {
                strMain += " ".repeat(3) + "$col".padStart(2, ' ') + "   "
                strLine += "-------+"
            }
            println(strMain)
            println(strLine)

            // Body
            for (row in 0 until NUM_ROWS) {
                strMain = "    $row |"
                for (col in 0 until NUM_COLS) {
                    if (scoreBoard.cell[row][col] == BoardStatus.UNKNOWN.value)
                        strMain += " ".repeat(7) + "|"
                    else
                        strMain += "  ${scoreBoard.cell[row][col].toString().padStart(3, ' ')}  |"
                }
                println(strMain)
                println(strLine)
            }

            // Footer
            println()
        }
    }

    // * Evaluates an specific cell in the board to check if we have a winning line, returns (WIN.value, LOSE.value, DRAW.value or UNKNOWN.value )
    private fun evaluateCell(board: Board, player: Int, evaluatedRow: Int, evaluatedCol: Int): Int {
        var status = BoardStatus.DRAW.value
        var haveWinner: Boolean
        var prev: Int
        var curr: Int
        val row =
            { l: Int, c: Int -> winLinesByCell.cell[evaluatedRow][evaluatedCol].linesList[l].cellList[c].row }             // Get the row value of a particular winning line index and board cell index
        val col =
            { l: Int, c: Int -> winLinesByCell.cell[evaluatedRow][evaluatedCol].linesList[l].cellList[c].col }             // Get the column value of a particular winning line index and board cell index

        // Check if there is a winning line
        for (l in winLinesByCell.cell[evaluatedRow][evaluatedCol].linesList.indices) {
            haveWinner = true
            for (c in 1 until ADJ_CELLS) {
                curr = board.cell[row(l, c)][col(
                    l,
                    c
                )]                          // Get the current board cell value of the winning line
                prev = board.cell[row(l, c - 1)][col(
                    l,
                    c - 1
                )]                          // Get the previous board cell value of the winning line
                if ((prev != curr) || (curr == NONE) || (prev == NONE)) {              // Check if previous cell and current are different, then we do not have a winner.
                    haveWinner = false
                    break
                }
            }
            if (haveWinner) {
                if (player == board.cell[row(l, 0)][col(
                        l,
                        0
                    )]
                )                          // Check if the player won or lose comparing with the first winner cell
                    status = BoardStatus.WIN.value
                else
                    status = BoardStatus.LOSE.value
                break
            }
        }
        if (status == BoardStatus.DRAW.value)                                          // Review if there are more cells to play, if so the status is Unknown
        {
            for (y in board.cell)
                if (y.contains(NONE)) {
                    status = BoardStatus.UNKNOWN.value
                    break
                }
        }
        return status
    }

    // * This routine determines all the possible winning lines in the board (updates 'winningLines')
    private fun initWinningLines() {
        val addList = { winningLines.linesList.add(ListOfCells()) }
        val addCell = { row: Int, col: Int ->
            winningLines.linesList[winningLines.linesList.lastIndex].cellList.add(
                BoardCell(
                    row,
                    col
                )
            )
        } // adds a cell to the last winning line

        // Clear list of winning lines
        winningLines.linesList.clear()

        // Determine all the winning lines per board
        for (y in 0..NUM_ROWS - ADJ_CELLS) {
            for (x in 0..NUM_COLS - ADJ_CELLS) {
                // Horizontal lines
                for (row in y until y + ADJ_CELLS) {
                    addList()
                    for (col in x until x + ADJ_CELLS)
                        addCell(row, col)
                }
                // Vertical lines
                for (col in x until x + ADJ_CELLS) {
                    addList()
                    for (row in y until y + ADJ_CELLS) {
                        addCell(row, col)
                    }
                }
                // Negative diagonal line "\"
                addList()
                for (i in 0 until ADJ_CELLS) {
                    addCell(y + i, x + i)
                }
                // Positive Diagonal line "/"
                addList()
                for (i in 0 until ADJ_CELLS) {
                    addCell(y + i, x + ADJ_CELLS - i - 1)
                }
            }
        }

        // Determine winning lines per cell
        var cellInLine: Boolean
        for (row in 0 until NUM_ROWS) {
            for (col in 0 until NUM_COLS) {
                // Clear winning lines by cell
                winLinesByCell.cell[row][col].linesList.clear()

                // Checking if the winning line includes the analyzed cell
                for (l in winningLines.linesList.indices) {
                    // Assuming cell is not in the winning line
                    cellInLine = false
                    for (c in winningLines.linesList[l].cellList.indices) {
                        if (winningLines.linesList[l].cellList[c].row == row && winningLines.linesList[l].cellList[c].col == col) {
                            cellInLine = true
                            break
                        }
                    }

                    // In cell is in this winning line then add it
                    if (cellInLine) {
                        // Add a list
                        winLinesByCell.cell[row][col].linesList.add(ListOfCells())
                        val i = winLinesByCell.cell[row][col].linesList.lastIndex

                        // Add the cells to the list
                        for (c in winningLines.linesList[l].cellList.indices) {
                            winLinesByCell.cell[row][col].linesList[i].cellList.add(winningLines.linesList[l].cellList[c])
                        }
                    }
                }
            }
        }

        // Showing winning lines per cell
        if (showDetails) {
            println("*** List of winning lines per cell ***")
            for (row in 0 until NUM_ROWS) {
                for (col in 0 until NUM_COLS) {
                    print("Cell [$row,$col] : Lists (${winLinesByCell.cell[row][col].linesList.size}) = {")
                    for (l in winLinesByCell.cell[row][col].linesList.indices) {
                        print(" (")
                        for (c in winLinesByCell.cell[row][col].linesList[l].cellList.indices) {
                            print("[${winLinesByCell.cell[row][col].linesList[l].cellList[c].row},")
                            print("${winLinesByCell.cell[row][col].linesList[l].cellList[c].col}]")
                        }
                        print(") ")
                    }
                    println("}")
                }
            }
            println()
        }
    }

}

// Board class (to store all plays)
class Board () {
    var cell = Array(NUM_ROWS) { Array(NUM_COLS) { 0 } }
}

// Board cell (row,col)
class BoardCell (var row:Int = 0, var col:Int = 0) {
}

// List of board cells, (e.g used to determine winning cells by line)
class ListOfCells () {
    var cellList = mutableListOf<BoardCell>()
}

// List of lines, (e.g used to determine winning lines)
class ListOfLines() {
    var linesList = mutableListOf<ListOfCells>()
}

// List of winning lines by cell
class WinLinesByCell {
    var cell : Array<Array<ListOfLines>> = Array (NUM_ROWS) { Array (NUM_COLS) { ListOfLines()} }
}
