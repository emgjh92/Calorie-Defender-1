package keti.com.mobiusytsampleapp;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.app.FragmentManager;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;

import org.eclipse.paho.android.service.MqttAndroidClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class GPSSet extends AppCompatActivity implements Button.OnClickListener, OnMapReadyCallback {
    Handler handler = MainActivity.handler;
    CSEBase csebase = MainActivity.csebase;
    AE ae = MainActivity.ae;
    String ServiceAEName = MainActivity.ServiceAEName;

    public Button btnMain;
    public Button btnGpsSet;
    public Button btnState;
    public Button btnPedometer;
    public Button btnRefresh;
    public Button btnEnrollment;

    public TextView textViewLimit;

    /* DB */
    public DBHelper dbHelper;
    public SQLiteDatabase db;
    public Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_set);

        RetrieveRequest reqLatitude = new RetrieveRequest();
        reqLatitude.setReceiver(new MainActivity.IReceived() {
            public void getResponseBody(final String msg) {
                handler.post(new Runnable() {
                    public void run() {
                        MainActivity.current_latitude = msg.replaceAll(".*<con>|</con>.*","");
                    }
                });
            }
        });
        reqLatitude.start();

        RetrieveRequest reqLongitude = new RetrieveRequest();
        reqLongitude.ContainerName = "gps/longitude";
        reqLongitude.setReceiver(new MainActivity.IReceived() {
            public void getResponseBody(final String msg) {
                handler.post(new Runnable() {
                    public void run() {
                        MainActivity.current_longitude = msg.replaceAll(".*<con>|</con>.*","");
                    }
                });
            }
        });
        reqLongitude.start();

        textViewLimit = (TextView) findViewById(R.id.textViewLimit);
        textViewLimit.setText("최대 20개까지 등록할 수 있습니다.\n\n현재 " + MainActivity.real_count +"개가 등록되었습니다.");

        btnMain = (Button) findViewById(R.id.btnMain);
        btnGpsSet = (Button) findViewById(R.id.btnGpsSet);
        btnState = (Button) findViewById(R.id.btnState);
        btnPedometer = (Button) findViewById(R.id.btnPedometer);
        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        btnEnrollment = (Button) findViewById(R.id.btnEnrollment);

        btnMain.setOnClickListener(this);
        btnGpsSet.setOnClickListener(this);
        btnState.setOnClickListener(this);
        btnPedometer.setOnClickListener(this);
        btnRefresh.setOnClickListener(this);
        btnEnrollment.setOnClickListener(this);

        FragmentManager fragmentManager = getFragmentManager();
        MapFragment mapFragment = (MapFragment)fragmentManager.findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /* DB */
        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();
    }

    @Override
    public void onMapReady(final GoogleMap map) {
        LatLng currentLocation = new LatLng(Double.valueOf(MainActivity.current_latitude), Double.valueOf(MainActivity.current_longitude));

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLocation);
        markerOptions.title("현재위치");
        map.addMarker(markerOptions);

        /* 위치 조절 */
        map.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        map.animateCamera(CameraUpdateFactory.zoomTo(15));

        /* +, - 버튼 */
        map.getUiSettings().setZoomControlsEnabled(true);

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                /* 최대 20개 등록 가능 */
                if(MainActivity.real_count + MainActivity.temp_count < 20) {
                    MarkerOptions mOptions = new MarkerOptions();

                    mOptions.title("지정"); // 마커 타이틀

                    Double latitude = point.latitude; // 위도
                    Double longitude = point.longitude; // 경도

                    MainActivity.temp_latitude_array[MainActivity.temp_count] = latitude;
                    MainActivity.temp_longitude_array[MainActivity.temp_count] = longitude;
                    MainActivity.temp_count++;

                    mOptions.snippet(latitude.toString() + ", " + longitude.toString()); // 마커 스니펫

                    mOptions.position(new LatLng(latitude, longitude)); // LatLng: 위도 경도 쌍을 나타냄

                    map.addMarker(mOptions); // 마커(핀) 추가
                }
                else if(MainActivity.real_count + MainActivity.temp_count >= 20) {
                    // 경고창 띄우기(최대 20까지 등록 가능합니다. 현재 몇 개가 등록되었습니다.)
                    Toast.makeText(GPSSet.this, "더는 추가하실 수 없습니다.\n기존의 것을 삭제하고 추가해주시길 바랍니다.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnMain : {
                db.close();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btnGpsSet : {
                break;
            }
            case R.id.btnState: {
                db.close();
                Intent intent = new Intent(getApplicationContext(), State.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btnPedometer: {
                db.close();
                Intent intent = new Intent(getApplicationContext(), Pedometer.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btnRefresh: {
                MainActivity.temp_count = 0;

                db.close();
                Intent intent = new Intent(getApplicationContext(), GPSSet.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btnEnrollment: {
                Enrollment();
                break;
            }
        }
    }

    /* Request latest latitude and longitude*/
    class RetrieveRequest extends Thread {
        private final Logger LOG = Logger.getLogger(MainActivity.RetrieveRequest.class.getName());
        private MainActivity.IReceived receiver;
        private String ContainerName = "gps/latitude";

        public RetrieveRequest(String containerName) {
            this.ContainerName = containerName;
        }
        public RetrieveRequest() {}
        public void setReceiver(MainActivity.IReceived hanlder) { this.receiver = hanlder; }

        @Override
        public void run() {
            try {
                String sb = csebase.getServiceUrl() + "/" + ServiceAEName + "/" + ContainerName + "/" + "latest";

                URL mUrl = new URL(sb);

                HttpURLConnection conn = (HttpURLConnection) mUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.setDoOutput(false);

                conn.setRequestProperty("Accept", "application/xml");
                conn.setRequestProperty("X-M2M-RI", "12345");
                conn.setRequestProperty("X-M2M-Origin", ae.getAEid() );
                conn.setRequestProperty("nmtype", "long");
                conn.connect();

                String strResp = "";
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String strLine= "";
                while ((strLine = in.readLine()) != null) {
                    strResp += strLine;
                }

                if ( strResp != "" ) {
                    receiver.getResponseBody(strResp);
                }
                conn.disconnect();

            } catch (Exception exp) {
                LOG.log(Level.WARNING, exp.getMessage());
            }
        }
    }

    void Enrollment()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(GPSSet.this);
        builder.setTitle("등록");
        builder.setMessage("지정한 위치를 등록하시겠습니까?");
        builder.setPositiveButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        for(int i = 0; i < MainActivity.temp_count; i++) {
                            db.execSQL("insert into location values (null, '"
                                    + MainActivity.temp_latitude_array[i] + "', '"
                                    + MainActivity.temp_longitude_array[i] + "');");
                        }
                        cursor = db.rawQuery("select * from location", null);
                        cursor.moveToFirst();
                        MainActivity.real_count = cursor.getCount(); // 실제 위치가 등록된 개수
                        MainActivity.temp_count = 0;

                        Toast.makeText(GPSSet.this,"등록되었습니다.",Toast.LENGTH_LONG).show();

                        db.close();
                        Intent intent = new Intent(getApplicationContext(), GPSSet.class);
                        startActivity(intent);
                        finish();
                    }
                });
        builder.setNegativeButton("아니오",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(GPSSet.this,"취소되었습니다.",Toast.LENGTH_LONG).show();
                    }
                });
        builder.show();
    }
}