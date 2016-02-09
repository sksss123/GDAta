package data.hci.gdata.Activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.net.URL;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import data.hci.gdata.Global.StaticVariable;
import data.hci.gdata.R;
import data.hci.gdata.Service.GpsService;
import data.hci.gdata.Service.GyroService;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMapClickListener, SensorEventListener {

    MapFragment mapFragment;
    GoogleMap mMap;
    UiSettings uiSettings;

    GoogleApiClient googleApiClient;

    Button myLoc, searchBtn, recommendBtn;
    ProgressBar progressBar;
    Boolean requestMyLoc = false;

    TextView textview;
    TextView gyroTextView;
    Document doc = null;

    //자이로 센서 사용
    SensorManager mSensorManager;
    Sensor gyroSensor;

    double latitude = 30, longitude = 100;

    private IntentFilter intentFilter;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(StaticVariable.BROADCAST_GPS)){
                latitude = intent.getDoubleExtra("Latitude", latitude);
                longitude = intent.getDoubleExtra("Longitude", longitude);

                updateUI();//화면 갱신
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);   //센서 매니저 사용
        gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);    //자이로센서 지정
        gyroTextView = (TextView)findViewById(R.id.tv_gyro);        //자이로 텍스트뷰 지정

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .build();
        }

        intentFilter = new IntentFilter();
        intentFilter.addAction(StaticVariable.BROADCAST_GPS);//잡을 액션을 명시

        initUI();
        registerReceiver(broadcastReceiver, intentFilter);
    }

    /**
     * UI들을 설정한 함수
     * */
    protected void initUI(){
        searchBtn = (Button)findViewById(R.id.btn_search);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPickerActivity();
            }
        });//검색 버튼시 picker Activity를 부른다.

        progressBar = (ProgressBar)findViewById(R.id.main_progressbar);

        //추천 장소 목록 표시
        recommendBtn = (Button)findViewById(R.id.btn_recommend);
        recommendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RecommendActivity.class);
                intent.putExtra("Latitude", latitude);
                intent.putExtra("Longitude", longitude);
                startActivity(intent);//RecommendActivity를 호출
            }
        });
        myLoc = (Button)findViewById(R.id.btn_loc);
        myLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                requestMyLoc = !(requestMyLoc);
                if(requestMyLoc)
                {
                    startService((new Intent(getApplicationContext(), GpsService.class)));
                    startService((new Intent(getApplicationContext(), GyroService.class)));
                    GpsService.isSend = false;
                    progressBar.setVisibility(View.VISIBLE);//프로그래스 바 화면에 표시
                }
                else
                {
                    mMap.clear();
                    GpsService.isSend = false;
                    stopService((new Intent(getApplicationContext(), GpsService.class)));
                    stopService((new Intent(getApplicationContext(), GyroService.class)));
                }

                GetXMLTask task = new GetXMLTask();
                task.execute("http://www.kma.go.kr/wid/queryDFS.jsp?gridx="+latitude+"&gridy="+longitude);
            }
        });
        textview = (TextView) findViewById(R.id.tv_temp);

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * 위치 자동 완성
     * */
    protected void createPickerActivity(){
        try {
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN).build(this);
            startActivityForResult(intent, 1);
          //  startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    //자동 검색 완성으로부터 나온 결과 값을 처리하는 함수
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                Log.d("gps place", place.getAddress()+" "+place.getPlaceTypes());
                longitude = place.getLatLng().longitude;
                latitude = place.getLatLng().latitude;
                updateUI();
                Log.d("result", longitude+" "+latitude);
            }
        }
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            if(broadcastReceiver!=null)  unregisterReceiver(broadcastReceiver);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 지도 초기 상태 설정
     * */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        uiSettings = mMap.getUiSettings();

        //마시멜로우 버전 이상일 때만 권한 설정이 적용
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //권한이 없을 때
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            }
        } else {//마시멜로우 이하일때
            mMap.setOnMapClickListener(this);
            uiSettings.setZoomControlsEnabled(true);

            // 초기 gps - 최근에 찍었던 장소를 표시해준다.
            PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace(googleApiClient, null);
            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(PlaceLikelihoodBuffer placeLikelihoods) {
                    Iterator<PlaceLikelihood> it = placeLikelihoods.iterator();
                    while(it.hasNext()){
                        PlaceLikelihood temp = it.next();
                        LatLng tempLat = temp.getPlace().getLatLng();
                        longitude = tempLat.longitude;
                        latitude = tempLat.latitude;
                    }
                    Log.d("gps init", latitude+" "+longitude);
                    updateUI();
                }
            });
        }
    }

    /**
     * 화면 업데이트
     * */
    private void updateUI() {
        mMap.clear();
        progressBar.setVisibility(View.VISIBLE);
        MarkerOptions marker = new MarkerOptions().position(new LatLng(latitude, longitude)).title("MyLoc");
        mMap.addMarker(marker);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 17));
        CircleOptions circle = new CircleOptions().center(new LatLng(latitude, longitude))
                .radius(StaticVariable.RADIUS)// 100m 반경의 원을 그린다.
                .strokeColor(Color.RED)
                .strokeWidth(3);
        mMap.addCircle(circle);
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {    }


    //사용자가 지도를 클릭했을 때 처리하는 함수
    @Override
    public void onMapClick(LatLng latLng) {
        longitude = latLng.longitude;
        latitude = latLng.latitude;
        Log.d("gps touch", longitude+" "+latitude);
        updateUI();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == gyroSensor.TYPE_GYROSCOPE){
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            String gyroString = "자이로스코프값 : " + "x : " + x + ", y : " + y + ", z : " +z;

            gyroTextView.setText(gyroString);
        }
    }

    //리스너 등록
    protected void onResume(){
        super.onResume();
        mSensorManager.registerListener(this,gyroSensor,SensorManager.SENSOR_DELAY_NORMAL);
    }
    //리스너 해제
    protected void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class GetXMLTask extends AsyncTask<String, Void, Document> {

        @Override
        protected Document doInBackground(String... urls) {
            URL url;
            try {
                url = new URL(urls[0]);
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder(); //XML문서 빌더 객체를 생성
                doc = db.parse(new InputSource(url.openStream())); //XML문서를 파싱한다.
                doc.getDocumentElement().normalize();

            } catch (Exception e) {
                Toast.makeText(getBaseContext(), "Parsing Error", Toast.LENGTH_SHORT).show();
            }
            return doc;
        }

        @Override
        protected void onPostExecute(Document doc) {

            String s = "";
            //data태그가 있는 노드를 찾아서 리스트 형태로 만들어서 반환
            NodeList nodeList = doc.getElementsByTagName("data");
            //data 태그를 가지는 노드를 찾음, 계층적인 노드 구조를 반환

            int i = 0 ;
            //날씨 데이터를 추출
            s += "현 위치의 날씨 정보: ";
            Node node = nodeList.item(i);
            Element fstElmnt = (Element) node;
            NodeList nameList  = fstElmnt.getElementsByTagName("temp");
            Element nameElement = (Element) nameList.item(0);
            nameList = nameElement.getChildNodes();
            s += "온도 = "+ ((Node) nameList.item(0)).getNodeValue() +" ,";

            NodeList websiteList = fstElmnt.getElementsByTagName("reh");
            s += "습도 = "+  websiteList.item(0).getChildNodes().item(0).getNodeValue() +"\n";


            textview.setText(s);

            super.onPostExecute(doc);
        }


    }//end inner class - GetXMLTask
}
