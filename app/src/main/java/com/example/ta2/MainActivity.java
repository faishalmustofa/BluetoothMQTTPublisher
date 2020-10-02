package com.example.ta2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button loadData,makeFiletxt, mulai, selesai,reset, btnOff, send,listDevices;
    ListView listView, listData;
    TextView timer,msg_box,status, dataFile;

    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L;
    int Seconds, Minutes, MilliSeconds ;
    String time_rec ;

    //Bluetooth
    BluetoothAdapter mbluetoothAdapter;
    BluetoothDevice[] btArray;
    BluetoothSocket btSocket;

    InputStream inputStream;
    OutputStream outputStream;

    SendRecieve sendRecieve;

    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECIEVED = 5;

    int REQUEST_ENABLE_BLUETOOTH = 1;

    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    String[] ListElements = new String[] {  };
//    int[] ListData = new int[] { };

    List<String> ListElementsArrayList ;

    ArrayAdapter<String> adapter ;

    volatile boolean stopWorker = true;

    //MQTT
    static String MQTTHOST = "tcp://192.168.0.103:1883";
    String topicStr = "datacad";

    MqttAndroidClient client;

    ArrayList<String> arrayData = new ArrayList<String>();

    //
    private static int index_FileText = 1;
    private static final String FILE_NAME = "dataSignal"+index_FileText+".txt";

    String fileText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //MQTT
        connectMQTT();

        findViewByIdes();

        mbluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        System.out.println(mbluetoothAdapter.getBondedDevices());

        if (!mbluetoothAdapter.isEnabled())
        {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        }

        ListElementsArrayList = new ArrayList<String>(Arrays.asList(ListElements));

        implementListeners();


    }

    private void loadData()
    {
        FileInputStream fis = null;

        try {
            fis = openFileInput(FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
//            StringBuilder sb = new StringBuilder();
            String dataText = br.readLine();

//            while ((dataText = br.readLine()) != null){
//                System.out.println("Data text : "+ dataText);
//                sb.append(dataText).append("\n");
//            }
            System.out.println("Data dari file txt : "+ dataText);
            fileText = dataText;


            dataFile.setText(FILE_NAME);
//            listData.setAdapter(sb.toString());

        } catch (FileNotFoundException e) {
            Toast.makeText(MainActivity.this,"File Does not Exist", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Connecting to MQTT
    private void connectMQTT()
    {
        MemoryPersistence memPer = new MemoryPersistence();

        client = new MqttAndroidClient(this.getApplicationContext(), MQTTHOST, "AndroidThings",memPer);
        System.out.println(client);
        try {
            client.connect();
            Toast.makeText(MainActivity.this,"Your Device Connected to MQTT", Toast.LENGTH_LONG).show();
        } catch (MqttException e) {
            Toast.makeText(MainActivity.this,"MQTT Connection Failed", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void findViewByIdes()
    {
        listDevices = (Button) findViewById(R.id.listDevices);
        loadData    = (Button) findViewById(R.id.loadData);
        makeFiletxt = (Button) findViewById(R.id.makeFiletxt);
        mulai       = (Button) findViewById(R.id.mulai);
        selesai     = (Button) findViewById(R.id.selesai);
        reset       = (Button) findViewById(R.id.reset);
        btnOff      = (Button) findViewById(R.id.btnOff);
        send        = (Button) findViewById(R.id.send);
        msg_box     = (TextView) findViewById(R.id.msg);
        status      = (TextView) findViewById(R.id.status);
        timer       = (TextView) findViewById(R.id.timer);
        dataFile    = (TextView) findViewById(R.id.dataFile);
        listView    = (ListView) findViewById(R.id.listView);
        listData    = (ListView) findViewById(R.id.listData);
    }

    private void implementListeners() {

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Connecting

                //Publishing
                String topic = topicStr;
                String stringInt;
                try {
                    stringInt = fileText;
                    //mengirim string dengan format 00:00:00.1|33
                    //didlama array = ["00:00:00.1|33","00:00:00.3|-25"]
                    client.publish(topic, stringInt.getBytes(),0,false);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
//                for (int i=0;i<arrayData.size();i++){
//                    try {
//                        stringInt = String.valueOf(arrayData.get(i));
//                        //mengirim string dengan format 00:00:00.1|33
//                        //didlama array = ["00:00:00.1|33","00:00:00.3|-25"]
//                        client.publish(topic, stringInt.getBytes(),0,false);
//                    } catch (MqttException e) {
//                        e.printStackTrace();
//                    }
//                }
                Toast.makeText(MainActivity.this,"Data published to MQTT Broker", Toast.LENGTH_LONG).show();

            }
        });

        loadData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadData();
            }
        });

        adapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1,
                ListElementsArrayList
        );

        listData.setAdapter(adapter);

        makeFiletxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String dataText;
                FileOutputStream fos = null;

                try {
                    dataText = String.valueOf(arrayData);
                    fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
                    fos.write(dataText.getBytes());

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null){
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                Toast.makeText(MainActivity.this, "Saved to " + getFilesDir() + "/" + FILE_NAME, Toast.LENGTH_LONG).show();
                System.out.println("File Direktori : "+getFilesDir());

                ListElementsArrayList.add(timer.getText().toString());
                adapter.notifyDataSetChanged();
            }
        });
        listDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Set<BluetoothDevice> bt = mbluetoothAdapter.getBondedDevices();
                String[] strings = new String[bt.size()];
                btArray = new BluetoothDevice[bt.size()];
                int index = 0;

                if (bt.size() > 0)
                {
                    for (BluetoothDevice device : bt)
                    {
                        btArray[index] = device;
                        strings[index] = device.getName();
                        index++;
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,strings);
                    listView.setAdapter(arrayAdapter);
                }
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ClientClass clientClass = new ClientClass(btArray[i]);
                clientClass.start();

                status.setText("Connecting");
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CloseBT closeBT = new CloseBT(btSocket);
                closeBT.start();

                status.setText("Disconnecting");
            }
        });

        mulai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopWorker = false;
                sendRecieve = new SendRecieve(btSocket);
                sendRecieve.start();

                StartTime = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, 0);

                reset.setEnabled(false);

            }
        });

        selesai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimeBuff += MillisecondTime;
                System.out.println("Time Buff : " + time_rec);

                handler.removeCallbacks(runnable);

                stopWorker = true;
                System.out.println("Stop Worker : "+stopWorker);

                reset.setEnabled(true);

                System.out.println(arrayData);

            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MillisecondTime = 0L ;
                StartTime = 0L ;
                TimeBuff = 0L ;
                UpdateTime = 0L ;
                Seconds = 0;
                Minutes = 0 ;
                MilliSeconds = 0 ;

                timer.setText("00:00:00");
                msg_box.setText("DATA");
                dataFile.setText("");

                arrayData.clear();
                time_rec = null;

                ListElementsArrayList.clear();

                adapter.notifyDataSetChanged();
            }
        });

    }

    private class ClientClass extends Thread
    {
        private BluetoothDevice device;
        private BluetoothSocket socket = null;

        public ClientClass(BluetoothDevice device1)
        {
            String addressDevice = device1.getAddress();
            device = mbluetoothAdapter.getRemoteDevice(addressDevice);
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(mUUID);
                System.out.println(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            mbluetoothAdapter.cancelDiscovery();
            try {
                System.out.println("Socket connection : "+socket.isConnected());
                socket.connect();
                System.out.println("Socket connection : "+socket.isConnected());
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                btSocket = socket;

            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what)
            {
                case STATE_CONNECTING:
                    status.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECIEVED:
//                    byte[] readBuff = (byte[]) msg.obj;
//                    String tempMsg = new String(readBuff,0,msg.arg1);
                    int tempMsg = msg.arg1;
                    msg_box.setText(String.format(String.valueOf(tempMsg)));
//                    String TimeandData = waktu+"-"+String.valueOf(tempMsg);
                    time_rec= String.valueOf(Minutes)+":"+String.valueOf(Seconds)+":"+String.valueOf(MilliSeconds);
                    String TimeandData = time_rec+"|"+String.valueOf(tempMsg);
                    arrayData.add(TimeandData);
                    break;
            }
            return true;
        }
    });

    private class SendRecieve extends Thread
    {
        private final BluetoothSocket bluetoothSocket;

        public SendRecieve (BluetoothSocket socket)
        {
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run()
        {
            int bytes;

            System.out.println("Stop Worker : "+stopWorker);
            try {
                outputStream.write(48);
                bytes = (byte) inputStream.read();
                System.out.println(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!stopWorker)
            {
                try {
                    outputStream.write(48);
                    bytes = (byte) inputStream.read();
                    System.out.println(bytes);
                    handler.obtainMessage(STATE_MESSAGE_RECIEVED,bytes,-1).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class CloseBT extends Thread
    {
        private final BluetoothSocket socket;

        public CloseBT(BluetoothSocket btSocket)
        {
            socket = btSocket;
        }

        public void run()
        {
            try {
                inputStream.close();
                outputStream.close();
                socket.close();
                System.out.println("Socket connection : "+socket.isConnected());
                status.setText("Disconnected");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            timer.setText("" + Minutes + ":" + String.format("%02d", Seconds) + ":" + String.format("%03d", MilliSeconds));

            handler.postDelayed(this, 0);
        }

    };

}