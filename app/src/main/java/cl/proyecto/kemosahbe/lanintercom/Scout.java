package cl.proyecto.kemosahbe.lanintercom;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Scout implements Runnable {
    private ScoutEvent mListener;
    private String nombre;
    //private NetworkInterface iface;
    private static MulticastSocket socket;
    private int servicePort;
    private InetAddress serviceAddr;
    //private Boolean status;
    private DatagramPacket packet;
    private byte[] buffer;

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
        this.mListener = listener;
    }

    @Override
    public void run() {
        //this.status = true;
        while(!Thread.currentThread().isInterrupted()){
            try {
                //mListener.onRecieve("ScoutIniciado".getBytes(),serviceAddr,servicePort);
                //Log.i("Scout","Esperando un mensaje: "+socket.getNetworkInterface().getName());
                //this.socket.setTimeToLive(30);
                socket.receive(packet);
                //Log.i("Scout","Se recibio un mensaje.");

                mListener.onRecieve(packet.getData(),packet.getAddress(),packet.getPort());

            }catch(Exception e){
                mListener.onRecieve(e.getMessage().getBytes(),serviceAddr,servicePort);
            }
        }
        this.closeScout();
    }

    public void closeScout(){
        try {

            mListener.onRecieve("Saliendo.".getBytes(),this.serviceAddr,this.servicePort);

            socket.leaveGroup(serviceAddr);
            socket.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
}
