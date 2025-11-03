package com.example.hitlist

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

class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val dbHelper: TaskDatabaseHelper,
    private val taskDetailLauncher: ActivityResultLauncher<Intent>
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val colorMap = mapOf(
        "#FFFFFF" to "#F0F0F0",
        "#FFCDD2" to "#E57373",
        "#BBDEFB" to "#64B5F6",
        "#FFCCBC" to "#FF8A65",
        "#FFF9C4" to "#FFF176",
        "#C8E6C9" to "#81C784"
    )

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.taskTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.taskDescription)
        val deadlineTextView: TextView = itemView.findViewById(R.id.taskDeadline)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        val taskCard: CardView = itemView.findViewById(R.id.taskCard)
        val titleBar: LinearLayout = itemView.findViewById(R.id.titleBar)
        // --- FIX 1: Add a reference to the new ImageView from the layout ---
        val taskImagePreview: ImageView = itemView.findViewById(R.id.taskImagePreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.titleTextView.text = task.title
        holder.descriptionTextView.text = task.description

        if (!task.deadline.isNullOrEmpty()) {
            holder.deadlineTextView.visibility = View.VISIBLE
            holder.deadlineTextView.text = "Deadline: ${task.deadline}"
        } else {
            holder.deadlineTextView.visibility = View.GONE
        }

        val lightColorString = task.color ?: "#FFFFFF"
        val darkColorString = colorMap[lightColorString] ?: lightColorString
        try {
            holder.taskCard.setCardBackgroundColor(Color.parseColor(lightColorString))
            holder.titleBar.background = ColorDrawable(Color.parseColor(darkColorString))
        } catch (e: IllegalArgumentException) {
            holder.taskCard.setCardBackgroundColor(Color.WHITE)
            holder.titleBar.setBackgroundColor(Color.parseColor("#F0F0F0"))
        }

        // --- FIX 2: Add logic to show or hide the image preview ---
        if (task.imageUri != null) {
            // If the task has an image URI, make the preview visible
            holder.taskImagePreview.visibility = View.VISIBLE
            // Use Glide to load the image from the URI into the ImageView
            Glide.with(holder.itemView.context)
                .load(Uri.parse(task.imageUri))
                .into(holder.taskImagePreview)
        } else {
            // If there's no image URI, make sure the preview is hidden
            holder.taskImagePreview.visibility = View.GONE
        }

        holder.deleteButton.setOnClickListener {
            val taskToDelete = tasks[position]
            dbHelper.deleteTask(taskToDelete.id)
            tasks.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, tasks.size)
        }

        holder.itemView.setOnClickListener { view ->
            val intent = Intent(view.context, TaskDetailActivity::class.java)
            intent.putExtra("EXTRA_TASK", task)
            taskDetailLauncher.launch(intent)
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks.toMutableList()
        notifyDataSetChanged()
    }
}
