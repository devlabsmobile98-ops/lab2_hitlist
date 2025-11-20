// The 'package' declaration organizes your code into a logical structure.
package com.example.hitlist

// Import necessary classes from the Android SDK and other libraries.
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

/**
 * The TaskAdapter is responsible for taking the list of 'Task' objects and adapting them
 * into views that can be displayed within the RecyclerView on the Homepage.
 *
 * @param tasks The mutable list of tasks to be displayed.
 * @param dbHelper A reference to the database helper for performing delete operations.
 * @param taskDetailLauncher A launcher to start the TaskDetailActivity for editing.
 */
class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val dbHelper: TaskDatabaseHelper,
    private val taskDetailLauncher: ActivityResultLauncher<Intent>
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    /**
     * A map to associate a light "note" color with a darker "title bar" color for UI contrast.
     */
    private val colorMap = mapOf(
        "#FFFFFF" to "#F0F0F0", "#FFCDD2" to "#E57373", "#BBDEFB" to "#64B5F6",
        "#FFCCBC" to "#FF8A65", "#FFF9C4" to "#FFF176", "#C8E6C9" to "#81C784"
    )

    /**
     * The TaskViewHolder represents a single item's view in the RecyclerView.
     * It holds references to the UI elements within the item_task.xml layout,
     * which improves performance by avoiding repeated 'findViewById' calls.
     */
    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.taskTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.taskDescription)
        val deadlineTextView: TextView = itemView.findViewById(R.id.taskDeadline)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        val taskCard: CardView = itemView.findViewById(R.id.taskCard)
        val titleBar: LinearLayout = itemView.findViewById(R.id.titleBar)
        val taskImagePreview: ImageView = itemView.findViewById(R.id.taskImagePreview)
    }

    /**
     * Called by the RecyclerView when it needs a new ViewHolder to represent an item.
     * This is where you inflate the layout for a single list item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        // Inflate the item_task.xml layout file.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        // Create and return a new TaskViewHolder instance with the inflated view.
        return TaskViewHolder(view)
    }

    /**
     * Called by the RecyclerView to display the data at a specified position.
     * This method updates the contents of the ViewHolder's views to reflect the item's data.
     * @param holder The ViewHolder which should be updated.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position] // Get the data model for this position.

        // --- Bind Data to Views ---
        holder.titleTextView.text = task.title

        holder.descriptionTextView.apply {
            text = task.description.orEmpty()
            visibility = if (text.isBlank()) View.GONE else View.VISIBLE
        }

        if (!task.deadline.isNullOrEmpty()) {
            holder.deadlineTextView.visibility = View.VISIBLE
            holder.deadlineTextView.text = "Deadline: ${task.deadline}"
        } else {
            holder.deadlineTextView.visibility = View.GONE
        }


        // Set the background colors for the card and title bar.
        val lightColorString = task.color ?: "#FFFFFF"
        val darkColorString = colorMap[lightColorString] ?: lightColorString
        try {
            holder.taskCard.setCardBackgroundColor(Color.parseColor(lightColorString))
            holder.titleBar.background = ColorDrawable(Color.parseColor(darkColorString))
        } catch (e: IllegalArgumentException) {
            // Fallback to default colors if the color string is invalid.
            holder.taskCard.setCardBackgroundColor(Color.WHITE)
            holder.titleBar.setBackgroundColor(Color.parseColor("#F0F0F0"))
        }

        // Show or hide the image preview based on whether an image URI exists.
        if (task.imageUri != null) {
            holder.taskImagePreview.visibility = View.VISIBLE
            // Use Glide to efficiently load the image into the ImageView.
            Glide.with(holder.itemView.context)
                .load(Uri.parse(task.imageUri))
                .into(holder.taskImagePreview)
        } else {
            holder.taskImagePreview.visibility = View.INVISIBLE
        }

        // --- Set Up Click Listeners ---

        // Set the click listener for the delete button.
        holder.deleteButton.setOnClickListener {
            val taskToDelete = tasks[position]
            dbHelper.deleteTask(taskToDelete.id) // Delete from the database.
            tasks.removeAt(position) // Remove from the local list.
            // Notify the adapter that an item was removed for a smooth animation.
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, tasks.size)
        }

        // Set the click listener for the entire card item to open the edit screen.
        holder.itemView.setOnClickListener { view ->
            val intent = Intent(view.context, TaskDetailActivity::class.java)
            // Pass the entire Task object to the detail activity to pre-fill its data.
            intent.putExtra("EXTRA_TASK", task)
            // Launch the activity using the launcher passed from the Homepage.
            taskDetailLauncher.launch(intent)
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     */
    override fun getItemCount(): Int = tasks.size

    /**
     * A public method to update the list of tasks in the adapter and refresh the RecyclerView.
     * @param newTasks The new list of tasks to display.
     */
    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks.toMutableList()
        // notifyDataSetChanged() re-renders the entire list. It's simple but less efficient
        // than more specific notify methods. For this app's scale, it's perfectly fine.
        notifyDataSetChanged()
    }
}
