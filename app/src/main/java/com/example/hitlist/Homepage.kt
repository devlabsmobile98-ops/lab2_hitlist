// The 'package' declaration specifies the namespace for the code, helping to organize it and avoid naming conflicts.
package com.example.hitlist

// Import statements bring in necessary classes from the Android SDK and other libraries.
import android.app.Activity // Used to check the result code from a launched activity.
import android.content.Intent // Used to create an intent for navigating to another activity (NewTask).
import android.os.Bundle // Used to pass data between states of an activity (e.g., on creation).
import android.text.Editable // Represents the modifiable text from an EditText.
import android.text.TextWatcher // An interface for listening to text changes in an EditText.
import android.widget.Button // The UI element for the "Add New Task" button.
import android.widget.EditText // The UI element for the search bar.
import androidx.activity.result.contract.ActivityResultContracts // A modern API for handling activity results.
import androidx.appcompat.app.AppCompatActivity // The base class for activities that use the AppCompat support library.
import androidx.recyclerview.widget.LinearLayoutManager // Arranges items in the RecyclerView in a vertical list.
import androidx.recyclerview.widget.RecyclerView // The UI component for displaying a scrollable list of tasks.

/**
 * Homepage is the main activity of the application.
 * It displays a list of tasks, allows the user to search for specific tasks,
 * and provides a button to navigate to the NewTask screen to create a new task.
 */
class Homepage : AppCompatActivity() {

    // 'private' means these variables can only be accessed within this class.
    // 'lateinit var' indicates that these variables will be initialized later (in onCreate), not at declaration.

    // A reference to the database helper class, which manages all database interactions (adding, fetching, deleting tasks).
    private lateinit var db: TaskDatabaseHelper

    // The adapter that manages the data (tasks) and binds it to the views in the RecyclerView.
    private lateinit var taskAdapter: TaskAdapter

    // The RecyclerView UI element that will display the list of task items.
    private lateinit var tasksRecyclerView: RecyclerView

    // The EditText UI element that serves as the search input field.
    private lateinit var searchEditText: EditText

    /**
     * This object handles the result returned from the NewTask activity.
     * It uses the modern Activity Result API, which is safer and cleaner than the older onActivityResult method.
     */
    private val newTaskResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // When the NewTask activity finishes, this lambda function is executed.
        // We check if the result code is RESULT_OK, which indicates the user successfully saved a new task.
        if (result.resultCode == Activity.RESULT_OK) {
            // If a new task was saved, we reload all tasks from the database to refresh the list on the homepage.
            loadTasks()
        }
    }

    /**
     * The onCreate method is the entry point for the activity. It is called when the activity is first created.
     * This is where all initialization, such as setting up the UI and listeners, should happen.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Always call the superclass's method first.
        setContentView(R.layout.activity_homepage) // Connects this Kotlin file to its XML layout file.

        // --- Initialization of Properties ---

        // Initialize the database helper, passing the application context.
        db = TaskDatabaseHelper(this)

        // Find and assign the UI elements from the XML layout to our Kotlin variables.
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
        searchEditText = findViewById(R.id.searchView)
        val addNewTaskButton: Button = findViewById(R.id.addNewTaskButton)

        // --- RecyclerView Setup ---

        // Set the LayoutManager, which determines how items are positioned. LinearLayoutManager arranges them in a single vertical list.
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the TaskAdapter. We pass it an empty list to start and the database helper for delete operations.
        taskAdapter = TaskAdapter(mutableListOf(), db)

        // Attach the adapter to the RecyclerView. The RecyclerView will now use the adapter to display data.
        tasksRecyclerView.adapter = taskAdapter

        // --- Initial Data Load ---

        // Load all existing tasks from the database and display them when the app starts.
        loadTasks()

        // --- UI Listeners Setup ---

        // Set a click listener on the "Add New Task" button.
        addNewTaskButton.setOnClickListener {
            // Create an Intent to navigate from the current activity (this) to the NewTask activity.
            val intent = Intent(this, NewTask::class.java)
            // Launch the NewTask activity using the result launcher we defined earlier.
            // This starts the activity and prepares to receive a result back from it.
            newTaskResultLauncher.launch(intent)
        }

        // Set up the search functionality for the search bar.
        setupSearch()
    }

    /**
     * Fetches all tasks from the database and updates the adapter.
     * This function centralizes the logic for refreshing the task list.
     */
    private fun loadTasks() {
        // Retrieve a list of all Task objects from the database.
        val allTasks = db.getAllTasks()
        // Pass the new list of tasks to the adapter, which will then update the RecyclerView.
        taskAdapter.updateTasks(allTasks)
    }

    /**
     * Sets up a TextWatcher on the search EditText to filter tasks in real-time as the user types.
     */
    private fun setupSearch() {
        // Add a TextWatcher to the search EditText to listen for text changes.
        searchEditText.addTextChangedListener(object : TextWatcher {
            // This method is called before the text is changed. We don't need it here.
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            // This method is called as the text is being changed. We don't need it here.
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            /**
             * This method is called immediately after the text has been changed.
             * This is the perfect place to perform the search.
             */
            override fun afterTextChanged(s: Editable?) {
                // Convert the editable text to a string to use as the search query.
                val query = s.toString()
                // Call the database helper's searchTasks method to get a filtered list of tasks.
                val filteredTasks = db.searchTasks(query)
                // Update the adapter with the filtered list, causing the RecyclerView to display only the search results.
                taskAdapter.updateTasks(filteredTasks)
            }
        })
    }
}
