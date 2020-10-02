package com.example.ta2;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class TestAc extends AppCompatActivity {
    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter mbluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        System.out.println(mbluetoothAdapter.getBondedDevices());

        BluetoothDevice hc06 = mbluetoothAdapter.getRemoteDevice("98:D3:35:70:C1:04");
        System.out.println(hc06.getName());

        BluetoothSocket btSocket = null;
        try {
            btSocket = hc06.createRfcommSocketToServiceRecord(mUUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(btSocket);
        try {
            btSocket.connect();
            System.out.println(btSocket.isConnected());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            OutputStream outputStream= btSocket.getOutputStream();
            outputStream.write(48);
        } catch (IOException e) {
            e.printStackTrace();
        }

        InputStream inputStream = null;
        try {
            inputStream = btSocket.getInputStream();
            inputStream.skip(inputStream.available());

            for (int i=0; i < 26; i++){

                byte b = (byte) inputStream.read();
                System.out.println((char) b);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            btSocket.close();
            System.out.println(btSocket.isConnected());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}