package cl.proyecto.kemosahbe.lanintercom;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.net.SocketOptions;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.SocketHandler;

public class ServicioMensajeria extends Service {
    private Messenger messenger, uMessenger;
    private mHandler msgHandler;
    private static final String tag = "MENSAJERIA";
    private String usuario, destinatario;
    private InetAddress localAddress, serverAddress;
    private InetSocketAddress sockaddr;
    private int serverPort;
    private MulticastSocket socket = null;
    private DatagramPacket outPackage, inPackage;
    private DataOutputStream output = null;

    @Override
    public void onCreate() {
        Log.i(tag, "Servicio Mensajeria Creado ["+Thread.currentThread().getName()+" PID: "+Thread.currentThread().getId()+"]");
        HandlerThread hThread = new HandlerThread("Servicio Mensajeria");
        hThread.start();
        msgHandler = new mHandler(hThread.getLooper());
        messenger = new Messenger(msgHandler);
    }

    public static String getIP(){
        List<InetAddress> addrs;
        String address = "";
        try{
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for(NetworkInterface intf : interfaces){
                addrs = Collections.list(intf.getInetAddresses());
                for(InetAddress addr : addrs){
                    if(!addr.isLoopbackAddress() && addr instanceof Inet4Address){
                        address = addr.getHostAddress();
                    }
                }
            }
        }catch (Exception e){
            Log.i(tag, "Ex getting IP value " + e.getMessage());
        }
        return address;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        messenger = null;
        uMessenger = null;
        if(socket != null) {
            if (!socket.isClosed()) {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else {
            socket = null;
        }
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Bundle extras = intent.getExtras();
        if(extras != null){
            uMessenger = (Messenger) extras.get("messenger");
            try {
                serverAddress = InetAddress.getByName(extras.getString("serverAddress"));
                Log.i(tag, extras.getString("serverAddress"));
                serverPort = extras.getInt("serverPort");
                sockaddr = new InetSocketAddress("239.255.255.249",1800);
                Log.i(tag, ""+extras.getInt("serverPort"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return messenger.getBinder();
    }

    class mHandler extends Handler{
        public mHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if(socket == null){
                try {
                    socket = new MulticastSocket(1800);
                    socket.setReuseAddress(true);
                    socket.setBroadcast(true);
                }catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(ServicioMensajeria.this, "Error al conectar Socket", Toast.LENGTH_SHORT).show();
                }
            }else if(socket.isClosed() && socket != null){
                try{
                    socket = new MulticastSocket(1800);
                    socket.setReuseAddress(true);
                    socket.setBroadcast(true);
                }catch(Exception e) {
                    e.printStackTrace();
                    Toast.makeText(ServicioMensajeria.this, "Error al conectar Socket", Toast.LENGTH_SHORT).show();
                }
            }
            Toast.makeText(ServicioMensajeria.this, "Conectado al servidor.", Toast.LENGTH_SHORT).show();
            Bundle data = msg.getData();
            try{
                byte[] msgBytes;
                msgBytes = data.getString("mensaje").getBytes();
                Log.i(tag,data.getString("mensaje"));
                outPackage = new DatagramPacket(msgBytes, msgBytes.length, sockaddr);
                socket.joinGroup(sockaddr.getAddress());
                //socket.connect(new InetSocketAddress(serverAddress,serverPort));
                socket.send(outPackage);
                byte[] recvBuffer = new byte[256];
                inPackage = new DatagramPacket(recvBuffer,recvBuffer.length);
                //Log.i(tag,"LocalAddress: "+socket.getLocalAddress().getHostAddress());
                socket.receive(inPackage);
                //DatagramSocket sock = new DatagramSocket(1810);
                //sock.receive(inPackage);
                //String Msg = new String(inPackage.getData()).trim();
                //Log.i(tag,"Mensaje: "+ Msg + " From: "+inPackage.getAddress().getHostAddress());
                socket.leaveGroup(sockaddr.getAddress());
                socket.close();
                //sock.close();
                Toast.makeText(ServicioMensajeria.this, "Mensaje enviado con exito.", Toast.LENGTH_SHORT).show();
            }catch(Exception e){
                e.printStackTrace();
            }
            super.handleMessage(msg);
        }
    }
}
