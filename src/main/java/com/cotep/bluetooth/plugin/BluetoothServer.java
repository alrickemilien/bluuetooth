/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cotep.bluetooth.plugin;

import java.util.UUID;
import java.io.*;
import javax.bluetooth.*;
import javax.microedition.io.*;

public class BluetoothServer implements Runnable {

	int 	isConnected;
	int 	closeRequest;

	byte 	inputBuffer[];
	int 	inputBufferSize;

	byte 	outputBuffer[];
	int 	outputBufferSize;

	final static long BT_NOACTIVITY_TIMEOUT=60000L;

	// The uid of the service, it has to be unique,
	// It can be generated randomly
	public final javax.bluetooth.UUID uuid =  new javax.bluetooth.UUID("27012f0c68af4fbf8dbe6bbaf7aa432a", false);

	// The name of the service
	public final String name = "BluetoothChat";

	//the service url
	public final String url  =  "btspp://localhost:"
		+ uuid
		+ ";name=" + name
		+ ";authenticate=false;encrypt=false;";

	LocalDevice local = null;
	StreamConnectionNotifier server = null;
	StreamConnection connection = null;

	public BluetoothServer() {
		inputBuffer=new byte[1024];
		inputBufferSize=0;
		outputBuffer=new byte[1024];
		outputBufferSize=0;
	}

	public int isConnected() {
		return isConnected;
	}

	public int open() {
		closeRequest=0;
		new Thread(this).start();

		return 1;
	}

	synchronized int addToInputBuffer(byte buf[], int off, int len) {
		if((inputBufferSize+len)>inputBuffer.length) return 0;

		System.arraycopy(buf, 0, inputBuffer, inputBufferSize, len);

		inputBufferSize+=len;

		return 1;
	}

	public synchronized int read(byte buf[], int off, int len) {
		if(len>inputBufferSize) len=inputBufferSize;

		System.arraycopy(inputBuffer, 0, buf, off, len);

		if(len-inputBufferSize>0) System.arraycopy(inputBuffer, len, inputBuffer, 0, len-inputBufferSize);

		inputBufferSize-=len;

		return len;
	}

	public synchronized int write(byte buf[], int off, int len) {
		if(len>outputBuffer.length-outputBufferSize) len=outputBuffer.length-outputBufferSize;

		if(len<=0) return 0;

		System.arraycopy(buf, off, outputBuffer, outputBufferSize, len);

		outputBufferSize+=len;

		return len;
	}

	public int available() {
		return inputBufferSize;
	}

	public int close() {
		closeRequest=1;
		return 1;
	}

	public void listen() {
		long lastReceivedTime=System.currentTimeMillis();
		byte buf[]=new byte[1024];
		int i, v, lastv=1234;

		try {
			while(true) {
				System.out.println("Start advertising service...");
				System.out.println(uuid.toString());

				server = (StreamConnectionNotifier)Connector.open(url);

				System.out.println("Waiting for incoming connection on url="+url);

				connection = server.acceptAndOpen();

				isConnected=1;

				System.out.println("Client Connected...");

				DataInputStream din = new DataInputStream(connection.openInputStream());

				DataOutputStream dout = new DataOutputStream(connection.openOutputStream());

				lastReceivedTime = System.currentTimeMillis();

				while(true) {
					Thread.sleep(500);

					if(closeRequest==1) break;

					try {
						v=din.available();
					} catch(Exception e) {
						System.out.println("device seems to be disconnected... closing connection.");
						break;
					}

					if(v>0) {
						v=din.read(buf);
						if(v==-1) {
							System.out.println("device seems to be disconnected... closing connection.");
							break;
						}
						addToInputBuffer(buf, 0, v);
						lastReceivedTime = System.currentTimeMillis();
						//for(i=0; i<v; i++) System.out.println("received="+buf[i]);
						//dout.write(buf, 0, v);
					}

					if(outputBufferSize>0) {
						synchronized (this) {
							try {
								dout.write(outputBuffer, 0, outputBufferSize);
							} catch(Exception e) {
								System.out.println("device seems to be disconnected... closing connection.");
								break;
							}
							outputBufferSize=0;
						}
					}
					/*
					   if(v==0) {
					   if(System.currentTimeMillis()-lastReceivedTime>BT_NOACTIVITY_TIMEOUT) {
					   System.out.println("Timeout reached : noactivity closing connection");
					   break;
					   }
					   }
					 */
					if(lastv!=v) {
						System.out.println("v="+v);
						lastv=v;
					}
				}
				/*while(true){
				  String cmd = "";
				  char c;
				  while (((c = din.readChar()) > 0) && (c!='\n') ){
				  cmd = cmd + c;
				  }
				  System.out.println("Received " + cmd);
				  }*/
				din.close();
				connection.close();
				server.close();

				if(closeRequest==1) break;
			}
		} catch(Exception e) {
			System.out.println(e.toString());
			return;
		}
	}

	public void run() {
		try {
			isConnected=0;

			local = LocalDevice.getLocalDevice();

			// http://www.bluecove.org/bluecove/apidocs/javax/bluetooth/DiscoveryAgent.html
			// Sets the way devices are recognized with bluetooth
			local.setDiscoverable(DiscoveryAgent.GIAC);

			System.out.println("Bluetooth API version : " + local.getProperty("bluetooth.api.version"));
		} catch (BluetoothStateException ex) {
			System.out.println("Cannot get local device : " + ex.toString());
			return;
		}

		listen();
	}
}
