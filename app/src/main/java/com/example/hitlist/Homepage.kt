package com.example.hitlist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Homepage : AppCompatActivity() {

    private lateinit var db: TaskDatabaseHelper
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var searchEditText: EditText

    private val taskDetailResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadTasks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        db = TaskDatabaseHelper(this)
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
        searchEditText = findViewById(R.id.searchView)
        val addNewTaskButton: Button = findViewById(R.id.addNewTaskButton)

        tasksRecyclerView.layoutManager = LinearLayoutManager(this)

        // MODIFIED: Initialize adapter with the new launcher
        taskAdapter = TaskAdapter(mutableListOf(), db, taskDetailResultLauncher)
        tasksRecyclerView.adapter = taskAdapter

        loadTasks()

        // MODIFIED: This now launches the unified TaskDetailActivity
        addNewTaskButton.setOnClickListener {
            val intent = Intent(this, TaskDetailActivity::class.java)
            taskDetailResultLauncher.launch(intent)
        }

        setupSearch()
    }

    private fun loadTasks() {
        val allTasks = db.getAllTasks()
        taskAdapter.updateTasks(allTasks)
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                val filteredTasks = db.searchTasks(query)
                taskAdapter.updateTasks(filteredTasks)
            }
        })
    }
}
