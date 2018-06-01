package buu.com.heartrate;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    public static BluetoothSocket btSocket;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> deviceAdapter;
    private List<String> listDevices;
    private ListView listView;
    private LinearLayout btContent;
//    private TextView btAllData;
    private Button openBT;
    private Button searchBT;
    final private static int MESSAGE_READ = 100;
    int i = 0;

    private TextView mHeartRate;
    private TextView mBreathRate;
    private TextView mRPE;
    private TextView mSuit;
    private TextView mFeel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView) this.findViewById(R.id.list);
        btContent = (LinearLayout) findViewById(R.id.bt_content_llt);
//        btAllData = (TextView) findViewById(R.id.all_data);
//        btAllData.setText(btAllData.getText(), TextView.BufferType.EDITABLE);

        mHeartRate = findViewById(R.id.heart_rate);
        mBreathRate = findViewById(R.id.breath_rate);
        mRPE = findViewById(R.id.rpe);
        mSuit = findViewById(R.id.suit);
        mFeel = findViewById(R.id.feel);

        openBT = (Button) findViewById(R.id.open_btn);
        searchBT = (Button) findViewById(R.id.search_btn);

        listDevices = new ArrayList<String>();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()) {
            openBT.setText("关闭蓝牙");
        }
        deviceAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.list_item, listDevices);

        openBT.setOnClickListener(new BTListener());
        searchBT.setOnClickListener(new BTListener());

        listView.setAdapter(deviceAdapter);
        listView.setOnItemClickListener(new ItemClickListener());//添加监听
    }


    private BroadcastReceiver receiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //下面几行是为了在logcat里面看到搜索到的设备细节，需要的话，可以将注释打开
