package com.example.matos.bluetoothasync;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import java.util.Set;
import java.util.ArrayList;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class DeviceList extends AppCompatActivity {

    // Declaring
    Button btnPaired;
    ListView pairedDeviceList;

    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;

    public static String EXTRA_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        btnPaired = (Button)findViewById(R.id.find);
        pairedDeviceList = (ListView)findViewById(R.id.listView);

        myBluetooth = BluetoothAdapter.getDefaultAdapter();  // Gets phone's bluetooth adapter

        if(myBluetooth == null){

            onScreenMessage("Bluetooth Device Not Available");
            finish();  // Will close the application

        } else{
            if (!myBluetooth.isEnabled()){
                //Ask user to turn bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon,1);
            }
        }

        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                pairedDevicesList();
            }
        });

    }

    private void pairedDevicesList(){

        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList pairedList = new ArrayList();

        if (pairedDevices.size() > 0){
            for(BluetoothDevice bt : pairedDevices) {

                pairedList.add(bt.getName() + "\n" + bt.getAddress()); //Adds all the paired devices to a list, where the list elements will be clickable

            }
        }
        else{
            onScreenMessage("No Paired Bluetooth Devices Found.");
        }

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, pairedList);
        pairedDeviceList.setAdapter(adapter);

        pairedDeviceList.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked

    }

    //The method for clicking on one of the paired devices
    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {

        public void onItemClick (AdapterView av, View v, int arg2, long arg3){

            // Gets the MAC adress from the paried device
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Creates and change to a new activity
            Intent i = new Intent(DeviceList.this, ControlScreen.class);
            i.putExtra(EXTRA_ADDRESS, address); //this will be received at ControlScreen (class) Activity
            startActivity(i);
        }
    };

    private void onScreenMessage(String message){

        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();

    }

}
