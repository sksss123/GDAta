package data.hci.gdata.Service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import data.hci.gdata.Global.StaticVariable;

public class GpsService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    public static Boolean isSend = true;

    public GpsService() {    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);//기본 5초마다 위치를 찾음
        locationRequest.setFastestInterval(2000);//빠르게 할 땐 2초마다 찾음.
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//정확하게 위치를 찾음
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        googleApiClient.connect();
        isSend = true;
        //return super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(StaticVariable.BROADCAST_GPS);//인텐트 액션 설정

        //위도와 경도를 인텐트에 추가한다.
        broadcastIntent.putExtra("Latitude", location.getLatitude());
        broadcastIntent.putExtra("Longitude", location.getLongitude());

        Log.d("gps Service", isSend.toString());

        if(isSend) {
            sendBroadcast(broadcastIntent);//브로드캐스트 인텐트를 보낸다.
            Log.d("gps Service", location.getLatitude() + " " + location.getLongitude());
        }

    }

    //장소 업데이트
    protected void startLocationUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            }
        }else{
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    //장소 업데이트를 중지시킴
    protected void stopLocationUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            }
        }
        else {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        createLocationRequest();

        if(isSend)
            startLocationUpdate();
    }

    @Override
    public void onConnectionSuspended(int i) {
        stopLocationUpdate();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

}
