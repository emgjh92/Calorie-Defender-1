package keti.com.mobiusytsampleapp;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.location.Geocoder;
import android.widget.ListView;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import 	java.io.IOException;
import java.util.Random;

public class State extends AppCompatActivity implements Button.OnClickListener {
    public Button btnMain;
    public Button btnGpsSet;
    public Button btnState;
    public Button btnPedometer;
    public Button btnDelete;

    public ListView listView;

    public ArrayList<String> convertedAddress;
    public ArrayAdapter adapter;

    /* DB */
    public DBHelper dbHelper;
    public SQLiteDatabase db;
    public Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_state);

        btnMain = (Button) findViewById(R.id.btnMain);
        btnGpsSet = (Button) findViewById(R.id.btnGpsSet);
        btnState = (Button) findViewById(R.id.btnState);
        btnPedometer = (Button) findViewById(R.id.btnPedometer);
        btnDelete = (Button) findViewById(R.id.btnDelete);

        btnMain.setOnClickListener(this);
        btnGpsSet.setOnClickListener(this);
        btnState.setOnClickListener(this);
        btnPedometer.setOnClickListener(this);
        btnDelete.setOnClickListener(this);

        convertedAddress = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, convertedAddress);

        listView = (ListView) findViewById(R.id.listViewAddress);
        listView.setAdapter(adapter);

        /* DB */
        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();
        cursor = db.rawQuery("select * from location", null);
        cursor.moveToFirst();

        /* 위도, 경도를 주소로 변환 */
        Geocoder geocoder = new Geocoder(State.this, Locale.KOREA);
        List<Address> address = null;

        String x = "";

        for (int i = 0; i < MainActivity.real_count; i++) {
            try {
                address = geocoder.getFromLocation(Double.parseDouble(cursor.getString(1)), Double.parseDouble(cursor.getString(2)), 1);
                //x = x + i + ". " + cursor.getString(1) + " " +cursor.getString(2) + "\n"; // test
                cursor.moveToNext();
                if (address != null && address.size() > 0) {
                    convertedAddress.add(i + 1 + ". " + address.get(0).getAddressLine(0));
                }
                else {
                    convertedAddress.add(i + 1 + ". " + "주소 정보가 존재하지 않습니다.");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("State", "Conversion error from latitude and longitude to address");
            }
        }

        //Toast.makeText(State.this, x, Toast.LENGTH_LONG).show(); // test

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnMain: {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btnGpsSet: {
                Intent intent = new Intent(getApplicationContext(), GPSSet.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btnState: {
                break;
            }
            case R.id.btnPedometer: {
                Intent intent = new Intent(getApplicationContext(), Pedometer.class);
                startActivity(intent);
                finish();
                break;
            }
            case R.id.btnDelete: {
                Delete();
                break;
            }
        }
    }

    void Delete()
    {
        Random random = new Random();

        int x = random.nextInt(100);
        int y = random.nextInt(100);
        final int z = x + y;

        final EditText edittext = new EditText(State.this);

        AlertDialog.Builder builder = new AlertDialog.Builder(State.this);
        builder.setMessage("다음 문제를 풀어주세요.\n" + x + " + " + y + " = ");
        builder.setView(edittext);
        builder.setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(String.valueOf(z).equals(edittext.getText().toString())) {
                            Toast.makeText(State.this,"정답입니다.\n선택한 부분을 삭제합니다.",Toast.LENGTH_LONG).show();

                            int count, checked;
                            count = adapter.getCount();

                            if (count > 0) {
                                checked = listView.getCheckedItemPosition();
                                if (checked > -1 && checked < count) {
                                    convertedAddress.remove(checked);
                                    listView.clearChoices();
                                    adapter.notifyDataSetChanged();

                                    /* 전역변수 MainActivity.real_latitude_array, MainActivity.real_longitude_array에 변경된 내용으로 적용 */
                                    // MainActivity.real_count--;

                                    cursor = db.rawQuery("select * from location", null);
                                    cursor.moveToFirst();
                                    cursor.moveToPosition(checked);

                                    db.execSQL("delete from location where _id='" + cursor.getString(0) + "'");
                                }
                            }
                        }
                        else {
                            Toast.makeText(State.this,"오답입니다.\n다시 시도해주세요.",Toast.LENGTH_LONG).show();
                        }
                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(State.this,"취소되었습니다.",Toast.LENGTH_LONG).show();
                    }
                });
        builder.show();
    }
}