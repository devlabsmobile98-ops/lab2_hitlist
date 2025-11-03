// The 'package' declaration organizes your code into a logical structure.
package com.example.hitlist

// Import necessary classes for SQLite database management.
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * TaskDatabaseHelper is a subclass of SQLiteOpenHelper, which is the standard Android class
 * for managing database creation and version management. This class handles all direct
 * interactions with the SQLite database for the application.
 *
 * @param context The context of the application, used to locate the database.
 */
class TaskDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    /**
     * A companion object holds static properties and methods for the class.
     * These constants define the database schema (table and column names) and version.
     * Keeping them here centralizes database configuration.
     */
    companion object {
        private const val DATABASE_NAME = "TaskDatabase.db" // The name of the database file.
        private const val DATABASE_VERSION = 5 // The version of the database. Increment this on schema changes.
        private const val TABLE_NAME = "Tasks" // The name of our single table.
        private const val COLUMN_ID = "id" // The primary key column.
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_COLOR = "color"
        private const val COLUMN_DEADLINE = "deadline"
        private const val COLUMN_IMAGE_URI = "image_uri" // The column to store the image URI string.
    }

    /**
     * Called the first time the database is created. This is where you should execute
     * the SQL statements to create your tables.
     */
    override fun onCreate(db: SQLiteDatabase?) {
        // The SQL query to create the 'Tasks' table with all its columns.
        val createTableQuery = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_TITLE TEXT NOT NULL," +
                "$COLUMN_DESCRIPTION TEXT," +
                "$COLUMN_COLOR TEXT," +
                "$COLUMN_DEADLINE TEXT," +
                "$COLUMN_IMAGE_URI TEXT)"
        db?.execSQL(createTableQuery)
    }

    /**
     * Called when the database needs to be upgraded, which happens when you increment DATABASE_VERSION.
     * This method defines how to migrate data from the old schema to the new one.
     * For this simple app, we use a destructive policy: drop the old table and create a new one.
     * NOTE: This is NOT suitable for production apps where user data must be preserved.
     */
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    /**
     * Adds a new task to the database.
     * @return The row ID of the newly inserted row, or -1 if an error occurred.
     */
    fun addTask(title: String, description: String, color: String, deadline: String?, imageUri: String?): Long {
        val db = this.writableDatabase
        // ContentValues is a key-value map used to insert or update rows in a database.
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_COLOR, color)
            put(COLUMN_DEADLINE, deadline)
            put(COLUMN_IMAGE_URI, imageUri)
        }
        val result = db.insert(TABLE_NAME, null, values)
        db.close() // It's good practice to close the database connection when done.
        return result
    }

    /**
     * Retrieves all tasks from the database, ordered by ID in descending order (newest first).
     * @return A list of 'Task' objects.
     */
    fun getAllTasks(): List<Task> {
        val taskList = mutableListOf<Task>()
        val db = this.readableDatabase
        // A 'Cursor' is an object that provides random read-write access to the result set of a query.
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC", null)

        // Loop through all rows in the cursor.
        if (cursor.moveToFirst()) {
            do {
                // Extract data from the current row for each column.
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
                val description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION))
                val color = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLOR))
                val deadline = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DEADLINE))
                val imageUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URI))

                // Create a Task object and add it to the list.
                taskList.add(Task(id, title, description, color, deadline, imageUri))
            } while (cursor.moveToNext())
        }
        cursor.close() // Always close the cursor to release its resources.
        db.close()
        return taskList
    }

    /**
     * Searches for tasks where the title matches a given query.
     * @param query The search term to match against task titles.
     * @return A list of matching 'Task' objects.
     */
    fun searchTasks(query: String): List<Task> {
        val taskList = mutableListOf<Task>()
        val db = this.readableDatabase
        // The '?' is a placeholder that will be replaced by the value in the 'arrayOf'.
        // The '%' are wildcards, so this query finds titles containing the query string.
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE $COLUMN_TITLE LIKE ? ORDER BY $COLUMN_ID DESC",
            arrayOf("%$query%")
        )

        // The logic to iterate through the cursor and create Task objects is the same as in getAllTasks.
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
                val description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION))
                val color = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLOR))
                val deadline = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DEADLINE))
                val imageUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URI))

                taskList.add(Task(id, title, description, color, deadline, imageUri))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return taskList
    }

    /**
     * Deletes a task from the database based on its ID.
     */
    fun deleteTask(taskId: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(taskId.toString()))
        db.close()
    }

    /**
     * Updates an existing task in the database.
     * @param task The Task object containing the new data.
     * @return The number of rows affected. Should be 1 if successful.
     */
    fun updateTask(task: Task): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, task.title)
            put(COLUMN_DESCRIPTION, task.description)
            put(COLUMN_COLOR, task.color)
            put(COLUMN_DEADLINE, task.deadline)
            put(COLUMN_IMAGE_URI, task.imageUri)
        }
        val result = db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(task.id.toString()))
        db.close()
        return result
    }
}
