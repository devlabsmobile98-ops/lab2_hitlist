// The 'package' declaration organizes your code into a logical structure.
package com.example.hitlist

// Import necessary classes from the Android SDK and other libraries.
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import java.io.Serializable
import java.util.Calendar

/**
 * TaskDetailActivity is a multi-purpose screen responsible for both CREATING a new task
 * and UPDATING an existing one. It determines its mode based on whether a 'Task' object
 * is passed to it via an Intent extra.
 */
class TaskDetailActivity : AppCompatActivity() {

    // --- UI View Properties ---
    // 'private lateinit' properties are initialized in onCreate to avoid nullability.
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var discardButton: Button
    private lateinit var deleteButton: Button
    private lateinit var setDeadlineButton: Button
    private lateinit var stickyNoteCard: CardView
    private lateinit var addImageButton: Button
    private lateinit var taskImageView: ImageView

    // --- Data and State Properties ---
    private lateinit var dbHelper: TaskDatabaseHelper // Manages all database interactions.
    private var existingTask: Task? = null // Holds the task being edited. Null if creating a new task.
    private var isEditMode = false // A boolean flag to easily check the current mode.
    private var selectedColor: String = "#FFFFFF" // Holds the currently selected color hex string. Defaults to white.
    private var selectedDeadline: String? = null // Holds the deadline string. Null if not set.
    private var selectedImageUri: String? = null // Holds the string representation of the selected image's URI.

