/*
 * This class takes a Bluetooth device and manages the connection state
 * Largely taken from the Android website.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;


public class BTConnection extends Thread{
	public static final String TAG = BTConnection.class.getName();
	private  final UUID MY_UUID_SECURE = 
		    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private  Handler mHandler;	
	private  BluetoothSocket mmSocket;
    private  BluetoothDevice mmDevice;
    private  BluetoothAdapter btAdapter;
    private  ConnectedThread btThread;

	/*
	 * 
	 */
    public BTConnection(BluetoothAdapter btAdapter,BluetoothDevice btDevice, Handler mHander){
		BluetoothSocket tmp = null;
		
		this.mHandler = mHander;
		this.mmDevice = btDevice;
		this.btAdapter = btAdapter;
		
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
        } catch (IOException e) { }
        mmSocket = tmp;	
	}
	/*
	 * 
	 */
    public void write(String text){
    	text = String.format(text+"%n");
    	btThread.write(text.getBytes());
    }
    
    /*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	 public void run() {
	        // Cancel discovery because it will slow down the connection
	    	btAdapter.cancelDiscovery();
	 
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	        } catch (IOException connectException) {
	            Log.e(TAG, "Unable to connect to " + mmDevice.getName());
	        	// Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	                return;
	            } catch (IOException closeException) {
	            	Log.e(TAG, "Unable to close failed connection");
	            	return;
	            }
	        }
	        // Do work to manage the connection (in a separate thread)
	        btThread = new ConnectedThread(mmSocket);
	        btThread.start();
            
            return;
	 	}
	 
	 public void cancel(){
		 if (btThread != null){
			 btThread.cancel();
		 }
	 }
	 
	 
	 private class ConnectedThread extends Thread {
		    private static final int MESSAGE_READ = 0;
			private final BluetoothSocket mmSocket;
		    private final InputStream mmInStream;
		    private final OutputStream mmOutStream;
		 
		    public ConnectedThread(BluetoothSocket socket) {
		        mmSocket = socket;
		        InputStream tmpIn = null;
		        OutputStream tmpOut = null;
		 
		        // Get the input and output streams, using temp objects because
		        // member streams are final
		        try {
		            tmpIn = socket.getInputStream();
		            tmpOut = socket.getOutputStream();
		        } catch (IOException e) { }
		 
		        mmInStream = tmpIn;
		        mmOutStream = tmpOut;
		    }
		 
		    public void run() {
		        byte[] buffer = new byte[1024];  // buffer store for the stream
		        int bytes; // bytes returned from read()
		 
		        // Keep listening to the InputStream until an exception occurs
		        while (true) {
		            try {
		                // Read from the InputStream
		                bytes = mmInStream.read(buffer);
		                // Send the obtained bytes to the UI activity
		                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
		                        .sendToTarget();
		            } catch (IOException e) {
		                break;
		            }
		        }
		    }
		 
		    /* Call this from the main activity to send data to the remote device */
		    public void write(byte[] bytes) {
		        try {
		            mmOutStream.write(bytes);
		        } catch (IOException e) { }
		    }
		 
		    /* Call this from the main activity to shutdown the connection */
		    public void cancel() {
		        try {
		            mmSocket.close();
		        } catch (IOException e) { }
		    }
		}
}




