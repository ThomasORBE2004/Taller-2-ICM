package com.example.taller2icm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ContactsActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_READ_CONTACTS = 100
    var mProjection: Array<String>? = null
    var mCursor: Cursor? = null
    var mContactsAdapter: ContactsAdapter? = null
    var mlista: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        mlista = findViewById<ListView>(R.id.contactsList)
        mProjection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )
        mContactsAdapter = ContactsAdapter(this, null, 0)
        mlista?.adapter = mContactsAdapter

        // Comenzamos solicitando el permiso
        checkPermissionsAndLoadContacts()
    }

    private fun checkPermissionsAndLoadContacts() {
        // Si el permiso ya está concedido, cargamos los contactos
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            loadContacts()
        } else {
            // Si no, solicitamos el permiso
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_CONTACTS), PERMISSIONS_REQUEST_READ_CONTACTS)
        }
    }

    // Este método es llamado cuando el usuario responde a la solicitud de permiso
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permiso concedido, cargamos los contactos
                loadContacts()
            } else {
                // Permiso denegado, mostramos un mensaje
                Toast.makeText(this, "Permiso de contactos denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadContacts() {
        mCursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI, mProjection, null, null, null
        )

        if (mCursor != null && mCursor!!.count > 0) {
            mContactsAdapter?.changeCursor(mCursor)
        } else {
            Toast.makeText(this, "No hay contactos disponibles", Toast.LENGTH_SHORT).show()
        }
    }
}
