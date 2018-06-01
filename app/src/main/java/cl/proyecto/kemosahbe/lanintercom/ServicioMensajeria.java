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

public class ServicioMensajeria extends Service {
    private Messenger messenger, uMessenger;
    private mHandler msgHandler;
    private final String tag = "MENSAJERIA";
    private String usuario, destinatario;
    private String localAddress, destAddress;
    private int localPort, destPort;

    @Override
    public void onCreate() {
        Log.i(tag, "Servicio Mensajeria Creado ["+Thread.currentThread().getName()+" PID: "+Thread.currentThread().getId()+"]");
        HandlerThread hThread = new HandlerThread("Servicio Mensajeria");
        hThread.start();
        msgHandler = new mHandler(hThread.getLooper());
        messenger = new Messenger(msgHandler);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Bundle extras = intent.getExtras();
        if(extras != null){
            uMessenger = (Messenger) extras.get("messenger");
        }
        return messenger.getBinder();
    }
    class mHandler extends Handler{
        public mHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
}
