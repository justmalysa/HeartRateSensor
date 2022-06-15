package com.example.heartratesensorapp;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;


import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "MainActivity";

    BluetoothAdapter mBluetoothAdapter;
    //Button btnEnableDisable_Discoverable;

    BluetoothConnectionService mBluetoothConnection;

    Button btnStartConnection;
    Button btnSend;
    Button btn_check_pulse;
    Button update_graph_btn;




    TextView incomingMessages;
    StringBuilder messages;



    List<Integer> HR_values = new ArrayList<>();
    List<Integer> SPo2_values = new ArrayList<>();

    LineChart lineChart;



    EditText etSend;


    public ArrayList<String> xAXES = new ArrayList<>();

    public ArrayList<Entry> yAXES_HR = new ArrayList<>();
    public ArrayList<Entry> yAXES_Spo2 = new ArrayList<>();





    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothDevice mBTDevice;

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    ListView lvNewDevices;


    //Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //when discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };


    /*
    Broadcast Receiver for changes made to bluetooth states such as:
    Discoverability mode on/off or expire:
     */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, mBluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting.....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected");
                        break;
                }
            }
        }
    };
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);


                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view,mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };


    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG,"BroadcastReceiver: BOND_BONDED.");
                    mBTDevice = mDevice;

                }
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG,"BroadcastReceiver: BOND_BONDING.");
                }
                if(mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG,"BroadcastReceiver: BOND_NONE.");
                }

            }
        }
    };




    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
        unregisterReceiver(mReceiver);
    }


    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnONOFF = (Button) findViewById(R.id.btnONOFF);

        //btnEnableDisable_Discoverable = (Button) findViewById(R.id.btnDiscoverable_on_off);
        //Button btnDiscover = (Button) findViewById(R.id.btnFindUnpairedDevices);

        //UWAGA BRAKUJE TYPU POWYZEJ 2 LINIJKI + PRZYCISKU DISCOVER
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();

        btnStartConnection = (Button) findViewById(R.id.btnStartConnection);
        btnSend = (Button) findViewById(R.id.btnSend);
        etSend = (EditText) findViewById(R.id.editText);
        btn_check_pulse = (Button) findViewById(R.id.btn_check_pulse);
        update_graph_btn = (Button) findViewById(R.id.update_graph_btn);

        incomingMessages = (TextView) findViewById(R.id.incomingMessage);

        lineChart = (LineChart) findViewById(R.id.lineChart);



        /*
        double x=0.0;

        for (int i=0;i<HR_values.size()-1;i++) {
            x=x + 1.0;


            float hr_val_fl = HR_values.get(i);
            float sp_val_fl = SPo2_values.get(i);
            yAXES_HR.add(new Entry(hr_val_fl,i));
            yAXES_Spo2.add(new Entry(sp_val_fl,i));
            xAXES.add(i,String.valueOf(x));

        }
        String[] xaxes = new String[xAXES.size()];
            for(int i=0;i<xAXES.size();i++)
            {
                xaxes[i] = xAXES.get(i).toString();
            }

            ArrayList<ILineDataSet> lineDataSets = new ArrayList<>();

            LineDataSet lineDataSet1 = new LineDataSet(yAXES_HR,"HR data");
            lineDataSet1.setDrawCircles(false);
            lineDataSet1.setCircleColor(Color.RED);

        LineDataSet lineDataSet2 = new LineDataSet(yAXES_Spo2,"SPo2 data");
        lineDataSet2.setDrawCircles(false);
        lineDataSet2.setCircleColor(Color.BLUE);

        lineDataSets.add(lineDataSet1);
        lineDataSets.add(lineDataSet2);

        lineChart.setData(new LineData(lineDataSets));
        //lineChart.setVisibleXRangeMaximum(65f);

        */






        messages = new StringBuilder();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4,filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        lvNewDevices.setOnItemClickListener(MainActivity.this);

        btnONOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth");
                enable_disable_BT();
            }
        });

        btnStartConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                startConnection();
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());
                //take The message that was taken from user !!!
                mBluetoothConnection.write(bytes);

                etSend.setText("");
            }
        });


        btn_check_pulse.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                String request;
                request = "R";

                byte[] bytes = request.getBytes(Charset.defaultCharset());
                //take The message that was taken from user !!!

                //messages.setLength(0);
                //incomingMessages.setText("");

                //everytime user clicks burrons check pulse
                //need to clean up the container before another pulse display!
                //incomingMessages.setText("");
                //send request to STM32:
                mBluetoothConnection.write(bytes);

            }


        });



        update_graph_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                print_graph2();

            }


        });








    }







    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");
            //String text = intent.putExtra("theMessage", edt_name.getText().toString());



            messages.append(text);


            //format wiadomosci zawsze stały:
            //sp099hr080 > do rysowania wykresu!
            //hr00000080 > do rysowania tętna
            //stala dlugosc wiadomosci!!!
            if(messages.length() == 10)
            {
                int HR_data;
                int Spo2_data;
                String data = messages.toString();
                char[] array = data.toCharArray();
                HR_data = (Character.getNumericValue(array[7]) *100 + Character.getNumericValue(array[8])*10 +Character.getNumericValue(array[9]));

                Spo2_data = (Character.getNumericValue(array[2])*100 + Character.getNumericValue(array[3])*10 + Character.getNumericValue(array[4]));

                Log.d(TAG, String.valueOf(HR_data) +" "+ String.valueOf(Spo2_data));

                Log.d(TAG, "HR data request was send");
                //sytuacja gdy wysylane jest na zyczenie pojedynczy BPM a nie na bierząco!
                if(array[0]=='h') {
                    //gdy otrzymano wiadomosc zgodnie z requestem!
                    incomingMessages.setText(String.valueOf(HR_data) + " BPM");
                }
                messages.setLength(0);

                HR_values.add(HR_data);

                SPo2_values.add(Spo2_data);

                Log.d(TAG, "tablica HR= "+String.valueOf(HR_values) +"tablica SpO2= "+ String.valueOf(SPo2_values));

                if(HR_values.size()==100 && SPo2_values.size()==100)
                {
                    //na ten moment czyści po zapisaniu 6ciu wartosci - do poprawki!
                 HR_values.clear();
                 SPo2_values.clear();
                }

            }



        }
    };
    public void startConnection(){

        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG,"startBTConnection: Initializing RFCOMM Bluetooth connection");

        mBluetoothConnection.startClient(device,uuid);

    }

    public void enable_disable_BT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enable_disable_BT: Does not have BT capabilities. ");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enable_disable_BT: enabling BT. ");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        if (mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enable_disable_BT: disabling BT. ");
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);

        }
    }
