// Defines the package name for the application's source code.
package edu.alonso.dotty

// Imports the Context class, which provides access to application-specific resources and classes.
import android.content.Context
// Imports the SoundPool class for playing short audio clips.
import android.media.SoundPool
// Imports the AudioAttributes class to describe the type of audio being played.
import android.media.AudioAttributes

// Declares a singleton class to manage all sound effects for the game.
// The 'private constructor' ensures only one instance of this class can be created.
class SoundEffects private constructor(context: Context){

    // Declares a nullable SoundPool object, which will manage and play the sounds.
    private var soundPool: SoundPool? = null
    // Creates a mutable list to store the integer IDs of the loaded 'select' sound effects.
    private val selectSoundIds = mutableListOf<Int>()
    // Declares a variable to keep track of the current sound to play in the sequence.
    private var soundIndex = 0
    // Declares a variable to store the integer ID of the 'game over' sound effect.
    private var endGameSoundId = 0

    // A 'companion object' is used to create static-like members, essential for the singleton pattern.
    companion object {
        // Declares a private, nullable property to hold the single instance of SoundEffects.
        private var instance: SoundEffects? = null

        // Defines the public method to get the singleton instance of the SoundEffects class.
        fun getInstance(context: Context): SoundEffects {
            // Checks if the instance has been created yet.
            if (instance == null) {
                // If not, creates a new SoundEffects object and assigns it to the 'instance' property.
                instance = SoundEffects(context)
            }
            // Returns the existing or newly created instance. The '!!' asserts that it's not null here.
            return instance!!
        }
    }

    // The 'init' block is a constructor-like block that runs when the instance is first created.
    init {
        // Creates an AudioAttributes object to specify that the sounds are for a game.
        val attributes = AudioAttributes.Builder()
            // Sets the usage type to USAGE_GAME, which is appropriate for game audio.
            .setUsage(AudioAttributes.USAGE_GAME)
            // Sets the content type to CONTENT_TYPE_SONIFICATION for sounds triggered by user interaction.
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            // Builds the AudioAttributes object.
            .build()

        // Creates a new SoundPool instance using a modern builder pattern.
        soundPool = SoundPool.Builder()
            // Applies the audio attributes defined above to the SoundPool.
            .setAudioAttributes(attributes)
            // Builds the SoundPool object.
            .build()

        // Safely executes the following block only if 'soundPool' was successfully created (is not null).
        soundPool?.let {
            // Loads the 'note_e' sound file and adds its ID to the list of selection sounds.
            selectSoundIds.add(it.load(context, R.raw.note_e, 1))
            // Loads the 'note_f' sound file and adds its ID to the list.
            selectSoundIds.add(it.load(context, R.raw.note_f, 1))
            // Loads the 'note_f_sharp' sound file and adds its ID to the list.
            selectSoundIds.add(it.load(context, R.raw.note_f_sharp, 1))
            // Loads the 'note_g' sound file and adds its ID to the list.
            selectSoundIds.add(it.load(context, R.raw.note_g, 1))

            // Loads the 'game_over' sound file and stores its ID in the 'endGameSoundId' variable.
            endGameSoundId = it.load(context, R.raw.game_over, 1)
        }

        // Calls the 'resetTones' function to initialize the sound index.
        resetTones()
    }

    // Defines a function to reset the sequence of selection sounds.
    fun resetTones() {
        // Sets the sound index to -1 so that the first call to playTone(true) will start at index 0.
        soundIndex = -1
    }

    // Defines a function to play a selection tone.
    fun playTone(advance: Boolean) {
        // Checks if the tone sequence should advance to the next note.
        if (advance) {
            // Increments the sound index to play the next tone in the sequence.
            soundIndex++
        } else {
            // Decrements the sound index to play the previous tone (for backtracking).
            soundIndex--
        }

        // Checks if the index has gone below the valid range.
        if (soundIndex < 0) {
            // If so, clamps it back to 0 to prevent a crash.
            soundIndex = 0
            // Checks if the index has gone above the valid range.
        } else if (soundIndex >= selectSoundIds.size) {
            // If so, wraps it back to the beginning of the sequence.
            soundIndex = 0
        }

        // Safely plays the sound from the SoundPool using the current sound index.
        // play(soundID, leftVolume, rightVolume, priority, loop, rate)
        soundPool?.play(selectSoundIds[soundIndex], 1f, 1f, 1, 0, 1f)
    }

    // Defines a function to play the game over sound effect.
    fun playGameOver() {
        // Safely plays the 'game over' sound from the SoundPool at half volume.
        soundPool?.play(endGameSoundId, 0.5f, 0.5f, 1, 0, 1f)
    }

    // Defines a function to release SoundPool resources to prevent memory leaks.
    fun release() {
        // Releases all memory and native resources used by the SoundPool.
        soundPool?.release()
        // Sets the soundPool reference to null, allowing the garbage collector to reclaim it.
        soundPool = null
    }
}
