package com.example.matos.bluetoothasync;

import android.graphics.SweepGradient;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class ControlScreen extends AppCompatActivity {

    Button btnUpdate, btnDis;
    SeekBar volume;
    TextView volumeLevel, erCount;
    Switch audioOnOff;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ControlScreen
        setContentView(R.layout.activity_control_screen);

        //Buttons
        btnUpdate = (Button)findViewById(R.id.btnUpdate);
        btnDis = (Button)findViewById(R.id.btnDis);

        // Seekbar
        volume = (SeekBar)findViewById(R.id.volume);

        //TextView
        volumeLevel = (TextView)findViewById(R.id.level);
        erCount = (TextView)findViewById(R.id.erCount);

        //switch
        audioOnOff = (Switch)findViewById(R.id.audioOnOff);


        volume.setProgress(0);
        volume.setMax(10);

        new ConnectBT().execute(); //Call the class to connect

        //commands to be sent via bluetooth

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                requestUpdate();   //Request update method is called
            }
        });

        btnDis.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                disconnect(); //close connection and returns to first screen
            }

        });



        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                    volumeLevel.setText(String.valueOf("Volume Level:" + seekBar.getProgress()));
                    try{

                        btSocket.getOutputStream().write(String.valueOf(seekBar.getProgress()).getBytes());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


            }
        });

    }

    private void requestUpdate(){

        if (btSocket!=null) {
            try{
                System.out.println("Request Update");
                btSocket.getOutputStream().write("update".toString().getBytes());
                String s = new receiveBT().execute().get();
            }
            catch (IOException e)
            {
                onScreenMessage("Failed to send message");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

    }

    private void disconnect() {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                System.out.println("Disconnected");
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { onScreenMessage("Error");}
        }
        finish(); //return to the first layout
    }

    private void command(String command){

        if (btSocket!=null) {
            try{
                System.out.println("Command sent");
                btSocket.getOutputStream().write(command.getBytes());
            }
            catch (IOException e)
            {
                onScreenMessage("Failed to send command");
            }
        }

    }

    private void interpretMessage(String receivedMessage){

        // TODO WHEN WE KNOW WHAT WE ARE GETTING

    }

    private void updateValues(String errorCount, boolean audioComp, String volumeLvl){

        erCount.setText("Total error corrected: " + errorCount);
        audioOnOff.setChecked(audioComp);
        volumeLevel.setText("Volume Level: " + volumeLvl);

    }


    private void onScreenMessage(String message){

        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();

    }


    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        //Will create a dialog, which will be shown while the device is connecting
        @Override
        protected void onPreExecute() {
            System.out.println("ConnectBT on pre execute");
            progress = ProgressDialog.show(ControlScreen.this, "Trying to connect to the bluetooth device.", "Please wait.");
        }

        //Tries to connect to bluetooth device
        @Override
        protected Void doInBackground(Void... devices){

            System.out.println("ConnectBT do in background");
            try{
                if (btSocket == null || !isConnected){
                    //Tries to connect
                    myBluetooth = BluetoothAdapter.getDefaultAdapter(); //gets bluetooth adapter of the phone
                    BluetoothDevice BTDevice = myBluetooth.getRemoteDevice(address); //connect to the adress of the device
                    btSocket = BTDevice.createInsecureRfcommSocketToServiceRecord(myUUID); //create a RFCOMM connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect(); //start connection
                    System.out.println("Connects via bluetooth!");
                }
            }
            catch (IOException e){
                ConnectSuccess = false;
            }
            return null;
        }

        //Checks whether it went fine or not
        @Override
        protected void onPostExecute(Void result) {
            System.out.println("ConnectBT on post execute");
            super.onPostExecute(result);

            if (!ConnectSuccess){
                onScreenMessage("Connection Failed. Try again.");
                finish();
            }
            else{
                onScreenMessage("Connected.");
                isConnected = true;
            }
            progress.dismiss();
        }
    }

    private class receiveBT extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {



            byte[] mmBuffer = new byte[1024];
            int numBytes = 0;

            try {
                Thread.sleep(2000);
                numBytes = btSocket.getInputStream().read(mmBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println(numBytes);

            byte[] result = new byte[numBytes];

            for(int i = 0; i < result.length; i++){
                result[i] = mmBuffer[i];
            }

            String receivedMessage = new String(result);

            System.out.print("Result string is " + receivedMessage);

            return receivedMessage;
        }
    }

}


