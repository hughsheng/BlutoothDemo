package com.example.sztangli.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.Context.BLUETOOTH_SERVICE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.bluetooth_open)
    Button open_btn;
    @BindView(R.id.bluetooth_close)
    Button close_btn;
    @BindView(R.id.bluetooth_show_detail)
    Button show_detail_btn;
    @BindView(R.id.bluetooth_device_list)
    ListView device_lv;
    @BindView(R.id.bluetooth_detail)
    TextView detail_tv;
    public BluetoothAdapter mBluetoothAdapter;//蓝牙适配器
    public List<BluetoothDevice> blueDeviceList;
    public List<Map<String, String>> blueDeviceNameList;
    protected static final String DEVICE_NAME = "device_name";//设备名字
    public static final String DEVICE_SIGNAL = "device_signal";//设备信号强度
    public static final String DEVICE_AD = "device_ad";//设备广告
    public static final String DEVICE_ADDRESS = "device_address";//设备地址
    public static final String TAG="bluetooth_tag";
    public static final String BASE_ADDRESS = "00000000-0000-1000-8000-00805F9B34FB"; //蓝牙UUID的基地址
    public static final String DEVICE_INFO_SERVICE = "00001800-0000-1000-8000-00805F9B34FB";
    public static final String SYSTEM_ID_CHAR = "00002a04-0000-1000-8000-00805F9B34FB";
    protected static final int SCAN_TIME = 10000;//蓝牙搜索时间
    // protected static final String GOLE_DEVICE_NAME="Amazfit Watch-EFAA";
    protected static final String GOLE_DEVICE_NAME = "小米运动蓝牙耳机mini";
    protected static final int MSG_SHOWDEVICE = 1;
    protected Handler mHandler;
    private SimpleAdapter adapter;
    private List<ScanFilter> bleScanFilters = new ArrayList<>();
    private List<BluetoothGattService> mGattServiceList;
    private BluetoothGatt mBluetoothGatt;
    ScanSettings bleScanSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkBLE();//检查是否支持蓝牙4.0以上版本
        ButterKnife.bind(this);
        init();
        showDevice();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.bluetooth_open:
                blueDeviceList.clear();
                blueDeviceNameList.clear();
                searchBlutooth();
                break;
            case R.id.bluetooth_close:
                disconnect();
                break;
            case R.id.bluetooth_show_detail:
                if (mBluetoothAdapter != null) {
                    //获取本机蓝牙名称
                    String name = mBluetoothAdapter.getName();
                    //获取本机蓝牙地址
                    String address = mBluetoothAdapter.getAddress();
                    StringBuilder blueToothDetai = new StringBuilder();
                    blueToothDetai.append("name=" + name + "\n" + "address=" + address);
                    //获取已配对蓝牙设备
                    Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
                    for (BluetoothDevice bonddevice : devices) {
                        blueToothDetai.append("bonded device name =" + bonddevice.getName() + " " +
                                "address" + bonddevice.getAddress() + "\n");
                    }
                    detail_tv.setText(blueToothDetai);
                }
                break;
            case R.id.bluetooth_device_list:
                break;
        }
    }

    private void init() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        blueDeviceList = new ArrayList<>();
        blueDeviceNameList = new ArrayList<>();
        mHandler = new BlutoothHandler(this);
        bleScanFilters.add(new ScanFilter.Builder().setDeviceName(GOLE_DEVICE_NAME).build());
        open_btn.setOnClickListener(this);
        close_btn.setOnClickListener(this);
        show_detail_btn.setOnClickListener(this);
    }


    BluetoothAdapter.LeScanCallback blutoothCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            String name = device.getName();
//            if(GOLE_DEVICE_NAME.equals(name)){
//                mBluetoothAdapter.stopLeScan(blutoothCallback);
//            if(name==null){//有些蓝牙设备没有名字
//                return;
//            }
            Map<String, String> map = new HashMap<>();
            map.put(DEVICE_NAME, device.getName());
            map.put(DEVICE_SIGNAL, String.valueOf(rssi));
            map.put(DEVICE_AD, new String(scanRecord));
            map.put(DEVICE_ADDRESS, device.getAddress());
            if (!isInList(device)) {
                blueDeviceNameList.add(map);
            }
            if (!blueDeviceList.contains(device)) {
                blueDeviceList.add(device);
            }

            mHandler.sendEmptyMessage(MSG_SHOWDEVICE);
