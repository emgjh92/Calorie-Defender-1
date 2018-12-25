package keti.com.mobiusytsampleapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Pedometer extends AppCompatActivity implements Button.OnClickListener {
    MqttAndroidClient mqttClient = MainActivity.mqttClient;
    Handler handler = MainActivity.handler;
    String TAG = MainActivity.TAG;
    String MQTT_Req_Topic = MainActivity.MQTT_Req_Topic;
    String MQTT_Resp_Topic = MainActivity.MQTT_Resp_Topic;
    CSEBase csebase = MainActivity.csebase;
    AE ae = MainActivity.ae;
    String ServiceAEName = MainActivity.ServiceAEName;

    public Button btnMain;
    public Button btnGpsSet;
    public Button btnState;
    public Button btnPedometer;
    public Button btnEnrollment;

    public TextView textViewData;
    public TextView textViewCalorie;
    public EditText editTextHeight;
    public EditText editTextWeight;

    public String stepsCount = "";

    static public double height = 170;
    static public double weight = 70;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedometer);

        RetrieveRequest reqPedometer = new RetrieveRequest();
        reqPedometer.setReceiver(new MainActivity.IReceived() {
            public void getResponseBody(final String msg) {
                handler.post(new Runnable() {
                    public void run() {
                        stepsCount = msg.replaceAll(".*<con>|</con>.*","");
                        textViewData.setText("걸음 수\n\n" + stepsCount + "보");

                        CalorieBurnedCalculator();
                    }
                });
            }
        });
        reqPedometer.start();

        btnMain = (Button) findViewById(R.id.btnMain);
        btnGpsSet = (Button) findViewById(R.id.btnGpsSet);
        btnState = (Button) findViewById(R.id.btnState);
        btnPedometer = (Button) findViewById(R.id.btnPedometer);
        btnEnrollment = (Button) findViewById(R.id.btnEnrollment);

        btnMain.setOnClickListener(this);
        btnGpsSet.setOnClickListener(this);
        btnState.setOnClickListener(this);
        btnPedometer.setOnClickListener(this);
        btnEnrollment.setOnClickListener(this);

        textViewData = (TextView) findViewById(R.id.textViewData);
        textViewCalorie = (TextView) findViewById(R.id.textViewCalorie);
        editTextHeight = (EditText) findViewById(R.id.editTextHeight);
        editTextWeight = (EditText) findViewById(R.id.editTextWeight);

        editTextHeight.setText(Double.toString(height));
        editTextWeight.setText(Double.toString(weight));

        Log.d(TAG, "MQTT Create");
        MQTT_Create(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnMain: {
                Log.d(TAG, "MQTT Close");
                MQTT_Create(false);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btnGpsSet: {
                Log.d(TAG, "MQTT Close");
                MQTT_Create(false);

                Intent intent = new Intent(getApplicationContext(), GPSSet.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btnState: {
                Log.d(TAG, "MQTT Close");
                MQTT_Create(false);

                Intent intent = new Intent(getApplicationContext(), State.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btnPedometer: {
                break;
            }
            case R.id.btnEnrollment: {
                height = Double.parseDouble(editTextHeight.getText().toString());
                weight = Double.parseDouble(editTextWeight.getText().toString());

                CalorieBurnedCalculator();
                Toast.makeText(Pedometer.this, "등록되었습니다.", Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    public void MQTT_Create(boolean mtqqStart) {
        if (mtqqStart && mqttClient == null) {
            /* Subscription Resource Create to Yellow Turtle */
            Pedometer.SubscribeResource subcribeResource = new Pedometer.SubscribeResource();
            subcribeResource.setReceiver(new MainActivity.IReceived() {
                public void getResponseBody(final String msg) {
                    handler.post(new Runnable() {
                        public void run() {
                            // textViewData.setText("Subscription Resource Create 요청 결과\n\n" + msg);
                        }
                    });
                }
            });
            subcribeResource.start();

            /* MQTT Subscribe */
            mqttClient = new MqttAndroidClient(this.getApplicationContext(), "tcp://" + csebase.getHost() + ":" + csebase.getMQTTPort(), MqttClient.generateClientId());
            mqttClient.setCallback(mainMqttCallback);
            try {
                IMqttToken token = mqttClient.connect();
                token.setActionCallback(mainIMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            /* MQTT unSubscribe or Client Close */
            mqttClient.setCallback(null);
            mqttClient.close();
            mqttClient = null;
        }
    }

    private IMqttActionListener mainIMqttActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.d(TAG, "onSuccess");
            String payload = "";
            int mqttQos = 1; /* 0: NO QoS, 1: No Check , 2: Each Check */

            MqttMessage message = new MqttMessage(payload.getBytes());
            try {
                mqttClient.subscribe(MQTT_Req_Topic, mqttQos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.d(TAG, "onFailure");
        }
    };
    /* MQTT Broker Message Received */
    private MqttCallback mainMqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            Log.d(TAG, "connectionLost");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {

            Log.d(TAG, "messageArrived");

            if(message.toString().contains("/pedometer/")) {
                stepsCount = message.toString().replaceAll(".*,\"con\":\"|\",\"cr\":\".*", "");
                textViewData.setText("걸음 수\n\n" + stepsCount + "보");
                CalorieBurnedCalculator();
            }

            Log.d(TAG, "Notify ResMessage:" + message.toString());

            /* Json Type Response Parsing */
            String retrqi = MqttClientRequestParser.notificationJsonParse(message.toString());
            Log.d(TAG, "RQI["+ retrqi +"]");

            String responseMessage = MqttClientRequest.notificationResponse(retrqi);
            Log.d(TAG, "Recv OK ResMessage ["+responseMessage+"]");

            /* Make json for MQTT Response Message */
            MqttMessage res_message = new MqttMessage(responseMessage.getBytes());

            try {
                mqttClient.publish(MQTT_Resp_Topic, res_message);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(TAG, "deliveryComplete");
        }

    };

    class SubscribeResource extends Thread {
        private final Logger LOG = Logger.getLogger(SubscribeResource.class.getName());
        private MainActivity.IReceived receiver;
        private String pedometer = "pedometer";

        public ContentSubscribeObject subscribeInstance;

        public SubscribeResource() {
            subscribeInstance = new ContentSubscribeObject();
            subscribeInstance.setUrl(csebase.getHost());
            subscribeInstance.setResourceName(ae.getAEid() + "_rn");
            subscribeInstance.setPath(ae.getAEid() + "_sub");
            subscribeInstance.setOrigin_id(ae.getAEid());
        }

        public void setReceiver(MainActivity.IReceived hanlder) {
            this.receiver = hanlder;
        }

        @Override
        public void run() {
            try {
                String sb = csebase.getServiceUrl() + "/" + ServiceAEName + "/" + pedometer;

                URL mUrl = new URL(sb);

                HttpURLConnection conn = (HttpURLConnection) mUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(false);

                conn.setRequestProperty("Accept", "application/xml");
                conn.setRequestProperty("Content-Type", "application/vnd.onem2m-res+xml; ty=23");
                conn.setRequestProperty("locale", "ko");
                conn.setRequestProperty("X-M2M-RI", "12345");
                conn.setRequestProperty("X-M2M-Origin", ae.getAEid());

                String reqmqttContent = subscribeInstance.makeXML();
                conn.setRequestProperty("Content-Length", String.valueOf(reqmqttContent.length()));

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.write(reqmqttContent.getBytes());
                dos.flush();
                dos.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String resp = "";
                String strLine = "";
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

    /* Request pedometer */
    class RetrieveRequest extends Thread {
        private final Logger LOG = Logger.getLogger(MainActivity.RetrieveRequest.class.getName());
        private MainActivity.IReceived receiver;
        private String ContainerName = "pedometer";

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

    /* 만보기 칼로리 소모량 구하는 식 https://fitness.stackexchange.com/questions/25472/how-to-calculate-calorie-from-pedometer 참고 */
    void CalorieBurnedCalculator() {
        double walkingFactor = 0.57;
        double CaloriesBurnedPerMile;
        double strip;
        double stepCountMile;
        double conversationFactor;
        double CaloriesBurned;

        CaloriesBurnedPerMile = walkingFactor * (weight * 2.2);
        strip = height * 0.415;
        stepCountMile = 160934.4 / strip;
        conversationFactor = CaloriesBurnedPerMile / stepCountMile;
        CaloriesBurned = Double.parseDouble(stepsCount) * conversationFactor;
        textViewCalorie.setText(Math.round(CaloriesBurned) + " kcal 소모");
    }
}
