package edu.alonso.dotty

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import edu.alonso.dotty.DotsView.DotsGridListener
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val dotsGame = DotsGame.getInstance()
    private lateinit var dotsView: DotsView
    private lateinit var movesRemainingTextView: TextView
    private lateinit var scoreTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        movesRemainingTextView = findViewById(R.id.moves_remaining_text_view)
        scoreTextView = findViewById(R.id.score_text_view)
        dotsView = findViewById(R.id.dots_view)

        findViewById<Button>(R.id.new_game_button).setOnClickListener { newGameClick() }

        dotsView.setGridListener(gridListener)

        startNewGame()
    }

    private val gridListener = object : DotsGridListener {
        override fun onDotSelected(dot: Dot, status: DotSelectionStatus) {
            // Ignore selections when game is over
            if (dotsGame.isGameOver) return

            // Add/remove dot to/from selected dots
            val addStatus = dotsGame.processDot(dot)

            // If done selecting dots then replace selected dots and display new moves and score
            if (status === DotSelectionStatus.Last) {
                if (dotsGame.selectedDots.size > 1) {
                    dotsGame.finishMove()
                    updateMovesAndScore()
                } else {
                    dotsGame.clearSelectedDots()
                }
            }

            // Display changes to the game
            dotsView.invalidate()
        }
    }

    private fun newGameClick() {
        startNewGame()
    }

    private fun startNewGame() {
        dotsGame.newGame()
        dotsView.invalidate()
        updateMovesAndScore()
    }

    private fun updateMovesAndScore() {
        movesRemainingTextView.text = String.format(Locale.getDefault(), "%d", dotsGame.movesLeft)
        scoreTextView.text = String.format(Locale.getDefault(), "%d", dotsGame.score)
    }
}

