// Defines the package name, organizing the project's source code files.
package edu.alonso.dotty

// Imports Android classes for creating animations.
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
// Imports an interpolator for animations to start fast and then accelerate.
import android.view.animation.AccelerateInterpolator
// Imports a special annotation to suppress warnings, in this case for touch event handling.
import android.annotation.SuppressLint
// Imports the fundamental context of the application, providing access to resources.
import android.content.Context
// Imports the Canvas class, which is used for drawing 2D graphics.
import android.graphics.Canvas
// Imports the Paint class, which holds style and color information for drawing.
import android.graphics.Paint
// Imports the Path class, used to draw custom shapes, like the connector lines.
import android.graphics.Path
// Imports the AttributeSet, used to get custom attributes from XML layouts.
import android.util.AttributeSet
// Imports the base View class, which is the building block for all UI components.
import android.view.View
// Imports the MotionEvent class to handle touch screen events.
import android.view.MotionEvent
// Imports an interpolator that makes animations "bounce" at the end.
import android.view.animation.BounceInterpolator

// Defines an enumeration to describe the state of a dot selection event.
enum class DotSelectionStatus {
    First,      // The first dot in a new selection chain.
    Additional, // Any additional dot selected after the first one.
    Last        // The final dot selected when the user lifts their finger.
}

// Defines a constant for the default radius of each dot when drawn.
const val DOT_RADIUS = 40f

