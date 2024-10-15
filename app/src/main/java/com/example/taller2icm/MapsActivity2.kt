package com.example.taller2icm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.Address
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
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.Writer
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class MapsActivity2 : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
        const val RADIUS_OF_EARTH_KM = 6.371
        const val MIN_DISTANCE_METERS = 30 // Mínimo desplazamiento en metros
        const val LUMINOSITY_THRESHOLD = 40 // Límite para cambiar entre modo claro y oscuro
    }

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMaps2Binding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var sensorManager: SensorManager
    private lateinit var geocoder: Geocoder
    private var lightSensor: Sensor? = null
    private var lastLocation: Location? = null
    private var currentMarker: Marker? = null
    private var isLocationUpdateEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaps2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar servicios de ubicación
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()

        // Inicializar el sensor de luz
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // Inicializae el geocodificador
        geocoder = Geocoder(this, Locale.getDefault())

        // Obtener el fragmento del mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configurar el campo de texto para buscar direcciones
        binding.texto.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation()
            }
            true
        }

        // Inicializar el callback de ubicación
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    lastLocation = location
                    updateMarker(location)
                    writeJSONObject(location)
                }
            }
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
            ) == PackageManager.PERMISSION_GRANTED && isLocationUpdateEnabled
        ) {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                null
            )
        }
    }

    // Busca la ubicación en el mapa
    private fun searchLocation() {

        val address = binding.texto.toString()

        try {
            val addressList = geocoder.getFromLocationName(address, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val addressFound = addressList[0]
                val latLng = LatLng(addressFound.latitude, addressFound.longitude)
                addMarkerWithAddress(latLng)  // Agrega un marcador con la dirección
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(this, "No se encontró la dirección", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error de geocodificación", Toast.LENGTH_LONG).show()
        }
    }

    // Agrega un marcador con la dirección
    private fun addMarkerWithAddress(latLng: LatLng) {
        try {
            val addresses: List<Address> = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) as List<Address>
            if (addresses.isNotEmpty()) {
                val address = addresses[0]
                val addressText = address.getAddressLine(0) ?: "Dirección no válida"
                mMap.addMarker(MarkerOptions().position(latLng).title(addressText))
                Toast.makeText(this, "Marcador agregado: $addressText", Toast.LENGTH_SHORT).show()
            } else {
                mMap.addMarker(MarkerOptions().position(latLng).title("Marcador sin dirección"))
                Toast.makeText(this, "No se encontró dirección para esta ubicación", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            mMap.addMarker(MarkerOptions().position(latLng).title("Marcador sin dirección"))
            Toast.makeText(this, "Error al obtener dirección", Toast.LENGTH_SHORT).show()
        }
    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (RADIUS_OF_EARTH_KM * c * 1000).roundToInt() // Convertir a metros
    }

    private fun writeJSONObject(location: Location) {
        try {
            val jsonObject = JSONObject().apply {
                put("provider", location.provider)
                put("latitude", location.latitude)
                put("longitude", location.longitude)
                put("altitude", location.altitude)
                put("accuracy", location.accuracy)
                put("time", Date(location.time))
            }
            val jsonArray = JSONArray().apply {
                put(jsonObject)
            }
            val file = File(getExternalFilesDir(null), "ubicacion.json")
            val writer: Writer = BufferedWriter(FileWriter(file))
            writer.use {
                it.write(jsonArray.toString())
            }
            Toast.makeText(this, "Ubicación guardada en archivo", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("JSON", "Error al escribir JSON", e)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

   /*override fun onResume() {
        super.onResume()
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if(isLocationUpdateEnabled){
            startLocationUpdates()
        }
    }
*/
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lightValue = event.values[0]
            if (lightValue < LUMINOSITY_THRESHOLD) {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark))
            } else {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_standard))
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        TODO("Not yet implemented")
    }
}
