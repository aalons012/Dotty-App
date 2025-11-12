// Defines the package name for the application's source code.
package edu.alonso.dotty

// BY Andy Alonso on 11/3/25

// Imports Android animation classes for creating visual effects.
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
// Imports the Bundle class, used to pass data between Android components, like saving state.
import android.os.Bundle
// Imports the Button UI widget.
import android.widget.Button
// Imports the TextView UI widget, used to display text.
import android.widget.TextView
// Imports the base class for activities that use the support library's action bar features.
import androidx.appcompat.app.AppCompatActivity
// Imports the listener interface defined inside DotsView to handle grid events.
import edu.alonso.dotty.DotsView.DotsGridListener
// Imports the Locale class to format strings based on the user's device settings.
import java.util.Locale

// Declares the main activity class, which is the entry point of the app, inheriting from AppCompatActivity.
class MainActivity : AppCompatActivity() {

    // Gets the single, shared instance of the DotsGame logic controller.
    private val dotsGame = DotsGame.getInstance()
    // Declares a 'late-initialized' variable for the custom DotsView UI component.
    private lateinit var dotsView: DotsView
    // Declares a 'late-initialized' variable for the TextView that shows the moves left.
    private lateinit var movesRemainingTextView: TextView
    // Declares a 'late-initialized' variable for the TextView that shows the current score.
    private lateinit var scoreTextView: TextView

    // Declares a 'late-initialized' variable to hold the sound effects manager instance.
    private lateinit var soundEffects: SoundEffects


    // This method is called when the Activity is first created. It's where most initialization goes.
    override fun onCreate(savedInstanceState: Bundle?) {
        // Calls the superclass's implementation of onCreate.
        super.onCreate(savedInstanceState)
        // Sets the user interface layout for this activity from the 'activity_main.xml' file.
        setContentView(R.layout.activity_main)

        // Finds the TextView for moves remaining by its ID and assigns it to the variable.
        movesRemainingTextView = findViewById(R.id.moves_remaining_text_view)
        // Finds the TextView for the score by its ID and assigns it to the variable.
        scoreTextView = findViewById(R.id.score_text_view)
        // Finds the custom DotsView by its ID and assigns it to the variable.
        dotsView = findViewById(R.id.dots_view)

        // Finds the "New Game" button and sets a click listener to call the 'newGameClick' function.
        findViewById<Button>(R.id.new_game_button).setOnClickListener { newGameClick() }

        // Sets this MainActivity as the listener for events happening within the DotsView.
        dotsView.setGridListener(gridListener)

        // Gets the singleton instance of the SoundEffects class, passing the application context.
        soundEffects = SoundEffects.getInstance(applicationContext)

        // Calls the function to set up and start the initial state of a new game.
        startNewGame()
    }

    // This method is called just before the activity is destroyed.
    override fun onDestroy() {
        // Calls the superclass's implementation of onDestroy.
        super.onDestroy()
        // Releases the resources used by the SoundPool in the soundEffects class to prevent memory leaks.
        soundEffects.release()
    }

    // Defines an anonymous object that implements the DotsGridListener interface.
    private val gridListener = object : DotsGridListener {
        // This method is called by DotsView whenever a dot is selected.
        override fun onDotSelected(dot: Dot, status: DotSelectionStatus) {
            // Ignores any dot selections if the game is already over.
            if (dotsGame.isGameOver) return

            // Checks if this is the first dot selected in a new chain.
            if (status == DotSelectionStatus.First) {
                // If so, resets the sequence of tones for the new selection.
                soundEffects.resetTones()
            }

            // Asks the game logic to process the selected dot.
            val addStatus = dotsGame.processDot(dot)
            // Checks if the dot was successfully added to the selection.
            if (addStatus == DotStatus.Added){
                // Plays a sound tone for adding a dot.
                soundEffects.playTone(true)
                // Checks if a dot was removed from the selection (backtracking).
            } else if (addStatus == DotStatus.Removed){
                // Plays a different sound tone for removing a dot.
                soundEffects.playTone(false)
            }
            // Checks if the user has lifted their finger, marking the end of the selection.
            if (status === DotSelectionStatus.Last) {
                // Checks if more than one dot was selected (a valid move).
                if (dotsGame.selectedDots.size > 1) {
                    // Triggers the animation for clearing the selected dots.
                    dotsView.animateDots()

                    // These game logic updates are commented out because they must wait for the animation to finish.
                    // dotsGame.finishMove()
                    // updateMovesAndScore()
                } else {
                    // If only one dot was selected, clear the selection without making a move.
                    dotsGame.clearSelectedDots()
                }
            }

            // Triggers a redraw of the DotsView to show the updated selection path.
            dotsView.invalidate()
        }

        // This method is called by DotsView when the animations for clearing dots are finished.
        override fun onAnimationFinished() {
            // Now that the animation is done, finalize the move in the game logic.
            dotsGame.finishMove()
            // Redraw the view to show the new state of the grid.
            dotsView.invalidate()
            // Update the score and moves remaining text views on the screen.
            updateMovesAndScore()
            // Checks if the game is over after the move.
            if(dotsGame.isGameOver){
                // Plays the game over sound effect.
                soundEffects.playGameOver()

            }
        }
    }

    // This function is called when the "New Game" button is clicked.
    private fun newGameClick() {
        // Gets the total height of the screen.
        val screenHeight = this.window.decorView.height.toFloat()
        // Creates an animation to move the DotsView down and off the screen.
        val moveBoardOff = ObjectAnimator.ofFloat(
            dotsView, "translationY", screenHeight)
        // Sets the duration of the animation to 700 milliseconds.
        moveBoardOff.duration = 700
        // Starts the animation.
        moveBoardOff.start()

        // Adds a listener to be notified when the "off-screen" animation ends.
        moveBoardOff.addListener(object : AnimatorListenerAdapter() {
            // This function is called when the animation finishes.
            override fun onAnimationEnd(animation: Animator) {
                // Resets the game logic and UI for a new game.
                startNewGame()

                // Creates an animation to bring the new board from above the screen down to its default position.
                val moveBoardOn = ObjectAnimator.ofFloat(
                    dotsView, "translationY", -screenHeight, 0f)
                // Sets the duration of this animation to 700 milliseconds.
                moveBoardOn.duration = 700
                // Starts the animation.
                moveBoardOn.start()
            }
        })
    }

    // This function sets up the state for a new game.
    private fun startNewGame() {
        // Tells the game logic to reset itself for a new game.
        dotsGame.newGame()
        // Triggers a redraw of the DotsView to display the new grid.
        dotsView.invalidate()
        // Updates the text views to show the starting score and moves.
        updateMovesAndScore()
    }

    // This function updates the text views with the current score and moves remaining.
    private fun updateMovesAndScore() {
        // Sets the text of the moves TextView, formatted as a number for the default locale.
        movesRemainingTextView.text = String.format(Locale.getDefault(), "%d", dotsGame.movesLeft)
        // Sets the text of the score TextView, formatted as a number for the default locale.
        scoreTextView.text = String.format(Locale.getDefault(), "%d", dotsGame.score)
    }
}
