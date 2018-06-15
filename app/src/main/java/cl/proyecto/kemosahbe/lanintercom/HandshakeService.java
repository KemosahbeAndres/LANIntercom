package cl.proyecto.kemosahbe.lanintercom;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class HandshakeService extends Service implements ScoutEvent {

    //Variables Privadas
    //private
    private Messenger sMessenger, mMessenger;
    private ServiceHandler mHandler;
    private Scout mScout;
    private Thread th;

    //Constantes
    private final String tag = "HandshakeService";

    @Override
    public void onCreate() {
        mHandler = new ServiceHandler();
        sMessenger = new Messenger(mHandler);
        mScout = new Scout("UserScout");
        mScout.setScoutEventListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Bundle extras = intent.getExtras();
        if(extras != null){
            mMessenger = (Messenger)extras.get("messenger");

        }
        th = new Thread(mScout,"ScoutThread");
        th.start();
        return sMessenger.getBinder();
    }

    @Override
    public void onDestroy() {

        mScout = null;
        mHandler = null;
        sMessenger = null;
        mMessenger = null;
        super.onDestroy();
    }

    @Override
    public void onRecieve(byte[] data, InetAddress addr, int port) {
        Log.i(tag,"Datos Recibidos: "+new String(data).trim()+" "+addr+" "+port);

    }

    private class ServiceHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            Log.i(tag,"Deteniendo scout");
            th.interrupt();


            super.handleMessage(msg);
        }
    }
}
