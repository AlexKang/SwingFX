package alexkang.swing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;


public class MainActivity extends ActionBarActivity implements MessageApi.MessageListener {

    private static final int REQUEST_CODE_PLAY_SERVICES = 1001;
    private static final String STOP_APP = "stop_app";
    private static final String LIGHT_SOUND = "x";
    private static final String HEAVY_SOUND = "y";

    protected GoogleApiClient googleApiClient;
    protected SoundPool soundPool;
    protected int soundIdPrimary;
    protected int soundIdSecondary;

    public static boolean isRunning = false;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextAppearance(this, R.style.TextAppearance);
        setSupportActionBar(toolbar);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        getFragmentManager().beginTransaction()
                .replace(R.id.content, new MainFragment())
                .commit();

        setupGoogleAPIClient();

        // We use the deprecated SoundPool constructor because the new method of doing so is not
        // available on 4.4 and lower.
        String primarySound = sharedPrefs.getString("primary_sound", "whoosh");
        String secondarySound = sharedPrefs.getString("secondary_sound", "explosion");
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundIdPrimary = soundPool.load(this, getSound(primarySound), 1);
        soundIdSecondary = soundPool.load(this, getSound(secondarySound), 1);
    }

    @Override
    public void onResume() {
        super.onResume();

        Wearable.MessageApi.addListener(googleApiClient, this);
        isRunning = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        googleApiClient.disconnect();
        Wearable.MessageApi.removeListener(googleApiClient, this);
        isRunning = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PLAY_SERVICES:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "You must install the latest Google Play Services.",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(LIGHT_SOUND)) {
            soundPool.play(soundIdPrimary, 1, 1, 0, 0, 1);
        } else if (messageEvent.getPath().equals(HEAVY_SOUND)) {
            soundPool.play(soundIdSecondary, 1, 1, 0, 0, 1);
        } else if (messageEvent.getPath().equals(STOP_APP)) {
            finish();
        }
    }

    private void setupGoogleAPIClient() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            GooglePlayServicesUtil.getErrorDialog(status, this, REQUEST_CODE_PLAY_SERVICES).show();
        }

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Toast.makeText(getBaseContext(), "Google Play Services not available.",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {}

                    @Override
                    public void onConnectionSuspended(int i) {
                        Toast.makeText(getBaseContext(), "Google Play Services has stopped.",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .build();

        googleApiClient.connect();
    }

    protected int getSound(String name) {
        return getResources().getIdentifier(name, "raw", getPackageName());
    }

}
