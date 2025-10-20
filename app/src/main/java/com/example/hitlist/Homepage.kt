package com.example.hitlist

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Homepage : AppCompatActivity() {

    private lateinit var dbHelper: TaskDatabaseHelper
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var searchEditText: EditText
    private lateinit var noTasksText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_homepage)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.display_date)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = TaskDatabaseHelper(this)

        // Initialize views
        val fab: FloatingActionButton = findViewById(R.id.fab)
        val fab2: FloatingActionButton = findViewById(R.id.fab2)
        searchEditText = findViewById(R.id.txt_search)
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)

        // Setup RecyclerView
        setupRecyclerView()

        // Load tasks
        loadTasks()

        // Search functionality
        setupSearch()

        // Back Button to Launcher Page
        fab.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.enter_left, R.anim.leave_right)
        }

        // Add Task Button to New Task Page
        fab2.setOnClickListener {
            val intent = Intent(this, NewTask::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.enter_right, R.anim.leave_left)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh tasks when returning from NewTask activity
        loadTasks()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(emptyList())
        tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@Homepage)
            adapter = taskAdapter
        }
    }

    private fun loadTasks() {
        val tasks = dbHelper.getAllTasks()
        taskAdapter.updateTasks(tasks)

        // Show empty state if no tasks
        if (tasks.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    loadTasks()
                } else {
                    val filteredTasks = dbHelper.searchTasks(query)
                    taskAdapter.updateTasks(filteredTasks)

                    // Show empty search state if no results
                    if (filteredTasks.isEmpty()) {
                        showEmptySearchState()
                    } else {
                        hideEmptyState()
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showEmptyState() {
        // You can add a TextView to show "No tasks yet" message
        // For now, we'll just log it
        println("No tasks available")
    }

    private fun showEmptySearchState() {
        // You can add a TextView to show "No tasks found" message
        println("No tasks found for search")
    }

    private fun hideEmptyState() {
        // Hide any empty state messages
    }
}