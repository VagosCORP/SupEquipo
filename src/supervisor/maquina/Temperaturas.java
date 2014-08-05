/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package supervisor.maquina;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author Francisco
 */
public class Temperaturas extends Service<Double[]>  {
    
    int nMuestras=100;
    Double[] t1;
    Double[] t2;
    Double[] t3;
    Double[] t4;
    Double[] t5;
    Double[] t6;
    Double[] t7;
    Double[] t8;
    
    
    public void iniciarMuestreo(int n){
        nMuestras=n;
    }
    private Double promediar(Double[] a){
        Double sum=0.000;
        int n=0;
        for (Double val : a) {
            sum+=val;
            n++;
        }
        return (double)sum/n;
    }
    
    
    
    @Override
    protected Task<Double[]> createTask() {
        return new Task<Double[]>(){

            @Override
            protected Double[] call() throws Exception {
                Double[] res=new Double[4];
                int nmax=nMuestras;
                
                System.out.println("Entre a la task!!");
                
                t1=new Double[nMuestras];
                t2=new Double[nMuestras];
                t3=new Double[nMuestras];
                t4=new Double[nMuestras];
                t5=new Double[nMuestras];
                t6=new Double[nMuestras];
                t7=new Double[nMuestras];
                t8=new Double[nMuestras];
                
                System.out.println("cree arrelgos de temperaturas con largo"+nMuestras);
                System.out.println("Creando socket");
                Socket cliente =new Socket();
                DataInputStream dis;
                DataOutputStream dos;
                byte datos[];
                String Res="";
                SocketAddress dir;
                try {
                    dir = new InetSocketAddress("20.0.0.6",2000);
                    int tmout=5000;  //tiene que ser modificable!! ojo!!
                    cliente.connect(dir, tmout);

                    System.out.println("Exito al vincular!!\n");
                    dis = new DataInputStream(cliente.getInputStream());
                    dos = new DataOutputStream(cliente.getOutputStream());
                    dos.writeBytes("A");
                    dos.writeByte(nmax);
                    datos = new byte[1024];
                    System.out.println("Adquiriendo");
                    
                    int to=0;
                    while(!Res.endsWith("$")){
                        Thread.sleep(1);
                        to++;
                        if(to>20000){
                            this.cancel();
                        }
                        if (dis.available()>0){
                            dis.read(datos);
                            for (byte valor : datos) {
                                if(valor!=0){
                                    Res+=(char)valor;
                                }
                            }
                        }
                        datos = new byte[1024];
                    }
                    System.out.println("Adquisicion Finalizada");
                    String categorias[];
                    String sensores[];
                    String valores[];
                    String prueba;
                    //int muestras;
                    categorias=Res.split("#");
                    prueba= categorias[1].substring(categorias[1].length()-2,categorias[1].length());
                    //txtAReporte.append("Coso :..."+prueba+"...\n");
                    categorias[1]= categorias[1].substring(0,categorias[1].length()-2);
                    sensores=categorias[1].split("/");
                    System.out.println("Seccion saludo: "+categorias[0]+"\n");
                    System.out.println("Seccion sensores: "+categorias[1]+"\n");
                    System.out.println("Seccion despedida: "+categorias[2]+"\n");
                    //muestras=sensores.length;
                    
                    int ccc=0;
                    
                    for (String muestra : sensores) {
                        valores=muestra.split("&");
                        switch(Integer.parseInt(valores[1])){
                            case 1:{
                                System.out.println("Sensor1 : "+valores[2]+"\n");
                                t1[ccc]=Double.parseDouble(valores[2]);
                                break;
                            }
                            case 2:{
                                System.out.println("Sensor2 : "+valores[2]+"\n");
                                t2[ccc]=Double.parseDouble(valores[2]);
                                break;
                            }
                            case 3:{
                                System.out.println("Sensor3 : "+valores[2]+"\n");
                                t3[ccc]=Double.parseDouble(valores[2]);
                                break;
                            }
                            case 4:{
                                System.out.println("Sensor4 : "+valores[2]+"\n");
                                t4[ccc]=Double.parseDouble(valores[2]);
                                break;
                            }
                            case 5:{
                                System.out.println("Sensor5 : "+valores[2]+"\n");
                                t5[ccc]=Double.parseDouble(valores[2]);
                                break;
                            }
                            case 6:{
                                System.out.println("Sensor6 : "+valores[2]+"\n");
                                t6[ccc]=Double.parseDouble(valores[2]);
                                break;
                            }
                            case 7:{
                                System.out.println("Sensor7 : "+valores[2]+"\n");
                                t7[ccc]=Double.parseDouble(valores[2]);
                                break;
                            }
                            case 8:{
                                System.out.println("Sensor8 : "+valores[2]+"\n");
                                t8[ccc]=Double.parseDouble(valores[2]);
                                System.out.println("Contador : "+ccc);
                                ccc++; 
                                break;
                            }
                        }
                        
                       
                    }
                        Res="";
                        dis.close();
                        dos.close();
                        cliente.close();

                        System.out.println("S1 : "+Arrays.toString(t1));
                        System.out.println("S2 : "+Arrays.toString(t2));
                        System.out.println("S3 : "+Arrays.toString(t3));
                        System.out.println("S4 : "+Arrays.toString(t4));
                        System.out.println("S5 : "+Arrays.toString(t5));
                        System.out.println("S6 : "+Arrays.toString(t6));
                        System.out.println("S7 : "+Arrays.toString(t7));
                        System.out.println("S8 : "+Arrays.toString(t8));
                        
                        
                        System.out.println("S1 : "+promediar(t1).toString()+"\n");
                        System.out.println("S2 : "+promediar(t2).toString()+"\n");
                        System.out.println("S3 : "+promediar(t3).toString()+"\n");
                        System.out.println("S4 : "+promediar(t4).toString()+"\n");
                        System.out.println("S5 : "+promediar(t5).toString()+"\n");
                        System.out.println("S6 : "+promediar(t6).toString()+"\n");
                        System.out.println("S7 : "+promediar(t7).toString()+"\n");
                        System.out.println("S8 : "+promediar(t8).toString()+"\n");

                        res =new Double[]{promediar(t1),promediar(t2),promediar(t3),promediar(t4),promediar(t5),promediar(t6),promediar(t7),promediar(t8)};
                    } catch (UnknownHostException ex) {
                        this.cancel();
                        System.out.println( "Problema de coneccion");

                    } catch (IOException ex) {
                        this.cancel();
                        System.out.println( "Problema de coneccion");

                    }
                
                
                
                return res;
            }
          
        };
    }
    
}
