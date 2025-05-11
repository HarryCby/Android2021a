package com.harry.a2021competition;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;



public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final int ONLY_CHECK = 3;
    private static final int PACKETLENGTH=50;//单位字节？ 1+12*4+1
    private static final byte HEAD = 0x24;   // '$' 包头
    private static final byte TAIL = 0x2B;   // '+' 包尾
    private static final byte S_0 = 0x30;
    private static final byte S_1 = 0x3f;
    private static final int MAX_BUFFER_SIZE = 2048; // 缓冲区最大限制
//    private static final int XIUZHENG_LEN=50;//xiuzheng平均次数
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Button btnSend;
    private Button btnSend1;
    private TextView statusText;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
//    private float[][] array_xiuzheng; // 定义为类的成员变量
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSend = findViewById(R.id.btn_send);
        btnSend1 = findViewById(R.id.btn_send1);
        statusText = findViewById(R.id.statusText);
        // 检查并请求必要权限
        // 读取 CSV 文件并获取 float 类型的数组
//        array_xiuzheng = CsvReader.readCsvFile(this, "xiuzheng_data.csv");

        checkAndRequestPermissions();
    }
    // 检查并请求必要权限 之后调用initBluetooth()函数
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 需要 BLUETOOTH_CONNECT 权限
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_PERMISSIONS);
            } else {
                initBluetooth();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-11 需要位置权限
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_PERMISSIONS);
            } else {
                initBluetooth();
            }
        } else {
            // Android 5.0 及以下版本不需要运行时权限
            initBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initBluetooth();
            } else {
                Toast.makeText(this, "需要权限才能使用蓝牙功能", Toast.LENGTH_SHORT).show();
            }

        }
        else if(requestCode==ONLY_CHECK){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //nothing to do
            } else {
                Toast.makeText(this, "需要权限才能使用蓝牙功能", Toast.LENGTH_SHORT).show();
            }
        }
    }


