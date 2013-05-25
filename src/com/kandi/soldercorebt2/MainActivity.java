/*
 * Simple demo app that connects a Bluetooth enabled Android device
 * to a BT module and sends simple string commands
 * 
 * Author: Iain Derrington (@IDerrington)
 * http://kandi-electronics.co.uk
 * http://soldercore.com (@SolderCore)
 * 
 * License: Car & Motorbike both clean.
 * 
 * Version: LOL
 * 
 */

package com.kandi.soldercorebt2;

import java.util.ArrayList;
import java.util.Set;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
	public final static String TAG =  MainActivity.class.getName();
	
	public enum BtState{
		DISCONNECTED, CONNNECTED, DISCOVERING
	};
	
	/* Bluetooth hardware*/
	BluetoothAdapter btAdapter;
	BluetoothDevice btDevice;
	
	/*btConnection manager object*/
	BTConnection mBluetooth;
	
	/*Android view related stuff */
	ListView btListView;
	ArrayAdapter<String> btDevicesAdaptor;
	ArrayList<String> pairedDevices;
	Button btnScan;
	Button btnSend;
	Button btnLedon;
	Button btnLedOff;
	TextView txtSend;
	TextView txtStatus;
	ProgressDialog progressBar;
	
	/* State Management*/
	BtState btState;
	
	

	/* 
	 * Messages from the Bluetooth Manager should come in here 
	 */
	private static Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message message){
			Log.d(TAG, "In Message Handler");	
		}
	};
	
	/* 
	 *  Begin here. 
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.activity_main);
		
		// Get a reference to the devices Bluetooth hardware
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if (btAdapter == null){
			// Bluetooth not supported.
			finish();
		}
		
	}
	@Override
	protected void onResume(){
		super.onResume();
		setUpUI();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (mBluetooth!=null){
			mBluetooth.cancel();
		}
		if (btRegReciever != null){
			unregisterReceiver(btRegReciever);
		}
	}

/*****************************************************
 * Called by onCreate. 
 * Tries to enable the Bluetooth hardware (if not already enabled).
 * If already enabled initiates a BT scan
 * 
 ****************************************************/
	private void setUpUI() {
		btDevicesAdaptor = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1);
		pairedDevices = new ArrayList<String>();
		btListView = (ListView)findViewById(R.id.listView1);
		btListView.setAdapter(btDevicesAdaptor);
		btnScan = (Button)findViewById(R.id.btnScan);
		btnSend = (Button)findViewById(R.id.btnSendData);
		btnLedOff = (Button)findViewById(R.id.btnLedOff);
		btnLedon = (Button)findViewById(R.id.btnLedOn);
		txtStatus  = (TextView) findViewById(R.id.txtUpdate);
		
		// if not enabled. Enable the BT adapter
		if (!btAdapter.isEnabled()){
			Log.d(TAG, "BT Adapter not enabled... firing ACTION_REQUEST_ENABLE");
			Intent intent =  new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(intent, 0);
		}else{
			
		}
		
		// Listen out for a Bluetooth broadcast
		IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		registerReceiver(btRegReciever, intentFilter);
		
		/*
		 * Configure UI elements for default state.
		 */
			btnSend.setEnabled(false);
			btnLedOff.setEnabled(false);
			btnLedon.setEnabled(false);
			btnScan.setEnabled(true);
			btnScan.setText("Scan");
			btDevicesAdaptor.clear();
			txtStatus.setText("Bluetooth Status: Disconnected");
			setProgressBarIndeterminateVisibility(false);

			btState = BtState.DISCONNECTED;
		
		/*
		 * When user selects an item on the list lets try to connect to it.
		 * If it successfully connects update the connection state.
		 * The broadcast receiver above should also detect a device connection and update the UI
		 */
		btListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			 
		     public void onItemClick(AdapterView<?> parentAdapter, View view, int position,
		                             long id) {      
		         
		    	 btAdapter.cancelDiscovery();
		    	 txtStatus.setText("Bluetooth Status: Connecting...");
		    	 
		    	 // We know the View is a TextView so we can cast it
		         TextView clickedView = (TextView) view;
		         
		         String devAddress = (String) clickedView.getText();
		         String[] parts = devAddress.split("\n");
		         parts = parts[1].split(" ");
		          
		         btDevice =  btAdapter.getRemoteDevice(parts[0]);
		         
		         mBluetooth = new BTConnection(btAdapter, btDevice, mHandler);
		         mBluetooth.start();
		          
		     }
		});
		
		/*
		 * Kicks off a  scan when user presses button.
		 */
		btnScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (btState == BtState.CONNNECTED){
					if (mBluetooth != null){
						setProgressBarIndeterminateVisibility(true);
						txtStatus.setText("Bluetooth Status: Disconnecting");
						btnScan.setText("Wait..");
						mBluetooth.cancel();
					}
				}
				else if (btState == BtState.DISCOVERING){
					// Already scanning so do nothing
				}
				else{
					btScan();
				}
			}});
		
		
		/*
		 * Commands send to the SolderCore
		 */
		btnSend.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View v) {
				if (btState == BtState.CONNNECTED) {
					mBluetooth.write("Hello");
				}
			}});
		
		btnLedOff.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (btState == BtState.CONNNECTED) {
					mBluetooth.write("LED OFF");
				}
			}});
		
		btnLedon.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (btState == BtState.CONNNECTED) {
					mBluetooth.write("LED ON");
				}
			}});
	}
	