    /**
     * An ActivityResultLauncher for handling the result of picking an image from the gallery.
     * This is the modern, recommended way to handle activity results.
     */
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // This lambda is executed when the user selects an image (or cancels).
        uri?.let {
            // Persist permission to read the URI across device reboots. This is crucial for long-term access.
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Store the URI as a string and update the ImageView to show the new image.
            selectedImageUri = it.toString()
            updateImageView()
        }
    }

    /**
     * A map to associate a light "note" color with a darker "title bar" color for better UI contrast.
     * The key is the light color, and the value is the corresponding dark color.
     */
    private val colorMap = mapOf(
        "#FFFFFF" to "#F0F0F0", "#FFCDD2" to "#E57373", "#BBDEFB" to "#64B5F6",
        "#FFCCBC" to "#FF8A65", "#FFF9C4" to "#FFF176", "#C8E6C9" to "#81C784"
    )

    /**
     * The onCreate method is the entry point for the activity's lifecycle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        // Initialize the database helper.
        dbHelper = TaskDatabaseHelper(this)

        // Find and assign all UI views from the layout file.
        titleEditText = findViewById(R.id.taskTitleEditText)
        descriptionEditText = findViewById(R.id.taskDescriptionEditText)
        saveButton = findViewById(R.id.saveButton)
        discardButton = findViewById(R.id.discardButton)
        deleteButton = findViewById(R.id.deleteButton)
        setDeadlineButton = findViewById(R.id.setDeadlineButton)
        stickyNoteCard = findViewById(R.id.stickyNoteCard)
        addImageButton = findViewById(R.id.addImageButton)
        taskImageView = findViewById(R.id.taskImageView)

        // --- Mode Detection ---
        // Check if the Intent that started this activity contains an "EXTRA_TASK".
        if (intent.hasExtra("EXTRA_TASK")) {
            // If it does, we are in "Edit Mode".
            isEditMode = true
            // Retrieve the Task object from the intent.
            existingTask = getSerializable(intent, "EXTRA_TASK", Task::class.java)
        }

        // Configure the UI based on the determined mode.
        if (isEditMode) {
            setupEditMode()
        } else {
            setupCreateMode()
        }

        // Set up all click listeners for buttons.
        setupClickListeners()
        // Set up custom handling for the system back button.
        setupBackPressHandling()
    }

    /**
     * Intercepts the default back button press to show a confirmation dialog,
     * preventing accidental data loss.
     */
    private fun setupBackPressHandling() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    /**
     * Displays an AlertDialog to confirm if the user wants to discard their changes.
     */
    private fun showExitConfirmationDialog() {
        val message = if (isEditMode) "Discard your changes?" else "Discard this new task?"
        AlertDialog.Builder(this)
            .setTitle("Discard")
            .setMessage(message)
            .setPositiveButton("Discard") { _, _ -> finish() } // Closes the activity.
            .setNegativeButton("Keep Editing", null) // Closes the dialog and does nothing.
            .show()
    }

    /**
     * Configures the UI for editing an existing task. It populates all fields
     * with the data from the 'existingTask' object.
     */
    private fun setupEditMode() {
        findViewById<TextView>(R.id.screenHeader).text = "Edit Your Task"
        saveButton.text = "Update"
        deleteButton.visibility = View.VISIBLE // Show the delete button only in edit mode.

        // Use a safe call '.let' to execute code only if existingTask is not null.
        existingTask?.let { task ->
            // Pre-fill all UI fields with the task's data.
            titleEditText.setText(task.title)
            descriptionEditText.setText(task.description)
            selectedColor = task.color ?: "#FFFFFF" // Use default color if none is set.
            selectedDeadline = task.deadline
            selectedImageUri = task.imageUri
            updateDeadlineDisplay()
            updateNoteColors()
            updateImageView()
        }
    }

    /**
     * Configures the UI for creating a new task. This is the default state.
     */
    private fun setupCreateMode() {
        findViewById<TextView>(R.id.screenHeader).text = "Define a new task to execute"
        saveButton.text = "Save Task"
        deleteButton.visibility = View.GONE // Hide the delete button when creating a new task.
        updateNoteColors() // Set the default note color.
    }

    /**
     * Centralizes the setup of all OnClickListeners for the buttons on this screen.
     */
    private fun setupClickListeners() {
        saveButton.setOnClickListener { saveOrUpdateTask() }
        discardButton.setOnClickListener { showExitConfirmationDialog() }
        deleteButton.setOnClickListener { deleteTask() }
        setDeadlineButton.setOnClickListener { showDatePickerDialog() }
        addImageButton.setOnClickListener { pickImageLauncher.launch("image/*") } // Triggers the image picker.

        // Set up click listeners for all the color selection buttons.
        findViewById<View>(R.id.colorDefault).setOnClickListener { onColorSelected("#FFFFFF") }
        findViewById<View>(R.id.colorRed).setOnClickListener { onColorSelected("#FFCDD2") }
        findViewById<View>(R.id.colorBlue).setOnClickListener { onColorSelected("#BBDEFB") }
        findViewById<View>(R.id.colorOrange).setOnClickListener { onColorSelected("#FFCCBC") }
        findViewById<View>(R.id.colorYellow).setOnClickListener { onColorSelected("#FFF9C4") }
        findViewById<View>(R.id.colorGreen).setOnClickListener { onColorSelected("#C8E6C9") }
    }

    /**
     * Called when a color button is clicked. Updates the state and refreshes the UI.
     * @param colorHex The hex string of the selected color.
     */
    private fun onColorSelected(colorHex: String) {
        selectedColor = colorHex
        updateNoteColors()
    }

    /**
     * Updates the background colors of the title EditText and the main CardView
     * based on the 'selectedColor'.
     */
    private fun updateNoteColors() {
        val lightColor = Color.parseColor(selectedColor)
        val darkColorString = colorMap[selectedColor] ?: selectedColor
        titleEditText.setBackgroundColor(Color.parseColor(darkColorString))
        stickyNoteCard.setCardBackgroundColor(lightColor)
    }

    /**
     * Handles the logic for saving a new task or updating an existing one.
     */
    private fun saveOrUpdateTask() {
        val taskTitle = titleEditText.text.toString().trim()
        // Basic validation to ensure the title is not empty.
        if (taskTitle.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }
        val taskDescription = descriptionEditText.text.toString().trim()

        if (isEditMode) {
            // If in edit mode, create an updated Task object using .copy() and update it in the DB.
            val updatedTask = existingTask!!.copy(
                title = taskTitle,
                description = taskDescription,
                color = selectedColor,
                deadline = selectedDeadline,
                imageUri = selectedImageUri
            )
            dbHelper.updateTask(updatedTask)
        } else {
            // If in create mode, add a new task to the DB.
            dbHelper.addTask(taskTitle, taskDescription, selectedColor, selectedDeadline, selectedImageUri)
        }

        // Set the result to RESULT_OK to notify the Homepage to refresh its list.
        setResult(RESULT_OK)
        // Close this activity and return to the previous screen.
        finish()
    }

    /**
     * Deletes the current task from the database and closes the activity.
     */
    private fun deleteTask() {
        existingTask?.let {
            dbHelper.deleteTask(it.id)
            setResult(RESULT_OK) // Notify Homepage to refresh.
            finish()
        }
    }

    /**
     * Shows a standard Android DatePickerDialog to allow the user to select a date.
     */
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            // Month is 0-indexed, so we add 1 for correct display.
            selectedDeadline = "$day/${month + 1}/$year"
            updateDeadlineDisplay()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    /**
     * Updates the text on the deadline button to reflect the currently selected date.
     */
    private fun updateDeadlineDisplay() {
        setDeadlineButton.text = if (selectedDeadline != null) "Deadline: $selectedDeadline" else "Set Deadline"
    }

    /**
     * Manages the visibility of the ImageView and loads the image using Glide.
     */
    private fun updateImageView() {
        if (selectedImageUri != null) {
            // If an image is selected, show the ImageView and update the button text.
            taskImageView.visibility = View.VISIBLE
            addImageButton.text = "Change Image"
            // Use Glide to efficiently load the image from its URI into the ImageView.
            Glide.with(this)
                .load(Uri.parse(selectedImageUri))
                .into(taskImageView)
        } else {
            // If no image is selected, hide the ImageView and reset the button text.
            taskImageView.visibility = View.GONE
            addImageButton.text = "Add Image"
        }
    }

    /**
     * A helper function to retrieve a Serializable object from an Intent extra in a
     * backward-compatible way, handling the deprecation in Android 13 (Tiramisu).
     */
    private fun <T : Serializable?> getSerializable(intent: Intent, key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(key) as? T
        }
    }
}
