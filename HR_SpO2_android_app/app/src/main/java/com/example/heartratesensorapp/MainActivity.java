package com.example.heartratesensorapp;

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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "MainActivity";

    BluetoothAdapter mBluetoothAdapter;
    Button btnEnableDisable_Discoverable;

    BluetoothConnectionService mBluetoothConnection;

    Button btnStartConnection;
    Button btnSend;
    Button btn_check_pulse;

    TextView incomingMessages;
    StringBuilder messages;


    EditText etSend;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnONOFF = (Button) findViewById(R.id.btnONOFF);
        btnEnableDisable_Discoverable = (Button) findViewById(R.id.btnDiscoverable_on_off);
        //Button btnDiscover = (Button) findViewById(R.id.btnFindUnpairedDevices);

        //UWAGA BRAKUJE TYPU POWYZEJ 2 LINIJKI + PRZYCISKU DISCOVER
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();

        btnStartConnection = (Button) findViewById(R.id.btnStartConnection);
        btnSend = (Button) findViewById(R.id.btnSend);
        etSend = (EditText) findViewById(R.id.editText);
        btn_check_pulse = (Button) findViewById(R.id.btn_check_pulse);

        incomingMessages = (TextView) findViewById(R.id.incomingMessage);
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
                messages.setLength(0);
                incomingMessages.setText("");

                //everytime user clicks burrons check pulse
                //need to clean up the container before another pulse display!
                //incomingMessages.setText("");
                //send request to STM32:
                mBluetoothConnection.write(bytes);

            }


        });




    }



    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");
            //String text = intent.putExtra("theMessage", edt_name.getText().toString());

            //messages.append(text + "\n");
            messages.append(text);
            incomingMessages.setText(messages + "BPM");

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

    public void btnEnableDisable_Discoverable(View view) {
        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2, intentFilter);
    }

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






    
}


