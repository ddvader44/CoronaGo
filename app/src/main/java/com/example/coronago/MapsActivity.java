package com.example.coronago;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private MarkerOptions options;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private FirebaseDatabase database;
    private DatabaseReference LocationReference;
    private FirebaseAuth mAuth;
    private String currentUserID;
    private ArrayList<LatLng> locationInfoUsers = new ArrayList<>();
    private Geocoder geocoder;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mAuth = FirebaseAuth.getInstance();
        options = new MarkerOptions();
        database = FirebaseDatabase.getInstance();
        LocationReference = FirebaseDatabase.getInstance().getReference().child("Location");
        currentUserID = mAuth.getCurrentUser().getUid();


        mMap = googleMap;
        MapStyleOptions mapStyleOptions = MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json);
        mMap.setMapStyle(mapStyleOptions);
        locationManager =(LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener =new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Double lat= location.getLatitude();
                Double longs = location.getLongitude();
                mMap.clear();
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location!"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));
                HashMap<String,Object> locationInfo = new HashMap<>();
                locationInfo.put("uid",currentUserID);
                locationInfo.put("latitude",lat);
                locationInfo.put("longitude",longs);
                LocationReference.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(locationInfo).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        getAllUsersLocation();
                    }
                });


            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
    }

    private void getAllUsersLocation() {
        final String[] address = {""};
        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        LocationReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists())
                {
                    for(DataSnapshot ds : snapshot.getChildren())
                    {
                        String usersLat = ds.child("latitude").getValue().toString();
                        String usersLong = ds.child("longitude").getValue().toString();
                        Double usersLatDouble = Double.parseDouble(usersLat);
                        Double usersLongDouble  = Double.parseDouble(usersLong);
                        LatLng loc = new LatLng(usersLatDouble,usersLongDouble);
                        locationInfoUsers.add(loc);
                        try {
                            List<Address> listAddress = geocoder.getFromLocation(usersLatDouble,usersLongDouble,1);
                            if(listAddress != null && listAddress.size() > 0) {
                                if (listAddress.get(0).getLocality() != null) {
                                    address[0] = listAddress.get(0).getLocality() + " ";
                                    for (LatLng point : locationInfoUsers) {
                                        options.position(point);
                                        options.title(address[0]);
                                        options.snippet("Area");
                                        mMap.addMarker(options);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapsActivity.this, "Unable to fetch location data!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
