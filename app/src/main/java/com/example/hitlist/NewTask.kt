package com.example.hitlist

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar
import android.app.DatePickerDialog
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class NewTask : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_task)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.display_date)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back Button to Homepage
        val fab3: FloatingActionButton = findViewById(R.id.fab3)
        val calendarIcon: ImageView=findViewById(R.id.calendarIcon)
        val buttonNext: Button=findViewById(R.id.buttonNext)
        val dateShow: TextView=findViewById(R.id.dateShow)
        val input_task: EditText=findViewById(R.id.input_task)
        val description_box: EditText=findViewById(R.id.description_box)

        fab3.setOnClickListener {
            val intent = Intent(this, Homepage::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.enter_left, R.anim.leave_right)
        }

        buttonNext.setOnClickListener {
            val task = input_task.text.toString()

            if (task.isNotEmpty()) {
                description_box.visibility = View.VISIBLE
                description_box.requestFocus()
                calendarIcon.visibility= View.VISIBLE
            } else {
                Toast.makeText(this, "Please enter a task first", Toast.LENGTH_SHORT).show()
            }
            calendarIcon.setOnClickListener {
                val cal = Calendar.getInstance()
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH)
                val day = cal.get(Calendar.DAY_OF_MONTH)


                val datePicker = DatePickerDialog(
                    this,
                    { _, selectedYear, selectedMonth, selectedDay ->
                        val date = "Date selected: $selectedDay/${selectedMonth + 1}/$selectedYear"
                        dateShow.text = date
                        dateShow.visibility = View.VISIBLE
                    },
                    year,
                    month,
                    day
                )
                datePicker.show()

        }

    }
        }}

