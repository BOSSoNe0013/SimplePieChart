package com.b1project.testsimplepiechart;

import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import com.b1project.simplepiechart.*;

public class TestSimplePieChartActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_simple_pie_chart);

        List<PieItem> pieData = new ArrayList<PieItem>(0);

        PieItem item1 = new PieItem();
        item1.Count = 15.6f;
        item1.Label = "item 1";
        item1.Color = Color.GREEN;
        pieData.add(item1);

        PieItem item2 = new PieItem();
        item2.Count = 24.8f;
        item2.Label = "item 2";
        item2.Color = Color.RED;
        pieData.add(item2);

        PieItem item3 = new PieItem();
        item3.Count = 2.85f;
        item3.Label = "item 3";
        item3.Color = Color.BLUE;
        pieData.add(item3);

        PieItem item4 = new PieItem();
        item4.Count = 9.0f;
        item4.Label = "item 4";
        item4.Color = Color.YELLOW;
        pieData.add(item4);

        PieItem item5 = new PieItem();
        item5.Count = 18.9f;
        item5.Label = "item 5";
        item5.Color = Color.MAGENTA;
        item5.Texture = R.drawable.hstripe;
        pieData.add(item5);

        PieChartView pieChartView = new PieChartView(this);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;

        pieChartView.setGeometry(width/1.5f, width/2.25f, 8, 8, 8, 8, width/10);
        pieChartView.setData(pieData);
        pieChartView.setChartType(PieChartView.CHART_TYPE_DONUT);
        LinearLayout grafContainer = (LinearLayout) findViewById(R.id.grafContainer);
        grafContainer.addView(pieChartView);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_test_simple_pie_chart, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
