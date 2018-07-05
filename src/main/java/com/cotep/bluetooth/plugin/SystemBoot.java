/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cotep.bluetooth.plugin;


/**
 *
 * @author pad
 */
public class SystemBoot {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
      for (String arg : args) {
    System.out.println(arg);
}

        if (args[0].equals("client")) {
          new BluetoothClient().run();

          return;
        }

        if (args[0].equals("server")) {
          new BluetoothServer().run();

          return;
        }

        System.out.println("Args: [client|server]");
    }
}
