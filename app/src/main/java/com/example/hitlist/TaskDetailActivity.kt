package com.example.hitlist

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.io.Serializable
import java.util.Calendar

class TaskDetailActivity : AppCompatActivity() {

    // UI Views
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var discardButton: Button
    private lateinit var deleteButton: Button
    private lateinit var setDeadlineButton: Button
    private lateinit var stickyNoteCard: CardView

    // Data and State
    private lateinit var dbHelper: TaskDatabaseHelper
    private var existingTask: Task? = null
    private var isEditMode = false
    private var selectedColor: String = "#FFFFFF"
    private var selectedDeadline: String? = null

    // Map to get the dark color for the sticky note's title bar
    private val colorMap = mapOf(
        "#FFFFFF" to "#F0F0F0",
        "#FFCDD2" to "#E57373",
        "#BBDEFB" to "#64B5F6",
        "#FFCCBC" to "#FF8A65",
        "#FFF9C4" to "#FFF176",
        "#C8E6C9" to "#81C784"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        dbHelper = TaskDatabaseHelper(this)

        // Initialize all views
        titleEditText = findViewById(R.id.taskTitleEditText)
        descriptionEditText = findViewById(R.id.taskDescriptionEditText)
        saveButton = findViewById(R.id.saveButton)
        discardButton = findViewById(R.id.discardButton)
        deleteButton = findViewById(R.id.deleteButton)
        setDeadlineButton = findViewById(R.id.setDeadlineButton)
        stickyNoteCard = findViewById(R.id.stickyNoteCard)

        // Determine if we are creating or editing a task
        if (intent.hasExtra("EXTRA_TASK")) {
            isEditMode = true
            existingTask = getSerializable(intent, "EXTRA_TASK", Task::class.java)
        }

        // Configure the screen based on the mode
        if (isEditMode) {
            setupEditMode()
        } else {
            setupCreateMode()
        }

        setupClickListeners()

        // --- NEW: Add back press handling ---
        setupBackPressHandling()
    }

    // --- NEW: Method to handle the system back button and show a confirmation dialog ---
    private fun setupBackPressHandling() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    // --- NEW: Confirmation dialog logic ---
    private fun showExitConfirmationDialog() {
        val message = if (isEditMode) "Are you sure you want to discard your changes?" else "Are you sure you want to discard this new task?"
        AlertDialog.Builder(this)
            .setTitle("Discard Changes?")
            .setMessage(message)
            .setPositiveButton("Discard") { _, _ ->
                // To safely finish the activity, we call the original back press behavior
                finish()
            }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    private fun setupEditMode() {
        findViewById<TextView>(R.id.screenHeader).text = "Edit Your Task"
        saveButton.text = "Update"
        discardButton.text = "Cancel" // "Cancel" can make more sense than "Discard" when editing
        deleteButton.visibility = View.VISIBLE

        existingTask?.let { task ->
            titleEditText.setText(task.title)
            descriptionEditText.setText(task.description)
            selectedColor = task.color ?: "#FFFFFF"
            selectedDeadline = task.deadline
            updateDeadlineDisplay()
            updateNoteColors()
        }
    }

    private fun setupCreateMode() {
        findViewById<TextView>(R.id.screenHeader).text = "Define a new task to execute"
        saveButton.text = "Save Task"
        deleteButton.visibility = View.GONE
        updateNoteColors()
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener { saveOrUpdateTask() }
        // The discard button should show the same confirmation as the back button
        discardButton.setOnClickListener { showExitConfirmationDialog() }
        deleteButton.setOnClickListener { deleteTask() }
        setDeadlineButton.setOnClickListener { showDatePickerDialog() }

        findViewById<View>(R.id.colorDefault).setOnClickListener { onColorSelected("#FFFFFF") }
        findViewById<View>(R.id.colorRed).setOnClickListener { onColorSelected("#FFCDD2") }
        findViewById<View>(R.id.colorBlue).setOnClickListener { onColorSelected("#BBDEFB") }
        findViewById<View>(R.id.colorOrange).setOnClickListener { onColorSelected("#FFCCBC") }
        findViewById<View>(R.id.colorYellow).setOnClickListener { onColorSelected("#FFF9C4") }
        findViewById<View>(R.id.colorGreen).setOnClickListener { onColorSelected("#C8E6C9") }
    }

    private fun onColorSelected(colorHex: String) {
        selectedColor = colorHex
        updateNoteColors()
    }

    private fun updateNoteColors() {
        val lightColor = Color.parseColor(selectedColor)
        val darkColorString = colorMap[selectedColor] ?: selectedColor
        val darkColor = Color.parseColor(darkColorString)
        stickyNoteCard.setCardBackgroundColor(lightColor)
        titleEditText.setBackgroundColor(darkColor)
    }

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
                deadline = selectedDeadline
            )
            dbHelper.updateTask(updatedTask)
        } else {
            dbHelper.addTask(taskTitle, taskDescription, selectedColor, selectedDeadline)
        }

        setResult(RESULT_OK)
        finish()
    }

    private fun deleteTask() {
        existingTask?.let {
            dbHelper.deleteTask(it.id)
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            selectedDeadline = "$day/${month + 1}/$year"
            updateDeadlineDisplay()
        },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDeadlineDisplay() {
        setDeadlineButton.text = if (selectedDeadline != null) "Deadline: $selectedDeadline" else "Set Deadline"
    }

    private fun <T : Serializable?> getSerializable(intent: Intent, key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(key) as? T
        }
    }
}
