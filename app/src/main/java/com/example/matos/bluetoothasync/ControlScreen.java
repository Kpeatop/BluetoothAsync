package com.example.matos.bluetoothasync;

import android.content.Context;
import android.content.SharedPreferences;
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

public class ControlScreen extends AppCompatActivity {

    private Button btnUpdate, btnDis;
    private SeekBar volume;
    private TextView volumeLevel, erCount;
    private Switch audioOnOff;
    private String address = null;
    private ProgressDialog progress;
    private BluetoothAdapter myBluetooth = null;
    private BluetoothSocket btSocket = null;
    private boolean isConnected,isReceiving, userChange;
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
        volume.setProgress(0);
        volume.setMax(10);

        //TextView
        volumeLevel = (TextView)findViewById(R.id.level);
        erCount = (TextView)findViewById(R.id.erCount);

        //switch
        audioOnOff = (Switch)findViewById(R.id.audioOnOff);

        //Booleans
        isReceiving = false;
        isConnected = false;
        userChange = false;

        //Async task that runs with the creation of the ControlScreen

        new ConnectBT().execute(); //Call the class to connect

        new autoUpdate().execute(); // Starts the auto update

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

        audioOnOff.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                 command();
            }
        }
        );

        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volumeLevel.setText(String.valueOf("Volume Level: " + seekBar.getProgress()));
                userChange = fromUser;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if(userChange){
                    command();
                    userChange = false;
                }


            }
        });

        //SharedPreferences sharedPref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        //int errors = sharedPref.getInt("Error", 0);
        //int volume = sharedPref.getInt("Volume", 0);
        //boolean compression = sharedPref.getBoolean("Compression", false);

        //updateValues(volume, errors, compression);

    }

    private void disconnect() {
        if (btSocket!=null) {

        SharedPreferences.Editor editor = getSharedPreferences("MyPref", Context.MODE_PRIVATE).edit();
        editor.putInt("Error", Integer.parseInt(erCount.getText().toString()));
        editor.putInt("Volume", volume.getProgress());
        editor.putBoolean("Compression", audioOnOff.isEnabled());
        editor.apply();

            try {
                System.out.println("Disconnected");
                btSocket.close(); //close connection
            }
            catch (IOException e) {
                onScreenMessage("Error");
            }
        }
        onScreenMessage("Disconnected");
        finish(); //return to the first layout
    }

    private void requestUpdate(){
        System.out.println("In REQUEST");
        if(!isReceiving){
            isReceiving = true;
            UpdateBT();
            new ReceiveBT().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new waitToUpdate().execute();
        }
    }

    private void UpdateBT(){

        if (btSocket!=null) {

            int onOff;
            if(audioOnOff.isEnabled()){
                onOff = 1;
            }else{
                onOff = 0;
            }

            try{
                String request;
                request = "{ \" TYPE \" : \"UPDATE\" , \" Volume \" :" + volume.getProgress() + ", \" Compression \" :" + onOff + "}";

                System.out.println("update bt");
                btSocket.getOutputStream().write(request.toString().getBytes());

            }
            catch (IOException e){
                disconnect();
                onScreenMessage("Failed to send message");
            }
        }
    }

    private void command(){

        System.out.println("in command");
        int onOff;
        if(audioOnOff.isEnabled()){
            onOff = 1;
        }else{
            onOff = 0;
        }

        try{
            // Command is written in JSON syntax
            String command = "{ \" TYPE \" : \"COMMAND\" , \" Volume \" :" + volume.getProgress() + ", \" Compression \" :" + onOff + "}";

            btSocket.getOutputStream().write(command.getBytes());
            System.out.println(command);
        }
        catch (IOException e){
            onScreenMessage("Failed to send command");
        }
        requestUpdate();
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

        System.out.println("Updating values with Volume = "+volume+" Error count = "+count+" Compression = "+comp);

        updateValues(volume,count,comp);

    }

    private void updateValues(int volumelvl, int errorCount, boolean compression){

        erCount.setText("Total error corrected: " + errorCount);
        audioOnOff.setChecked(compression);
        volumeLevel.setText("Volume Level: " + volumelvl);
        volume.setProgress(volumelvl);
        onScreenMessage("Values has been updated");
        System.out.println("Values has been updated");
    }

    private void onScreenMessage(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
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
                    btSocket = BTDevice.createInsecureRfcommSocketToServiceRecord(myUUID); //creates a RFCOMM connection
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

        //Checks if ConnectSucces is true or false
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
            System.out.println("inside receivebt");

            String receivedMessage = "";
            boolean done = false;
            //While loop that runs until a complete JSON object has been created
            while(!done){

                System.out.println("Trying to complete JSON Object");
                byte[] mmBuffer = new byte[1024];
                int numBytes = 0;
                JSONObject jsonObject;

                try {
                    // Creates a delay of 200 ms
                    Thread.sleep(200);
                    // Reads from inputstream and puts data in to mmBuffer. Numbytes has the number of bytes.
                    numBytes = btSocket.getInputStream().read(mmBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                    disconnect();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                byte[] result = new byte[numBytes];
                // Result array is created from number of bytes and filled with the data from mmBuffer
                for(int i = 0; i < result.length; i++){
                    result[i] = mmBuffer[i];
                }

                // Creates a string temp that is concatonated with the received message.
                String temp = new String(result);
                receivedMessage += temp;
                System.out.println(receivedMessage);

                try {
                    // Tries to create a JSON object. If success the loop will terminate, otherwise it will continue until possible
                    jsonObject = new JSONObject(receivedMessage);
                    done = true;
                } catch (JSONException e) {
                }
            }

            return receivedMessage;
        }
        @Override
        protected void onPostExecute(String receivedMessage){

            isReceiving = false;
            //Calls the interpretMessage with the receivedMessage from the doInBackground
            try {
                interpretMessage(receivedMessage);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class autoUpdate extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            requestUpdate();
            new autoUpdate().execute();
            onScreenMessage("Auto update sent");
        }
    }

    private class waitToUpdate extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {

            System.out.println("Inside while loop in wait to update");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }
        @Override
        protected void onPostExecute(Void result) {


            if(isReceiving){
                new waitToUpdate().execute();
            } else {
                requestUpdate();
            }

        }

    }

}

