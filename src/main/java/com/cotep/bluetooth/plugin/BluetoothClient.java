package com.cotep.bluetooth.plugin;

import java.io.*;
import java.util.*;
import javax.bluetooth.*;
import javax.microedition.io.*;

public class BluetoothClient implements DiscoveryListener {
	Vector discoveredDevices = new Vector();

	//object used for waiting
	private static Object lock = new Object();

	private static String connectionURL = null;

  public static void main(String[] args) {
      System.out.println("Client booting ...");

      new BluetoothClient().run();
  }

	// //////////////////////////////////
	// DiscoveryListener Callbacks
	// //////////////////////////////////

	public void run() {
		try {
			//display local device address and name
			LocalDevice localDevice = LocalDevice.getLocalDevice();

			System.out.println("Address: "+localDevice.getBluetoothAddress());
			System.out.println("Name: "+localDevice.getFriendlyName());

			//find devices
			DiscoveryAgent agent = localDevice.getDiscoveryAgent();

			System.out.println("Starting device inquiry...");
			agent.startInquiry(DiscoveryAgent.GIAC, this);

			try {
				synchronized(lock){
					lock.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}


			System.out.println("Device Inquiry Completed. ");

			//print all devices in discoveredDevices
			int deviceCount = discoveredDevices.size();

			if(deviceCount <= 0){
				System.out.println("No Devices Found .");

				return;
			}

			//print bluetooth device addresses and names in the format [ No. address (name) ]
			System.out.println("Bluetooth Devices: ");

			for (int i = 0; i < deviceCount; i++) {
				RemoteDevice remoteDevice = (RemoteDevice)discoveredDevices.elementAt(i);

				System.out.println((i + 1) + ". " + remoteDevice.getBluetoothAddress() + " ("+ remoteDevice.getFriendlyName(true) + ")");
			}

			System.out.print("Choose Device index: ");
			BufferedReader bReader=new BufferedReader(new InputStreamReader(System.in));

			String chosenIndex=bReader.readLine();
			int index=Integer.parseInt(chosenIndex.trim());

			// Check for spp service
			RemoteDevice remoteDevice=(RemoteDevice)discoveredDevices.elementAt(index - 1);

			javax.bluetooth.UUID[] uuidSet = new javax.bluetooth.UUID[1];
			uuidSet[0] = new javax.bluetooth.UUID("1101", true);
			int[] attrSet={0x1101};

			System.out.println("\nSearching for service...");
			agent.searchServices(attrSet,uuidSet,remoteDevice,this);

			try {
				synchronized(lock){
					lock.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if(connectionURL == null){
				System.out.println("Device does not support Simple SPP Service.");

				return;
			}


			// Connect to the server and send a line of text
			try {
				StreamConnection streamConnection=(StreamConnection)Connector.open(connectionURL);

				//send string
				OutputStream outStream=streamConnection.openOutputStream();
				PrintWriter pWriter=new PrintWriter(new OutputStreamWriter(outStream));

				pWriter.write("Test String from SPP Client\r\n");
				pWriter.flush();

				//read response
				InputStream inStream=streamConnection.openInputStream();
				BufferedReader bReader2=new BufferedReader(new InputStreamReader(inStream));

				String lineRead=bReader2.readLine();
				System.out.println(lineRead);
			} catch(Exception e) {
				e.printStackTrace();
			}
		} catch(Exception e) {

		}
	}

	/**
	 * deviceDiscovered() is called by the DiscoveryAgent when
	 * it discovers a device during an inquiry.
	 */
	public void deviceDiscovered(javax.bluetooth.RemoteDevice remoteDevice, javax.bluetooth.DeviceClass deviceClass) {
		// Keep track of discovered remote devices by inserting
		// them into a Vector

		//add the device to the vector
		if(!discoveredDevices.contains(remoteDevice)){
			discoveredDevices.addElement(remoteDevice);
		}
	}

	/**
	 * inquiryCompleted() is called by the DiscoveryAgent when
	 * a device discovery cycle finishes.
	 */
	public void inquiryCompleted(int param) {
		// Now that the inquiry has been completed, if any
		// devices were discovered trigger the search for services

		synchronized(lock){
			lock.notify();
		}
	}

	/**
	 * servicesDiscovered() is called by the DiscoveryAgent when
	 * a service search finds services.
	 * transID identifies the service search that returned results.
	 * serviceRecord holds references to the services found.
	 */
	public void servicesDiscovered(int transID, javax.bluetooth.ServiceRecord[] serviceRecord) {
		// Keep track of discovered services, adding them
		// to a Vector
		System.out.println("Inside Service Discovered");

		if(serviceRecord != null && serviceRecord.length > 0){
			connectionURL = serviceRecord[0].getConnectionURL(0,false);

			for (int i = 0; i < serviceRecord.length; i++) {
				DataElement serviceName = serviceRecord[i].getAttributeValue(0x0100);

				System.out.println("service " + serviceName.getValue() + " found ");
			}
			/*
			   for (int i = 0; i < serviceRecord.length; i++) {
			   String url = serviceRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
			// System.out.println("Connection URL="+url);
			if (url == null) {
			continue;
			}
			// serviceFound.add(url);
			DataElement serviceName = serviceRecord[i].getAttributeValue(0x0100);
			if (serviceName != null) {
			System.out.println("service " + serviceName.getValue() + " found " + url);
			// if(serviceName.getValue().equals("OBEX Object Push")){
			// sendMessageToDevice(url);
			//}

			} else {
			System.out.println("service found " + url);
			}
			}*/
		}

		synchronized(lock){
			lock.notify();
		}
	}

	/**
	 * serviceSearchCompleted() is called by the DiscoveryAgent
	 * implementation when a service search finishes.
	 * transID identifies a particular service search.
	 * responseCode indicates why the service search ended.
	 */
	public void serviceSearchCompleted(int transID, int responseCode) {
		// Now that the service discovery has been completed,
		// dispatch thread to handle the discovered services

		synchronized(lock){
			lock.notify();
		}
	}
}
