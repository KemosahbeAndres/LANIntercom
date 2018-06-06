package cl.proyecto.kemosahbe.lanintercom;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;

public class Scout implements Runnable {
    private List<ScoutEvent> mListeners = new ArrayList<ScoutEvent>();
    private String nombre;
    //private NetworkInterface iface;
    private static MulticastSocket socket;
    private int servicePort;
    private InetAddress serviceAddr;
    //private Boolean status;
    private DatagramPacket packet;
    private byte[] buffer;
    private final String tag = "ScoutInstance";

    public Scout(String name) {
        this.nombre = name;
        //this.status = false;
        this.buffer = new byte[1024];
        this.packet = new DatagramPacket(buffer, buffer.length);
        try {
            //this.iface = NetworkInterface.getByName("wlan0");
            this.servicePort = 1800;
            this.serviceAddr = InetAddress.getByName("230.255.255.200");
            socket = new MulticastSocket(this.servicePort);
            //socket.setNetworkInterface(iface);
            socket.setReuseAddress(true);
            socket.joinGroup(this.serviceAddr);
        }catch(Exception e){
            Log.i("Scout","[Error] "+e.getMessage());
        }
    }

    public void setScoutEventListener(ScoutEvent listener){
        this.mListeners.add(listener);
    }
    public void removeScoutEventListener(ScoutEvent listener){
        this.mListeners.remove(listener);
    }

    @Override
    public void run() {
        //this.status = true;
        //while(!Thread.currentThread().isInterrupted()){
        try {
            //mListener.onRecieve("ScoutIniciado".getBytes(),serviceAddr,servicePort);
            //Log.i("Scout","Esperando un mensaje: "+socket.getNetworkInterface().getName());
            //this.socket.setTimeToLive(30);
            socket.receive(packet);
            //Log.i(tag,"Se recibio un mensaje.");


            launchOnRecieve(packet.getData(),packet.getAddress(),packet.getPort());

        }catch(Exception e){
            launchOnRecieve(e.getMessage().getBytes(),serviceAddr,servicePort);
        }
        //}
        this.closeScout();
    }

    private void closeScout(){
        try {
            Log.i(tag, "Cerrando Scout Thread.");
            socket.leaveGroup(serviceAddr);
            socket.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void launchOnRecieve(byte[] mData, InetAddress mAddr, int mPort){
        for(ScoutEvent listener : this.mListeners){
            listener.onRecieve(mData, mAddr, mPort);
        }
    }
}
