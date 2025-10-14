package com.example.hitlist

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar

class NewTask : AppCompatActivity() {

    private var selectedColor: Int = 0       // committed task color
    private var previewColor: Int? = null    // temporary press-preview color

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_task)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.display_date)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Views ---
        val fabBack: FloatingActionButton = findViewById(R.id.fab3)
        val fabDone: FloatingActionButton = findViewById(R.id.fab4)
        val calendarIcon: ImageView = findViewById(R.id.calendarIcon)
        val buttonNext: Button = findViewById(R.id.buttonNext)
        val dateShow: TextView = findViewById(R.id.dateShow)
        val inputTask: EditText = findViewById(R.id.input_task)
        val descriptionBox: EditText = findViewById(R.id.description_box)

        val colorRow: View = findViewById(R.id.colorRow)
        val colorGroup: MaterialButtonToggleGroup = findViewById(R.id.colorGroup)
        val customColorBtn: MaterialButton = findViewById(R.id.customColorBtn)

        // --- Defaults ---
        selectedColor = ContextCompat.getColor(this, R.color.task_blue)

        // --- Back button (to Homepage) ---
        fabBack.setOnClickListener {
            startActivity(Intent(this, Homepage::class.java))
            overridePendingTransition(R.anim.enter_left, R.anim.leave_right)
        }


        // --- Next: reveal Step 2 (colors + description + calendar) ---
        buttonNext.setOnClickListener {
            val title = inputTask.text.toString().trim()
            if (title.isEmpty()) {
                inputTask.error = "Title is required"
                Toast.makeText(this, "Please enter a task first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // reveal bits
            showView(colorRow)
            showView(descriptionBox)
            showView(calendarIcon)

            // default to blue swatch once visible
            if (colorGroup.checkedButtonId == View.NO_ID) {
                val def = findViewById<MaterialButton>(R.id.colorBlue)
                colorGroup.check(def.id)
                applyFieldTint(inputTask, descriptionBox, selectedColor)
            }
        }

        // --- Press-to-preview on swatches (optional but fun) ---
        setPreviewTouchHandlers(colorGroup, inputTask, descriptionBox)

        // --- Commit color on swatch selection ---
        colorGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val btn = group.findViewById<MaterialButton>(checkedId)
            val resId = btn.tag as? Int
            selectedColor =
                if (resId != null) ContextCompat.getColor(this, resId)
                else btn.backgroundTintList?.defaultColor ?: selectedColor

            applyFieldTint(inputTask, descriptionBox, selectedColor)
        }

        // --- Custom color picker bottom sheet ---
        customColorBtn.setOnClickListener {
            showCustomColorSheet(
                initialColor = selectedColor,
                onApply = { chosen ->
                    selectedColor = chosen
                    // clear swatch selection (custom color)
                    if (colorGroup.checkedButtonId != View.NO_ID) colorGroup.clearChecked()
                    applyFieldTint(inputTask, descriptionBox, selectedColor)
                }
            )
        }

        // --- Calendar picker -> dateShow ---
        calendarIcon.setOnClickListener {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                this,
                { _, y, m, d ->
                    dateShow.text = "Deadline: $d/${m + 1}/$y"
                    dateShow.visibility = View.VISIBLE
                },
                year, month, day
            ).show()
        }

        // --- Done/save (validate title, carry color) ---
        fabDone.setOnClickListener {
            val title = inputTask.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                inputTask.requestFocus()
                return@setOnClickListener
            }

            val result = Intent().apply {
                putExtra("title", title)
                putExtra("description", descriptionBox.text.toString().trim())
                putExtra("deadlineText", dateShow.text.toString())
                putExtra("color", selectedColor) // ★ save picked color
            }
            // Example if returning to previous Activity:
            // setResult(RESULT_OK, result)

            setResult(RESULT_OK, result)  // ✅ sends data back to Homepage
            finish()
        }
    }

    // --- Helpers ---

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
                        val resId = btn.tag as? Int
                        previewColor = if (resId != null)
                            ContextCompat.getColor(this, resId)
                        else
                            btn.backgroundTintList?.defaultColor
                        previewColor?.let { applyFieldTint(title, desc, it) }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // revert to committed color
                        applyFieldTint(title, desc, selectedColor)
                        previewColor = null
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
        // If using default EditText underline, this tints the indicator drawable too:
        ViewCompat.setBackgroundTintList(title, android.content.res.ColorStateList.valueOf(color))
        ViewCompat.setBackgroundTintList(desc, android.content.res.ColorStateList.valueOf(color))
    }


    private fun showCustomColorSheet(
        initialColor: Int,
        onApply: (Int) -> Unit
    ) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_color_picker, null)
        dialog.setContentView(view)

        val previewDot: View = view.findViewById(R.id.previewDot)
        val hexLayout: TextInputLayout = view.findViewById(R.id.hexLayout)
        val hexEdit: TextInputEditText = view.findViewById(R.id.hexEdit)
        val seekR: SeekBar = view.findViewById(R.id.seekR)
        val seekG: SeekBar = view.findViewById(R.id.seekG)
        val seekB: SeekBar = view.findViewById(R.id.seekB)
        val cancelBtn: MaterialButton = view.findViewById(R.id.cancelBtn)
        val applyBtn: MaterialButton = view.findViewById(R.id.applyBtn)

        var r = Color.red(initialColor)
        var g = Color.green(initialColor)
        var b = Color.blue(initialColor)

        fun updatePreviewAndHex() {
            val c = Color.rgb(r, g, b)
            previewDot.setBackgroundColor(c)
            hexLayout.error = null
            val txt = "#%02X%02X%02X".format(r, g, b)
            if (hexEdit.text?.toString() != txt) {
                hexEdit.setText(txt)
                hexEdit.setSelection(txt.length)
            }
        }

        seekR.max = 255; seekG.max = 255; seekB.max = 255
        seekR.progress = r; seekG.progress = g; seekB.progress = b
        updatePreviewAndHex()

        val sbListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                when (seekBar?.id) {
                    R.id.seekR -> r = progress
                    R.id.seekG -> g = progress
                    R.id.seekB -> b = progress
                }
                updatePreviewAndHex()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        seekR.setOnSeekBarChangeListener(sbListener)
        seekG.setOnSeekBarChangeListener(sbListener)
        seekB.setOnSeekBarChangeListener(sbListener)

        hexEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val raw = s?.toString()?.trim().orEmpty()
                val cleaned = raw.removePrefix("#")
                if (cleaned.length == 6 || cleaned.length == 8) {
                    try {
                        val color = Color.parseColor("#$cleaned")
                        r = Color.red(color); g = Color.green(color); b = Color.blue(color)
                        if (seekR.progress != r) seekR.progress = r
                        if (seekG.progress != g) seekG.progress = g
                        if (seekB.progress != b) seekB.progress = b
                        hexLayout.error = null
                    } catch (_: IllegalArgumentException) {
                        hexLayout.error = "Invalid color"
                    }
                } else if (cleaned.isNotEmpty()) {
                    hexLayout.error = "Use 6 or 8 hex digits"
                } else {
                    hexLayout.error = null
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        cancelBtn.setOnClickListener { dialog.dismiss() }
        applyBtn.setOnClickListener {
            val color = Color.rgb(r, g, b)
            onApply(color)
            dialog.dismiss()
        }

        dialog.show()
    }
}
