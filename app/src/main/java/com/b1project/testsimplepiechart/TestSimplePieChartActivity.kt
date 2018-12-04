package com.b1project.testsimplepiechart

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.widget.LinearLayout

import com.b1project.simplepiechart.*

class TestSimplePieChartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_simple_pie_chart)

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val width = (screenWidth / 1.5f).toInt()
        val height = (screenWidth / 2.25f).toInt()

        // Create chart data array
        val pieData: List<PieItem> = listOf(
                PieItem(15.6f, "item 1", Color.GREEN),
                PieItem(24.8f, "item 2", Color.RED),
                PieItem(2.85f, "item 3", Color.BLUE),
                PieItem(9.0f,"item 4", Color.YELLOW),
                PieItem( 18.9f, "item 5", Color.MAGENTA, R.drawable.hstripe))


        // Method 1: add chart into an existing view
        val pieChartView = PieChartView(this)

        pieChartView.setGeometry(
                width,
                height,
                8,
                8,
                8,
                8,
                80)
        pieChartView.setShadowRadius(8)
        pieChartView.setShadowColor(Color.BLACK)
        pieChartView.setData(pieData)
        pieChartView.setChartType(PieChartView.CHART_TYPE_DONUT)
        val chartContainer = findViewById<LinearLayout>(R.id.grafContainer)
        chartContainer.addView(pieChartView)

        // Method 2: Use XML layout integration
        val pieChartViewXml = findViewById<PieChartView>(R.id.myChart)
        pieChartViewXml.setData(pieData)
    }

}
