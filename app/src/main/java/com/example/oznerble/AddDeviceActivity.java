package com.example.oznerble;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.oznerble.R.id;
import com.example.oznerble.R.layout;
import com.ozner.application.OznerBLEService.OznerBLEBinder;
import com.ozner.bluetooth.BluetoothScan;
import com.ozner.device.NotSupportDevcieException;
import com.ozner.device.OznerBluetoothDevice;
import com.ozner.device.OznerDevice;
import com.ozner.wifi.mxchip.mxchip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class AddDeviceActivity extends Activity {


	ListView list;
	ListAdapter adapter;
	EditText wifi_ssid;
	EditText wifi_passwd;
	Button wifi_bind;
	Monitor mMonitor=new Monitor();
	mxchip mxChip;
	private void loadWifi() {
		WifiManager wifi_service = (WifiManager) getSystemService(WIFI_SERVICE);
		wifi_bind.setEnabled(wifi_service.getWifiState() == WifiManager.WIFI_STATE_ENABLED);
		WifiInfo wifiInfo = wifi_service.getConnectionInfo();
		if (wifiInfo != null) {
			wifi_ssid.setText(wifiInfo.getSSID().replace("\"", ""));
		} else {
			wifi_ssid.setText("");
		}
	}

	class Monitor extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				loadWifi();
			}
			if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				loadWifi();
			} else {
				adapter.Reload();
			}
		}
	}
	@Override
	protected void onDestroy() {
		this.unregisterReceiver(mMonitor);
		super.onDestroy();
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		IntentFilter filter=new IntentFilter();
		filter.addAction(BluetoothScan.ACTION_SCANNER_FOUND);
		filter.addAction(OznerBluetoothDevice.ACTION_OZNER_BLUETOOTH_BIND_MODE);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

		this.registerReceiver(mMonitor, filter);
		setTitle("设备配对");
		setContentView(layout.activity_add);
		adapter = new ListAdapter(this);
		list = (ListView) findViewById(R.id.deviceList);
		list.setAdapter(adapter);

		wifi_ssid = (EditText) findViewById(id.wifi_ssid);
		wifi_passwd = (EditText) findViewById(id.wifi_password);
		wifi_bind = (Button) findViewById(id.wifi_bind);
		wifi_bind.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				mxChip.start(wifi_ssid.getText().toString(), wifi_passwd.getText().toString());
			}
		});
		mxChip = new mxchip(this);
		loadWifi();

	}

	class ListAdapter extends BaseAdapter implements View.OnClickListener
	{
		ArrayList<OznerBluetoothDevice> list=new ArrayList<OznerBluetoothDevice>();
		Context mContext;
		LayoutInflater mInflater;

		public ListAdapter(Context context)
		{
			mContext=context;
			mInflater=LayoutInflater.from(context); 
			this.Reload();
		}
		private void Reload()
		{
			OznerBLEApplication app=(OznerBLEApplication)getApplication();
			OznerBLEBinder service=app.getService();
			if (service==null) return;
			list.clear();
			for (OznerBluetoothDevice device : service.getDeviceManager().getNotBindDevices())
			{
				list.add(device);
			}
			this.notifyDataSetInvalidated();
		}
		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView==null)
			{
				convertView=mInflater.inflate(R.layout.add_device_item, null);
			}
			OznerBluetoothDevice device=(OznerBluetoothDevice)getItem(position);
			SimpleDateFormat fmt=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			if (device!=null)
			{
				((TextView) convertView.findViewById(id.Device_Name)).setText(
						device.getName() + "(" + device.getAddress() + ")");
				((TextView) convertView.findViewById(id.Device_Model)).setText(
						device.getModel());
				((TextView) convertView.findViewById(id.Device_Platfrom)).setText(
						device.getPlatform());
				((TextView) convertView.findViewById(id.Device_Firmware)).setText(
						fmt.format(new Date(device.getFirmware())));
				
				((TextView) convertView.findViewById(id.Device_Custom)).setText(
						device.getCustomObject() != null ? device.getCustomObject().toString() : "");
				if (device.isBindMode())
					convertView.findViewById(id.addDeviceButton).setEnabled(true);
				else
					convertView.findViewById(id.addDeviceButton).setEnabled(false);
				convertView.findViewById(id.addDeviceButton).setTag(device);
				convertView.findViewById(id.addDeviceButton).setOnClickListener(this);

			}
			
			return convertView;
		}

		@Override
		public void onClick(View v) {
			switch (v.getId())
			{

				case id.addDeviceButton: {
					//获取点击的蓝牙设备
					OznerBluetoothDevice bluetooth = (OznerBluetoothDevice) v.getTag();

					OznerBLEApplication app = (OznerBLEApplication) getApplication();
					//获取服务
					OznerBLEBinder service = app.getService();
					OznerDevice device;
					try {
						//通过找到的蓝牙对象控制对象获取设备对象
						device = service.getDeviceManager().getDevice(bluetooth);
						//设置设备名称
						device.Setting().name("test");
						if (device != null) {
							//保存设备
							service.getDeviceManager().save(device);
							//配对完成,重新加载配对设备列表
							this.Reload();
						}
					} catch (NotSupportDevcieException e) {
						e.printStackTrace();
					}

				}
				break;

			}

		}
	}
}
