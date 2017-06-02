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

        myBluetooth = BluetoothAdapter.getDefaultAdapter();  // Gets your bluetooth adapter

        if(myBluetooth == null){

            //Shows message telling device has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();

            // finish app
            finish();

        } else{

            if (myBluetooth.isEnabled()){

                // Do nothing, everything is as it is supposed to be

            } else{

                //Ask user to turn bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); // this request needs permission in andriod
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

        if (pairedDevices.size()>0){

            for(BluetoothDevice bt : pairedDevices) {

                pairedList.add(bt.getName() + "\n" + bt.getAddress()); //List all the names and addresses of paired devices

            }
        }
        else{
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, pairedList);
        pairedDeviceList.setAdapter(adapter);
        pairedDeviceList.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked

    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {

        public void onItemClick (AdapterView av, View v, int arg2, long arg3){

            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Make an intent to start next activity.
            Intent i = new Intent(DeviceList.this, ControlScreen.class);

            //Change the activity.
            i.putExtra(EXTRA_ADDRESS, address); //this will be received at ControlScreen (class) Activity
            startActivity(i);
        }
    };

}
