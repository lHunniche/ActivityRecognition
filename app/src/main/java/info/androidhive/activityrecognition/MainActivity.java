package info.androidhive.activityrecognition;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.DetectedActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Date;

public class MainActivity extends AppCompatActivity
{

    private String TAG = MainActivity.class.getSimpleName();
    BroadcastReceiver broadcastReceiver;

    private TextView txtActivity, txtConfidence;
    private ImageView imgActivity;
    private Button btnStartTrcking, btnStopTracking;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtActivity = findViewById(R.id.txt_activity);
        txtConfidence = findViewById(R.id.txt_confidence);
        imgActivity = findViewById(R.id.img_activity);
        btnStartTrcking = findViewById(R.id.btn_start_tracking);
        btnStopTracking = findViewById(R.id.btn_stop_tracking);

        btnStartTrcking.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                startTracking();
            }
        });

//        btnStopTracking.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View view)
//            {
//                stopTracking();
//            }
//        });

        broadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY))
                {
                    int type = intent.getIntExtra("type", -1);
                    int confidence = intent.getIntExtra("confidence", 0);
                    handleUserActivity(type, confidence);
                }
            }
        };

        startTracking();
    }

    private void handleUserActivity(int type, int confidence)
    {
        String label = getString(R.string.activity_unknown);
        int icon = R.drawable.ic_still;

        switch (type)
        {
            case DetectedActivity.IN_VEHICLE:
            {
                label = getString(R.string.activity_in_vehicle);
                icon = R.drawable.ic_driving;
                break;
            }
            case DetectedActivity.ON_BICYCLE:
            {
                label = getString(R.string.activity_on_bicycle);
                icon = R.drawable.ic_on_bicycle;
                break;
            }
            case DetectedActivity.ON_FOOT:
            {
                label = getString(R.string.activity_on_foot);
                icon = R.drawable.ic_walking;
                break;
            }
            case DetectedActivity.RUNNING:
            {
                label = getString(R.string.activity_running);
                icon = R.drawable.ic_running;
                break;
            }
            case DetectedActivity.STILL:
            {
                label = getString(R.string.activity_still);
                break;
            }
            case DetectedActivity.TILTING:
            {
                label = getString(R.string.activity_tilting);
                icon = R.drawable.ic_tilting;
                break;
            }
            case DetectedActivity.WALKING:
            {
                label = getString(R.string.activity_walking);
                icon = R.drawable.ic_walking;
                break;
            }
            case DetectedActivity.UNKNOWN:
            {
                label = getString(R.string.activity_unknown);
                break;
            }
        }

        Log.e(TAG, "User activity: " + label + ", Confidence: " + confidence);

        if (confidence > Constants.CONFIDENCE)
        {
            txtActivity.setText(label);
            txtConfidence.setText("Confidence: " + confidence);
            imgActivity.setImageResource(icon);
            logModeToSever(label);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void logModeToSever(String label)
    {
        Date date = new Date();
        final String fileName = "mads";
        final String content = label + ":" + date.getTime();

        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... voids)
            {
                try
                {
                    final String appendUrl = "http://klevang.dk:8080/append?file=" + fileName + "&content=" + content;

                    URL url = new URL(appendUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.connect();
                    connection.getResponseCode();

                    BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String text = "", line;
                    while ((line = rd.readLine()) != null) {
                        text += line + "\n";
                    }
                }
                catch (ProtocolException e)
                {
                    e.printStackTrace();
                }
                catch (MalformedURLException e)
                {
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }


                return null;
            }
        }.execute();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void startTracking()
    {
        Intent intent = new Intent(MainActivity.this, BackgroundDetectedActivitiesService.class);
        startService(intent);
    }

    private void stopTracking()
    {
        Intent intent = new Intent(MainActivity.this, BackgroundDetectedActivitiesService.class);
        stopService(intent);
    }
}
