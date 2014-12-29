package alexkang.swing;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;

public class MainFragment extends PreferenceFragment {

    private static final float MAX_SENSITIVITY = 30;
    private static final int MAX_FREQUENCY = 2000; // In milliseconds.
    private static final float SENSITIVITY_OFFSET = 10; // To prevent extremely high sensitivities.
    private static final String UPDATE_VIBRATE = "vibrate/";
    private static final String UPDATE_SENSITIVITY = "sensitivity/";
    private static final String UPDATE_FREQUENCY = "frequency/";

    private CheckBoxPreference vibratePref;
    private Preference sensitivityPref;
    private Preference frequencyPref;
    private ListPreference primarySoundPref;
    private ListPreference secondarySoundPref;
    private SharedPreferences sharedPrefs;

    private LayoutInflater inflater;
    private TextView sensitivityValue;
    private TextView frequencyValue;
    private SeekBar sensitivitySeekBar;
    private SeekBar frequencySeekBar;

    private GoogleApiClient googleApiClient;
    private List<Node> nodes = new ArrayList<>();

    private float sensitivity;
    private int frequency;
    private String primarySound;
    private String secondarySound;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        vibratePref = (CheckBoxPreference) findPreference("vibrate");
        sensitivityPref = findPreference("sensitivity");
        frequencyPref = findPreference("frequency");
        primarySoundPref = (ListPreference) findPreference("primary_sound");
        secondarySoundPref = (ListPreference) findPreference("secondary_sound");

        inflater = getActivity().getLayoutInflater();

        googleApiClient = ((MainActivity) getActivity()).googleApiClient;

        retrieveDefaultPreferences();
        setupPreferenceListeners();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Get all connected bluetooth "nodes".
        Wearable.NodeApi.getConnectedNodes(googleApiClient)
                .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        nodes = getConnectedNodesResult.getNodes();
                    }
                });
    }

    /*
     *  Converts a float value (minimum acceleration to trigger sound) to a percentage for the
     *  settings slider. Lower values should convert to higher percentages.
     */
    private int sensitivityToPercent(float value) {
        return (int) (100 - (((value - SENSITIVITY_OFFSET) / MAX_SENSITIVITY) * 100));
    }

    /*
     *  Converts a sensitivity percentage to a float value indicating the minimum acceleration to
     *  trigger a sound. Accelerations range from 0 to 40 (m/s)^2.
     */
    private float percentToSensitivity(int value) {
        return (((float) (100 - value) / (float) 100) * MAX_SENSITIVITY) + SENSITIVITY_OFFSET;
    }

    /*
     * Translates a ListPreference value to its corresponding human-readable key.
     */
    private CharSequence valueToKey(String value) {
        return primarySoundPref.getEntries()[primarySoundPref.findIndexOfValue(value)];
    }

    private AlertDialog.Builder createDialog(String title, View view,
            DialogInterface.OnClickListener onPositiveClick,
            DialogInterface.OnClickListener onNegativeClick) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(view)
                .setPositiveButton("Okay", onPositiveClick)
                .setNegativeButton("Cancel", onNegativeClick);
    }

    /*
     * Sends data to the Android Wear device. We iterate through a list of "nodes" in case the
     * phone may be connected to multiple bluetooth devices.
     */
    private void sendMessage(String message) {
        for (Node node : nodes) {
            Wearable.MessageApi.sendMessage(
                    googleApiClient, node.getId(), message, null);
        }
    }

    private void retrieveDefaultPreferences() {
        boolean vibrate = sharedPrefs.getBoolean("vibrate", true);
        sensitivity = sharedPrefs.getFloat("sensitivity", (float) 25);
        frequency = sharedPrefs.getInt("frequency", 150);
        primarySound = sharedPrefs.getString("primary_sound", "whoosh");
        secondarySound = sharedPrefs.getString("secondary_sound", "explosion");

        vibratePref.setChecked(vibrate);
        primarySoundPref.setValue(primarySound);
        primarySoundPref.setSummary(valueToKey(primarySound));
        secondarySoundPref.setValue(secondarySound);
        secondarySoundPref.setSummary(valueToKey(secondarySound));
    }

    private boolean onSensitivityClick() {
        View sensitivityLayout = inflater.inflate(R.layout.fragment_seekbar, null);
        sensitivityValue = (TextView) sensitivityLayout.findViewById(R.id.value);
        sensitivitySeekBar = (SeekBar) sensitivityLayout.findViewById(R.id.seekbar);
        sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensitivityValue.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        int sensitivityPercent = sensitivityToPercent(sensitivity);
        sensitivityValue.setText(sensitivityPercent + "%");
        sensitivitySeekBar.setProgress(sensitivityPercent);

        AlertDialog.Builder sensitivityDialog = createDialog(
                "Sensitivity", sensitivityLayout,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        float updatedValue = percentToSensitivity(sensitivitySeekBar.getProgress());
                        sensitivity = updatedValue;

                        sharedPrefs.edit().putFloat("sensitivity", updatedValue).apply();
                        sendMessage(UPDATE_SENSITIVITY + updatedValue);

                        dialog.dismiss();
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );

        sensitivityDialog.show();
        return true;
    }

    private boolean onFrequencyClick() {
        View frequencyLayout = inflater.inflate(R.layout.fragment_seekbar, null);
        frequencyValue = (TextView) frequencyLayout.findViewById(R.id.value);
        frequencySeekBar = (SeekBar) frequencyLayout.findViewById(R.id.seekbar);
        frequencySeekBar.setMax(MAX_FREQUENCY);
        frequencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                frequencyValue.setText(progress + " ms");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        frequencyValue.setText(frequency + " ms");
        frequencySeekBar.setProgress(frequency);

        AlertDialog.Builder frequencyDialog = createDialog(
                "Frequency", frequencyLayout,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Convert to milliseconds.
                        frequency = frequencySeekBar.getProgress();

                        sharedPrefs.edit().putInt("frequency", frequency).apply();
                        sendMessage(UPDATE_FREQUENCY + frequency);

                        dialog.dismiss();
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
        );

        frequencyDialog.show();
        return true;
    }

    private boolean updateSoundPref(ListPreference pref, String value, boolean isPrimary) {
        MainActivity activity = (MainActivity) getActivity();

        // Unload the current sound to prevent high memory usage.
        if (isPrimary) {
            primarySound = value;
            activity.soundPool.unload(activity.soundIdPrimary);
        } else {
            secondarySound = value;
            activity.soundPool.unload(activity.soundIdSecondary);
        }

        int newSoundId = activity.soundPool.load(activity, activity.getSound(value), 1);
        pref.setSummary(valueToKey(value));

        if (isPrimary) {
            sharedPrefs.edit().putString("primary_sound", value).apply();
            activity.soundIdPrimary = newSoundId;
        } else {
            sharedPrefs.edit().putString("secondary_sound", value).apply();
            activity.soundIdSecondary = newSoundId;
        }

        return true;
    }

    private void setupPreferenceListeners() {
        vibratePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                sharedPrefs.edit().putBoolean("vibrate", (boolean) newValue).apply();
                sendMessage(UPDATE_VIBRATE + newValue);
                return true;
            }
        });

        sensitivityPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return onSensitivityClick();
            }
        });

        frequencyPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return onFrequencyClick();
            }
        });

        primarySoundPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return updateSoundPref(primarySoundPref, (String) newValue, true);
            }
        });

        secondarySoundPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return updateSoundPref(secondarySoundPref, (String) newValue, false);
            }
        });
    }

}