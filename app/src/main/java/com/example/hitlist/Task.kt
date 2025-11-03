// The 'package' declaration for organizing the codebase.
package com.example.hitlist

/**
 * The 'data class' is a special type of class in Kotlin designed specifically for holding data.
 * When you declare a class as 'data', the Kotlin compiler automatically generates several
 * useful functions for you, such as:
 * - .equals(): For comparing the contents of two instances.
 * - .hashCode(): For use in hash-based collections like HashMap.
 * - .toString(): To get a human-readable string representation of the object's properties.
 * - .copy(): To create a new instance with some properties modified.
 *
 * This 'Task' class serves as a blueprint or model for what constitutes a single task
 * in your application. Each instance of this class will represent one task item.
 *
 * @property id The unique identifier for the task. This is crucial for database operations
 *              like updating or deleting a specific task. It's a Long because database IDs
 *              can become very large.
 * @property title The main title or name of the task (e.g., "Finish report").
 * @property description A more detailed description of what the task involves.
 * @property color A string representing the background color of the task's sticky note.
 *                 This is stored as a hex string (e.g., "#FFCDD2") for easy parsing.
 * @property deadline A string representing the due date for the task (e.g., "12/25/2025").
 */
data class Task(
    val id: Long,
    val title: String,
    val description: String,
    val color: String?, // The '?' makes this property nullable, meaning it can hold a null value if no color is set.
    val deadline: String?, // This is also nullable in case a task is created without a deadline.
    val imageUri: String? // to hold the image data
) : java.io.Serializable // This allows us to pass Task objects between activities
