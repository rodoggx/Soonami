package com.example.rodoggx.soonami;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    public static final String Log_TAG = MainActivity.class.getSimpleName();

    private static final String USG_URL = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2014-01-01&endtime=2014-12-01&minmagnitude=7";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TsunamiAsyncTask task = new TsunamiAsyncTask();
        task.execute();
    }

    private void updateUi(Event earthquake) {
        TextView titleView = (TextView) findViewById(R.id.title);
        titleView.setText(earthquake.title);

        TextView dateView = (TextView) findViewById(R.id.date);
        dateView.setText(getDateString(earthquake.time));

        TextView tsunamiView = (TextView) findViewById(R.id.tsunami_alert);
        tsunamiView.setText(getTsunamiAlertString(earthquake.tsunamiAlert));
    }

    private String getDateString(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm:ss z");
        return formatter.format(time);
    }

    private String getTsunamiAlertString(int tsunamiAlert) {
        switch (tsunamiAlert) {
            case 0:
                return getString(R.string.alert_no);
            case 1:
                return getString(R.string.alert_yes);
            default:
                return getString(R.string.alert_not_available);
        }
    }

    private class TsunamiAsyncTask extends AsyncTask<URL, Void, Event> {
        @Override
        protected Event doInBackground(URL... urls) {
            URL url = createUrl(USG_URL);
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                Log.i(Log_TAG, "doInBackground: " + e);
            }
            Event earthquake = extractFeatureFromJson(jsonResponse);
            return earthquake;
        }

        @Override
        protected void onPostExecute(Event earthquake) {
            if (earthquake == null) {
                return;
            }
            updateUi(earthquake);
        }

        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException e) {
                Log.e(Log_TAG, "createUrl: ", e);
                return null;
            }
            return url;
        }

        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";
            if (url == null) {
                return jsonResponse;
            }

            HttpURLConnection urlConn = null;
            InputStream inputStream = null;
            try {
                urlConn = (HttpURLConnection) url.openConnection();
                urlConn.setRequestMethod("GET");
                urlConn.setReadTimeout(10000);
                urlConn.setConnectTimeout(15000);
                urlConn.connect();
                if (urlConn.getResponseCode() == 200) {
                    inputStream = urlConn.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                }
            } catch (IOException e) {
                Log.e(Log_TAG, "makeHttpRequest: ", e);
            } finally {
                if (urlConn != null) {
                    urlConn.disconnect();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            return jsonResponse;
        }

        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

        private Event extractFeatureFromJson(String jsonResponse) {
            if (TextUtils.isEmpty(jsonResponse)) {
                return null;
            }
            try {
                JSONObject baseJsonResponse = new JSONObject(jsonResponse);
                JSONArray featureArray = baseJsonResponse.getJSONArray("features");

                if (featureArray.length() > 0) {
                    JSONObject firstFeature = featureArray.getJSONObject(0);
                    JSONObject properties = firstFeature.getJSONObject("properties");

                    String title = properties.getString("title");
                    long time = properties.getLong("time");
                    int tsunamiAlert = properties.getInt("tsunami");

                    return new Event(title, time, tsunamiAlert);
                }
            } catch (JSONException e) {
                Log.e(Log_TAG, "extractFeatureFromJson: ", e);
            }
            return null;
        }
    }
}
