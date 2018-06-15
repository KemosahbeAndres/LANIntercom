package cl.proyecto.kemosahbe.lanintercom;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MultimediaService extends Service {

    //Constantes
    public static final String MIC_STATE = "srv_mic";
    public static final String SOUND_STATE = "srv_sound";
    public static final String SRV_ADDRESS = "srv_address";
    public static final String SRV_PORT = "srv_port";
    private final String tag = "MEDIA";

    //Variables Privadas
    private static Boolean MIC = false;
    private static Boolean SOUND = false;
    private InetAddress ADDR = null;
    private int PORT = -1;
    private MulticastSocket rSOCKET;
    private DatagramSocket sSOCKET;
    private AudioTrack TRACK;
    private AudioRecord RECORD;
    private static byte[] BUFFER;
    private Boolean status = false;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mRate = 44100;

    private void configurarServicio(Intent intent){
        int bufferSize;
        try {
            Bundle mBundle = intent.getExtras();
            MIC = (Boolean) mBundle.get(MIC_STATE);
            SOUND = (Boolean) mBundle.get(SOUND_STATE);
            ADDR = InetAddress.getByName((String) mBundle.get(SRV_ADDRESS));
            PORT = (int) mBundle.get(SRV_PORT);
            if(ADDR != null && PORT != -1) {
                rSOCKET = new MulticastSocket(PORT);
                rSOCKET.joinGroup(ADDR);
            }
            //Obteniendo tamaño minimo del buffer.
            bufferSize = AudioRecord.getMinBufferSize(mRate, AudioFormat.CHANNEL_IN_MONO,mAudioFormat);
            //bufferSize *= 2;
            Log.i(tag,"Tamanño minimo del buffer: "+bufferSize);
            BUFFER = new byte[bufferSize];

            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                Log.i(tag,"Tengo los Permisos");
            }

            //Inicializando objeto record.
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                RECORD = new AudioRecord.Builder()
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(mAudioFormat)
                                .setSampleRate(44100)
                                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                                .build())
                        .setAudioSource(MediaRecorder.AudioSource.MIC)
                        .setBufferSizeInBytes(bufferSize)
                        .build();
            }else {
                RECORD = new AudioRecord(MediaRecorder.AudioSource.MIC, mRate, AudioFormat.CHANNEL_IN_MONO, mAudioFormat, bufferSize);
            }

            //Inicializando DatagramSocket
            sSOCKET = new DatagramSocket();


            int min = AudioTrack.getMinBufferSize(mRate,AudioFormat.CHANNEL_OUT_MONO,mAudioFormat);
            if(min == AudioTrack.ERROR_BAD_VALUE){
                Log.i(tag,"Error en el buffer");
            }
            //min *= 2;
            //Inicializando AudioTrack
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.i(tag,"Inicializando");
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                AudioFormat format = new AudioFormat.Builder()
                        .setSampleRate(mRate)
                        .setEncoding(mAudioFormat)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build();
                TRACK = new AudioTrack(attributes,format,min,AudioTrack.MODE_STREAM,0);

            }else {
                TRACK = new AudioTrack(AudioManager.STREAM_MUSIC, mRate,AudioFormat.CHANNEL_OUT_MONO, mAudioFormat, min, AudioTrack.MODE_STREAM);
            }

            if(TRACK.getState() == AudioTrack.STATE_UNINITIALIZED){
                TRACK = new AudioTrack(AudioManager.STREAM_MUSIC, mRate,AudioFormat.CHANNEL_OUT_MONO, mAudioFormat, min, AudioTrack.MODE_STREAM);
                Log.i(tag,"Otra vez");
            }else{
                Log.i(tag,"AudioTrack Inicializado.");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
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

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        configurarServicio(intent);
        status = true;
        MediaTask.execute();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        status = false;
        MediaTask.cancel(true);
        MIC = false;
        SOUND = false;
        RECORD.stop();
        RECORD = null;
        //TRACK.stop();
        TRACK.release();
        TRACK.flush();
        TRACK = null;
        try {
            rSOCKET.leaveGroup(ADDR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        rSOCKET.close();
        rSOCKET = null;
        sSOCKET.close();
        sSOCKET = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private AsyncTask<Void,Void,Void> MediaTask = new AsyncTask<Void, Void, Void>() {

        void conectar(){
            int min = AudioTrack.getMinBufferSize(mRate, AudioFormat.CHANNEL_OUT_MONO, mAudioFormat);
            byte[] mBuffer = new byte[min];
            //if(min <= 0) {
                try {
                    int buff = RECORD.read(BUFFER, 0, BUFFER.length);
                    DatagramPacket data = new DatagramPacket(BUFFER, BUFFER.length, ADDR, PORT);
                    if (rSOCKET != null) rSOCKET.send(data);
                    //Log.i(tag, "Enviado LocalHost: " + getLocalAddress() + ":" + sSOCKET.getLocalPort());



                    DatagramPacket datain = new DatagramPacket(BUFFER, BUFFER.length);
                    if (rSOCKET != null) rSOCKET.receive(datain);
                    //Log.i(tag, "Recibido RemoteHost:" + datain.getAddress().getHostAddress() + ":" + datain.getPort());
                    if (BUFFER.length > 0  && (String) datain.getAddress().getHostAddress() != getLocalAddress()) {
                        TRACK.write(BUFFER, 0, buff);
                        //TRACK.play();
                        //Thread.sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            //}
        }

        @Override
        protected Void doInBackground(Void... voids) {
            RECORD.startRecording();

            //TRACK.play();
            //while(status){
            if (BUFFER.length > 0) {
                try {
                    conectar();
                    TRACK.play();
                    while(status){
                        conectar();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //}
            return null;
        }
    };


}
