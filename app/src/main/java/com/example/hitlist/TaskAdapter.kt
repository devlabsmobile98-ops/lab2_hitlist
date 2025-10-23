// The 'package' declaration.
package com.example.hitlist

// Import statements for necessary classes.
import android.graphics.Color // Used to parse color hex strings.
import android.graphics.drawable.ColorDrawable // Used to set a solid color as a background.
import android.view.LayoutInflater // Used to inflate an XML layout file into its corresponding View objects.
import android.view.View // The base class for all UI components.
import android.view.ViewGroup // A view that can contain other views. RecyclerView is a ViewGroup.
import android.widget.ImageButton // The UI component for the delete button.
import android.widget.LinearLayout // The layout used for the title bar.
import android.widget.TextView // The UI component for displaying text.
import androidx.cardview.widget.CardView // The root view for each task item.
import androidx.recyclerview.widget.RecyclerView // The base classes for the adapter and ViewHolder.

/**
 * TaskAdapter is responsible for adapting a list of Task objects to be displayed in a RecyclerView.
 * It manages the creation of views and the binding of task data to those views.
 * @param tasks A mutable list of tasks to be displayed.
 * @param dbHelper A reference to the TaskDatabaseHelper, needed to perform delete operations.
 */
class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val dbHelper: TaskDatabaseHelper
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    /**
     * A map to translate the light color (stored in the database) to its corresponding dark shade.
     * This is necessary to color the title bar on the homepage consistently with the NewTask screen.
     * This map should be kept in sync with the one in NewTask.kt.
     */
    private val colorMap = mapOf(
        "#FFFFFF" to "#F0F0F0",  // Default
        "#FFCDD2" to "#E57373",  // Red
        "#BBDEFB" to "#64B5F6",  // Blue
        "#FFCCBC" to "#FF8A65",  // Orange
        "#FFF9C4" to "#FFF176",  // Yellow
        "#C8E6C9" to "#81C784"   // Green
    )

    /**
     * The ViewHolder class holds references to the UI views for a single item in the list.
     * By caching these views, the RecyclerView avoids repeatedly calling findViewById(), which improves performance.
     * It inherits from RecyclerView.ViewHolder.
     * @param itemView The root view of the single item layout (in this case, the CardView from item_task.xml).
     */
    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Find and cache the views within the item layout. This is done only once per ViewHolder creation.
        val titleTextView: TextView = itemView.findViewById(R.id.taskTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.taskDescription)
        val deadlineTextView: TextView = itemView.findViewById(R.id.taskDeadline)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        val taskCard: CardView = itemView.findViewById(R.id.taskCard)
        val titleBar: LinearLayout = itemView.findViewById(R.id.titleBar)
    }

    /**
     * Called by the RecyclerView when it needs a new ViewHolder to represent an item.
     * This is where we inflate the XML layout for a single task item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        // LayoutInflater is used to create View objects from an XML layout file.
        // We inflate 'item_task.xml', which defines the appearance of a single row.
        // 'parent' is the RecyclerView itself. 'false' means do not attach to root yet; the RecyclerView will do it.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        // Create and return a new TaskViewHolder instance, passing the inflated view.
        return TaskViewHolder(view)
    }

    /**
     * Called by the RecyclerView to display the data at a specific position.
     * This method is where we bind the data from our `Task` object to the views in the `TaskViewHolder`.
     * @param holder The ViewHolder for the current item.
     * @param position The position of the item in the data set.
     */
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        // Get the Task object for the current position from our list.
        val task = tasks[position]

        // --- Data Binding ---
        // Set the text of the TextViews using the data from the Task object.
        holder.titleTextView.text = task.title
        holder.descriptionTextView.text = task.description

        // Check if a deadline exists.
        if (!task.deadline.isNullOrEmpty()) {
            // If it exists, make the deadline TextView visible and set its text.
            holder.deadlineTextView.visibility = View.VISIBLE
            holder.deadlineTextView.text = "Deadline: ${task.deadline}"
        } else {
            // If there's no deadline, hide the TextView so it doesn't take up space.
            holder.deadlineTextView.visibility = View.GONE
        }

        // --- Color Logic ---
        // Get the light color from the task. Use a default of white if it's null.
        val lightColorString = task.color ?: "#FFFFFF"
        // Look up the corresponding dark color from our map. If not found, use the light color as a fallback.
        val darkColorString = colorMap[lightColorString] ?: lightColorString

        // A 'try-catch' block is used for safety. Color.parseColor() can crash if the string is not a valid color.
        try {
            // Set the background of the entire card to the light color.
            holder.taskCard.setCardBackgroundColor(Color.parseColor(lightColorString))
            // Set the background of the title bar layout to the dark color.
            holder.titleBar.background = ColorDrawable(Color.parseColor(darkColorString))
        } catch (e: IllegalArgumentException) {
            // If an error occurs (e.g., invalid color string), fall back to default white/grey colors.
            holder.taskCard.setCardBackgroundColor(Color.WHITE)
            holder.titleBar.setBackgroundColor(Color.parseColor("#F0F0F0"))
        }

        // --- Listener for Deletion ---
        // Set a click listener on the delete button for this specific item.
        holder.deleteButton.setOnClickListener {
            // 1. Delete the task from the database using its unique ID.
            dbHelper.deleteTask(task.id)
            // 2. Remove the task from the local list that the adapter is using.
            tasks.removeAt(position)
            // 3. Notify the adapter that an item has been removed. This triggers a smooth removal animation.
            notifyItemRemoved(position)
            // 4. Notify the adapter that the positions of subsequent items have changed. This prevents potential crashes from index mismatches.
            notifyItemRangeChanged(position, tasks.size)
        }
    }

    /**
     * Returns the total number of items in the data set.
     * The RecyclerView calls this to know how many items it needs to display.
     */
    override fun getItemCount(): Int = tasks.size

    /**
     * A public method to update the entire list of tasks in the adapter.
     * This is called from the Homepage to refresh the data (e.g., after loading or searching).
     * @param newTasks The new list of tasks to display.
     */
    fun updateTasks(newTasks: List<Task>) {
        // Replace the old list with the new one. We convert it to a mutable list to allow for removals.
        tasks = newTasks.toMutableList()
        // Notify the adapter that the entire data set has changed. This causes the RecyclerView to redraw itself completely.
        // While less efficient than item-specific notifications, it's simple and effective for a full refresh.
        notifyDataSetChanged()
    }
}
