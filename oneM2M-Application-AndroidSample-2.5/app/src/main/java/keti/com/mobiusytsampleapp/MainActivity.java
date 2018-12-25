package keti.com.mobiusytsampleapp;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements Button.OnClickListener {
    public TextView textViewData;
    public ImageView imageViewLogo;
    public static Handler handler;

    public static CSEBase csebase = new CSEBase();
    public static AE ae = new AE();
    public static String TAG = "MainActivity";
    private String MQTTPort = "1883";
    public static String ServiceAEName = "Calorie_Defender";
    public static String MQTT_Req_Topic = "";
    public static String MQTT_Resp_Topic = "";
    public static MqttAndroidClient mqttClient = null;

    public Button btnMain;
    public Button btnGpsSet;
    public Button btnState;
    public Button btnPedometer;
    public Button btnLock;
    public Button btnUnlock;

    public static double[] temp_latitude_array = new double[20];
    public static double[] temp_longitude_array = new double[20];
    public static int temp_count = 0;
    public static int real_count = 0; // DB 행 개수

    public static String current_latitude = "37.56421";
    public static String current_longitude = "127.00169";

    public float distance = 100;
    public int sec = 0;

    public String convertedAddress;

    public DBHelper dbHelper;
    public SQLiteDatabase db;
    public Cursor cursor;

    // Main
    public MainActivity() {
        handler = new Handler();
    }
    /* onCreate */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewData = (TextView) findViewById(R.id.textViewData);
        imageViewLogo = (ImageView) findViewById(R.id.imageViewLogo);

        btnMain = (Button) findViewById(R.id.btnMain);
        btnGpsSet = (Button) findViewById(R.id.btnGpsSet);
        btnState = (Button) findViewById(R.id.btnState);
        btnPedometer = (Button) findViewById(R.id.btnPedometer);
        btnLock = (Button) findViewById(R.id.btnLock);
        btnUnlock = (Button) findViewById(R.id.btnUnlock);

        btnMain.setOnClickListener(this);
        btnGpsSet.setOnClickListener(this);
        btnState.setOnClickListener(this);
        btnPedometer.setOnClickListener(this);
        btnLock.setOnClickListener(this);
        btnUnlock.setOnClickListener(this);

        // Create AE and Get AEID
        GetAEInfo();

        /* DB에 저장한 위치 개수 반환 */
        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();
        cursor = db.rawQuery("select * from location", null);
        cursor.moveToFirst();
        real_count = cursor.getCount();
    }

    /* AE Create for Androdi AE */
    public void GetAEInfo() {
        csebase.setInfo("203.253.128.161","7579","Mobius","1883");
        //csebase.setInfo("203.253.128.151","7579","Mobius","1883");
        // AE Create for Android AE
        ae.setAppName("Calorie_Defender");
        aeCreateRequest aeCreate = new aeCreateRequest();
        aeCreate.setReceiver(new IReceived() {
            public void getResponseBody(final String msg) {
                handler.post(new Runnable() {
                    public void run() {
                        Log.d(TAG, "** AE Create ResponseCode[" + msg +"]");
                        if( Integer.parseInt(msg) == 201 ){
                            MQTT_Req_Topic = "/oneM2M/req/Mobius2/"+ae.getAEid()+"_sub"+"/#";
                            MQTT_Resp_Topic = "/oneM2M/resp/Mobius2/"+ae.getAEid()+"_sub"+"/json";
                            Log.d(TAG, "ReqTopic["+ MQTT_Req_Topic+"]");
                            Log.d(TAG, "ResTopic["+ MQTT_Resp_Topic+"]");
                        }
                        else { // If AE is Exist , GET AEID
                            aeRetrieveRequest aeRetrive = new aeRetrieveRequest();
                            aeRetrive.setReceiver(new IReceived() {
                                public void getResponseBody(final String resmsg) {
                                    handler.post(new Runnable() {
                                        public void run() {
                                            Log.d(TAG, "** AE Retrive ResponseCode[" + resmsg +"]");
                                            MQTT_Req_Topic = "/oneM2M/req/Mobius2/"+ae.getAEid()+"_sub"+"/#";
                                            MQTT_Resp_Topic = "/oneM2M/resp/Mobius2/"+ae.getAEid()+"_sub"+"/json";
                                            Log.d(TAG, "ReqTopic["+ MQTT_Req_Topic+"]");
                                            Log.d(TAG, "ResTopic["+ MQTT_Resp_Topic+"]");
                                        }
                                    });
                                }
                            });
                            aeRetrive.start();
                        }
                    }
                });
            }
        });
        aeCreate.start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnMain: {
                break;
            }
            case R.id.btnGpsSet: {
                RetrieveRequest reqLatitude = new RetrieveRequest();
                reqLatitude.setReceiver(new IReceived() {
                    public void getResponseBody(final String msg) {
                        handler.post(new Runnable() {
                            public void run() {
                                current_latitude = msg.replaceAll(".*<con>|</con>.*","");
                            }
                        });
                    }
                });
                reqLatitude.start();

                RetrieveRequest reqLongitude = new RetrieveRequest();
                reqLongitude.ContainerName = "gps/longitude";
                reqLongitude.setReceiver(new IReceived() {
                    public void getResponseBody(final String msg) {
                        handler.post(new Runnable() {
                            public void run() {
                                current_longitude = msg.replaceAll(".*<con>|</con>.*","");
                                // textViewData.setText("현재 위도: " + current_latitude + "\n" + "현재 경도: " + current_longitude);
                            }
                        });
                    }
                });
                reqLongitude.start();

                Intent intent = new Intent(getApplicationContext(), GPSSet.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btnState: {
                Intent intent = new Intent(getApplicationContext(), State.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btnPedometer: {
                Intent intent = new Intent(getApplicationContext(), Pedometer.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btnLock: {
                textViewData.setVisibility(View.VISIBLE);
                imageViewLogo.setVisibility(View.GONE);

                RetrieveRequest reqLatitude = new RetrieveRequest();
                reqLatitude.setReceiver(new IReceived() {
                    public void getResponseBody(final String msg) {
                        handler.post(new Runnable() {
                            public void run() {
                                current_latitude = msg.replaceAll(".*<con>|</con>.*","");
                            }
                        });
                    }
                });
                reqLatitude.start();

                RetrieveRequest reqLongitude = new RetrieveRequest();
                reqLongitude.ContainerName = "gps/longitude";
                reqLongitude.setReceiver(new IReceived() {
                    public void getResponseBody(final String msg) {
                        handler.post(new Runnable() {
                            public void run() {
                                current_longitude = msg.replaceAll(".*<con>|</con>.*","");
                                // textViewData.setText("현재 위도: " + current_latitude + "\n" + "현재 경도: " + current_longitude);
                            }
                        });
                    }
                });
                reqLongitude.start();

                Location locationA = new Location("point A");
                locationA.setLatitude(Double.parseDouble(current_latitude));
                locationA.setLongitude(Double.parseDouble(current_longitude));

                cursor = db.rawQuery("select * from location", null); // cursor 초기화
                cursor.moveToFirst();

                for(int i = 0; i < real_count; i++) {
                    Location locationB = new Location("point B");

                    locationB.setLatitude(Double.parseDouble(cursor.getString(1)));
                    locationB.setLongitude(Double.parseDouble(cursor.getString(2)));

                    distance = locationA.distanceTo(locationB);

                    /* 지정된 위치와 50m 이내에 있을 경우 */
                    if(distance <= 50) {
                        Geocoder geocoder = new Geocoder(this, Locale.KOREA);
                        List<Address> address = null;

                        try {
                            address = geocoder.getFromLocation(Double.parseDouble(cursor.getString(1)), Double.parseDouble(cursor.getString(2)), 1);
                            if (address != null && address.size() > 0) {
                                convertedAddress = address.get(0).getAddressLine(0);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e("State", "Conversion error from latitude and longitude to address");
                        }

                        textViewData.setText("현재 지정하신 위치 " + convertedAddress + "에서 " + Math.round(distance) + "M 이내에 있습니다.\n\n" +
                                "50M 이상 떨어진 후 잠금을 해제해주시기 바랍니다.");

                        break;
                    }
                    /*
                    textViewData.setText("위도: " + Double.parseDouble(current_latitude) + "경도: " + Double.parseDouble(current_longitude) + "\n" +
                            "위도: " + cursor.getString(1) + "경도: " + cursor.getString(2) + "\n" + "차이: " + String.valueOf(distance));
                    */

                    cursor.moveToNext(); // 다음 행
                }

                if(distance > 50) {
                    sec = 0;
                    btnLock.setVisibility(View.GONE);
                    btnUnlock.setVisibility(View.VISIBLE);

                    ControlRequest req = new ControlRequest("0");
                    req.setReceiver(new IReceived() {
                        public void getResponseBody(final String msg) {
                            handler.post(new Runnable() {
                                public void run() {
                                    Toast.makeText(MainActivity.this, "잠금 해제에 성공하였습니다.\n\n30초 후에 다시 잠금 상태로 변경됩니다.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                    req.start();

                    final Timer m_timer = new Timer();
                    TimerTask m_task = new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(sec < 30) {
                                        sec++;
                                        textViewData.setText(sec + "초가 경과하였습니다...");
                                    }

                                    else {
                                        btnLock.setVisibility(View.VISIBLE);
                                        btnUnlock.setVisibility(View.GONE);

                                        ControlRequest req = new ControlRequest("90");
                                        req.setReceiver(new IReceived() {
                                            public void getResponseBody(final String msg) {
                                                handler.post(new Runnable() {
                                                    public void run() {
                                                        textViewData.setText("잠금 상태로 변경되었습니다.");
                                                    }
                                                });
                                            }
                                        });
                                        req.start();

                                        m_timer.cancel();
                                    }
                                }
                            });
                        }
                    };
                    m_timer.schedule(m_task, 1000, 1000);
                }
                break;
            }
        }
    }
    @Override
    public void onStart() {
        super.onStart();

    }
    @Override
    public void onStop() {
        super.onStop();

    }

    /* Response callback Interface */
    public interface IReceived {
        void getResponseBody(String msg);
    }

    /* Request latest latitude and longitude*/
    class RetrieveRequest extends Thread {
        private final Logger LOG = Logger.getLogger(RetrieveRequest.class.getName());
        private IReceived receiver;
        private String ContainerName = "gps/latitude";

        public RetrieveRequest(String containerName) {
            this.ContainerName = containerName;
        }
        public RetrieveRequest() {}
        public void setReceiver(IReceived hanlder) { this.receiver = hanlder; }

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

    /* Request Control Lock */
    class ControlRequest extends Thread {
        private final Logger LOG = Logger.getLogger(ControlRequest.class.getName());
        private IReceived receiver;
        private String container_name = "lock";

        public ContentInstanceObject contentinstance;
        public ControlRequest(String comm) {
            contentinstance = new ContentInstanceObject();
            contentinstance.setContent(comm);
        }
        public void setReceiver(IReceived hanlder) { this.receiver = hanlder; }

        @Override
        public void run() {
            try {
                String sb = csebase.getServiceUrl() +"/" + ServiceAEName + "/" + container_name;

                URL mUrl = new URL(sb);

                HttpURLConnection conn = (HttpURLConnection) mUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(false);

                conn.setRequestProperty("Accept", "application/xml");
                conn.setRequestProperty("Content-Type", "application/vnd.onem2m-res+xml;ty=4");
                conn.setRequestProperty("locale", "ko");
                conn.setRequestProperty("X-M2M-RI", "12345");
                conn.setRequestProperty("X-M2M-Origin", ae.getAEid() );

                String reqContent = contentinstance.makeXML();
                conn.setRequestProperty("Content-Length", String.valueOf(reqContent.length()));

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.write(reqContent.getBytes());
                dos.flush();
                dos.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String resp = "";
                String strLine="";
                while ((strLine = in.readLine()) != null) {
                    resp += strLine;
                }
                if (resp != "") {
                    receiver.getResponseBody(resp);
                }
                conn.disconnect();

            } catch (Exception exp) {
                LOG.log(Level.SEVERE, exp.getMessage());
            }
        }
    }
    /* Request AE Creation */
    class aeCreateRequest extends Thread {
        private final Logger LOG = Logger.getLogger(aeCreateRequest.class.getName());
        String TAG = aeCreateRequest.class.getName();
        private IReceived receiver;
        int responseCode=0;
        public ApplicationEntityObject applicationEntity;
        public void setReceiver(IReceived hanlder) { this.receiver = hanlder; }
        public aeCreateRequest(){
            applicationEntity = new ApplicationEntityObject();
            applicationEntity.setResourceName(ae.getappName());
        }
        @Override
        public void run() {
            try {

                String sb = csebase.getServiceUrl();
                URL mUrl = new URL(sb);

                HttpURLConnection conn = (HttpURLConnection) mUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(false);

                conn.setRequestProperty("Content-Type", "application/vnd.onem2m-res+xml;ty=2");
                conn.setRequestProperty("Accept", "application/xml");
                conn.setRequestProperty("locale", "ko");
                conn.setRequestProperty("X-M2M-Origin", "S"+ae.getappName());
                conn.setRequestProperty("X-M2M-RI", "12345");
                conn.setRequestProperty("X-M2M-NM", ae.getappName() );

                String reqXml = applicationEntity.makeXML();
                conn.setRequestProperty("Content-Length", String.valueOf(reqXml.length()));

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.write(reqXml.getBytes());
                dos.flush();
                dos.close();

                responseCode = conn.getResponseCode();

                BufferedReader in = null;
                String aei = "";
                if (responseCode == 201) {
                    // Get AEID from Response Data
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String resp = "";
                    String strLine;
                    while ((strLine = in.readLine()) != null) {
                        resp += strLine;
                    }

                    ParseElementXml pxml = new ParseElementXml();
                    aei = pxml.GetElementXml(resp, "aei");
                    ae.setAEid( aei );
                    Log.d(TAG, "Create Get AEID[" + aei + "]");
                    in.close();
                }
                if (responseCode != 0) {
                    receiver.getResponseBody( Integer.toString(responseCode) );
                }
                conn.disconnect();
            } catch (Exception exp) {
                LOG.log(Level.SEVERE, exp.getMessage());
            }

        }
    }
    /* Retrieve AE-ID */
    class aeRetrieveRequest extends Thread {
        private final Logger LOG = Logger.getLogger(aeCreateRequest.class.getName());
        private IReceived receiver;
        int responseCode=0;

        public aeRetrieveRequest() {
        }
        public void setReceiver(IReceived hanlder) {
            this.receiver = hanlder;
        }

        @Override
        public void run() {
            try {
                String sb = csebase.getServiceUrl()+"/"+ ae.getappName();
                URL mUrl = new URL(sb);

                HttpURLConnection conn = (HttpURLConnection) mUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.setDoOutput(false);

                conn.setRequestProperty("Accept", "application/xml");
                conn.setRequestProperty("X-M2M-RI", "12345");
                conn.setRequestProperty("X-M2M-Origin", "Sandoroid");
                conn.setRequestProperty("nmtype", "short");
                conn.connect();

                responseCode = conn.getResponseCode();

                BufferedReader in = null;
                String aei = "";
                if (responseCode == 200) {
                    // Get AEID from Response Data
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String resp = "";
                    String strLine;
                    while ((strLine = in.readLine()) != null) {
                        resp += strLine;
                    }

                    ParseElementXml pxml = new ParseElementXml();
                    aei = pxml.GetElementXml(resp, "aei");
                    ae.setAEid( aei );
                    //Log.d(TAG, "Retrieve Get AEID[" + aei + "]");
                    in.close();
                }
                if (responseCode != 0) {
                    receiver.getResponseBody( Integer.toString(responseCode) );
                }
                conn.disconnect();
            } catch (Exception exp) {
                LOG.log(Level.SEVERE, exp.getMessage());
            }
        }
    }
}
