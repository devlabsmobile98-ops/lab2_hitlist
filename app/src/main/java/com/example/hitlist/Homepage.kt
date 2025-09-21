package com.example.hitlist

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Homepage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_homepage)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.display_date)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back Button to Launcher Page
        val fab: FloatingActionButton = findViewById(R.id.fab)

        fab.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.enter_left, R.anim.leave_right)
        }

        // Add Task Button to New Task Page
        val fab2: FloatingActionButton = findViewById(R.id.fab2)

        fab2.setOnClickListener {
            val intent = Intent(this, NewTask::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.enter_right, R.anim.leave_left)
        }

    }
}