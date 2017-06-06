package com.example.matos.bluetoothasync;

import android.graphics.SweepGradient;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import org.json.*;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static android.R.attr.button;

//Helps?

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

        audioOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton button, boolean OnOff){
              command();

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

                    volumeLevel.setText(String.valueOf("Volume Level: " + seekBar.getProgress()));
                    try{

                        //JSONObject jo = new JSONObject("\"V\":\"1\"");
                        //btSocket.getOutputStream().write(String.valueOf(seekBar.getProgress()).getBytes());

                        String s = "{\"Volume\":\"3\", \"Error\":\"123\", \"Compression\":\"1\"}";
                        btSocket.getOutputStream().write(s.getBytes());

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
                btSocket.getOutputStream().write("Update".toString().getBytes());
                String receivedMessage = new ReceiveBT().execute().get();
                interpretMessage(receivedMessage);
            }
            catch (IOException e){
                onScreenMessage("Failed to send message");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }

    private void disconnect() {
        if (btSocket!=null) {
            try {
                System.out.println("Disconnected");
                btSocket.close(); //close connection
            }
            catch (IOException e) {
                onScreenMessage("Error");
            }
        }
        finish(); //return to the first layout
    }

    private void command(){

        if (btSocket!=null) {
            try{
                int onOff;
                if(audioOnOff.isEnabled()){
                    onOff = 1;
                }else{
                    onOff = 0;
                }

                int progress = volume.getProgress();

                // Command is written in JSON syntax
                String command = "{ \" Volume \" :" + progress + ", \" Compression \" :" + onOff + "}";

                btSocket.getOutputStream().write(command.getBytes());


            }
            catch (IOException e){
                onScreenMessage("Failed to send command");
            }

        }

    }

    private void interpretMessage(String receivedMessage) throws JSONException {

        // Makes a JSON object
        JSONObject jobject = new JSONObject(receivedMessage);
        // Retrieves the volume level
        int volume = (Integer.parseInt(jobject.getString("Volume")));
        // Retrieves the error count
        int count = (Integer.parseInt(jobject.getString("Error")));
        // Retrieves the compression boolean
        int compression = (Integer.parseInt(jobject.getString("Compression")));

        boolean comp;
        if(compression == 1){
            comp = true;
        }else{
            comp = false;
        }

        updateValues(volume,count,comp);

    }

    private void updateValues(int volumelvl, int errorCount, boolean compression){

        erCount.setText("Total error corrected: " + errorCount);
        audioOnOff.setChecked(compression);
        volumeLevel.setText("Volume Level: " + volumelvl);
        volume.setProgress(volumelvl);

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
                    BluetoothDevice BTDevice = myBluetooth.getRemoteDevice(address); //connect to the address of the device
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
                finish(); // returns to devicelist
            }
            else{
                onScreenMessage("Connected.");
                isConnected = true;
            }
            progress.dismiss(); // dismisses the progress bar created in onPreExecute
        }
    }

    private class ReceiveBT extends AsyncTask<Void, Void, String> {


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

            byte[] result = new byte[numBytes];

            for(int i = 0; i < result.length; i++){
                result[i] = mmBuffer[i];
            }

            String receivedMessage = new String(result);

            return receivedMessage;
        }
    }

}