//    private void checkPermissions() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            // Android 12+ 需要 BLUETOOTH_CONNECT 权限
//            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[]{
//                        Manifest.permission.BLUETOOTH_CONNECT,
//                        Manifest.permission.ACCESS_FINE_LOCATION
//                }, REQUEST_PERMISSIONS);
//            }
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            // Android 6.0-11 需要位置权限
//            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[]{
//                        Manifest.permission.ACCESS_FINE_LOCATION
//                }, REQUEST_PERMISSIONS);
//            }
//        }
//    }


    //请求打开蓝牙！！！     之后setupBluetoothConnection()
    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            // 安全地请求开启蓝牙
            try {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        //onActivityResult中会执行setupBluetoothConnection函数
                    }
                } else {
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            } catch (SecurityException e) {
                Toast.makeText(this, "蓝牙权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 蓝牙已开启，继续你的逻辑
            setupBluetoothConnection();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                setupBluetoothConnection();
            } else {
                Toast.makeText(this, "蓝牙未启用，功能受限", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void setupBluetoothConnection() {
        // 这里放置你的蓝牙连接代码

            Button btnConnect = findViewById(R.id.btn_connect);
            btnConnect.setOnClickListener(v -> {
                // 确保有权限
                if(!flag_connected) {
                    if (hasBluetoothPermissions()) {
                        //                connectToDevice("00:18:e4:35:29:45");

                        connectToBluetoothDevice();
                    } else {
                        checkAndRequestPermissions();
                    }
                }
            });
        //0x30 表示开始基础测量
        //0x31 表示开始提高测量
        // 在setupBluetoothConnection方法中添加发送按钮的点击事件
        btnSend.setOnClickListener(v -> {
            if (flag_connected) {
                // 示例：发送开始信号
                SendStartSingal(S_0);
            } else {
                Toast.makeText(this, "请先连接蓝牙设备", Toast.LENGTH_SHORT).show();
            }
        });
        btnSend1.setOnClickListener(v -> {
            if (flag_connected) {
                // 示例：发送开始信号
                SendStartSingal(S_1);
            } else {
                Toast.makeText(this, "请先连接蓝牙设备", Toast.LENGTH_SHORT).show();
            }
        });

    }
    //返回是否有权限
    private static final int STARTLENRTH=7;
    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void SendStartSingal(byte S){
        ByteBuffer buffer = ByteBuffer.allocate(STARTLENRTH)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(HEAD);
        for(int i=0;i<STARTLENRTH-2;i++){
            buffer.put(S);
        }
        buffer.put(TAIL);
        sendData(buffer.array());
    }
    // 添加发送数据的方法
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void sendData(byte[] data) {
        if (outputStream == null || !flag_connected) {
            runOnUiThread(() -> {
                Toast.makeText(this, "蓝牙未连接", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        new Thread(() -> {
            try {
                outputStream.write(data);
                outputStream.flush();

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "数据发送成功", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                Log.e("Bluetooth", "发送失败", e);
                flag_connected = false;
                runOnUiThread(() -> {
                    statusText.setText("发送失败");
                    Toast.makeText(MainActivity.this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * 连接蓝牙设备的方法
     */
    // 标准SPP（串口协议）的UUID
    private boolean flag_connected=false;
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void connectToBluetoothDevice() {
        try {
            // 替换为目标设备的MAC地址
            String deviceAddress = "00:18:E4:35:29:45"; // 注意MAC地址字母要大写
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

            // 创建Socket并连接（需要在子线程中执行）
            new Thread(() -> {
                try {
                    // 在主线程中更新 UI
                    runOnUiThread(() -> {
                        statusText.setText("正在连接...");
                    });
//                    *************************没有显示检查
                    //可能出问题
                    socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    socket.connect(); // 阻塞操作

                    // 获取输入输出流
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();

                    flag_connected=true;
                    // 连接成功提示（需返回主线程更新UI）
                    runOnUiThread(() -> {
                        statusText.setText("连接成功");
                        Toast.makeText(MainActivity.this, "蓝牙连接成功", Toast.LENGTH_SHORT).show();
                        btnSend.setEnabled(true);  // 启用发送按钮
                        btnSend1.setEnabled(true);  // 启用发送按钮
                    });

                    // 这里可以开始监听数据或发送数据
                    listenForData();

                } catch (IOException e) {
                    Log.e("Bluetooth", "连接失败", e);
                    runOnUiThread(() -> {
                        statusText.setText("等待连接...");
                        Toast.makeText(MainActivity.this, "连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();

        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "无效的MAC地址", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 监听蓝牙数据的示例方法
     */
    // 数据接收线程
    private void listenForData() {
        new Thread(() -> {
            byte[] readBuffer = new byte[1024]; // 临时读取缓冲区
            ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream(MAX_BUFFER_SIZE); // 累积缓冲区

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 1. 读取数据
                    int bytesRead = inputStream.read(readBuffer);
                    if (bytesRead == -1) {
                        Log.e("Bluetooth", "连接已断开");
                        flag_connected=false;
                        btnSend.setEnabled(false);  // 禁用发送按钮
                        btnSend1.setEnabled(false);  // 禁用发送按钮
                        break;
                    } else if(bytesRead>0) {

                        // 2. 检查缓冲区大小（防内存溢出）
                        if (dataBuffer.size() + bytesRead > MAX_BUFFER_SIZE) {
                            Log.w("BT", "缓冲区溢出，重置缓冲区");

                            runOnUiThread(() -> {
                                statusText.setText("缓冲区溢出，重置缓冲区");
                                Toast.makeText(MainActivity.this, "缓冲区溢出", Toast.LENGTH_SHORT).show();
                            });
                            dataBuffer.reset();
                        }else {
                            // 3. 写入累积缓冲区
                            dataBuffer.write(readBuffer, 0, bytesRead);

                            // 4. 处理完整数据包
                            processPackets(dataBuffer);

                        }

                    }
                } catch (IOException e) {
                    Log.e("BT", "连接异常", e);
                    flag_connected=false;
                    runOnUiThread(() -> {
                        statusText.setText("连接断开");
                        btnSend.setEnabled(false);  // 禁用发送按钮
                        btnSend1.setEnabled(false);  // 禁用发送按钮
                        Toast.makeText(this, "蓝牙连接已断开", Toast.LENGTH_SHORT).show();

                    });
                    break;
                }
            }

            // 线程结束时释放资源
        }).start();
    }
    // 线程结束时释放资源
    // 包处理核心逻辑
    private void processPackets(ByteArrayOutputStream buffer) {
        byte[] rawData = buffer.toByteArray();
        int processedBytes = 0; // 记录已处理的字节数

        while (processedBytes < rawData.length) {
            // 1. 查找包头（从当前位置开始）
            int headPos = findByte(rawData, processedBytes, HEAD);
            if (headPos == -1) break;

            // 4. 检查是否收到完整包
            if (headPos + PACKETLENGTH > rawData.length) {
                break; // 数据不完整，等待后续数据
            }

            // 5. 提取完整包
            byte[] packet = Arrays.copyOfRange(rawData, headPos, headPos + PACKETLENGTH);

            // 6. 验证包尾
            if (packet[packet.length - 1] != TAIL) {
                processedBytes = headPos + 1; // 跳过无效包
                continue;
            }
            //*******************处理数据




            // 7. 处理有效包
            runOnUiThread(() -> handlePacket(packet));
            processedBytes = headPos + PACKETLENGTH;
        }

        // 8. 保留未处理数据
        if (processedBytes > 0) {
            byte[] remaining = Arrays.copyOfRange(rawData, processedBytes, rawData.length);
            buffer.reset();
            buffer.write(remaining, 0, remaining.length);
        }
    }

//    private  int xiuzhng_num=0;
//    private float[] U = new float[5];
//    private float F;
//    R.id.u1,    // values[2]
//    R.id.u2,    // values[3]
//    R.id.u3,    // values[4]
//    R.id.u4,    // values[5]
//    R.id.u5,    // values[6]

//    private void xiuzheng(byte [] packet){
//        if(xiuzhng_num<XIUZHENG_LEN){
//            if (packet.length < 50) return; // 检查最小长度（HEAD + 10*int + TAIL）
//
//            ByteBuffer buffer = ByteBuffer.wrap(packet)
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//            // 1. 检查包头（position=0）
//            if (buffer.get() != HEAD) return;
//
//            // 2. 检查包尾（position=41）
//            buffer.position(packet.length - 1);
//            if (buffer.get() != TAIL) return;
//
//            // 3. 读取数据（重置position=1）  单片机数扩大10000倍  数据个数：12
//            buffer.position(1);
//            float[] values = new float[DATA_NUM];
//            for (int i = 0; i < DATA_NUM; i++) {
//                values[i] = buffer.getInt() / 10000.0f; // 转换为float
//            }
//            F=values[0];
//            for(int i=0;i<5;i++) {
//                U[i]+=1/XIUZHENG_LEN*values[i+2];
//            }
//            xiuzhng_num++;
//            }
//        else{
//            xiuzhng_num=0;
//
//        }
//    }
    // 辅助方法：查找特定字节位置
    private int findByte(byte[] data, int start, byte target) {
        for (int i = start; i < data.length; i++) {
            if (data[i] == target) return i;
        }
        return -1;
    }
    private  static  final  int DATA_NUM=12;
    // 示例包处理 在uiThread中执行 主线程   显示所需数据
    private void handlePacket(byte[] packet) {
        if (packet.length < 50) return; // 检查最小长度（HEAD + 10*int + TAIL）

        ByteBuffer buffer = ByteBuffer.wrap(packet)
                .order(ByteOrder.LITTLE_ENDIAN);

        // 1. 检查包头（position=0）
        if (buffer.get() != HEAD) return;

        // 2. 检查包尾（position=41）
        buffer.position(packet.length - 1);
        if (buffer.get() != TAIL) return;

        // 3. 读取数据（重置position=1）  单片机数扩大10000倍  数据个数：12
        buffer.position(1);
        float[] values = new float[DATA_NUM];
        for (int i = 0; i < DATA_NUM; i++) {
            values[i] = buffer.getInt() / 10000.0f; // 转换为float
        }
        //单片机已经处理好了
//        int index0=(int)(values[0]/1000);
//        for(int i=3;i<7;i++){
//            values[i]=values[i]/array_xiuzheng[index0-1][i-3];
//        }
//        values[1] = (float) Math.sqrt(values[3] * values[3] + values[4] * values[4] + values[5] * values[5] + values[6] * values[6]);
//        for(int i=7;i<12;i++){
//
//            values[i]=0;
//        }
        // 更新UI
        runOnUiThread(() -> updateUI(values));
    }

    private void updateUI(float[] values) {
        // 1. 数据有效性验证
        if (values == null || values.length < 12) {
            Log.w("UI_Update", "Invalid data: array is null or too short");
            showErrorViews();
            return;
        }

        // 2. 定义视图ID与数据索引的映射关系
        final int[] viewIds = {
                R.id.Freq,  // values[0]
                R.id.THD,   // values[1]
                R.id.u1,    // values[2]
                R.id.u2,    // values[3]
                R.id.u3,    // values[4]
                R.id.u4,    // values[5]
                R.id.u5,    // values[6]
                R.id.p1,    // values[7]
                R.id.p2,    // values[8]
                R.id.p3,    // values[9]
                R.id.p4,    // values[10]
                R.id.p5     // values[11]
        };
        //接受usart信息

        // 3. 批量更新视图
        for (int i = 0; i < viewIds.length; i++) {
            TextView textView = findViewById(viewIds[i]);
            if (textView == null) continue;
            // 特殊处理THD（添加百分号）
            if (i==1) {
                textView.setText(String.format(Locale.getDefault(), "THD: %.3f%%", 100*values[1]));
            }
            //处理u
            else if (i>1&&i<=6) {
                textView.setText(String.format(Locale.getDefault(), "%.3f", values[i]));
            }else if(i>6){
            // 默认处理（保留1位小数）
            textView.setText(String.format(Locale.getDefault(), "%.1f°", values[i]));
            }else if(i==0){
                textView.setText(String.format(Locale.getDefault(), "%.1f Hz", values[i]));
            }
        }
        float[] amplitudes = new float[5]; // 假设你只需要复制 5 个元素
        System.arraycopy(values,2,amplitudes,0,5);
        float[] phases = new float[5]; // 假设你只需要复制 5 个元素
        System.arraycopy(values,7,phases,0,5);
        setupWaveformChart(amplitudes,phases,values[0]);//values[0]为基频
    }


    // 错误状态显示
    private void showErrorViews() {
        int[] allViewIds = {
                R.id.Freq, R.id.THD,
                R.id.u1, R.id.u2, R.id.u3, R.id.u4, R.id.u5,
                R.id.p1, R.id.p2, R.id.p3, R.id.p4, R.id.p5
        };

        for (int id : allViewIds) {
            TextView tv = findViewById(id);
            if (tv != null) {
                tv.setText("--");
            }
        }
    }

    private void setupWaveformChart(float[] amplitudes, float[] phases,float Freq) {
        LineChart chart = findViewById(R.id.chart);

        // 生成波形数据
        ArrayList<Entry> entries = new ArrayList<>();
        int pointCount = 200; // 采样点数

        for (int i = 0; i < pointCount; i++) {
            float x = (float)i / pointCount * 2 * (float)Math.PI;
            float y = 0;

            // 叠加各次谐波
            for (int h = 0; h < amplitudes.length; h++) {
                // (h+1)   1=基波, 2=二次谐波...
                y += (float) (amplitudes[h] * Math.sin((h + 1) * x + phases[h]* (Math.PI / 180.0)));//x前不加频率系数  因为只展示一个周期的波形
            }

            entries.add(new Entry(i, y));
        }

        LineDataSet dataSet = new LineDataSet(entries, "合成波形");
        dataSet.setColor(Color.BLUE);
        dataSet.setDrawCircles(false);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // 图表样式设置
        chart.getDescription().setEnabled(false);
        chart.getXAxis().setEnabled(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.invalidate(); // 刷新图表
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭蓝牙连接
        try {
            if (socket != null) {
                socket.close();
            }
            if(inputStream != null){
            inputStream.close();}
            if(outputStream != null){
            outputStream.close();}
        } catch (IOException e) {
            Log.e("Bluetooth", "关闭连接失败", e);
        }
    }
}
    // 你的蓝牙连接方法...
//    private void connectToDevice(String macAddress) {
//        // 实现你的蓝牙连接逻辑
//    }


//"00:18:e4:35:29:45"