package cl.proyecto.kemosahbe.lanintercom;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
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
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    TextView txt;
    EditText msg;
    Boolean iniciado = false;
    final String tag = "MAIN";
    private Messenger messenger, sMessenger;
    WifiManager wm;
    WifiManager.MulticastLock ml;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(tag, "Servicio Creado: "+Thread.currentThread().getName()+" ("+Thread.currentThread().getId()+")");
        txt = (TextView) findViewById(R.id.texto);
        msg = (EditText) findViewById(R.id.msg);
        txt.setText("Servicio No iniciado");
        //Comprobar que el dispositivo este conectado a una red WIFI
        //Admitir Multicast.
        wm = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ml = wm.createMulticastLock("MultiCast");
        ml.acquire();
        WifiInfo wi = wm.getConnectionInfo();
        if(wi != null){
            Log.i(tag, "get BSSID: "+wi.getBSSID()+" MAC: "+wi.getMacAddress()+" IPV4: "+wi.getIpAddress());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //messenger = new Messenger(new mHandler());
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
        Log.i(tag,"Mensaje: "+mensaje);
        mAsyncTask mTask = new mAsyncTask(mensaje);
        mTask.execute();
    }

    class mAsyncTask extends AsyncTask<Void,Boolean,Integer>{
        private MulticastSocket socket;
        private DatagramChannel channel;
        private String buffer;
        private InetSocketAddress serverAddress = new InetSocketAddress("239.255.255.249",1800);
        private DatagramPacket packet;
        //vbkhb
        NetworkInterface mInterface;
        Enumeration<NetworkInterface> mInterfaces;
        Enumeration<InetAddress> mAddress;
        int ifaceCount;

        public mAsyncTask(String msg) {
            this.buffer = msg;
        }

        public void println(String msg){
            Log.i(tag, msg);
        }

        public void initInfo(){
            try{
                mInterfaces = NetworkInterface.getNetworkInterfaces();
                ifaceCount = Collections.list(mInterfaces).size();
                Log.i(tag,"N° Interfaces: "+ifaceCount+"\r\n");
            }catch(Exception e){
                e.printStackTrace();
            }

            for(int i=0;i<=ifaceCount-1;i++) {
                showInfo(i);
            }
        }
        public void showInfo(int index) {
            try {
                mInterfaces = NetworkInterface.getNetworkInterfaces();
                mInterface = Collections.list(mInterfaces).get(index);

                println("N°  : " + index + 1);
                println("Name: " + mInterface.getDisplayName());
                println("ID  : " + mInterface.getName());
                println("MAC : " + Arrays.toString(mInterface.getHardwareAddress()));

                mAddress = mInterface.getInetAddresses();
                for (InetAddress i : Collections.list(mAddress)) {
                    println("IP  : " + i.getHostAddress());
                    println("HOST: " + i.getHostName());
                }

                String status = "DOWN!";
                if (mInterface.isUp()) {
                    status = "UP!";
                }
                println("STATUS: " + status);

                String point = "NO!";
                if (mInterface.isPointToPoint()) {
                    point = "YES!";
                }
                println("POINT TO POINT: " + point);

                String multicast = "NO!";
                if (mInterface.supportsMulticast()) {
                    multicast = "YES!";
                }
                println("MULTICAST: " + multicast);

                String loopback = "NO!";
                if (mInterface.isLoopback()) {
                    loopback = "YES!";
                }
                println("LOOPBACK: " + loopback);

                String virtual = "NO!";
                if (mInterface.isVirtual()) {
                    virtual = "YES!";
                }
                println("VIRTUAL: " + virtual);

                //System.in.read();
                println("");

            } catch (Exception e) {
                System.out.println(e);
            }
        }
        //@Override
        //protected void onPreExecute() {
           // initInfo();
        //}

        @Override
        protected void onProgressUpdate(Boolean... values) {
            //Toast.makeText(MainActivity.this, "Mensaje enviado.", Toast.LENGTH_SHORT).show();
            super.onProgressUpdate(values);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            //initInfo();
            //InetAddress groupAddr = null;

            try {
                NetworkInterface Interf = NetworkInterface.getByName("wlan0");
                MulticastSocket socket  = new MulticastSocket();
                socket.setReuseAddress(true);
                Log.i(tag,"Multicast Sender running at: "+socket.getLocalSocketAddress());
                socket.setNetworkInterface(Interf);
                InetAddress addr = InetAddress.getByName("239.255.255.249");
                socket.joinGroup(addr);

                byte[] data = buffer.getBytes();
                DatagramPacket dp = new DatagramPacket(data,data.length, addr, 1800);

                socket.send(dp);

                socket.leaveGroup(addr);
                socket.close();

                //Enumeration<InetAddress> mAddress = Interf.getInetAddresses();
                //for(InetAddress i : Collections.list(mAddress)){
                    //println("IP  : "+i.getHostAddress());
                    //println("HOST: "+i.getHostName());
                    //try {
                        //if (InetAddress.getByName(i.getHostAddress()) instanceof Inet4Address) {
                            //groupAddr = InetAddress.getByName(i.getHostAddress());
                            //break;
                        //}
                    //}catch(Exception e){
                        //e.printStackTrace();
                    //}
                //}
            } catch (Exception e) {
                e.printStackTrace();
            }

            //openChannel(serverAddress, groupAddr);

            return 0;
        }
        private void openSocket(){
            try {
                socket = new MulticastSocket(1800);
                socket.setReuseAddress(true);
                packet = new DatagramPacket(buffer.getBytes(), buffer.length(),InetAddress.getByName("192.168.0.16"),1800);
                socket.joinGroup(serverAddress.getAddress());
                socket.send(packet);
                Log.i(tag,"Paquete enviado: "+buffer+ " "+packet.getAddress()+ " "+packet.getPort());
                //this.onProgressUpdate();
                socket.leaveGroup(serverAddress.getAddress());
                socket.close();
                socket = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        private void openChannel(InetSocketAddress address, InetAddress group){
            openChannel(address.getAddress(),group,address.getPort());
        }
        private void openChannel(InetAddress address, InetAddress group, int port){
            try {
                channel = DatagramChannel.open();
                channel.socket().setReuseAddress(true);
                channel.socket().setBroadcast(true);
                //channel.setOption(StandardSocketOptions.IP_MULTICAST_TTL, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void enviarObsolete(View v){
        String mensaje = msg.getText().toString();
        if (mensaje != "") {
            Bundle mBundle = new Bundle();
            mBundle.putString("mensaje", mensaje);
            Log.i(tag,"Mensaje: "+mensaje);
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
        ml.release();
        ml = null;
        //if(mConnection != null) unbindService(mConnection);
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
