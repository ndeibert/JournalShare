package com.plusultra.csci448.journalshare;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ndeibert on 3/16/2018.
 */

public class MapFragment extends SupportMapFragment {

    private static final String TAG = "MapFragment";
    private static final String[] LOCATION_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };
    private static final int REQUEST_LOCATION_PERMISSIONS = 0;

    FirebaseDatabase mDatabase;
    DatabaseReference mRef;

    private GoogleApiClient mClient;
    private GoogleMap mMap;

    private List<JournalEntry> entries = new ArrayList<>();

    public static MapFragment newInstance() {
        Bundle args = new Bundle();
        MapFragment fragment = new MapFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

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
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                //updateUI();
                mDatabase = FirebaseDatabase.getInstance();
                mRef = mDatabase.getReference("entries");
                mRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Map<String, Object> journalMap = (HashMap<String, Object>)dataSnapshot.getValue();
                        for (Object o : journalMap.values()) {
                            if (o instanceof Map) {
                                //Log.d(TAG, "title: " + ((Map) o).get("title"));
                                //Log.d(TAG, "text: " + ((Map) o).get("text"));
                                JournalEntry entry = new JournalEntry();
                                entry.setTitle(((Map) o).get("title").toString());
                                entry.setText(((Map) o).get("text").toString());
                                entries.add(entry);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });

        if (hasLocationPermission()) {
            //findImage();
        } else {
            Log.d(TAG, "No permissions, ask for them.");
            requestPermissions(LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS);
        }

        Toast.makeText(getActivity(), R.string.toast_map, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().invalidateOptionsMenu();
        mClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mClient.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case REQUEST_LOCATION_PERMISSIONS:
                if (hasLocationPermission()) {
                    //findImage();
                }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasLocationPermission() {
        int result = ContextCompat.checkSelfPermission(getActivity(), LOCATION_PERMISSIONS[0]);
        return (result == PackageManager.PERMISSION_GRANTED);
    }


    private void updateUI() {
        if (mMap == null) {
            return;
        }
        mMap.clear();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        if (entries.size() > 0) {
            for (int i = 0; i < entries.size(); i++) {
                /*LatLng point = new LatLng(entries.get(i).getLat(), entries.get(i).getLon());
                MarkerOptions marker = new MarkerOptions()
                        .position(point)
                        .title("lat/lng: (" + point.latitude + ", " + point.longitude + ")");
                Marker m = mMap.addMarker(marker);
                m.setTag(entries.get(i).getId().toString());
                boundsBuilder.include(point);*/
            }
        } else { // If our app has an empty database, create bounds to include the entire state of colorado
            LatLng coloradoNorthEast = new LatLng(41f, -102f);
            LatLng coloradoSouthWest = new LatLng(37f, -109f);
            boundsBuilder.include(coloradoNorthEast);
            boundsBuilder.include(coloradoSouthWest);
        }
        LatLngBounds bounds = boundsBuilder.build();
        //int margin = getResources().getDimensionPixelSize(R.dimen.map_inset_margin);
        //CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, margin);
        //mMap.animateCamera(update);
    }


}