/*
    public void btnEnableDisable_Discoverable(View view) {
        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2, intentFilter);
    }

 */

    public void btnDiscover(View view) {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");


        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }

        if (!mBluetoothAdapter.isDiscovering()) {

            checkBTPermissions();
            //mBluetoothAdapter.startDiscovery();

            if(mBluetoothAdapter.startDiscovery() == false)
            {
                Log.d(TAG, "mBluetoothAdapter.startDiscovery(): disocvery DIDN'T START!!");
            }
            else
            {
                Log.d(TAG, "mBluetoothAdapter.startDiscovery(): disocvery started properly ");
            }
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);



        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            Log.d(TAG,"checkBTPermissions: Checking required permissions...");

            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck+= this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if(permissionCheck !=0){

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},1001);

            }

        }else{
            Log.d(TAG,"checkBTPermissions: No need to check permissions, SDK version < LOLLIPOP.");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG,"onItemClick: You clicked on a device.");
        String deviceName = mBTDevices.get(position).getName();
        String deviceAddress = mBTDevices.get(position).getAddress();

        Log.d(TAG,"onItemClick: deviceName = "+ deviceName);
        Log.d(TAG,"onItemClick: deviceAddress = "+ deviceAddress);

        if(Build.VERSION.SDK_INT> Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG,"Trying to pair with" + deviceName);
            mBTDevices.get(position).createBond();

            mBTDevice = mBTDevices.get(position);
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        }

    }



public void print_graph(){
    double x=0.0;
    //
    for (int i=0;i<HR_values.size()-1;i++) {
        x=x + 1.0;


        float hr_val_fl = HR_values.get(i);
        float sp_val_fl = SPo2_values.get(i);
        yAXES_HR.add(new Entry(hr_val_fl,i));
        yAXES_Spo2.add(new Entry(sp_val_fl,i));
        xAXES.add(i,String.valueOf(x));

    }
    String[] xaxes = new String[xAXES.size()];
    for(int i=0;i<xAXES.size();i++)
    {
        xaxes[i] = xAXES.get(i).toString();
    }

    ArrayList<ILineDataSet> lineDataSets = new ArrayList<>();

    LineDataSet lineDataSet1 = new LineDataSet(yAXES_HR,"HR data");
    lineDataSet1.setDrawCircles(false);
    lineDataSet1.setCircleColor(Color.RED);

    LineDataSet lineDataSet2 = new LineDataSet(yAXES_Spo2,"SPo2 data");
    lineDataSet2.setDrawCircles(false);
    lineDataSet2.setCircleColor(Color.BLUE);

    lineDataSets.add(lineDataSet1);
    lineDataSets.add(lineDataSet2);

    lineChart.setData(new LineData(lineDataSets));


}

    public void print_graph2(){
        double x=0.0;
        //
        for (int i=0;i<SPo2_values.size()-1;i++) {
            x=x + 1.0;


            float hr_val_fl = HR_values.get(i);
            float sp_val_fl = SPo2_values.get(i);
            yAXES_HR.add(new Entry(i,hr_val_fl));
            yAXES_Spo2.add(new Entry(i,sp_val_fl));
            xAXES.add(i,String.valueOf(x));

        }
        String[] xaxes = new String[xAXES.size()];
        for(int i=0;i<xAXES.size();i++)
        {
            xaxes[i] = xAXES.get(i).toString();
        }

        ArrayList<ILineDataSet> lineDataSets = new ArrayList<>();

        LineDataSet lineDataSet1 = new LineDataSet(yAXES_HR,"HR data");
        //lineDataSet1.setDrawCircles(false);


        LineDataSet lineDataSet2 = new LineDataSet(yAXES_Spo2,"SPo2 data");
        //lineDataSet2.setDrawCircles(false);
        //lineDataSet2.setCircleColor(Color.BLUE);

        //lineDataSets.add(lineDataSet1);
        //lineDataSets.add(lineDataSet2);
        //lineChart.addDataSet(dataSet);

        lineChart.setData(new LineData(lineDataSet1));
        lineChart.invalidate();

        lineChart.setData(new LineData(lineDataSet2));
        lineChart.invalidate();


        lineChart.setNoDataText("Data not Available");
        lineDataSet1.setColor(Color.RED);
        lineDataSet2.setCircleColor(Color.BLUE);

        lineDataSet1.setFormLineWidth(3);
        lineDataSet2.setFormLineWidth(3);



    }
    
}


