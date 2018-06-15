package cl.proyecto.kemosahbe.lanintercom;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {
    public final String userConfigFile = "Config/user.properties";
    public final String roomsConfigFile = "Config/rooms.properties";
    private FileInputStream fis = null;
    private FileOutputStream fos = null;
    private Properties properties;
    Boolean iniciado = false;
    final String tag = "MAIN";
    private Messenger messenger, sMessenger;
    WifiManager wm;
    WifiManager.MulticastLock ml;

    TextView mediatxt, handtxt;
    //Button mediabtn, handbtn;
    Boolean mediaon = false, handon = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.barmenu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initProperties();

        //mediabtn = (Button) findViewById(R.id.mediabtn);
        mediatxt = (TextView) findViewById(R.id.mediatxt);
        //handbtn = (Button) findViewById(R.id.handbtn);
        handtxt = (TextView) findViewById(R.id.handtxt);


        Log.i(tag, "Servicio Creado: "+Thread.currentThread().getName()+" ("+Thread.currentThread().getId()+")");


        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},0);
            Log.i(tag,"Permisos para grabar garantizados");
        }else{
            Log.i(tag,"No hay permisos");
        }

        //UUID uuid = UUID.fromString("AndresCubillos");
        //Log.i(tag, ""+uuid.toString());
        //txt = (TextView) findViewById(R.id.texto);
        //msg = (EditText) findViewById(R.id.msg);
        //txt.setText("Servicio No iniciado");
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case 0:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.i(tag, "Permisos Garantizados");
                }else{
                    Log.i(tag, "Permisos Garantizados");
                }
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        messenger = new Messenger(new mHandler());
    }

    private void loadUserProperties(){
        //Intentar leer archivo de configuracion.
        try {
            fis = new FileInputStream(userConfigFile);
        }catch(FileNotFoundException e){
            Log.i(tag, "Archivo de configuracion no encontrado.\n\rCreando archivo de configuracion.");
            try {
                fos = openFileOutput(userConfigFile, Context.MODE_PRIVATE);
                fos.close();
            }catch(FileNotFoundException e2){
                e2.printStackTrace();
            }catch(IOException ioe){
                ioe.printStackTrace();
            }
        }finally {
            //codigo para tratar el archivo de configuracion.
        }
    }

    private Boolean loadRoomsProperties(){
        try {
            fis = new FileInputStream(roomsConfigFile);
        } catch (FileNotFoundException e){return false;}

        return true;
    }

    public String getLocalAddress(){
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

    public void media(View view) {
        Intent intent = new Intent(this, MultimediaService.class);
        Bundle mBundle = new Bundle();
        mBundle.putBoolean(MultimediaService.MIC_STATE, true);
        mBundle.putBoolean(MultimediaService.SOUND_STATE, true);
        mBundle.putString(MultimediaService.SRV_ADDRESS, "230.255.255.100");
        mBundle.putInt(MultimediaService.SRV_PORT, 1800);
        intent.putExtras(mBundle);

        if(mediaon){
            try{
                stopService(intent);
                mediatxt.setText("Servicio Apagado");
                mediaon = false;
            }catch(Exception e){
                e.printStackTrace();
            }
        }else{
            startService(intent);
            mediatxt.setText("Servicio Iniciado");
            mediaon = true;
        }
    }

    public void hand(View view){
        iniciar(view);
    }

    public void iniciar(View v){
        Intent intent = new Intent(this, HandshakeService.class);
        if(iniciado){
            try {
                unbindService(mConnection);
                handtxt.setText("Servicio Apagado");
                iniciado = false;
            }catch(Exception e){
                e.printStackTrace();
            }
        }else {
            try {
                intent.putExtra("messenger", messenger);
                intent.putExtra("serverAddress","230.255.255.200");
                intent.putExtra("serverPort", 1800);
                bindService(intent, mConnection, Service.BIND_AUTO_CREATE);
                handtxt.setText("Servicio Iniciado");
                iniciado = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void stopThread(View v){
        Message msge = Message.obtain();
        try {
            sMessenger.send(msge);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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
                //NetworkInterface Interf = NetworkInterface.getByName("wlan0");
                MulticastSocket socket  = new MulticastSocket();
                socket.setReuseAddress(true);
                Log.i(tag,"Multicast Sender running at: "+socket.getLocalSocketAddress());
                //socket.setNetworkInterface(Interf);
                InetAddress addr = InetAddress.getByName("230.255.255.200");
                socket.joinGroup(addr);

                //byte[] data = buffer.getBytes();
                byte[] data = ("UUID:1a561d-65ds1ds5-f1sd51f\n\r" +
                        "TYPE:node.user\n\r" +
                        "NAME:Andres\n\r" +
                        "ADDRESS:162.168.0.16").getBytes();
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
