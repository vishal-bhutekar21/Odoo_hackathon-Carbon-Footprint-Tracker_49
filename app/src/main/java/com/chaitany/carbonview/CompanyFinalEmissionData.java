package com.chaitany.carbonview;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chaitany.carbonview.R;
import com.chaitany.carbonview.VehicleDataAdapter;
import com.chaitany.carbonview.EmissionsDataRepository;
import com.chaitany.carbonview.CompanyEmissionData;
import com.chaitany.carbonview.VehicleData;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class CompanyFinalEmissionData extends AppCompatActivity {
    private EmissionsDataRepository repository;
    private Spinner dateRangeSpinner;
    private PieChart scopePieChart;
    private BarChart scope1Chart;
    private LineChart scope2Chart;
    private BarChart scope3Chart;
    private RecyclerView vehicleDataRecyclerView;
    private TextView totalVehiclesValue;
    private TextView totalKilometersValue;
    private TextView totalEmissionsValue;
    private Button downloadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_final_emission_data);

        repository = new EmissionsDataRepository();
        initializeViews();
        setupDateRangeSpinner();
        setupCharts();
        setupVehicleDataRecyclerView();
        setupDownloadButton();
        loadAllData();
    }

    private void initializeViews() {
        dateRangeSpinner = findViewById(R.id.dateRangeSpinner);
        scopePieChart = findViewById(R.id.scopePieChart);
        scope1Chart = findViewById(R.id.scope1Chart);
        scope2Chart = findViewById(R.id.scope2Chart);
        scope3Chart = findViewById(R.id.scope3Chart);
        vehicleDataRecyclerView = findViewById(R.id.vehicleDataRecyclerView);
        totalVehiclesValue = findViewById(R.id.totalVehiclesValue);
        totalKilometersValue = findViewById(R.id.totalKilometersValue);
        totalEmissionsValue = findViewById(R.id.totalEmissionsValue);
        downloadButton = findViewById(R.id.downloadButton);
    }

    private void setupDateRangeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.date_ranges,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateRangeSpinner.setAdapter(adapter);

        dateRangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadAllData(); // Reload data based on selected date range
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupCharts() {
        setupScopePieChart();
        setupScope1BarChart();
        setupScope2LineChart();
        setupScope3BarChart();
    }

    private void setupScopePieChart() {
        scopePieChart.setUsePercentValues(true);
        scopePieChart.getDescription().setEnabled(false);
        scopePieChart.setExtraOffsets(5, 10, 5, 5);
        scopePieChart.setDragDecelerationFrictionCoef(0.95f);
        scopePieChart.setDrawHoleEnabled(true);
        scopePieChart.setHoleColor(Color.WHITE);
        scopePieChart.setTransparentCircleRadius(61f);

        Legend l = scopePieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
    }

    private void setupScope1BarChart() {
        scope1Chart.getDescription().setEnabled(false);
        scope1Chart.setPinchZoom(false);
        scope1Chart.setDrawBarShadow(false);
        scope1Chart.setDrawGridBackground(false);

        XAxis xAxis = scope1Chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        scope1Chart.getAxisLeft().setDrawGridLines(true);
        scope1Chart.getAxisRight().setEnabled(false);
        scope1Chart.getLegend().setEnabled(true);
    }

    private void setupScope2LineChart() {
        scope2Chart.getDescription().setEnabled(false);
        scope2Chart.setPinchZoom(false);
        scope2Chart.setDrawGridBackground(false);

        XAxis xAxis = scope2Chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        scope2Chart.getAxisLeft().setDrawGridLines(true);
        scope2Chart.getAxisRight().setEnabled(false);
        scope2Chart.getLegend().setEnabled(true);
    }

    private void setupScope3BarChart() {
        scope3Chart.getDescription().setEnabled(false);
        scope3Chart.setPinchZoom(false);
        scope3Chart.setDrawBarShadow(false);
        scope3Chart.setDrawGridBackground(false);

        XAxis xAxis = scope3Chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        scope3Chart.getAxisLeft().setDrawGridLines(true);
        scope3Chart.getAxisRight().setEnabled(false);
        scope3Chart.getLegend().setEnabled(true);
    }

    private void setupVehicleDataRecyclerView() {
        vehicleDataRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        vehicleDataRecyclerView.setHasFixedSize(true);
    }

    private void setupDownloadButton() {
        downloadButton.setOnClickListener(v -> {
            // Implement download functionality
            Toast.makeText(this, "Downloading report...", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadAllData() {
        updateSummaryCards();
        loadScopePieChartData();
        loadScope1ChartData();
        loadScope2ChartData();
        loadScope3ChartData();
        loadVehicleData();
    }

    private void updateSummaryCards() {
        totalVehiclesValue.setText(String.valueOf(1200));
        totalKilometersValue.setText(String.format("%,d", 450000));
        totalEmissionsValue.setText(String.valueOf(650));
    }

    private void loadScopePieChartData() {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(30f, "Scope 1"));
        entries.add(new PieEntry(20f, "Scope 2"));
        entries.add(new PieEntry(50f, "Scope 3"));

        PieDataSet dataSet = new PieDataSet(entries, "Emissions by Scope");
        dataSet.setColors(
                getColor(R.color.scope1_color),
                getColor(R.color.scope2_color),
                getColor(R.color.scope3_color)
        );
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(scopePieChart));

        scopePieChart.setData(data);
        scopePieChart.invalidate();
    }

    private void loadScope1ChartData() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        String[] fuelTypes = {"Diesel", "Natural Gas", "Gasoline"};

        entries.add(new BarEntry(0, 45f));
        entries.add(new BarEntry(1, 35f));
        entries.add(new BarEntry(2, 40f));

        BarDataSet dataSet = new BarDataSet(entries, "Emissions by Fuel Type");
        dataSet.setColor(getColor(R.color.scope1_color));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);

        scope1Chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(fuelTypes));
        scope1Chart.setData(data);
        scope1Chart.invalidate();
    }

    private void loadScope2ChartData() {
        ArrayList<Entry> entries = new ArrayList<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};

        entries.add(new Entry(0, 80f));
        entries.add(new Entry(1, 85f));
        entries.add(new Entry(2, 75f));
        entries.add(new Entry(3, 88f));
        entries.add(new Entry(4, 82f));
        entries.add(new Entry(5, 79f));

        LineDataSet dataSet = new LineDataSet(entries, "Monthly Electricity Emissions");
        dataSet.setColor(getColor(R.color.scope2_color));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(getColor(R.color.scope2_color));
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);

        LineData data = new LineData(dataSet);

        scope2Chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(months));
        scope2Chart.setData(data);
        scope2Chart.invalidate();
    }

    private void loadScope3ChartData() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};

        entries.add(new BarEntry(0, 450f));
        entries.add(new BarEntry(1, 460f));
        entries.add(new BarEntry(2, 440f));
        entries.add(new BarEntry(3, 470f));
        entries.add(new BarEntry(4, 455f));
        entries.add(new BarEntry(5, 445f));

        BarDataSet dataSet = new BarDataSet(entries, "Vehicle Usage Emissions");
        dataSet.setColor(getColor(R.color.scope3_color));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);

        scope3Chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(months));
        scope3Chart.setData(data);
        scope3Chart.invalidate();
    }

    private void loadVehicleData() {
        List<VehicleData> vehicleDataList = repository.getVehicleData();
        VehicleDataAdapter adapter = new VehicleDataAdapter(vehicleDataList);
        vehicleDataRecyclerView.setAdapter(adapter);
    }
}