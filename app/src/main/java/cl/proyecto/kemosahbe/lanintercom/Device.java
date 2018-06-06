package cl.proyecto.kemosahbe.lanintercom;

import java.util.UUID;

public class Device {
    private UUID uuid;
    private String type;
    private String name;
    private String address;
    private int port;
    private Boolean modeRoom;
    private byte[] buffer;

    public byte[] getData(){
        return buffer;
    }
    public int getLength(){
        return 0;
    }

}