// Declares the custom view class 'DotsView', inheriting from Android's base 'View' class.
class DotsView(context: Context, attrs: AttributeSet) :
    View(context, attrs) {

    // Defines an interface to communicate events from this view (DotsView) back to the hosting Activity/Fragment.
    interface DotsGridListener {
        // Called when a dot is selected by the user.
        fun onDotSelected(dot: Dot, status: DotSelectionStatus)
        // Called when all animations have finished playing.
        fun onAnimationFinished()
    }

    // Gets the single instance of the DotsGame logic controller.
    private val dotsGame = DotsGame.getInstance()
    // Creates a Path object to draw the connecting lines between selected dots.
    private val dotPath = Path()
    // A nullable property to hold the listener for grid events (the Activity).
    private var gridListener: DotsGridListener? = null
    // Retrieves the array of colors defined in the 'arrays.xml' resource file.
    private val dotColors = resources.getIntArray(R.array.dotColors)
    // A variable to store the calculated width of a single grid cell.
    private var cellWidth = 0
    // A variable to store the calculated height of a single grid cell.
    private var cellHeight = 0
    // Creates a Paint object for drawing the dots, with anti-aliasing for smooth edges.
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Creates a Paint object for drawing the connector path, with anti-aliasing.
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // The 'init' block runs when an instance of DotsView is created.
    init {
        // Sets the thickness of the line used to draw the connector path.
        pathPaint.strokeWidth = 10f
        // Sets the paint style to STROKE, so it draws the outline of the path, not a filled shape.
        pathPaint.style = Paint.Style.STROKE
    }

    // Declares an AnimatorSet, which can be used to play multiple animations together.
    private var animatorSet = AnimatorSet()

    // Defines a function to start the animations for clearing dots and making new ones fall.
    fun animateDots() {

        // Creates a mutable list to hold all the individual animation objects.
        val animationList = mutableListOf<Animator>()

        // Gets the animation for making the selected dots shrink and disappear, and adds it to the list.
        animationList.add(getDisappearingAnimator())

        // Loops through the lowest selected dot in each column to calculate falling animations.
        for (dot in dotsGame.lowestSelectedDots) {
            // Initializes a counter for how many rows a dot needs to fall.
            var rowsToMove = 1
            // Loops from the row just above the cleared dot up to the top of the grid.
            for (row in dot.row - 1 downTo 0) {
                // Gets the dot at the current position in the column.
                val dotToMove = dotsGame.getDot(row, dot.col)
                // Safely unwraps the nullable 'dotToMove' (using 'let').
                dotToMove?.let {
                    // If the dot at this position was also part of the selection...
                    if (it.isSelected) {
                        // ...increment the number of rows subsequent dots need to fall.
                        rowsToMove++
                    } else {
                        // If it's a dot that needs to fall, calculate its target Y-position.
                        val targetY = it.centerY + rowsToMove * cellHeight
                        // Creates a falling animation for this dot and adds it to the list.
                        animationList.add(getFallingAnimator(it, targetY))
                    }
                }
            }
        }

        // Creates a new AnimatorSet to coordinate all the created animations.
        animatorSet = AnimatorSet()
        // Configures the AnimatorSet to play all animations in the list simultaneously.
        animatorSet.playTogether(animationList)
        // Adds a listener to be notified when the entire animation set has finished.
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            // This function is called when the animation sequence ends.
            override fun onAnimationEnd(animation: Animator) {
                // Resets the positions and radii of all dots on the grid.
                resetDots()
                // Notifies the listener (the Activity) that the animation is complete.
                gridListener?.onAnimationFinished()
            }
        })
        // Starts playing the animations.
        animatorSet.start()
    }

    // Creates and returns a ValueAnimator that shrinks the selected dots.
    private fun getDisappearingAnimator(): ValueAnimator {
        // Creates an animator that animates a float value from 1.0 down to 0.0.
        val animator = ValueAnimator.ofFloat(1f, 0f)
        // Sets the duration of the animation to 100 milliseconds.
        animator.duration = 100
        // Sets an interpolator that makes the animation start fast and accelerate.
        animator.interpolator = AccelerateInterpolator()
        // Adds a listener that updates the dot properties on each frame of the animation.
        animator.addUpdateListener { animation: ValueAnimator ->
            // Loops through all currently selected dots.
            for (dot in dotsGame.selectedDots) {
                // Updates the radius of each selected dot based on the current animated value (from 1 to 0).
                dot.radius = DOT_RADIUS * animation.animatedValue as Float
            }
            // Triggers a redraw of the view to show the updated dot radii.
            invalidate()
        }
        // Returns the configured animator.
        return animator
    }

    // This method is called when the size of the view changes, including when it's first laid out.
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        // Calculates the usable width of the board, accounting for padding.
        val boardWidth = width - paddingLeft - paddingRight
        // Calculates the usable height of the board, accounting for padding.
        val boardHeight = height - paddingTop - paddingBottom
        // Calculates the width of a single grid cell.
        cellWidth = boardWidth / GRID_SIZE
        // Calculates the height of a single grid cell.
        cellHeight = boardHeight / GRID_SIZE
        // Calls resetDots to set the initial positions and sizes of all dots.
        resetDots()
    }

    // This method is called whenever the view needs to be redrawn.
    override fun onDraw(canvas: Canvas) {
        // Calls the superclass's onDraw method.
        super.onDraw(canvas)

        // Loops through each row of the game grid.
        for (row in 0 until GRID_SIZE) {
            // Loops through each column of the game grid.
            for (col in 0 until GRID_SIZE) {
                // Safely gets the dot at the current row and column.
                dotsGame.getDot(row, col)?.let {
                    // Sets the paint color to the color of the current dot.
                    dotPaint.color = dotColors[it.color]
                    // Draws the dot on the canvas as a circle at its specified center and radius.
                    canvas.drawCircle(it.centerX, it.centerY, it.radius, dotPaint)
                }
            }
        }
        // Checks if an animation is NOT currently running.
        if (!animatorSet.isRunning) {
            // Gets the list of currently selected dots.
            val selectedDots = dotsGame.selectedDots
            // Checks if there is at least one dot selected.
            if (selectedDots.isNotEmpty()) {
                // Resets the path to clear any previous lines.
                dotPath.reset()
                // Gets the first selected dot.
                var dot = selectedDots[0]
                // Moves the starting point of the path to the center of the first dot.
                dotPath.moveTo(dot.centerX, dot.centerY)
                // Loops through the rest of the selected dots.
                for (i in 1 until selectedDots.size) {
                    // Gets the next dot in the selection.
                    dot = selectedDots[i]
                    // Draws a line from the previous dot's position to the current dot's position.
                    dotPath.lineTo(dot.centerX, dot.centerY)
                }
                // Sets the color of the path to match the color of the selected dots.
                pathPaint.color = dotColors[dot.color]
                // Draws the complete path on the canvas.
                canvas.drawPath(dotPath, pathPaint)
            }
        }
    }

    // Suppresses a lint warning about accessibility for custom touch handling.
    @SuppressLint("ClickableViewAccessibility")
    // This method is called to handle all touch screen events.
    override fun onTouchEvent(event: MotionEvent): Boolean {

        // If there's no listener or an animation is running, ignore the touch event but consume it.
        if (gridListener == null || animatorSet.isRunning) return true

        // Converts the touch event's X-coordinate to a grid column index.
        val col = event.x.toInt() / cellWidth
        // Converts the touch event's Y-coordinate to a grid row index.
        val row = event.y.toInt() / cellHeight
        // Gets the dot object at the calculated row and column.
        var selectedDot = dotsGame.getDot(row, col)

        // If the touch moves outside the grid, it uses the last valid dot.
        if (selectedDot == null) {
            // This prevents the selection chain from breaking if the finger slides off the grid briefly.
            selectedDot = dotsGame.lastSelectedDot
        }

        // If a valid dot is determined (either under the finger or the last one selected)...
        if (selectedDot != null) {
            // ...check the type of touch action.
            when (event.action) {
                // When the user first touches the screen...
                MotionEvent.ACTION_DOWN -> {
                    // ...notify the listener that the first dot has been selected.
                    gridListener!!.onDotSelected(selectedDot, DotSelectionStatus.First)
                }
                // When the user drags their finger across the screen...
                MotionEvent.ACTION_MOVE -> {
                    // ...notify the listener that an additional dot has been selected.
                    gridListener!!.onDotSelected(selectedDot, DotSelectionStatus.Additional)
                }
                // When the user lifts their finger from the screen...
                MotionEvent.ACTION_UP -> {
                    // ...notify the listener that this is the last dot in the selection.
                    gridListener!!.onDotSelected(selectedDot, DotSelectionStatus.Last)
                }
            }
        }

        // Returns true to indicate that the touch event has been handled.
        return true
    }

    // A public method to allow the hosting Activity/Fragment to register itself as a listener.
    fun setGridListener(gridListener: DotsGridListener) {
        // Assigns the provided listener to the local 'gridListener' property.
        this.gridListener = gridListener
    }

    // A private function to reset the visual properties of all dots on the grid.
    private fun resetDots() {
        // Loops through each row of the grid.
        for (row in 0 until GRID_SIZE) {
            // Loops through each column of the grid.
            for (col in 0 until GRID_SIZE) {
                // Safely gets the dot at the current position.
                dotsGame.getDot(row, col)?.let {
                    // Resets the dot's radius to the default full size.
                    it.radius = DOT_RADIUS
                    // Recalculates and sets the dot's horizontal center based on its column and cell width.
                    it.centerX = col * cellWidth + cellWidth / 2f
                    // Recalculates and sets the dot's vertical center based on its row and cell height.
                    it.centerY = row * cellHeight + cellHeight / 2f
                }
            }
        }
    }

    // Creates and returns a ValueAnimator for a single dot falling into place.
    private fun getFallingAnimator(dot: Dot, destinationY: Float): ValueAnimator {
        // Creates an animator that animates a float value from the dot's current Y-position to its target Y-position.
        val animator = ValueAnimator.ofFloat(dot.centerY, destinationY)
        // Sets the duration of the animation to 300 milliseconds.
        animator.duration = 300
        // Sets an interpolator that creates a "bounce" effect at the end of the fall.
        animator.interpolator = BounceInterpolator()
        // Adds a listener that updates the dot's position on each frame of the animation.
        animator.addUpdateListener { animation: ValueAnimator ->
            // Updates the dot's vertical center position based on the current animated value.
            dot.centerY = animation.animatedValue as Float
            // Triggers a redraw of the view to show the dot's new position.
            invalidate()
        }
        // Returns the configured animator.
        return animator
    }
}

