// Defines the package name for the application's source code.
package edu.alonso.dotty

// Imports Java's utility classes, including the `Random` class needed for color generation.
import java.util.*

// Constant defining the number of different colors the dots can have.
const val NUM_COLORS = 5
// Constant defining the size of the game grid (6x6).
const val GRID_SIZE = 6
// Constant for the initial number of moves the player starts with.
const val INIT_MOVES = 10

// Defines an enumeration to represent the possible outcomes when a player tries to select a dot.
enum class DotStatus {
    Added, // The dot was successfully added to the current selection.
    Rejected, // The dot could not be added (e.g., wrong color or not adjacent).
    Removed // The dot was removed from the selection (the player is backtracking).
}

// A singleton class that manages the entire state and logic of the Dots game.
// The constructor is private to ensure only one instance of the game can be created.
class DotsGame private constructor() {

    // Tracks the number of moves remaining for the player.
    var movesLeft = 0
    // Tracks the player's current score.
    var score = 0

    // Creates a 2D grid to hold the Dot objects, representing the game board.
    private val dotGrid = MutableList(GRID_SIZE) { MutableList(GRID_SIZE) { Dot() } }
    // A list to keep track of the dots the user has currently selected in a single move.
    private val selectedDotList = mutableListOf<Dot>()

    // A computed property that returns true if the game is over (when no moves are left).
    val isGameOver: Boolean
        get() = movesLeft == 0

    // Provides a read-only (unmodifiable) view of the currently selected dots to prevent external changes.
    val selectedDots: List<Dot>
        get() = Collections.unmodifiableList(selectedDotList)

    // A computed property to get the last dot that was added to the selection.
    val lastSelectedDot: Dot?
        get() {
            // Returns null if no dots have been selected yet.
            return if (selectedDotList.isEmpty()) {
                null
            }
            // Otherwise, returns the last element in the selection list.
            else {
                selectedDotList[selectedDotList.size - 1]
            }
        }

    // A computed property that identifies the lowest selected dot in each column.
    val lowestSelectedDots: List<Dot>
        get() {
            // This is used to create the animation of dots falling from above.
            val dotList = mutableListOf<Dot>()
            // Iterate through each column of the grid.
            for (col in 0 until GRID_SIZE) {
                // Iterate through each row in that column, from the bottom up.
                for (row in GRID_SIZE - 1 downTo 0) {
                    // If the dot at this position is selected...
                    if (dotGrid[row][col].isSelected) {
                        // ...add it to the list and stop searching this column.
                        dotList.add(dotGrid[row][col])
                        break
                    }
                }
            }
            // Return the list of the lowest selected dots.
            return dotList
        }

    // A companion object is used here to implement the singleton pattern.
    companion object {
        // This property will hold the single, private instance of the DotsGame.
        private var instance: DotsGame? = null

        // This is the public method to access the singleton instance of the game.
        fun getInstance(): DotsGame {
            // If an instance doesn't exist yet, create one.
            if (instance == null) {
                instance = DotsGame()
            }
            // Return the existing or newly created instance (the `!!` asserts it's not null).
            return instance!!
        }
    }

    // The `init` block is executed when the DotsGame instance is first created.
    init {
        // Loop through every position in the grid.
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                // Initialize each Dot object with its row and column coordinates.
                dotGrid[row][col].row = row
                dotGrid[row][col].col = col
            }
        }
    }

    // Safely retrieves a dot from the grid at a specific row and column.
    fun getDot(row: Int, col: Int): Dot? {
        // Check if the requested coordinates are outside the grid boundaries.
        return if (row >= GRID_SIZE || row < 0 || col >= GRID_SIZE || col < 0) {
            null // Return null if out of bounds.
        } else {
            dotGrid[row][col] // Otherwise, return the dot at the specified position.
        }
    }

    // Resets the game state to start a new game.
    fun newGame() {
        // Resets the score to zero.
        score = 0
        // Resets the number of moves to the initial value.
        movesLeft = INIT_MOVES
        // Loop through the entire grid.
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                // Assign a new random color to every dot.
                dotGrid[row][col].setRandomColor()
            }
        }
    }

    // Processes a user's attempt to select a dot and determines the result.
    fun processDot(dot: Dot): DotStatus {
        // Assume the selection will be rejected by default.
        var status = DotStatus.Rejected

        // Check if this is the very first dot being selected in a chain.
        if (selectedDotList.isEmpty()) {
            selectedDotList.add(dot) // Add it to the selection list.
            dot.isSelected = true // Mark the dot as selected.
            status = DotStatus.Added // Set the status to Added.
        } else if (!dot.isSelected) {
            // If the dot isn't already selected, check if it's a valid next move.
            val lastDot: Dot? = this.lastSelectedDot
            // It's valid if it has the same color as the last dot AND is adjacent to it.
            if (lastDot?.color == dot.color && lastDot.isAdjacent(dot)) {
                selectedDotList.add(dot) // Add it to the selection list.
                dot.isSelected = true // Mark it as selected.
                status = DotStatus.Added // Set status to Added.
            }
        } else if (selectedDotList.size > 1) {
            // If the dot is already selected, check if the user is backtracking.
            // Backtracking means selecting the second-to-last dot in the chain.
            val secondLast = selectedDotList[selectedDotList.size - 2]
            if (secondLast == dot) {
                // If backtracking, remove the last dot from the selection.
                val removedDot = selectedDotList.removeAt(selectedDotList.size - 1)
                removedDot.isSelected = false // Unmark the removed dot as selected.
                status = DotStatus.Removed // Set the status to Removed.
            }
        }
        // Return the final status of the operation (Added, Rejected, or Removed).
        return status
    }

    // Clears the list of currently selected dots, typically after a move is finished or cancelled.
    fun clearSelectedDots() {

        // Iterate through each dot in the selection list and unmark it.
        selectedDotList.forEach { it.isSelected = false }

        // Clear all items from the selection list.
        selectedDotList.clear()
    }

    // Finalizes a move, updates the grid, score, and remaining moves.
    fun finishMove() {
        // A valid move requires at least two dots to be selected.
        if (selectedDotList.size > 1) {

            // Sort the selected dots by row to process them from the top of the grid down.
            val sortedDotList = selectedDotList.sortedWith(compareBy { it.row })

            // For each removed dot, cause the dots above it in the same column to fall.
            for (dot in sortedDotList) {
                // Iterate from the removed dot's row up to the top of the column.
                for (row in dot.row downTo 1) {
                    val dotCurrent = dotGrid[row][dot.col]
                    val dotAbove = dotGrid[row - 1][dot.col]
                    // The current dot takes on the color of the dot directly above it.
                    dotCurrent.color = dotAbove.color
                }

                // A new, randomly colored dot appears at the top of the column.
                val topDot = dotGrid[0][dot.col]
                topDot.setRandomColor()
            }

            // Increase the score by the number of dots that were cleared.
            score += selectedDotList.size
            // Decrement the number of moves left.
            movesLeft--
            // Clear the selection list to prepare for the next move.
            clearSelectedDots()
        }
    }
}
