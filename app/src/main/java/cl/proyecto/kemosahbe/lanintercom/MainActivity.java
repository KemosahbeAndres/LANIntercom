package cl.proyecto.kemosahbe.lanintercom;

import android.content.Intent;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {
    TextView txt;
    Boolean iniciado = false;
    final String tag = "MAIN";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(tag, "Servicio Creado: "+Thread.currentThread().getName()+" ("+Thread.currentThread().getId()+")");
        txt = (TextView) findViewById(R.id.texto);
        txt.setText("Servicio No iniciado");
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
}
