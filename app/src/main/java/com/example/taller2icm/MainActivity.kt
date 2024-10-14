package com.example.taller2icm

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setButtons()

    }

    private fun setButtons() {
        val contactsButton = findViewById<ImageButton>(R.id.contactsButton)
        val cameraButton = findViewById<ImageButton>(R.id.cameraButton)
        val mapsButton = findViewById<ImageButton>(R.id.mapsButton)

        contactsButton.setOnClickListener {
            val intentContacts = Intent(this, ContactsActivity::class.java)
            startActivity(intentContacts)
        }

        cameraButton.setOnClickListener {
            val intentCamera = Intent(this, CameraActivity::class.java)
            startActivity(intentCamera)
        }


        mapsButton.setOnClickListener {
            val intentMaps = Intent(this, MapsActivity2::class.java)
            startActivity(intentMaps)
        }
    }
}