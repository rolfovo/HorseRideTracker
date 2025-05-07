package com.example.horseridetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.util.*

class RideDetailActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pokud nemáš layout v XML, nastavíme vše ručně
        setContentView(R.layout.activity_ride_detail)

        Configuration.getInstance().load(this, getSharedPreferences("osm", MODE_PRIVATE))

        map = findViewById(R.id.map)
        pieChart = findViewById(R.id.pieChart)

        setupMap()
        setupChart()
    }

    private fun setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(16.0)
        map.controller.setCenter(GeoPoint(50.08804, 14.42076)) // Praha, změň dle potřeby

        val path = Polyline()
        path.setPoints(
            listOf(
                GeoPoint(50.08804, 14.42076),
                GeoPoint(50.08820, 14.42150),
                GeoPoint(50.08850, 14.42220)
            )
        )
        map.overlays.add(path)
    }

    private fun setupChart() {
        val entries = listOf(
            PieEntry(30f, "Krok"),
            PieEntry(50f, "Klus"),
            PieEntry(20f, "Cval")
        )
        val dataSet = PieDataSet(entries, "Chody")
        dataSet.colors = listOf(
            android.graphics.Color.BLUE,
            android.graphics.Color.MAGENTA,
            android.graphics.Color.RED
        )
        pieChart.data = PieData(dataSet)
        pieChart.invalidate() // refresh
    }
}