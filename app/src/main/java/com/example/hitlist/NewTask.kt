// The 'package' declaration for organizing the codebase.
package com.example.hitlist

// Import statements for necessary classes.
import android.app.DatePickerDialog // NEW: Import for the calendar dialog.
import android.graphics.Color // Used to parse color strings into color integers.
import android.graphics.drawable.ColorDrawable // Used to set a solid color as a background.
import android.os.Bundle // For managing the activity's state.
import android.view.View // The base class for all UI widgets.
import android.widget.* // Imports Button, DatePicker, EditText, ImageButton, and LinearLayout.
import androidx.activity.OnBackPressedCallback // Used to handle back button presses.
import androidx.appcompat.app.AlertDialog // Used to build and show confirmation dialogs.
import androidx.appcompat.app.AppCompatActivity // The base class for the activity.
import androidx.cardview.widget.CardView // The UI component for the floating "sticky note".
import java.util.Calendar // NEW: Used to get the current date for the DatePickerDialog.

class NewTask : AppCompatActivity() {

    // --- Class Properties ---
    private lateinit var db: TaskDatabaseHelper // Manages database operations.
    private lateinit var titleEditText: EditText // Input field for the task's title.
    private lateinit var descriptionEditText: EditText // Input field for the task's description.
    private lateinit var saveTaskButton: Button // The button to save the task.
    private lateinit var stickyNoteCard: CardView // The main card view that represents the sticky note.

    // UPDATED: Removed backButton, clearDeadlineButton, and the inline datePicker.
    private lateinit var setDeadlineButton: Button // Button to show the calendar dialog.
    private lateinit var cancelButton: Button // NEW: The cancel button at the bottom.

    // NEW: This nullable string will hold our selected deadline.
    private var deadline: String? = null

    private lateinit var colorSelectorLayout: LinearLayout
    private var selectedColor: String = "#FFFFFF"
    private var selectedView: View? = null

    /**
     * The onCreate method is called when the activity is first created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_task)

        // --- Initialization ---
        db = TaskDatabaseHelper(this)

        // Find and assign all the UI elements from the XML layout.
        titleEditText = findViewById(R.id.taskTitleEditText)
        descriptionEditText = findViewById(R.id.taskDescriptionEditText)
        saveTaskButton = findViewById(R.id.saveTaskButton)
        stickyNoteCard = findViewById(R.id.stickyNoteCard)
        colorSelectorLayout = findViewById(R.id.colorSelectorLayout)
        // UPDATED: Find new and updated buttons.
        setDeadlineButton = findViewById(R.id.setDeadlineButton)
        cancelButton = findViewById(R.id.cancelButton)

        // --- Setup Logic ---
        setupColorSelectors()
        setupDeadlineControls() // NEW: Set up listener for the calendar dialog button.
        setupBackPressHandling() // Handles both the new Cancel button and the system back press.

        saveTaskButton.setOnClickListener { saveTask() }
    }

    /**
     * NEW: Sets up the click listener for the "Set Deadline" button to show a DatePickerDialog.
     */
    private fun setupDeadlineControls() {
        setDeadlineButton.setOnClickListener {
            // Get the current date to pre-fill the calendar dialog.
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Create a DatePickerDialog.
            val datePickerDialog = DatePickerDialog(
                this,
                // This is the listener that gets called when the user clicks "OK".
                { _, selectedYear, selectedMonth, selectedDay ->
                    // The month is 0-indexed, so we add 1 for proper formatting.
                    val formattedDate = "${selectedMonth + 1}/$selectedDay/$selectedYear"
                    // Store the selected date in our 'deadline' variable.
                    deadline = formattedDate
                    // Update the button text to show the selected date, providing user feedback.
                    setDeadlineButton.text = "Deadline: $formattedDate"
                },
                year, month, day
            )
            // Show the dialog.
            datePickerDialog.show()
        }
    }

    /**
     * UPDATED: Centralizes the setup for handling back navigation and cancellation.
     */
    private fun setupBackPressHandling() {
        // Set a listener for our new "Cancel" button.
        cancelButton.setOnClickListener {
            showExitConfirmationDialog()
        }

        // This callback handles the system back button press (the navigation bar button or gesture).
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // When the system back button is pressed, show the same confirmation dialog.
                showExitConfirmationDialog()
            }
        }
        // Add the callback to the activity's dispatcher.
        onBackPressedDispatcher.addCallback(this, callback)
    }

    /**
     * Builds and displays an AlertDialog to confirm if the user wants to discard changes.
     */
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Discard Changes?")
            .setMessage("Are you sure you want to discard this new task and return to the homepage?")
            .setPositiveButton("Discard") { _, _ ->
                finish() // Finishes the activity, returning to the homepage without saving.
            }
            .setNegativeButton("Keep Editing", null) // Updated text for clarity. 'null' dismisses the dialog.
            .show()
    }

    /**
     * Configures the click listeners for each color selector ImageButton.
     */
    private fun setupColorSelectors() {
        val colorMap = mapOf(
            findViewById<ImageButton>(R.id.colorDefault) to Pair("#FFFFFF", "#F0F0F0"),
            findViewById<ImageButton>(R.id.colorRed) to Pair("#FFCDD2", "#E57373"),
            findViewById<ImageButton>(R.id.colorBlue) to Pair("#BBDEFB", "#64B5F6"),
            findViewById<ImageButton>(R.id.colorOrange) to Pair("#FFCCBC", "#FF8A65"),
            findViewById<ImageButton>(R.id.colorYellow) to Pair("#FFF9C4", "#FFF176"),
            findViewById<ImageButton>(R.id.colorGreen) to Pair("#C8E6C9", "#81C784")
        )

        for ((view, colors) in colorMap) {
            view.setOnClickListener {
                selectedView?.isSelected = false
                it.isSelected = true
                selectedView = it
                updateNoteColor(colors.first, colors.second)
            }
        }
        findViewById<ImageButton>(R.id.colorDefault).performClick()
    }

    /**
     * Updates the background colors of the sticky note card and its title bar.
     */
    private fun updateNoteColor(lightColor: String, darkColor: String) {
        selectedColor = lightColor
        stickyNoteCard.setCardBackgroundColor(Color.parseColor(lightColor))
        titleEditText.background = ColorDrawable(Color.parseColor(darkColor))
    }

    /**
     * Gathers all user input, validates it, and saves the new task to the database.
     */
    private fun saveTask() {
        val title = titleEditText.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }

        val description = descriptionEditText.text.toString().trim()

        // --- Database Operation ---
        // Pass the nullable 'deadline' variable directly to the addTask method.
        // We need to ensure the `addTask` method in the database helper can handle a nullable String.
        db.addTask(title, description, selectedColor, deadline)

        // --- Finish Activity ---
        setResult(RESULT_OK) // Signal to the Homepage that the operation was successful.
        finish() // Close this activity.
    }
}