//            }
        }
    };

    private void searchBlutooth() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.startLeScan(blutoothCallback);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(blutoothCallback);
                    if (blueDeviceNameList.size() == 0) {
                        Toast.makeText(MainActivity.this, "未搜索到指定设备", Toast.LENGTH_SHORT).show();
                    }
                }
            }, SCAN_TIME);
        } else {//没有蓝牙，尝试开启蓝牙
            Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enabler, 200);
        }
    }


    private static class BlutoothHandler extends Handler {
        WeakReference<MainActivity> wf;

        public BlutoothHandler(MainActivity mainActivity) {
            this.wf = new WeakReference<MainActivity>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final MainActivity mainActivity = wf.get();
            if (mainActivity != null) {
                switch (msg.what) {
                    case MainActivity.MSG_SHOWDEVICE:
                        mainActivity.adapter.notifyDataSetChanged();
                        break;
                }
            }

        }
    }

    private void checkBLE() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showDevice() {
        adapter = new SimpleAdapter(this, blueDeviceNameList,
                android.R.layout.simple_expandable_list_item_1, new
                String[]{DEVICE_ADDRESS}
                , new int[]{android.R.id.text1});
        device_lv.setAdapter(adapter);
        device_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {


            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = blueDeviceList.get(position);
                mBluetoothGatt = device.connectGatt(MainActivity.this, true, new
                        BluetoothGattCallback() {
                            @Override
                            public void onConnectionStateChange(BluetoothGatt gatt, int
                                    status, int newState) {
                                super.onConnectionStateChange(gatt, status, newState);
                                if (newState == BluetoothGatt.STATE_CONNECTED) {
                                    boolean success = gatt.discoverServices();//查找BLE设备提供的所有服务
                                    Toast.makeText(MainActivity.this, "已连接", Toast.LENGTH_SHORT)
                                            .show();
                                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                                    Toast.makeText(MainActivity.this, "已断开", Toast.LENGTH_SHORT)
                                            .show();
                                }
                            }

                            @Override
                            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                                super.onServicesDiscovered(gatt, status);
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    mGattServiceList = gatt.getServices();//获取BLE设备提供的所有服务列表
                                    BluetoothGattService devInfoService = gatt.getService(UUID
                                            .fromString
                                            (DEVICE_INFO_SERVICE));
                                    BluetoothGattCharacteristic systemID = devInfoService
                                            .getCharacteristic(UUID.fromString(SYSTEM_ID_CHAR));
                                    gatt.readCharacteristic(systemID);
                                }

                                super.onServicesDiscovered(gatt, status);
                            }

                            @Override
                            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                                BluetoothGattCharacteristic
                                                                        characteristic) {
                                try {
                                    byte[] value = characteristic.getValue();
                                    if (value != null) {
                                        String str = String.valueOf(value);
                                        Log.i("hello", str);
                                    }
                                } catch (Exception e) {
                                    Log.i("hello", e.getMessage());
                                }
                                super.onCharacteristicChanged(gatt, characteristic);
                            }

                            @Override
                            public void onCharacteristicRead(BluetoothGatt gatt,
                                                             BluetoothGattCharacteristic
                                                                     characteristic,
                                                             int status) {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    try {
                                        byte[] value = characteristic.getValue();
                                        if (value != null) {
                                            String str = String.valueOf(value);
                                            Log.i("hello", str);
                                        }
                                    } catch (Exception e) {
                                        Log.i("hello", e.getMessage());
                                    }
                                }

                                super.onCharacteristicRead(gatt, characteristic, status);
                            }

                            @Override
                            public void onCharacteristicWrite(BluetoothGatt gatt,
                                                              BluetoothGattCharacteristic
                                                                      characteristic,
                                                              int status) {
                                super.onCharacteristicWrite(gatt, characteristic, status);
                            }


                            @Override
                            public void onDescriptorRead(BluetoothGatt gatt,
                                                         BluetoothGattDescriptor descriptor, int
                                                                 status) {
                                super.onDescriptorRead(gatt, descriptor, status);
                            }

                            @Override
                            public void onDescriptorWrite(BluetoothGatt gatt,
                                                          BluetoothGattDescriptor descriptor, int
                                                                  status) {
                                super.onDescriptorWrite(gatt, descriptor, status);
                            }
                        });
            }
        });
    }

    private boolean isInList(BluetoothDevice device) {
        boolean result = false;
        if (blueDeviceNameList.size() > 0) {
            for (Map map : blueDeviceNameList) {
                if (map.get(DEVICE_ADDRESS).equals(device.getAddress())) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public void disconnect() {

        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.stopLeScan(blutoothCallback);
        }

        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    //设置BLE通知特性
    public void setCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, UUID uuid, boolean enable) {
        if (gatt == null || characteristic == null || uuid == null) return;
        gatt.setCharacteristicNotification(characteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(uuid);
        descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }
//解析服务列表
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        for (BluetoothGattService gattService : gattServices) { // 遍历出gattServices里面的所有服务
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) { // 遍历每条服务里的所有Characteristic
                if (gattCharacteristic.getUuid().toString().equalsIgnoreCase("213213")) {
                    // 有哪些UUID，每个UUID有什么属性及作用，一般硬件工程师都会给相应的文档。我们程序也可以读取其属性判断其属性。
                    // 此处可以可根据UUID的类型对设备进行读操作，写操作，设置notification等操作
                    // BluetoothGattCharacteristic gattNoticCharacteristic 假设是可设置通知的Characteristic
                    // BluetoothGattCharacteristic gattWriteCharacteristic 假设是可读的Characteristic
                    // BluetoothGattCharacteristic gattReadCharacteristic  假设是可写的Characteristic
                }
            }
        }
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
        String branch1="branch1";
    }

    public void wirteCharacteristic(BluetoothGattCharacteristic characteristic) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.writeCharacteristic(characteristic);

    }
}
