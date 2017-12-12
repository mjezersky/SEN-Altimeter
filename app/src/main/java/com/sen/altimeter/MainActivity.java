package com.sen.altimeter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private SensorManager senManager;
    private TextView mainResult;
    private TextView mainResultDecimal;
    private TextView sensorAPIResult;
    private TextView currentPressure;
    private TextView statusText;
    private EditText inOffset;
    private EditText inTemperature;
    private EditText inSeaLevelPressure;
    private Button autocalibButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        mainResult = (TextView) findViewById(R.id.mainResult);
        mainResultDecimal = (TextView) findViewById(R.id.mainResultDecimal);
        sensorAPIResult = (TextView) findViewById(R.id.sensorAPIResult);
        currentPressure = (TextView) findViewById(R.id.currentPressure);
        statusText = (TextView) findViewById(R.id.statustext);

        inOffset = (EditText) findViewById(R.id.inOffset);
        inTemperature = (EditText) findViewById(R.id.inTemperature);
        inSeaLevelPressure = (EditText) findViewById(R.id.inSeaLevelPressure);

        autocalibButton = (Button) findViewById(R.id.autocalibrate);





        // Now first make a criteria with your requirements
        // this is done to save the battery life of the device
        // there are various other other criteria you can search for..
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_COARSE);

        // Now create a location manager
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // This is the Best And IMPORTANT part
        final Looper looper = null;
        final Context activityContext = this.getApplicationContext();
        final MainActivity thisActivity = this;

        // Now whenever the button is clicked fetch the location one time
        autocalibButton.setOnClickListener(new View.OnClickListener() {

                                               @Override
                                               public void onClick(View v) {
                                                   int permCheck = ContextCompat.checkSelfPermission(activityContext, Manifest.permission.ACCESS_FINE_LOCATION);
                                                   if (permCheck != PackageManager.PERMISSION_GRANTED) {
                                                       ActivityCompat.requestPermissions(thisActivity,
                                                               new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                               1);
                                                   }
                                                   permCheck = ContextCompat.checkSelfPermission(activityContext, Manifest.permission.ACCESS_FINE_LOCATION);
                                                   if (permCheck == PackageManager.PERMISSION_GRANTED) {
                                                       statusText.setText("Acquiring location...");
                                                       locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, thisActivity, looper);
                                                   }
                                      }
        });




        inOffset.setText("0");
        inTemperature.setText("15");
        inSeaLevelPressure.setText(String.valueOf(SensorManager.PRESSURE_STANDARD_ATMOSPHERE));

        senManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = senManager.getSensorList(Sensor.TYPE_PRESSURE);

        if (sensors.size() > 0) {
            Sensor sensor = sensors.get(0);
            senManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Location Changes", location.toString());
        Altimeter.latitude = String.valueOf(location.getLatitude());
        Altimeter.longtitude = String.valueOf(location.getLongitude());
        autocalibrate();
        //statusText.setText(String.valueOf(location.getLatitude()) + String.valueOf(location.getLongitude()));

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Status Changed", String.valueOf(status));
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Provider Enabled", provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Provider Disabled", provider);
    }

    private void autocalibrate() {
        JSONObject o;

        new WTask().execute();
    }

    @Override
    public void onAccuracyChanged(Sensor sen, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent evt) {


        float pressure = evt.values[0];

        float offset, temp, slp;

        try { offset = Float.parseFloat(inOffset.getText().toString()); }
        catch (NumberFormatException nfe) { offset = 0; }

        try { temp = Float.parseFloat(inTemperature.getText().toString()); }
        catch (NumberFormatException nfe) { temp = 15; }

        try { slp = Float.parseFloat(inSeaLevelPressure.getText().toString()); }
        catch (NumberFormatException nfe) { slp = SensorManager.PRESSURE_STANDARD_ATMOSPHERE; }


        double result = Altimeter.calculate(pressure, slp, temp) + offset;
        String resultString = String.format(Locale.UK, "%1.2f", result);

        mainResult.setText(resultString.substring(0, resultString.length()-3));
        mainResultDecimal.setText(resultString.substring(resultString.length()-3) + " m");
        currentPressure.setText(String.valueOf(pressure)+" hPa");
        sensorAPIResult.setText(String.valueOf(SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure)) + " m");
    }






    protected class WTask extends AsyncTask<Void, Void, JSONObject>
    {
        @Override
        protected void onPreExecute() {
            statusText.setText("Fetching weather data...");
        }

        protected JSONObject doInBackground(Void... params)
        {
            String urlString="https://api.darksky.net/forecast/"+Altimeter.WEATHER_API_KEY+"/"+Altimeter.latitude+","+Altimeter.longtitude+
                    "?exclude=minutely,hourly,daily,alerts,flags&units=si";
            URLConnection urlConn = null;
            BufferedReader bufferedReader = null;
            try
            {
                URL url = new URL(urlString);
                urlConn = url.openConnection();
                bufferedReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

                StringBuffer stringBuffer = new StringBuffer();
                String line;
                while ((line = bufferedReader.readLine()) != null)
                {
                    stringBuffer.append(line);
                }

                return new JSONObject(stringBuffer.toString());
            }
            catch(Exception ex)
            {
                return null;
            }
            finally
            {
                if(bufferedReader != null)
                {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(JSONObject response)
        {
            if(response != null)
            {

                try {
                    inTemperature.setText(response.getJSONObject("currently").getString("temperature"));
                    inSeaLevelPressure.setText(response.getJSONObject("currently").getString("pressure"));

                    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy");
                    Date date = new Date();
                    statusText.setText("Successful weather update at: "+ dateFormat.format(date));
                } catch (JSONException ex) {
                    Log.e("App", "Failure", ex);
                }
            }
            else {
                statusText.setText("Failed to get weather data.");
            }
        }
    }

}
