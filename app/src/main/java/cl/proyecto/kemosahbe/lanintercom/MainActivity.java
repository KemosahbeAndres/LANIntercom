package cl.proyecto.kemosahbe.lanintercom;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, ServicioMensajeria.class);
        messenger = new Messenger(new mHandler());
        intent.putExtra("messenger", messenger);
        bindService(intent,mConnection, Service.BIND_AUTO_CREATE);
    }

    public void iniciar(View v){
        Intent intent = new Intent(this, ServerService.class);
        if(iniciado){
            try {
                stopService(intent);
                txt.setText("Servicio Detenido");
                iniciado = false;
            }catch(Exception e){
                e.printStackTrace();
            }
        }else {
            try {
                startService(intent);
                txt.setText("Servicio Iniciado");
                iniciado = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void enviar(View v){
        String mensaje = msg.getText().toString();
        Bundle mBundle = new Bundle();
        mBundle.putString("mensaje",mensaje);
        Message mMessage = Message.obtain();
        mMessage.setData(mBundle);
        try {
            sMessenger.send(mMessage);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
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
