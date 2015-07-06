package org.zywx.wbpalmstar.plugin.uexbluetoothble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.plugin.uexbluetoothble.vo.BluetoothDeviceVO;
import org.zywx.wbpalmstar.plugin.uexbluetoothble.vo.CharacteristicVO;
import org.zywx.wbpalmstar.plugin.uexbluetoothble.vo.GattDescriptorVO;
import org.zywx.wbpalmstar.plugin.uexbluetoothble.vo.GattServiceVO;
import org.zywx.wbpalmstar.plugin.uexbluetoothble.vo.ResultVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import dalvik.system.DexClassLoader;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class EUExBluetoothBLE extends EUExBase {

    private static final String BUNDLE_DATA = "data";
    private static final int MSG_INIT = 1;
    private static final int MSG_CONNECT = 2;
    private static final int MSG_DISCONNECT = 3;
    private static final int MSG_SCAN_DEVICE = 4;
    private static final int MSG_STOP_SCAN_DEVICE = 5;
    private static final int MSG_WRITE_CHARACTERISTIC = 6;
    private static final int MSG_READ_CHARACTERISTIC = 7;

    private String mBluetoothDeviceAddress;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private Gson mGson;

    private String mCharFormat=null;
    private List<BluetoothGattService> mGattServices;

    public EUExBluetoothBLE(Context context, EBrowserView eBrowserView) {
        super(context, eBrowserView);
    }

    @Override
    protected boolean clean() {
        return false;
    }


    public void init(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_INIT;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void initMsg(String[] params) {
        mGson=new Gson();
        String json = params[0];
        try {
            JSONObject jsonObject = new JSONObject(json);
            mCharFormat=jsonObject.optString("charFormat");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mContext.
                    getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {

            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {

        }

    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            ResultVO resultVO=new ResultVO();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                resultVO.setResultCode(ResultVO.RESULT_OK);
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                resultVO.setResultCode(ResultVO.RESULT_FAILD);
            }
            callBackPluginJs(JsConst.ON_CONNECTION_STATE_CHANGE,mGson.toJson(resultVO));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mGattServices =mBluetoothGatt.getServices();
                try {
                    displayGattServices(mGattServices);
                } catch (InterruptedException e) {
                }
            } else {

            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            ResultVO resultVO=new ResultVO();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                resultVO.setResultCode(ResultVO.RESULT_OK);
            }else if (status==BluetoothGatt.GATT_FAILURE){
                resultVO.setResultCode(ResultVO.RESULT_FAILD);
            }
            resultVO.setData(getDataFromCharacteristic(characteristic));
            callBackPluginJs(JsConst.ON_CHARACTERISTIC_READ, mGson.toJson(resultVO));
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            callBackPluginJs(JsConst.ON_CHARACTERISTIC_CHANGED,mGson.toJson(getDataFromCharacteristic(characteristic)));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            ResultVO resultVO=new ResultVO();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                resultVO.setResultCode(ResultVO.RESULT_OK);
            }else if (status==BluetoothGatt.GATT_FAILURE){
                resultVO.setResultCode(ResultVO.RESULT_FAILD);
            }
            resultVO.setData(getDataFromCharacteristic(characteristic));
            callBackPluginJs(JsConst.ON_CHARACTERISTIC_WRITE, mGson.toJson(resultVO));
        }


    };

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) throws InterruptedException {
        if (gattServices == null) return;
        List<GattServiceVO> gattServiceVOs=new ArrayList<GattServiceVO>();
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            GattServiceVO gattServiceVO=new GattServiceVO();
            String uuid = gattService.getUuid().toString();
            gattServiceVO.setUuid(uuid);
            List<CharacteristicVO> characteristicVOs=new ArrayList<CharacteristicVO>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
                characteristicVOs.add(getDataFromCharacteristic(gattCharacteristic));
                Thread.sleep(200);
            }
            gattServiceVO.setCharacteristicVOs(characteristicVOs);
            gattServiceVOs.add(gattServiceVO);
        }
        callBackPluginJs(JsConst.ON_SERVICES_DISCOVERED,mGson.toJson(gattServiceVOs));
    }

    public CharacteristicVO getDataFromCharacteristic(BluetoothGattCharacteristic characteristic){
        CharacteristicVO characteristicVO=new CharacteristicVO();
        final byte[] data=characteristic.getValue();
        if (!TextUtils.isEmpty(mCharFormat)) {
            StringBuilder stringBuilder=new StringBuilder(data.length);
            for (byte byteChar : data) {
                stringBuilder.append(String.format(mCharFormat, byteChar));
            }
            characteristicVO.setValueString(stringBuilder.toString());
        }else{
            characteristicVO.setValueString(new String(data));
        }
        characteristicVO.setPermissions(characteristic.getPermissions());
        characteristicVO.setWriteType(characteristic.getWriteType());
        characteristicVO.setUuid(characteristic.getUuid().toString());
        List<BluetoothGattDescriptor> descriptors=characteristic.getDescriptors();
        List<GattDescriptorVO> gattDescriptorVOs=new ArrayList<GattDescriptorVO>();
        if (descriptors!=null&&!descriptors.isEmpty()){
            for (BluetoothGattDescriptor descriptor:descriptors){
                gattDescriptorVOs.add(transfromDescriptor(descriptor));
            }
        }
        characteristicVO.setGattDescriptorVOs(gattDescriptorVOs);
        return characteristicVO;
    }

    private GattDescriptorVO transfromDescriptor(BluetoothGattDescriptor descriptor){
        GattDescriptorVO gattDescriptorVO=new GattDescriptorVO();
        if (descriptor.getUuid()!=null) {
            gattDescriptorVO.setUuid(descriptor.getUuid().toString());
        }
        if (descriptor.getValue()!=null) {
            gattDescriptorVO.setValue(descriptor.getValue().toString());
        }
        gattDescriptorVO.setPermissions(descriptor.getPermissions());
        return gattDescriptorVO;
    }

    public void connect(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_CONNECT;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void connectMsg(String[] params) {
        String json = params[0];
        String address=null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            address=jsonObject.optString("address").toUpperCase();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (mBluetoothAdapter == null || address == null) {
            return;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            mBluetoothGatt.connect();
            return;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);

        mBluetoothDeviceAddress = address;

    }

    public void disconnect(String[] params) {
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_DISCONNECT;
        mHandler.sendMessage(msg);
    }

    private void disconnectMsg(String[] params) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void scanDevice(String[] params) {
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_SCAN_DEVICE;
        mHandler.sendMessage(msg);
    }

    private void scanDeviceMsg(String[] params) {
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            BluetoothDeviceVO deviceVO=new BluetoothDeviceVO();
            deviceVO.setAddress(device.getAddress());
            deviceVO.setName(device.getName());
            callBackPluginJs(JsConst.ON_LE_SCAN, mGson.toJson(deviceVO));
        }
    };

    public void stopScanDevice(String[] params) {
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_STOP_SCAN_DEVICE;
        mHandler.sendMessage(msg);
    }

    private void stopScanDeviceMsg(String[] params) {
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    public void writeCharacteristic(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_WRITE_CHARACTERISTIC;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void writeCharacteristicMsg(String[] params) {
        String json = params[0];
        String serviceUUID=null;
        String characteristicUUID=null;
        String value=null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            serviceUUID=jsonObject.optString("serviceUUID");
            characteristicUUID=jsonObject.optString("characteristicUUID");
            value=jsonObject.optString("value");
        } catch (JSONException e) {
        }
        writeCharacteristicByUUID(serviceUUID,characteristicUUID,value);
    }

    private void writeCharacteristicByUUID(String serviceUUID,String characteristicUUID,String value){
        if (mGattServices==null){
            return;
        }
        for (int i = 0; i < mGattServices.size(); i++) {
            BluetoothGattService bluetoothGattService = mGattServices.get(i);
            if (serviceUUID.equals(bluetoothGattService.getUuid().toString())){
                BluetoothGattCharacteristic gattCharacteristic=bluetoothGattService.
                        getCharacteristic(UUID.fromString(characteristicUUID));
                gattCharacteristic.setValue(value);
                mBluetoothGatt.writeCharacteristic(gattCharacteristic);
               break;
            }
         }
    }

    public void readCharacteristic(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_READ_CHARACTERISTIC;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void readCharacteristicMsg(String[] params) {
        String json = params[0];
        String serviceUUID=null;
        String characteristicUUID=null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            serviceUUID=jsonObject.optString("serviceUUID");
            characteristicUUID=jsonObject.optString("characteristicUUID");
        } catch (JSONException e) {
        }
        readCharacteristicByUUID(serviceUUID,characteristicUUID);
    }

    private void readCharacteristicByUUID(String serviceUUID,String characteristicUUID){
        if (mGattServices==null){
            return;
        }
        for (int i = 0; i < mGattServices.size(); i++) {
            BluetoothGattService bluetoothGattService = mGattServices.get(i);
            if (serviceUUID.equals(bluetoothGattService.getUuid().toString())){
                BluetoothGattCharacteristic gattCharacteristic=bluetoothGattService.
                        getCharacteristic(UUID.fromString(characteristicUUID));
                mBluetoothGatt.readCharacteristic(gattCharacteristic);
              break;
        }
          }
    }

    @Override
    public void onHandleMessage(Message message) {
        if(message == null){
            return;
        }
        Bundle bundle=message.getData();
        switch (message.what) {

            case MSG_INIT:
                initMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_CONNECT:
                connectMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_DISCONNECT:
                disconnectMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_SCAN_DEVICE:
                scanDeviceMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_STOP_SCAN_DEVICE:
                stopScanDeviceMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_WRITE_CHARACTERISTIC:
                writeCharacteristicMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_READ_CHARACTERISTIC:
                readCharacteristicMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            default:
                super.onHandleMessage(message);
        }
    }

    private void callBackPluginJs(String methodName, String jsonData){
        String js = SCRIPT_HEADER + "if(" + methodName + "){"
                + methodName + "('" + jsonData + "');}";
        onCallback(js);
    }

}
