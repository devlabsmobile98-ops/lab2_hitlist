package com.example.hitlist

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TaskDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "TaskDatabase.db"
        // --- FIX 1: Increment the database version ---
        private const val DATABASE_VERSION = 5
        private const val TABLE_NAME = "Tasks"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_COLOR = "color"
        private const val COLUMN_DEADLINE = "deadline"
        // --- FIX 2: Add the new column name ---
        private const val COLUMN_IMAGE_URI = "image_uri"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // --- FIX 3: Add the new column to the table creation query ---
        val createTableQuery = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_TITLE TEXT NOT NULL," +
                "$COLUMN_DESCRIPTION TEXT," +
                "$COLUMN_COLOR TEXT," +
                "$COLUMN_DEADLINE TEXT," +
                "$COLUMN_IMAGE_URI TEXT)" // Note the comma on the previous line
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // This simple upgrade policy deletes the old table and creates a new one.
        // This is fine for development but not for a production app with user data.
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // --- FIX 4: Update addTask to accept the image URI ---
    fun addTask(title: String, description: String, color: String, deadline: String?, imageUri: String?): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_COLOR, color)
            put(COLUMN_DEADLINE, deadline)
            put(COLUMN_IMAGE_URI, imageUri)
        }
        val result = db.insert(TABLE_NAME, null, values)
        db.close()
        return result
    }

    // --- FIX 5: Update getAllTasks to retrieve the image URI ---
    fun getAllTasks(): List<Task> {
        val taskList = mutableListOf<Task>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC", null)

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

    // --- FIX 6: Update searchTasks to retrieve the image URI ---
    fun searchTasks(query: String): List<Task> {
        val taskList = mutableListOf<Task>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE $COLUMN_TITLE LIKE ? ORDER BY $COLUMN_ID DESC",
            arrayOf("%$query%")
        )

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

    fun deleteTask(taskId: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(taskId.toString()))
        db.close()
    }

    // --- FIX 7: Update updateTask to handle the image URI ---
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