/*
 *  Call this function when the Bluetooth adapter is enabled to start a BT scan
 */
	private void btScan() {
		
		//Empty the adaptor or else duplicates will appear
		btDevicesAdaptor.clear();
		
		setProgressBarIndeterminateVisibility(true);
		btnScan.setEnabled(false);
		txtStatus.setText("Bluetooth Status: Scanning...");
		//Toast.makeText(getApplicationContext(), "Scanning for devices...",	Toast.LENGTH_LONG).show();
			 
		// Get a list of devices previously paired to
		Set<BluetoothDevice> psetDev = btAdapter.getBondedDevices();
					
		// Add name and address to a list 
		for (BluetoothDevice device : psetDev) {
		// Add the name and address to an array adapter to show in a ListView
			pairedDevices.add(device.getName() + "\n" + device.getAddress());
		}
					
		// Adapter is on. Start discovery
		if(btAdapter.startDiscovery()){
			// and resister various Bluetooth broadcast messages with our broadcast receiver
			registerReceiver(btRegReciever, new IntentFilter(BluetoothDevice.ACTION_FOUND));
			registerReceiver(btRegReciever, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
			registerReceiver(btRegReciever, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
			registerReceiver(btRegReciever, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
			registerReceiver(btRegReciever, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED));
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
/*
 * Broadcast Receiver that listens for the following:
 * 
 * BluetoothDevice.ACTION_FOUND
 * BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
 * BluetoothDevice.ACTION_ACL_CONNECTED
 */
	private final BroadcastReceiver btRegReciever = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context arg0, Intent intent) {
			String action = intent.getAction();	
			String toastText;
			//String prevStateExtra = btAdapter.EXTRA_PREVIOUS_STATE;
			String stateExtra = BluetoothAdapter.EXTRA_STATE;
			int state = intent.getIntExtra(stateExtra,-1);
			//intent.getIntExtra(prevStateExtra, -1);
			
			Log.d(TAG, "BroadCast Reciver (Action):" + action);
			
			/*
			 * Handle Bluetooth Adapter state and messages
			 */
			switch(state){
				case(BluetoothAdapter.STATE_TURNING_ON):
				{
					toastText = "Bluetooth Turning On";
					break;
				}
				case(BluetoothAdapter.STATE_ON):
				{
					toastText = "Bluetooth On";
					Toast.makeText(MainActivity.this,toastText, Toast.LENGTH_SHORT).show();
					break;
				}
				case (BluetoothAdapter.STATE_TURNING_OFF):
				{
					toastText = "Bluetooth Turning Off";
					Toast.makeText(MainActivity.this,toastText, Toast.LENGTH_SHORT).show();
					break;
				}
				case (BluetoothAdapter.STATE_OFF):
				{
					toastText = "Bluetooth Off";
					Toast.makeText(MainActivity.this,toastText, Toast.LENGTH_SHORT).show();
					break;
				}	
			} 
			
			if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
				btnScan.setEnabled(true);
				setProgressBarIndeterminateVisibility(false);
				txtStatus.setText("Bluetooth Status: Not Connected");
				//Toast.makeText(getApplicationContext(), "Discovery Complete", Toast.LENGTH_SHORT).show();
			}
			
			/*
			 * Handle Bluetooth DEVICE messages
			 */
			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
				 // Get the BluetoothDevice object from the Intent
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				String deviceName = device.getName();
	            //toastText = "Connected to "+ deviceName;
	            //Toast.makeText(MainActivity.this,toastText, Toast.LENGTH_SHORT).show();
	            txtStatus.setText("Bluetooth Status: Connected (" + deviceName +")");
	            btnSend.setEnabled(true);
	            btnLedOff.setEnabled(true);
	            btnLedon.setEnabled(true);
	            btState = BtState.CONNNECTED;
	            btDevicesAdaptor.clear();
	            btnScan.setText("Disconnect");
			}
			
			if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action) || BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action) ) {
				 // Get the BluetoothDevice object from the Intent
				setProgressBarIndeterminateVisibility(false);
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				String deviceName = device.getName();
	            toastText = "Disconnected from "+ deviceName;
	            Toast.makeText(MainActivity.this,toastText, Toast.LENGTH_SHORT).show();
	            txtStatus.setText("Bluetooth Status: Disconnected");
	            btnScan.setText("Scan");
	            btnSend.setEnabled(false);
	            btnLedOff.setEnabled(false);
	            btnLedon.setEnabled(false);
	            btState = BtState.DISCONNECTED;
			}
			
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            // Get the BluetoothDevice object from the Intent
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            
	            for (int i = 0; i< pairedDevices.size(); i++){
	            	if (pairedDevices.get(i).contains( device.getName() ) ){
	            		btDevicesAdaptor.add(device.getName() + "\n" + device.getAddress() + " PAIRED");
	            		//toastText = "Found: " + device.getName() + "Already Paired!";
	    	            //Toast.makeText(MainActivity.this,toastText, Toast.LENGTH_SHORT).show();
	            		return;  
	            	}
	            }
	           
	            // Add the name and address to an array adapter to show in a ListView
	            btDevicesAdaptor.add(device.getName() + "\n" + device.getAddress());
	    		toastText = "Found: " + device.getName();
	            //Toast.makeText(MainActivity.this,toastText, Toast.LENGTH_SHORT).show();
	        }
	}
};	
}

