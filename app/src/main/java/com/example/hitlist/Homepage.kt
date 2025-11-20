// The 'package' declaration organizes your code into a logical structure.
package com.example.hitlist

// Import necessary classes from the Android SDK and other libraries.
import android.app.Activity
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.sqrt

/**
 * The Homepage activity serves as the main screen of the application.
 * It implements SensorEventListener to detect shake gestures.
 */
class Homepage : AppCompatActivity(), SensorEventListener {

    private lateinit var db: TaskDatabaseHelper
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var searchEditText: EditText

    // --- SENSOR-RELATED PROPERTIES ---
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Shake detection variables
    private var acceleration = 0f
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var lastAcceleration = SensorManager.GRAVITY_EARTH
    private val shakeThreshold = 5f

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

        // --- INITIALIZE THE SENSOR MANAGER & SENSORS ---
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        // Get the default accelerometer
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Log if accelerometer is not found
        if (accelerometer == null) { Log.e("SensorError", "This device does not have an accelerometer.") }

        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(mutableListOf(), db, taskDetailResultLauncher)
        tasksRecyclerView.adapter = taskAdapter

        loadTasks()

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

    // --- SENSOR LISTENER METHODS ---

    override fun onSensorChanged(event: SensorEvent?) {
        // Ensure the event is not null and is specifically from the accelerometer.
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration
            currentAcceleration = sqrt(x * x + y * y + z * z)
            val delta = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta

            if (acceleration > shakeThreshold) {
                Log.d("ShakeDebug", "Shake Detected! Launching New Task screen.")
                val intent = Intent(this, TaskDetailActivity::class.java)
                taskDetailResultLauncher.launch(intent)
                acceleration = 0f // Reset after shake
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used.
    }

    // --- REGISTER AND UNREGISTER THE LISTENER ---

    override fun onResume() {
        super.onResume()
        // Register the accelerometer listener.
        accelerometer?.also { acc ->
            sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the listener to save battery.
        sensorManager.unregisterListener(this)
    }
}
