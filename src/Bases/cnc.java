/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Bases;

/**
 *
 * @author Francisco
 */
public class cnc {
   public native void init();
   public native int modbus_connect(String IP,short port);
   public native int modbus_close();
   /**
    * 
    * @return 0 si el equipo esta libre, 1 si esta ocupado y 2 en caso de error de comando
    */
   public native int is_working();
   public native int mov_parameter(short[] axis,int[] init_speed,int[] speed,int[] acc,int[] pmm,short num);
   public native int mov_parameter(short axis,int init_speed,int speed,int acc,int pmm);
   public native int output_on(short output);
   public native int output_off(short output);
   public native int output_read(short output);
   public native int input_read(short input);
   
   
   public native int cont_mov(short axis,short dir);
   public native int cont_mov(short[] axis,short[] dir,short num);
   public native int line(short axis1,int pulse1,short axis2, int pulse2);
   public native int line(short axis1,int pulse1,short axis2, int pulse2,short axis3, int pulse3);
   public native int line(int pulse1, int pulse2, int pulse3,int pulse4);
   public native int line(int pulse1, int pulse2, int pulse3,int pulse4,int pulse5);
   public native int line(int pulse1, int pulse2, int pulse3,int pulse4,int pulse5,int pulse6);
   public native int fifo(short axis1,int pulse1,int speed);
   public native int fifo(short axis1,int pulse1,short axis2,int pulse2,int speed);
   public native int fifo(short axis1,int pulse1,short axis2,int pulse2,short axis3,int pulse3,int speed);
   public native int fifo(int pulse1,int pulse2,int pulse3,int pulse4,int speed);
   public native int fifo(int pulse1,int pulse2,int pulse3,int pulse4,int pulse5,int speed);
   public native int fifo(int pulse1,int pulse2,int pulse3,int pulse4,int pulse5,int pulse6,int speed);
   
   public native int move(short axis,int pulse);
   public native int move(short[] axis,int[] pulse,short num);
   
   public native int stop();
   public native int run();
   public native int pause();
   public native int resume();
   
   
   public native int set_pos(short axis,int pulses);
   public native int set_pos(short[] axis,int[] pulses,short num);
   public native int set_actual_pos(short axis,int pulses);// ver diferencias frente a set pos
   
   
   public native int set_actual_pos(short[] axis,int[] pulses,short num);
   //Actualiza la informacion de posicion y velocidad obtenida del equipo
   /**
    * 
    * @return     arreglo de enteros donde  devuelve 0 en caso de exito y 1 en caso de error 
    */
   public native int get_axis_pos();
   public native int get_pos1();
   public native int get_pos2();
   public native int get_pos3();
   public native int get_pos4();
   public native int get_pos5();
   public native int get_pos6();
   
   public native int get_v1();
   public native int get_v2();
   public native int get_v3();
   public native int get_v4();
   public native int get_v5();
   public native int get_v6();
   
   public native int get_c_pos1();
   public native int get_c_pos2();
   public native int get_c_pos3();
   public native int get_c_pos4();
   public native int get_c_pos5();
   public native int get_c_pos6();
   
   public native int[] get_com_pos(short param);//a implementar si se requiere
   
}
