package cl.proyecto.kemosahbe.lanintercom;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    TextView txt;
    EditText msg;
    Boolean iniciado = false;
    final String tag = "MAIN";
    private Messenger messenger, sMessenger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(tag, "Servicio Creado: "+Thread.currentThread().getName()+" ("+Thread.currentThread().getId()+")");
        txt = (TextView) findViewById(R.id.texto);
        msg = (EditText) findViewById(R.id.msg);
        txt.setText("Servicio No iniciado");
        //Comprobar que el dispositivo este conectado a una red WIFI
    }

    @Override
    protected void onStart() {
        super.onStart();
        messenger = new Messenger(new mHandler());
    }

    public void iniciar(View v){
        Intent intent = new Intent(this, ServicioMensajeria.class);
        if(iniciado){
            try {
                unbindService(mConnection);
                txt.setText("Servicio Detenido");
                iniciado = false;
            }catch(Exception e){
                e.printStackTrace();
            }
        }else {
            try {
                intent.putExtra("messenger", messenger);
                intent.putExtra("serverAddress","239.255.255.249");
                intent.putExtra("serverPort", 1800);
                bindService(intent, mConnection, Service.BIND_AUTO_CREATE);
                txt.setText("Servicio Iniciado");
                iniciado = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void enviar(View v){
        String mensaje = msg.getText().toString();
        if (mensaje != "") {
            Bundle mBundle = new Bundle();
            mBundle.putString("mensaje", mensaje);
            Message mMessage = Message.obtain();
            mMessage.setData(mBundle);
            try {
                sMessenger.send(mMessage);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void refresh(View v){
        NetworkInfo info = MainActivity.getNetInfo(this);
        if(info != null && info.isConnected()){
            Log.i(tag, "TIPO: " + info.getTypeName() + " " + info.getType() + " SUBTIPO: " + info.getSubtypeName() + " EXTRA INFO: " + info.getExtraInfo() + " REASON: " + info.getReason());
        }else{
            Log.i(tag,"Su dispositivo no esta conectado a ninguna red.");
        }
    }

    private static NetworkInfo getNetInfo(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mConnection != null) unbindService(mConnection);
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){sMessenger = new Messenger(service);}
        @Override
        public void onServiceDisconnected(ComponentName name){sMessenger = null;}
    };

    class mHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
}
