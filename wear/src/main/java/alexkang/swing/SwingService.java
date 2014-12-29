package alexkang.swing;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

public class SwingService extends Service implements SensorEventListener, MessageApi.MessageListener {

    private static final String FIRE_SOUND = "x";
    private static final String UPDATE_VIBRATE = "vibrate/";
    private static final String UPDATE_SENSITIVITY = "sensitivity/";

    private WatchViewStub stub;
    private TextView title;
    private GoogleApiClient googleApiClient;
    private List<Node> nodes;
    protected SensorManager sManager;
    private Sensor sensor;
    private Vibrator vibrator;

    private SharedPreferences sharedPrefs;
    private boolean vibrate;
    private float sensitivity;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        setContentView(R.layout.activity_main);

        NotificationBroadcastReceiver broadcastReceiver = new NotificationBroadcastReceiver();
        broadcastReceiver.setActivityHandler(this);
        IntentFilter intentFilter = new IntentFilter("notification_cancelled");
        registerReceiver(broadcastReceiver, intentFilter);

        Intent deleteIntent = new Intent(this, NotificationBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, deleteIntent, 0);

        Notification notification =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("SwingFX running")
                        .setContentText("Swipe to exit.")
                        .setPriority(2)
                        .setDeleteIntent(pendingIntent)
                        .build();

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        notificationManager.notify(1, notification);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

//        stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
//        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
//            @Override
//            public void onLayoutInflated(WatchViewStub watchViewStub) {
//                title = (TextView) stub.findViewById(R.id.title);
//            }
//        });

        setUpGoogleAPIClient();

        Wearable.NodeApi.getConnectedNodes(googleApiClient)
                .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        nodes = getConnectedNodesResult.getNodes();
                    }
                });

        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        vibrate = sharedPrefs.getBoolean("vibrate", true);
        sensitivity = sharedPrefs.getFloat("sensitivity", 25);

        sManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        Wearable.MessageApi.addListener(googleApiClient, this);

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        sManager.unregisterListener(this);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String message = messageEvent.getPath();
        if (message.contains(UPDATE_VIBRATE)) {
            vibrate = Boolean.parseBoolean(message.split("/")[1]);
            sharedPrefs.edit().putBoolean("vibrate", vibrate).apply();
        } else if (message.contains(UPDATE_SENSITIVITY)) {
            sensitivity = Float.parseFloat(message.split("/")[1]);
            sharedPrefs.edit().putFloat("sensitivity", sensitivity).apply();
        }
    }

    private void setUpGoogleAPIClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Toast.makeText(getBaseContext(), "Google Play Services not available.",
                                Toast.LENGTH_SHORT).show();
                        stopSelf();
                    }
                })
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {}

                    @Override
                    public void onConnectionSuspended(int i) {
                        Toast.makeText(getBaseContext(), "Google Play Services has stopped.",
                                Toast.LENGTH_SHORT).show();
                        stopSelf();
                    }
                })
                .build();

        googleApiClient.connect();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        double acceleration = Math.sqrt(x*x + y*y + z*z);

        if (acceleration > sensitivity) {
//            stub.setBackgroundColor(getResources().getColor(R.color.yellow));
//            title.setTextSize(32);

            if (vibrate) {
                vibrator.vibrate(150);
            }

            for (Node node : nodes) {
                Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), FIRE_SOUND, null);
            }
        } else {
//            stub.setBackgroundColor(getResources().getColor(android.R.color.black));
//            title.setTextSize(24);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

}
