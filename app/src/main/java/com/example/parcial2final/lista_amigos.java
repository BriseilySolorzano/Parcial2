package com.example.parcial2final;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import kotlin.contracts.Returns;

public class lista_amigos extends AppCompatActivity {
    Bundle paramatros = new Bundle();
    DB db;
    ListView lts;
    Cursor cAmigos;
    final ArrayList<amigos> alAmigos = new ArrayList<amigos>();
    final ArrayList<amigos> alAmigosCopy = new ArrayList<amigos>();
    amigos datosAmigos;
    FloatingActionButton btn;
    JSONArray datosJSON; //para los datos que vienen del servidor.
    JSONObject jsonObject;
    obtenerDatosServidor datosServidor;
    detectarInternet di;
    int posicion = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lista_amigos);
        db = new DB(getApplicationContext(),"", null, 1);
        btn = findViewById(R.id.fabAgregarAmigos);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                paramatros.putString("accion","nuevo");
                abrirActividad(paramatros);
            }
        });
        di = new detectarInternet(getApplicationContext());
        if( di.hayConexionInternet() ){
            obtenerDatosAmigosServidor();
        }else{
            mostrarMsg("No hay conexion, datos en local");
            obtenerAmigos();//offline
        }
        buscarAmigos();
    }
    private void obtenerDatosAmigosServidor(){
        try{
            datosServidor = new obtenerDatosServidor();
            String data = datosServidor.execute().get();

            jsonObject =new JSONObject(data);
            datosJSON = jsonObject.getJSONArray("rows");
            mostrarDatosAmigos();
        }catch (Exception e){
            mostrarMsg("Error al obtener datos productos del server: "+ e.getMessage());
        }
    }
    private void mostrarDatosAmigos(){
        try{
            if( datosJSON.length()>0 ){
                lts = findViewById(R.id.ltsAmigos);
                alAmigos.clear();
                alAmigosCopy.clear();

                JSONObject misDatosJSONObject;
                for (int i=0; i<datosJSON.length(); i++){
                    misDatosJSONObject = datosJSON.getJSONObject(i).getJSONObject("value");
                    datosAmigos = new amigos(
                            misDatosJSONObject.getString("_id"),
                            misDatosJSONObject.getString("_rev"),
                            misDatosJSONObject.getString("idAmigo"),
                            misDatosJSONObject.getString("nombre"),
                            misDatosJSONObject.getString("descripcion"),
                            misDatosJSONObject.getString("marca"),
                            misDatosJSONObject.getString("presentacion"),
                            misDatosJSONObject.getString("precio"),
                            misDatosJSONObject.getString("urlCompletaFoto"),
                            misDatosJSONObject.getString("stock"),
                            misDatosJSONObject.getString("costo")
                    );
                    alAmigos.add(datosAmigos);
                }
                alAmigosCopy.addAll(alAmigos);

                adaptadorImagenes adImagenes = new adaptadorImagenes(getApplicationContext(), alAmigos);
                lts.setAdapter(adImagenes);

                registerForContextMenu(lts);
            }else{
                mostrarMsg("No hay datos que mostrar");
            }
        }catch (Exception e){
            mostrarMsg("Error al mostrar datos: "+ e.getMessage());
        }
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mimenu, menu);
        try {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            posicion = info.position;
            menu.setHeaderTitle("Que deseas hacer con " + datosJSON.getJSONObject(posicion).getJSONObject("value").getString("nombre"));
        }catch (Exception e){
            mostrarMsg("Error al mostrar el menu: "+ e.getMessage());
        }
    }
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        try{
            int itemId = item.getItemId();
            if (itemId == R.id.mnxAgregar) {
                paramatros.putString("accion", "nuevo");
                abrirActividad(paramatros);
            } else if (itemId == R.id.mnxModificar) {
                paramatros.putString("accion", "modificar");
                paramatros.putString("amigos", datosJSON.getJSONObject(posicion).toString());
                abrirActividad(paramatros);
            } else if (itemId == R.id.mnxEliminar) {
                eliminarAmigo();
            }
            return true;
        }catch (Exception e){
            mostrarMsg("Error al seleccionar el item: "+ e.getMessage());
            return super.onContextItemSelected(item);
        }
    }
    private void eliminarAmigo(){
        try {
            AlertDialog.Builder confirmar = new AlertDialog.Builder(lista_amigos.this);
            confirmar.setTitle("Esta seguro de eliinar a: ");
            confirmar.setMessage(datosJSON.getJSONObject(posicion).getJSONObject("value").getString("nombre"));
            confirmar.setPositiveButton("SI", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        String respuesta = db.administrar_amigos("eliminar", new String[]{"", "", datosJSON.getJSONObject(posicion).getJSONObject("value").getString("idAmigo")});
                        if (respuesta.equals("ok")) {
                            mostrarMsg("Producto eliminado con exito");
                            obtenerAmigos();
                        } else {
                            mostrarMsg("Error al eliminar el producto: " + respuesta);
                        }
                    }catch (Exception e){
                        mostrarMsg("Error al eliminar datos: "+ e.getMessage());
                    }
                }
            });
            confirmar.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            confirmar.create().show();
        }catch (Exception e){
            mostrarMsg("Error al eliminar: "+ e.getMessage());
        }
    }
    private void buscarAmigos(){
        TextView tempVal = findViewById(R.id.txtBuscarAmigos);
        tempVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try{
                    alAmigos.clear();
                    String valor = tempVal.getText().toString().trim().toLowerCase();
                    if( valor.length()<=0 ){
                        alAmigos.addAll(alAmigosCopy);
                    }else{
                        for (amigos amigo : alAmigosCopy){
                            String nombre = amigo.getNombre();
                            String descripcion = amigo.getDescripcion();
                            String tel = amigo.getMarca();
                            String presentacion = amigo.getPresentacion();
                            if( nombre.trim().toLowerCase().contains(valor) ||
                                    descripcion.trim().toLowerCase().contains(valor) ||
                                    tel.trim().contains(valor) ||
                                    presentacion.trim().toLowerCase().contains(valor)){
                                alAmigos.add(amigo);
                            }
                        }
                        adaptadorImagenes adImagenes=new adaptadorImagenes(getApplicationContext(), alAmigos);
                        lts.setAdapter(adImagenes);
                    }
                }catch (Exception e){
                    mostrarMsg("Error al buscar: "+ e.getMessage());
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }
    private void abrirActividad(Bundle parametros){
        Intent abrirActividad = new Intent(getApplicationContext(), MainActivity.class);
        abrirActividad.putExtras(parametros);
        startActivity(abrirActividad);
    }
    private void obtenerAmigos(){ //offline
        try{
            cAmigos = db.obtener_amigos();
            if( cAmigos.moveToFirst() ){
                datosJSON = new JSONArray();
                do{
                    jsonObject = new JSONObject();
                    JSONObject jsonObjectValue = new JSONObject();

                    jsonObject.put("_id", cAmigos.getString(0));
                    jsonObject.put("_rev", cAmigos.getString(1));
                    jsonObject.put("idAmigo", cAmigos.getString(2));
                    jsonObject.put("nombre", cAmigos.getString(3));
                    jsonObject.put("descripcion", cAmigos.getString(4));
                    jsonObject.put("marca", cAmigos.getString(5));
                    jsonObject.put("presentacion", cAmigos.getString(6));
                    jsonObject.put("precio", cAmigos.getString(7));
                    jsonObject.put("urlCompletaFoto", cAmigos.getString(8));
                    jsonObject.put("stock", cAmigos.getString(9));
                    jsonObject.put("costo", cAmigos.getString(10));
                    jsonObjectValue.put("value", jsonObject);

                    datosJSON.put(jsonObjectValue);
                }while (cAmigos.moveToNext());
                mostrarMsg("Punto");
                mostrarDatosAmigos();
            }else {
                paramatros.putString("accion", "nuevo");
                abrirActividad(paramatros);
                mostrarMsg("No hay Datos de amigos.");
            }
        }catch (Exception e){
            mostrarMsg("Error al obtener los productos : "+ e.getMessage());
        }
    }
    private void mostrarMsg(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}