// The 'package' declaration organizes your code into a logical structure.
package com.example.hitlist

// Import necessary classes from the Android SDK and other libraries.
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

/**
 * The Homepage activity serves as the main screen of the application.
 * It displays a list of all saved tasks and provides functionality for searching,
 * creating new tasks, and navigating to the detail view for editing.
 */
class Homepage : AppCompatActivity() {

    // 'private lateinit' properties are initialized later, typically in onCreate.
    // This avoids making them nullable.

    // A reference to the database helper class to interact with the SQLite database.
    private lateinit var db: TaskDatabaseHelper
    // The adapter that manages the data and provides views for the RecyclerView.
    private lateinit var taskAdapter: TaskAdapter
    // The UI element that displays the scrollable list of tasks.
    private lateinit var tasksRecyclerView: RecyclerView
    // The input field for filtering tasks by title.
    private lateinit var searchEditText: EditText

    /**
     * The ActivityResultLauncher is the modern and recommended way to handle results from activities
     * that you start. When TaskDetailActivity finishes, this launcher receives the result.
     * If the result is RESULT_OK, it means a task was created, updated, or deleted,
     * so we need to refresh the list of tasks on this screen.
     */
    private val taskDetailResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Check if the activity that finished (TaskDetailActivity) returned a successful result.
        if (result.resultCode == Activity.RESULT_OK) {
            // If so, reload all tasks from the database to reflect the changes.
            loadTasks()
        }
    }

    /**
     * The onCreate method is the entry point for the activity's lifecycle.
     * It's where you perform one-time initializations, such as setting up the UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the XML layout file for this activity.
        setContentView(R.layout.activity_homepage)

        // Initialize the database helper.
        db = TaskDatabaseHelper(this)
        // Find and assign UI components from the layout file to their corresponding variables.
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
        searchEditText = findViewById(R.id.searchView)
        val addNewTaskButton: Button = findViewById(R.id.addNewTaskButton)

        // Set the layout manager for the RecyclerView. LinearLayoutManager arranges items in a vertical list.
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the TaskAdapter with an empty list, the db helper, and the result launcher.
        // The adapter will be populated with data later in loadTasks().
        taskAdapter = TaskAdapter(mutableListOf(), db, taskDetailResultLauncher)
        // Connect the RecyclerView with its adapter.
        tasksRecyclerView.adapter = taskAdapter

        // Perform the initial load of tasks from the database.
        loadTasks()

        // Set up the click listener for the "Add New Task" button.
        addNewTaskButton.setOnClickListener {
            // Create an intent to open the TaskDetailActivity for creating a *new* task.
            val intent = Intent(this, TaskDetailActivity::class.java)
            // Launch the activity using our result launcher to handle the outcome.
            taskDetailResultLauncher.launch(intent)
        }

        // Set up the search functionality.
        setupSearch()
    }

    /**
     * Fetches all tasks from the database and tells the adapter to update the RecyclerView.
     */
    private fun loadTasks() {
        val allTasks = db.getAllTasks()

        // DEBUG: print what we really got back from the DB
        allTasks.forEachIndexed { i, t ->
            android.util.Log.d(
                "HITLIST/DB",
                "[$i] id=${t.id} title='${t.title}' desc='${t.description}' deadline='${t.deadline}' image=${t.imageUri}"
            )
        }

        taskAdapter.updateTasks(allTasks)
    }


    /**
     * Sets up a listener on the search EditText to filter the task list in real-time as the user types.
     */
    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            // These methods are required by the interface but are not needed for this implementation.
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            /**
             * This method is called after the text in the EditText has changed.
             */
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                // Perform a search in the database using the current query text.
                val filteredTasks = db.searchTasks(query)
                // Update the adapter with the filtered list of tasks.
                taskAdapter.updateTasks(filteredTasks)
            }
        })
    }
}
