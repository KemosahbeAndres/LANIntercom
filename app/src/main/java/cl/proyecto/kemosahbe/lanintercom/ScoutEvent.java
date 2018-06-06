package cl.proyecto.kemosahbe.lanintercom;

import java.net.InetAddress;

public interface ScoutEvent extends java.util.EventListener {
    public void onRecieve(byte[] data, InetAddress addr, int port);
}
