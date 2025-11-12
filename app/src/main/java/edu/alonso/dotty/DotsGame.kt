// Defines the package name for the application's source code.
package edu.alonso.dotty

// Imports Java's utility classes, including the `Random` class needed for color generation.
import java.util.*

// Defines a constant for the number of different colors the dots can have.
const val NUM_COLORS = 5
// Defines a constant for the size of the game grid (in this case, 6x6).
const val GRID_SIZE = 6
// Defines a constant for the initial number of moves a player starts a new game with.
const val INIT_MOVES = 10

// Defines an enumeration to represent the possible outcomes when a player interacts with a dot.
enum class DotStatus {
    Added, // Represents that the dot was successfully added to the current selection.
    Rejected, // Represents that the dot could not be added (e.g., wrong color or not adjacent).
    Removed // Represents that the dot was removed from the selection (i.e., the player is backtracking).
}

// Declares a singleton class named 'DotsGame' to manage the game's state and logic.
// The constructor is 'private' to enforce the singleton pattern, meaning only one instance can exist.
class DotsGame private constructor() {

    // Declares a variable to track the number of moves the player has left.
    var movesLeft = 0
    // Declares a variable to track the player's current score.
    var score = 0

    // Creates a 2D grid (a list of lists) to hold the Dot objects, representing the game board.
    private val dotGrid = MutableList(GRID_SIZE) { MutableList(GRID_SIZE) { Dot() } }
    // Creates a mutable list to keep track of the dots the user has selected in a single, continuous move.
    private val selectedDotList = mutableListOf<Dot>()

    // Defines a computed property 'isGameOver' that returns true if no moves are left.
    val isGameOver: Boolean
        // The 'get' accessor returns the result of the expression 'movesLeft == 0'.
        get() = movesLeft == 0

    // Defines a read-only property 'selectedDots' to safely expose the list of selected dots.
    val selectedDots: List<Dot>
        // The 'get' accessor returns a read-only (unmodifiable) version of the selectedDotList.
        get() = Collections.unmodifiableList(selectedDotList)

    // Defines a computed property 'lastSelectedDot' to get the most recently selected dot.
    val lastSelectedDot: Dot? // The '?' indicates it can be null if no dots are selected.
        get() {
            // Checks if the list of selected dots is empty.
            return if (selectedDotList.isEmpty()) {
                // Returns null if no dots have been selected yet.
                null
            }
            // If the list is not empty...
            else {
                // ...returns the last element in the selection list.
                selectedDotList[selectedDotList.size - 1]
            }
        }

    // Defines a computed property that finds the lowest selected dot in each column.
    val lowestSelectedDots: List<Dot>
        get() {
            // Creates a temporary mutable list to store the lowest dots.
            val dotList = mutableListOf<Dot>()
            // Iterates through each column index of the grid.
            for (col in 0 until GRID_SIZE) {
                // Iterates through each row index in that column, starting from the bottom and moving up.
                for (row in GRID_SIZE - 1 downTo 0) {
                    // Checks if the dot at the current position is marked as selected.
                    if (dotGrid[row][col].isSelected) {
                        // If a selected dot is found, adds it to the list.
                        dotList.add(dotGrid[row][col])
                        // Exits the inner loop ('break') to move to the next column, as we only need the lowest one.
                        break
                    }
                }
            }
            // Returns the final list of the lowest selected dots from each column.
            return dotList
        }

    // A 'companion object' is used to hold static-like members for the class, essential for the singleton pattern.
    companion object {
        // Declares a private, nullable property to hold the single instance of the DotsGame.
        private var instance: DotsGame? = null

        // Defines the public, static-like method to get the singleton instance of the game.
        fun getInstance(): DotsGame {
            // Checks if the instance has been created yet.
            if (instance == null) {
                // If not, creates a new DotsGame object and assigns it to the 'instance' property.
                instance = DotsGame()
            }
            // Returns the existing or newly created instance. The '!!' asserts that 'instance' is not null here.
            return instance!!
        }
    }

    // The 'init' block is a constructor-like block that runs when the DotsGame instance is first created.
    init {
        // Begins a loop to iterate through each row index of the grid.
        for (row in 0 until GRID_SIZE) {
            // Begins a nested loop to iterate through each column index of the grid.
            for (col in 0 until GRID_SIZE) {
                // Accesses the Dot object at the current grid position.
                // and sets its 'row' property to the correct row index.
                dotGrid[row][col].row = row
                // Sets the 'col' property of the same Dot object to the correct column index.
                dotGrid[row][col].col = col
            }
        }
    }

