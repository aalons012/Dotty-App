// Defines the package name, which helps organize the source code.
package edu.alonso.dotty

// Imports the 'abs' function from the Kotlin math library to calculate absolute values.
import kotlin.math.abs
// Imports the 'Random' class from Kotlin's random library to generate random numbers for colors.
import kotlin.random.Random

// Defines a class representing a single dot on the game grid.
// 'row' and 'col' are its coordinates on the grid, initialized to 0.
class Dot(var row: Int = 0, var col: Int = 0) {
    // 'color' holds an integer representing the dot's color, initialized to a random value
    // between 0 and NUM_COLORS - 1.
    var color = Random.nextInt(NUM_COLORS)
    // 'centerX' stores the x-coordinate of the dot's center on the screen (for drawing).
    var centerX = 0f
    // 'centerY' stores the y-coordinate of the dot's center on the screen (for drawing).
    var centerY = 0f
    // 'radius' stores the radius of the dot (for drawing).
    var radius = 1f
    // 'isSelected' is a boolean flag that is true if the dot is part of the user's current selection.
    var isSelected = false

    // A function to assign a new random color to this dot.
    fun setRandomColor() {
        // Sets the 'color' property to a new random integer.
        color = Random.nextInt(NUM_COLORS)
    }

    // A function to check if another dot is directly adjacent (not diagonally).
    fun isAdjacent(dot: Dot): Boolean {
        // Calculates the absolute difference in columns between this dot and the other dot.
        val colDiff = abs(col - dot.col)
        // Calculates the absolute difference in rows between this dot and the other dot.
        val rowDiff = abs(row - dot.row)
        // Dots are adjacent if the sum of row and column differences is exactly 1.
        return colDiff + rowDiff == 1
    }
}

