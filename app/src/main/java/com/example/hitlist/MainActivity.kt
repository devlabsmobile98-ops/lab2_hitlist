// The 'package' declaration organizes your code into a logical structure.
package com.example.hitlist

// Import necessary classes from the Android SDK.
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * MainActivity serves as the splash screen or initial entry point of the application.
 * Its primary purpose is to display a welcome screen and provide a button to navigate
 * to the main part of the app (the Homepage).
 */
class MainActivity : AppCompatActivity() {

    /**
     * The onCreate method is the entry point for the activity's lifecycle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() allows the app to draw behind the system bars (status bar, navigation bar)
        // for a more modern, immersive look. This is part of modern Android UI design.
        enableEdgeToEdge()
        // Set the XML layout file for this activity.
        setContentView(R.layout.activity_main)

        // This listener helps adjust the padding of the main view ('display_date' seems to be the root layout's ID)
        // to prevent UI elements from being hidden behind the system bars.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.display_date)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the ImageButton from the layout that will trigger navigation to the homepage.
        val imageButton5: ImageButton = findViewById(R.id.imageButton5)

        // Set a click listener on the button.
        imageButton5.setOnClickListener {
            // Create an Intent, which is a message to the Android system to start another component.
            // In this case, we're asking to start the Homepage activity.
            val intent = Intent(this, Homepage::class.java)
            // Execute the intent to start the new activity.
            startActivity(intent)
            // overridePendingTransition specifies custom animations for the activity transition.
            // R.anim.enter_right: The new activity (Homepage) slides in from the right.
            // R.anim.leave_left: The current activity (MainActivity) slides out to the left.
            overridePendingTransition(R.anim.enter_right, R.anim.leave_left)
        }
    }
}