    // Defines a function to safely retrieve a dot from the grid at a specific row and column.
    fun getDot(row: Int, col: Int): Dot? { // Returns a nullable Dot.
        // Checks if the requested row or column is outside the valid grid boundaries.
        return if (row >= GRID_SIZE || row < 0 || col >= GRID_SIZE || col < 0) {
            // Returns null if the coordinates are out of bounds.
            null
        } else {
            // Otherwise, returns the Dot object at the specified position.
            dotGrid[row][col]
        }
    }

    // Defines a function to reset the game to its initial state for a new game.
    fun newGame() {
        // Resets the player's score to zero.
        score = 0
        // Resets the number of moves back to the initial starting value.
        movesLeft = INIT_MOVES
        // Begins a loop to iterate through each row of the grid.
        for (row in 0 until GRID_SIZE) {
            // Begins a nested loop to iterate through each column of the grid.
            for (col in 0 until GRID_SIZE) {
                // Calls the 'setRandomColor' method on each dot to give it a new random color.
                dotGrid[row][col].setRandomColor()
            }
        }
    }

    // Defines a function to process a user's selection of a dot and determine the outcome.
    fun processDot(dot: Dot): DotStatus {
        // Initializes the status to 'Rejected' by default. It will be changed if the selection is valid.
        var status = DotStatus.Rejected

        // Checks if this is the very first dot being selected in a chain (i.e., the selection list is empty).
        if (selectedDotList.isEmpty()) {
            selectedDotList.add(dot) // Adds the dot to the selection list.
            dot.isSelected = true // Marks the dot object's state as selected.
            status = DotStatus.Added // Updates the status to 'Added'.
        } // Checks if the dot is not already selected (to prevent adding it twice).
        else if (!dot.isSelected) {
            // Gets the last dot that was added to the selection list.
            val lastDot: Dot? = this.lastSelectedDot
            // Checks if the new dot has the same color as the last dot AND is physically adjacent to it on the grid.
            if (lastDot?.color == dot.color && lastDot.isAdjacent(dot)) {
                selectedDotList.add(dot) // If valid, adds the dot to the selection list.
                dot.isSelected = true // Marks the dot's state as selected.
                status = DotStatus.Added // Updates the status to 'Added'.
            }
        } // Checks if the selected dot is already in the list and the list has more than one dot.
        else if (selectedDotList.size > 1) {
            // This condition handles backtracking (undoing the last selection).
            // Gets the second-to-last dot from the selection list.
            val secondLast = selectedDotList[selectedDotList.size - 2]
            // Checks if the user is clicking on the second-to-last dot.
            if (secondLast == dot) {
                // If so, removes the last dot from the selection list.
                val removedDot = selectedDotList.removeAt(selectedDotList.size - 1)
                removedDot.isSelected = false // Unmarks the removed dot's state.
                status = DotStatus.Removed // Updates the status to 'Removed'.
            }
        }
        // Returns the final status of the operation (Added, Rejected, or Removed).
        return status
    }

    // Defines a function to clear the list of currently selected dots.
    fun clearSelectedDots() {

        // Iterates through each dot currently in the selection list.
        selectedDotList.forEach { it.isSelected = false }

        // Removes all elements from the selection list, making it empty.
        selectedDotList.clear()
    }

    // Defines a function to finalize a move, updating the grid, score, and moves count.
    fun finishMove() {
        // A move is only valid if more than one dot has been selected.
        if (selectedDotList.size > 1) {

            // Sorts the selected dots by their row number to process them from top to bottom.
            val sortedDotList = selectedDotList.sortedWith(compareBy { it.row })

            // Iterates through each of the selected (and now sorted) dots.
            for (dot in sortedDotList) {
                // For each cleared dot, this loop makes the dots above it "fall" down.
                // It starts from the cleared dot's row and goes up to the top of the grid.
                for (row in dot.row downTo 1) {
                    // Gets the dot at the current position in the column.
                    val dotCurrent = dotGrid[row][dot.col]
                    // Gets the dot directly above it.
                    val dotAbove = dotGrid[row - 1][dot.col]
                    // The current dot takes on the color of the dot that was above it.
                    dotCurrent.color = dotAbove.color
                }

                // After all dots in the column have "fallen", a new dot is needed at the top.
                // Gets the dot at the very top of the column (row 0).
                val topDot = dotGrid[0][dot.col]
                // Assigns a new random color to this top dot.
                topDot.setRandomColor()
            }

            // Increases the player's score by the number of dots that were cleared.
            score += selectedDotList.size
            // Decrements the number of moves the player has left.
            movesLeft--
            // Clears the selection list to prepare for the next move.
            clearSelectedDots()
        }
    }
}
