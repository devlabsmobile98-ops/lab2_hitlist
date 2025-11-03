// The 'package' declaration organizes your code into a logical structure.
package com.example.hitlist

// --- Import necessary classes for file handling and the FileProvider ---
import java.io.File
import androidx.core.content.FileProvider

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
    private lateinit var dbHelper: TaskDatabaseHelper
    private var existingTask: Task? = null
    private var isEditMode = false
    private var selectedColor: String = "#FFFFFF"
    private var selectedDeadline: String? = null
    private var selectedImageUri: String? = null
    // A temporary URI to hold the path for the camera to save the new photo.
    private var tempImageUri: Uri? = null

    // --- ActivityResultLaunchers ---

    /**
     * Launcher for picking an image from the device's gallery. This is the modern,
     * recommended way to handle results from other activities.
     */
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // This lambda is executed when the user selects an image (or cancels).
        uri?.let {
            // Take persistent permission to read the URI. This is crucial for long-term access,
            // ensuring the app can still display the image after a device reboot.
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Store the URI as a string and update the ImageView to show the new image.
            selectedImageUri = it.toString()
            updateImageView()
        }
    }

    /**
     * Launcher for capturing a photo using the device's camera.
     */
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        // This lambda executes after the camera app closes. The 'success' boolean indicates
        // if the camera successfully saved a picture to the provided URI.
        if (success) {
            // If a photo was successfully taken, the image data is now at 'tempImageUri'.
            // We set this as our selected URI and update the UI.
            selectedImageUri = tempImageUri.toString()
            updateImageView()
        }
    }

    /**
     * A map to associate a light "note" color with a darker "title bar" color for better UI contrast.
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

        // Initialize the database helper and all UI views.
        dbHelper = TaskDatabaseHelper(this)
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
            existingTask = getSerializable(intent, "EXTRA_TASK", Task::class.java)
        }

        // Configure the UI based on the determined mode.
        if (isEditMode) {
            setupEditMode()
        } else {
            setupCreateMode()
        }

        // Set up all user interaction handlers.
        setupClickListeners()
        setupBackPressHandling()
    }

    /**
     * Centralizes the setup of all OnClickListeners for the buttons on this screen.
     */
    private fun setupClickListeners() {
        saveButton.setOnClickListener { saveOrUpdateTask() }
        discardButton.setOnClickListener { showExitConfirmationDialog() }
        deleteButton.setOnClickListener { deleteTask() }
        setDeadlineButton.setOnClickListener { showDatePickerDialog() }

        // The "Add Image" button now opens a selection dialog.
        addImageButton.setOnClickListener { showImagePickerDialog() }

        // Set up click listeners for all the color selection buttons.
        findViewById<View>(R.id.colorDefault).setOnClickListener { onColorSelected("#FFFFFF") }
        findViewById<View>(R.id.colorRed).setOnClickListener { onColorSelected("#FFCDD2") }
        findViewById<View>(R.id.colorBlue).setOnClickListener { onColorSelected("#BBDEFB") }
        findViewById<View>(R.id.colorOrange).setOnClickListener { onColorSelected("#FFCCBC") }
        findViewById<View>(R.id.colorYellow).setOnClickListener { onColorSelected("#FFF9C4") }
        findViewById<View>(R.id.colorGreen).setOnClickListener { onColorSelected("#C8E6C9") }
    }

    /**
     * Displays an AlertDialog giving the user the choice to either take a photo or choose one from their gallery.
     */
    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Add Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCamera() // "Take Photo" was clicked.
                    1 -> launchGallery() // "Choose from Gallery" was clicked.
                }
            }
            .setNegativeButton("Cancel", null) // A "Cancel" button to dismiss the dialog.
            .show()
    }

    /**
     * Launches the gallery picker using the 'pickImageLauncher'.
     */
    private fun launchGallery() {
        pickImageLauncher.launch("image/*")
    }

    /**
     * Prepares a temporary file and launches the camera app to capture a photo.
     */
    private fun launchCamera() {
        // Create a file in the app's private "images" directory to store the photo.
        val imageFile = File(filesDir, "images/hitlist_capture_${System.currentTimeMillis()}.jpg").apply {
            parentFile?.mkdirs() // Ensure the 'images' directory exists.
        }

        // Generate a secure, shareable content URI for the file using our FileProvider.
        tempImageUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider", // Authority must match AndroidManifest.
            imageFile
        )

        // Launch the camera app, but only if the tempImageUri was successfully created.
        // This safe-call handles the (unlikely) case where getUriForFile returns null.
        tempImageUri?.let { uri ->
            takePictureLauncher.launch(uri)
        }
    }

    // --- The methods below are correct and do not need changes. ---

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
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    /**
     * Configures the UI for editing an existing task by pre-filling all fields.
     */
    private fun setupEditMode() {
        findViewById<TextView>(R.id.screenHeader).text = "Edit Your Task"
        saveButton.text = "Update"
        deleteButton.visibility = View.VISIBLE
        existingTask?.let { task ->
            titleEditText.setText(task.title)
            descriptionEditText.setText(task.description)
            selectedColor = task.color ?: "#FFFFFF"
            selectedDeadline = task.deadline
            selectedImageUri = task.imageUri
            updateDeadlineDisplay()
            updateNoteColors()
            updateImageView()
        }
    }

    /**
     * Configures the UI for creating a new task.
     */
    private fun setupCreateMode() {
        findViewById<TextView>(R.id.screenHeader).text = "Define a new task to execute"
        saveButton.text = "Save Task"
        deleteButton.visibility = View.GONE
        updateNoteColors()
    }

    /**
     * Called when a color button is clicked. Updates the state and refreshes the UI.
     */
    private fun onColorSelected(colorHex: String) {
        selectedColor = colorHex
        updateNoteColors()
    }

    /**
     * Updates the background colors of the UI based on the 'selectedColor'.
     */
    private fun updateNoteColors() {
        val lightColor = Color.parseColor(selectedColor)
        val darkColorString = colorMap[selectedColor] ?: selectedColor
        titleEditText.setBackgroundColor(Color.parseColor(darkColorString))
        stickyNoteCard.setCardBackgroundColor(lightColor)
    }

    /**
     * Handles the logic for saving a new task or updating an existing one in the database.
     */
    private fun saveOrUpdateTask() {
        val taskTitle = titleEditText.text.toString().trim()
        if (taskTitle.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }
        val taskDescription = descriptionEditText.text.toString().trim()
        if (isEditMode) {
            val updatedTask = existingTask!!.copy(
                title = taskTitle,
                description = taskDescription,
                color = selectedColor,
                deadline = selectedDeadline,
                imageUri = selectedImageUri
            )
            dbHelper.updateTask(updatedTask)
        } else {
            dbHelper.addTask(taskTitle, taskDescription, selectedColor, selectedDeadline, selectedImageUri)
        }
        setResult(RESULT_OK)
        finish()
    }

    /**
     * Deletes the current task from the database and closes the activity.
     */
    private fun deleteTask() {
        existingTask?.let {
            dbHelper.deleteTask(it.id)
            setResult(RESULT_OK)
            finish()
        }
    }

    /**
     * Shows a standard Android DatePickerDialog to allow the user to select a date.
     */
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
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
            taskImageView.visibility = View.VISIBLE
            addImageButton.text = "Change Image"
            Glide.with(this)
                .load(Uri.parse(selectedImageUri))
                .into(taskImageView)
        } else {
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
