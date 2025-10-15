package com.example.hitlist

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar

class NewTask : AppCompatActivity() {

    private var selectedColorName: String = "blue" // Save color name as a String
    private var selectedColorInt: Int = 0          // Integer value for UI tinting
    private var previewColorInt: Int? = null       // Temporary press-preview color
    private lateinit var dbHelper: TaskDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_task)

        dbHelper = TaskDatabaseHelper(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.display_date)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Views
        val fabBack: FloatingActionButton = findViewById(R.id.fab3)
        val fabDone: FloatingActionButton = findViewById(R.id.fab4)
        val calendarIcon: ImageView = findViewById(R.id.calendarIcon)
        val buttonNext: Button = findViewById(R.id.buttonNext)
        val dateShow: TextView = findViewById(R.id.dateShow)
        val inputTask: EditText = findViewById(R.id.input_task)
        val pickColor: TextView = findViewById(R.id.pickColor)
        val descriptionBox: EditText = findViewById(R.id.description_box)
        val colorRow: View = findViewById(R.id.colorRow)
        val colorGroup: MaterialButtonToggleGroup = findViewById(R.id.colorGroup)

        // Default Colors for Selection
        selectedColorInt = ContextCompat.getColor(this, R.color.task_blue)
        selectedColorName = "blue"

        // --- Back button (to Homepage) ---
        fabBack.setOnClickListener {
            startActivity(Intent(this, Homepage::class.java))
            overridePendingTransition(R.anim.enter_left, R.anim.leave_right)
        }

        // Reveal colors, description and calendar upon entered task name
        buttonNext.setOnClickListener {
            val title = inputTask.text.toString().trim()
            if (title.isEmpty()) {
                inputTask.error = "Title is required"
                Toast.makeText(this, "Please enter a task first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Visibility toggle
            showView(colorRow)
            showView(pickColor)
            showView(descriptionBox)
            showView(calendarIcon)

            // Default to blue as original
            if (colorGroup.checkedButtonId == View.NO_ID) {
                val def = findViewById<MaterialButton>(R.id.colorBlue)
                colorGroup.check(def.id)
                applyFieldTint(inputTask, descriptionBox, selectedColorInt)
            }
        }

        // Press to preview each of the colors of the tasks
        setPreviewTouchHandlers(colorGroup, inputTask, descriptionBox)

        // Commit color on preview selection
        colorGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            val checkedButton = group.findViewById<MaterialButton>(checkedId)
            // Get color name from the button's tag
            selectedColorName = checkedButton.tag.toString()
            // Get integer color for the UI from the button's background
            selectedColorInt = checkedButton.backgroundTintList?.defaultColor ?: selectedColorInt

            applyFieldTint(inputTask, descriptionBox, selectedColorInt)
        }


        // Select calendar and show the date based on the selection
        calendarIcon.setOnClickListener {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                this,
                { _, y, m, d ->
                    dateShow.text = "Deadline: $d/${m + 1}/$y"
                    showView(dateShow)
                },
                year, month, day
            ).show()
        }

        // Done/save (validate title, carry color)
        fabDone.setOnClickListener {
            val title = inputTask.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                inputTask.requestFocus()
                return@setOnClickListener
            }

            // Create and show the AlertDialog
            AlertDialog.Builder(this)
                .setTitle("Save Task")
                .setMessage("Would you like to save this task?")
                .setPositiveButton("Save") { _, _ ->
                    // User clicks "Save"
                    val description = descriptionBox.text.toString().trim()
                    val deadline = dateShow.text.toString()

                    // Save the data entered to the database
                    dbHelper.addTask(title, description, selectedColorName, deadline)
                    Toast.makeText(this, "Task Inserted!", Toast.LENGTH_SHORT).show()

                    val result = Intent().apply {
                        putExtra("title", title)
                        putExtra("description", description)
                        putExtra("deadlineText", deadline)
                        putExtra("colorName", selectedColorName) // Send back the color name
                    }

                    setResult(RESULT_OK, result)
                    finish()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    // User clicks "Cancel"
                    dialog.dismiss()
                }
                .show()
        }
    }

    // Helpers for showing previews, visibility, and color tint

    private fun showView(v: View) {
        if (v.visibility != View.VISIBLE) {
            v.alpha = 0f
            v.visibility = View.VISIBLE
            v.animate().alpha(1f).setDuration(160L).start()
        }
    }

    private fun setPreviewTouchHandlers(
        group: MaterialButtonToggleGroup,
        title: EditText,
        desc: EditText
    ) {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i) as? MaterialButton ?: continue
            child.setOnTouchListener { v, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val btn = v as MaterialButton
                        previewColorInt = btn.backgroundTintList?.defaultColor
                        previewColorInt?.let { applyFieldTint(title, desc, it) }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // revert to committed color
                        applyFieldTint(title, desc, selectedColorInt)
                        previewColorInt = null
                    }
                }
                // return false so regular click/selection still works
                false
            }
        }
    }

    private fun applyFieldTint(title: EditText, desc: EditText, color: Int) {
        val bg = ColorUtils.setAlphaComponent(color, (255 * 0.5f).toInt())
        title.setBackgroundColor(bg)
        desc.setBackgroundColor(bg)
        ViewCompat.setBackgroundTintList(title, android.content.res.ColorStateList.valueOf(color))
        ViewCompat.setBackgroundTintList(desc, android.content.res.ColorStateList.valueOf(color))
    }
}
