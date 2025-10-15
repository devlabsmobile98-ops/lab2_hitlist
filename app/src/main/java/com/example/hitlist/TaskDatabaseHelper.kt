package com.example.hitlist

import android.content.ContentValues // Import ContentValues for dynamic insertion into SQLite Database
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TaskDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // Use Companion Object to store constants for database
    companion object {
        private const val DATABASE_NAME = "TaskDatabase.db"
        private const val DATABASE_VERSION = 4 // Update database version every time changes to schema are made
        private const val TABLE_NAME = "Tasks"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_COLOR = "color"
        private const val COLUMN_DEADLINE = "deadline"
    }

    // OnCreate function to create the database table for tasks. Title cannot be empty.
    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_TITLE TEXT NOT NULL," +
                "$COLUMN_DESCRIPTION TEXT," +
                "$COLUMN_COLOR TEXT," +
                "$COLUMN_DEADLINE TEXT)"
        db?.execSQL(createTableQuery)
    }

    // OnUpgrade to recreate database if need be. Recreate table if schema changes.
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // AddTask function to insert task name, description, color (as string), and deadline into "Tasks" table
    // Insert from NewTask.
    fun addTask(title: String, description: String, color: String, deadline: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_COLOR, color)
            put(COLUMN_DEADLINE, deadline)
        }
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    // GetTask function to retrieve task details from "Tasks" table.
    // Retrieve for Homepage.
    fun getTask(): List<String> {
        val taskList = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        if (cursor.moveToFirst()) {
            do {
                val titleIndex = cursor.getColumnIndex(COLUMN_TITLE)
                // Check if column exists
                if (titleIndex != -1) {
                    val title = cursor.getString(titleIndex)
                    taskList.add(title)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return taskList
    }
}
