package com.example.taller2icm

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.taller2icm.databinding.ActivityMaps2Binding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.Writer
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class MapsActivity2 : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
        const val RADIUS_OF_EARTH_KM = 6.371
        const val MIN_DISTANCE_METERS = 30 // Mínimo desplazamiento en metros
    }

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMaps2Binding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback

    private var lastLocation: Location? = null
    private var currentMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaps2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()

        // Obtener el fragmento del mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configurar el campo de texto para buscar direcciones
        val editTextAddress = findViewById<EditText>(R.id.texto)
        editTextAddress.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val address = editTextAddress.text.toString()
                searchLocationByAddress(address)
            }
            true
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Habilitar gestos y controles solo después de que el mapa esté listo
        mMap.uiSettings.isZoomGesturesEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true

        // Verificar permisos de ubicación
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Solicitar permisos si no han sido otorgados
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            // Si los permisos ya han sido otorgados, obtener la última ubicación
            mMap.isMyLocationEnabled = true
            mFusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    updateMarker(location)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                }
            }

            // Comenzar a recibir actualizaciones de ubicación
            startLocationUpdates()
        }

        // Manejo del evento LongClick para crear un marcador
        mMap.setOnMapLongClickListener { latLng ->
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val addressText = addresses?.get(0)?.getAddressLine(0) ?: "Sin dirección"

            mMap.addMarker(MarkerOptions().position(latLng).title(addressText))

            // Mostrar la distancia entre el usuario y el nuevo marcador
            if (lastLocation != null) {
                val distanceToMarker = distance(
                    lastLocation!!.latitude,
                    lastLocation!!.longitude,
                    latLng.latitude,
                    latLng.longitude
                )
                Toast.makeText(
                    this,
                    "Distancia al marcador: $distanceToMarker metros",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateMarker(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        currentMarker?.remove() // Quitar marcador anterior
        currentMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Ubicación actual"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = MIN_DISTANCE_METERS.toFloat()
            interval = 60000 // Actualizar ubicación cada minuto
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                null
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    // Función para buscar una dirección usando Geocoder
    private fun searchLocationByAddress(address: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocationName(address, 1)
        if (addresses != null && addresses.isNotEmpty()) {
            val location = addresses[0]
            val latLng = LatLng(location.latitude, location.longitude)
            mMap.addMarker(MarkerOptions().position(latLng).title(address))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        } else {
            Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeJSONObject(location: Location) {
        val localizaciones = readJSONArrayFromFile("locations.json")

        localizaciones.put(
            MyLocation(
                Date(System.currentTimeMillis()),
                location.latitude,
                location.longitude
            ).toJSON()
        )

        val filename = "locations.json"
        try {
            val file = File(baseContext.getExternalFilesDir(null), filename)
            Log.i("LOCATION", "Ubicacion de archivo: $file")
            val output: Writer = BufferedWriter(FileWriter(file))
            output.write(localizaciones.toString())
            output.close()

            Toast.makeText(applicationContext, "Ubicación guardada", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("LOCATION", "Error al guardar la ubicación", e)
        }
    }

    private fun readJSONArrayFromFile(fileName: String): JSONArray {
        val file = File(baseContext.getExternalFilesDir(null), fileName)
        if (!file.exists()) {
            Log.i("LOCATION", "Archivo no encontrado: $file")
            return JSONArray()
        }
        val jsonString = file.readText()
        return JSONArray(jsonString)
    }

    // Calcular distancia entre dos puntos
    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = (sin(latDistance / 2) * sin(latDistance / 2)
                + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2))
                * sin(lngDistance / 2) * sin(lngDistance / 2))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = RADIUS_OF_EARTH_KM * c * 1000 // Convert to meters
        return (distance * 100.0).roundToInt() / 100.0
    }
}
