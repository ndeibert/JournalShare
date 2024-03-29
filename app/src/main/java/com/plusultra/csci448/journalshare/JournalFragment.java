package com.plusultra.csci448.journalshare;

import android.*;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.telecom.Call;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JournalFragment provides the majority of the functionality of the app.
 * This is where the user interaction with views makes modifications to the
 * local database. This is also where the app requests permissions and will
 * make a location request if the user decides to share the journal entry
 * to the remote database.
 *
 * Created by Han on 2/27/18.
 */

public class JournalFragment extends Fragment {

    private static final String ARG_ENTRY_ID = "entry_id";
    private static final String TAG = "JournalFragment";

    private static final String[] LOCATION_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
    };
    private static final int REQUEST_LOCATION_PERMISSIONS = 1;

    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;
    private GoogleApiClient mClient;

    private JournalEntry mEntry;
    private EditText mTitle;
    private EditText mText;
    private TextView mDate;
    private Callbacks mCallbacks;


    public interface Callbacks {
        void onEntryUpdated(JournalEntry entry);
    }

    private void updateEntry() {
        mCallbacks.onEntryUpdated(mEntry);
    }

    public static JournalFragment newInstance(UUID entryId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_ENTRY_ID, entryId);
        JournalFragment fragment = new JournalFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // get database reference
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference("entries");
        // get current journal entry
        UUID entryId = (UUID) getArguments().getSerializable(ARG_ENTRY_ID);
        JournalBook journalBook = JournalBook.get(getActivity());
        mEntry = journalBook.getEntry(entryId);
        // get Google API client
        mClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        getActivity().invalidateOptionsMenu();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate view
        View v = inflater.inflate(R.layout.fragment_entry, container, false);

        // set title with listener
        mTitle = (EditText) v.findViewById(R.id.entry_title);
        mTitle.setText(mEntry.getTitle());
        mTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String converted = s.toString();
                if(converted.contains("\n")){
                    //There are occurences of the newline character
                    converted = converted.replace("\n", "");
                    mEntry.setTitle(converted);
                } else {
                    mEntry.setTitle(converted);
                }
                updateEntry();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // set entry content with listener
        mText = (EditText) v.findViewById(R.id.entry_box);
        mText.setText(mEntry.getText());
        mText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mEntry.setText(s.toString());
                updateEntry();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // set date
        mDate = (TextView) v.findViewById(R.id.entry_date);
        mDate.setText(mEntry.getDate().toString());


        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_journal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_item_map_share:
                Toast.makeText(getActivity(), R.string.toast_share, Toast.LENGTH_SHORT).show();
                if (hasLocationPermission()) {
                    share();
                } else {
                    requestPermissions(LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS);
                }
                return true;
            case R.id.menu_item_delete:
                JournalBook journal = JournalBook.get(getActivity());
                journal.deleteEntry(mEntry.getId());
                Intent intent = new Intent(getActivity(), JournalListActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    private void share() {
        // make request to location services
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setNumUpdates(1);
        request.setInterval(0);
        try {
            LocationServices.FusedLocationApi
                    .requestLocationUpdates(mClient, request, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            Log.i(TAG, "Got a location: " + location);
                            // save location and share with remote database
                            mEntry.setLat(location.getLatitude());
                            mEntry.setLon(location.getLongitude());
                            DatabaseReference mNewEntryRef = mRef.child(mEntry.getId().toString());
                            mNewEntryRef.setValue(mEntry);
                        }
                    });
        } catch (SecurityException se) {
            Log.e(TAG, "Failed to access location services", se);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case REQUEST_LOCATION_PERMISSIONS:
                if (hasLocationPermission()) {
                    share();
                }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasLocationPermission() {
        int result = ContextCompat.checkSelfPermission(getActivity(), LOCATION_PERMISSIONS[0]);
        return (result == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().invalidateOptionsMenu();
        mClient.connect();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        JournalBook.get(getActivity()).updateEntry(mEntry);
    }
}
