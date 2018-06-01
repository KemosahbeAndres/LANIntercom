package cl.proyecto.kemosahbe.lanintercom;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.sql.Time;

public class ServerService extends Service {
    private mHandler serverHandler;
    private HandlerThread mThread;
    final String tag = "SERVICIO";
    public ServerService() {
    }

    @Override
    public void onCreate() {
        Log.i(tag, "Servicio Creado: "+Thread.currentThread().getName()+" ("+Thread.currentThread().getId()+")");
        mThread = new HandlerThread("ServerServiceThread");
        mThread.start();
        serverHandler = new mHandler(mThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        //Log.i(tag, "Servicio Iniciado: "+Thread.currentThread().getName()+" ("+Thread.currentThread().getId()+")");
        Message msg = new Message();
        msg.arg1 = startId;
        msg.obj = intent;
        serverHandler.sendMessage(msg);
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(tag, "Servicio Destruido: "+Thread.currentThread().getName()+" ("+Thread.currentThread().getId()+")");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    class mHandler extends Handler{
        public mHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(tag, Thread.currentThread().getName()+" ("+Thread.currentThread().getId()+")");
            //Instrucciones en segundo plano.
        }
    }
}