//            Bundle b = intent.getExtras();
//            Object[] lstName = b.keySet().toArray();
//            // 显示所有收到的消息及其细节
//            for (int i = 0; i < lstName.length; i++) {
//                String keyName = lstName[i].toString();
//                Log.e("-----" + keyName, String.valueOf(b.get(keyName)));
//            }

            //搜索设备时，取得设备的MAC地址
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String str = device.getName() + "|" + device.getAddress();
                if (listDevices.indexOf(str) == -1)// 防止重复添加
                    listDevices.add(str); // 获取设备名称和mac地址
                if (deviceAdapter != null) {
                    deviceAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    /**
     * 蓝牙开启与搜索按钮点击监听
     */
    class BTListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.open_btn) {
                if (!bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable();//开启蓝牙
                    Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    enable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); //300秒为蓝牙设备可见时间
                    startActivity(enable);
                    openBT.setText("关闭蓝牙");

                } else {
                    bluetoothAdapter.disable();//关闭蓝牙
                    openBT.setText("开启蓝牙");
                    if (btSocket != null) {
                        try {
                            btSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (view.getId() == R.id.search_btn) {
                if (!bluetoothAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(), "请先开启蓝牙", Toast.LENGTH_SHORT).show();
                } else {
                    btContent.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                    if (listDevices != null) {
                        listDevices.clear();
                        if (deviceAdapter != null) {
                            deviceAdapter.notifyDataSetChanged();
                        }
                    }
                    bluetoothAdapter.startDiscovery();
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(receiver, filter);

                }
            }
        }
    }

    /**
     * 蓝牙选项，listview列表点击监听
     */
    class ItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(getApplicationContext(), "请先开启蓝牙", Toast.LENGTH_SHORT).show();
            } else {
                bluetoothAdapter.cancelDiscovery();//停止搜索
                String str = listDevices.get(position);
                String macAdress = str.split("\\|")[1];

                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAdress);
                try {
                    Method clientMethod = device.getClass()
                            .getMethod("createRfcommSocket", new Class[]{int.class});
                    btSocket = (BluetoothSocket) clientMethod.invoke(device, 1);
                    connect(btSocket);//连接设备

                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * 连接蓝牙及获取数据
     */
    public void connect(final BluetoothSocket btSocket) {
        try {
            btSocket.connect();//连接
            if (btSocket.isConnected()) {
                Log.e("----connect--- :", "连接成功");
                Toast.makeText(getApplicationContext(), "蓝牙连接成功", Toast.LENGTH_SHORT).show();
                listView.setVisibility(View.GONE);
                btContent.setVisibility(View.VISIBLE);
                new ConnetThread().start();//通信

            } else {
                Toast.makeText(getApplicationContext(), "蓝牙连接失败", Toast.LENGTH_SHORT).show();
                btSocket.close();
                listView.setVisibility(View.VISIBLE);
                btContent.setVisibility(View.GONE);
                Log.e("--------- :", "连接关闭");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 蓝牙通信管理
     */
    private class ConnetThread extends Thread {
        public void run() {
            try {
                InputStream inputStream = btSocket.getInputStream();
                byte[] data = new byte[1024];
                int len = 0;
                String result = "";

                while (len != -1) {
                    if (inputStream.available() > 0 == false) {
                        continue;
                    } else {
                        try {
                            Thread.sleep(500);//等待0.5秒，让数据接收完整
                            len = inputStream.read(data);
                            result = URLDecoder.decode(new String(data, "utf-8"));
//                          Log.e("----result：----- :", ">>>" + result);
                            Message msg = new Message();
                            msg.what = MESSAGE_READ;
                            msg.obj = result;
                            handler.sendMessage(msg);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                inputStream.close();
                Log.e("--------- :", "关闭inputStream");
                if (btSocket != null) {
                    btSocket.close();
                }
            } catch (IOException e) {
                Log.e("TAG", e.toString());
            }
        }

    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    String result = (String) msg.obj;
                    Log.e("----result：----- :", ">>>" + result);
                    String[] data = result.split("##");
//                    String heartRate = "";
//                    String breathRate = "";
                    for (int i=0;i<data.length;i++){
                        if (data[i].contains(":")){
                            String[] s = data[0].split(":");
                            if (s[0].substring(s[0].length()-1, s[0].length()).equals("i")){
                                mBreathRate.setText(s[1]);
                            }else if (s[0].substring(s[0].length()-1, s[0].length()).equals("e")){
                                mHeartRate.setText(s[1]);
                                String replace = s[1].replace(" ", "");
                                int rate = Integer.parseInt(replace);
                                if (rate < 70){
                                    mRPE.setText("6");
                                    mSuit.setVisibility(View.VISIBLE);
                                    mFeel.setText("安静，不费力");
                                } else if (rate < 90) {
                                    mRPE.setText("8");
                                    mFeel.setText("极其轻松");
                                    mSuit.setVisibility(View.VISIBLE);
                                } else if (rate < 110){
                                    mRPE.setText("10");
                                    mSuit.setVisibility(View.VISIBLE);
                                    mFeel.setText("很轻松");
                                } else if (rate < 130){
                                    mRPE.setText("13");
                                    mSuit.setVisibility(View.VISIBLE);
                                    mFeel.setText("轻松");
                                } else if (rate < 150){
                                    mRPE.setText("15");
                                    mSuit.setVisibility(View.GONE);
                                    mFeel.setText("有点吃力");
                                } else if (rate < 150){
                                    mRPE.setText("16");
                                    mSuit.setVisibility(View.GONE);
                                    mFeel.setText("吃力");
                                } else if (rate < 170){
                                    mRPE.setText("17");
                                    mSuit.setVisibility(View.GONE);
                                    mFeel.setText("非常吃力");
                                } else if (rate < 195){
                                    mRPE.setText("18");
                                    mSuit.setVisibility(View.GONE);
                                    mFeel.setText("极其吃力");
                                } else {
                                    mRPE.setText("20");
                                    mSuit.setVisibility(View.GONE);
                                    mFeel.setText("精疲力竭");
                                }
                            }else {
                                mHeartRate.setText("worry");
                            }
                        }
                    }

//                    String[] s1 = data[0].split(":");
//                    breathRate = s1[1];
//                    if (data.length>1){
//                        String[] s2 = data[1].split(":");
//                        if (s2.length>1){
//                            heartRate = s2[1];
//                        }
//                    }

//                    String data = result.split("\\r\\n")[0];
//                    Log.e("----data：----- :", ">>>" + data);
//                    if (i < 6) {
////                        Editable text = (Editable) mBreathRate.getText();
////                        text.append(data);
//                        mHeartRate.setText(heartRate);
//                        mBreathRate.setText(breathRate);
//                        i++;
//                    } else {
//                        mHeartRate.setText(heartRate);
//                        mBreathRate.setText(breathRate);
//                        i = 0;
//                    }
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }
}
