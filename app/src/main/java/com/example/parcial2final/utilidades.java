package com.example.parcial2final;

import java.util.Base64;

public class utilidades {
    static String urlConsulta = "http://192.168.84.223:5984/angel/_design/briseily/_view/briseily";
    static String urlMto = "http://192.168.84.223:5984/angel/";
    static String user = "admin";
    static String passwd = "admin";
    static String credencialesCodificadas = Base64.getEncoder().encodeToString((user +":"+ passwd).getBytes());
    public String generarIdUnico(){
        return java.util.UUID.randomUUID().toString();
    }
}
