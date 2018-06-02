package cl.proyecto.kemosahbe.lanintercom;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
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
    private int serverPort;
    private Socket socket = null;
    //private OutputStream os;
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
        if(!socket.isClosed()){
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        socket = null;
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
            if(socket == null && output == null){
                try {
                    socket = new Socket(serverAddress, serverPort);
                    output = new DataOutputStream(socket.getOutputStream());
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            Bundle data = msg.getData();
            try{
                output.writeBytes(data.getString("mensaje"));
            }catch(Exception e){
                e.printStackTrace();
            }
            super.handleMessage(msg);
        }
    }
}
