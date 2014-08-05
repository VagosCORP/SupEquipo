/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package supervisor.maquina;

import Bases.GestPos;
import Bases.cnc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.Bounds;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

/**
 *
 * @author Francisco
 */
public class CopyOfGruaCncAntiguo {
	// Eventos
	// Coneccion al dispositivo
	OnConnectionListener conListener;

	public interface OnConnectionListener {
		public void OnConnectionSucces();

		public void OnConnectionExistsErr();

		public void OnConnectionMaxLimitErr();

		public void OnConnectionSocketErr();

		public void OnConnectionTcpErr();
	}

	public void setOnConnectionListener(OnConnectionListener cmdLst) {
		conListener = cmdLst;
	}

	// Movimiento
	OnMovementListener movListener;

	public interface OnMovementListener {
		public void OnCommandFailed(String fuente);// fallo el enviar el comando
													// de movimiento

		public void OnMovementSucces(int mov);// movimiento finalizado donde mov
												// es el tipo de movimiento
												// finalizado

		public void OnPositionUpdate(Double x, Double y, Double z, Double r);
		public void OnDeviceIsBussy();// el equipo esta ocupado
		public void OnOutOfBounds(); // comando fuera de limites
	}

	public void setOnOnMovementListener(OnMovementListener mvLst) {
		movListener = mvLst;
	}

	// Variable de almacenamiento de tipo de movimiento
	private int mov = 99;
	// Constantes de tipo de movimiento
	public static final int FREEMOVE = 0;
	public static final int GOTO_R_TOOL = 1;
	public static final int GOTO_R_JAV = 2;
	public static final int MIX = 3;
	public static final int INSRT_TOOL = 4;
	public static final int RTRT_TOOL = 5;
	public static final int INSRT_JAV = 6;
	public static final int RTRT_JAV = 7;
	public static final int MOV_XY_TOOL = 8;
	public static final int MOV_XY_JAV = 9;

	// Clase de control del equipo
	// de momento la dll se inicializara en la clase principal luego se vera si
	// trasladar eso a esta clase

	cnc control = new cnc();
	// constante para codigos de error
	/*
	 * Codigos de error para la funcion de coneccion 0-->no error 1-->existe la
	 * coneccion 2-->maximo numero de conecciones alcanzado 3-->errror de socket
	 * 4-->fallo en la coneccion TCP
	 */
	public final int COM_NO_ERR = 0;
	public final int COM_ERR_CON_EXISTS = 1;
	public final int COM_ERR_MAX_CON = 2;
	public final int COM_ERR_SOCKET = 3;
	public final int COM_ERR_TCP = 4;

	/*
	 * Codigos de error para los demas parametros -1-->fallo al enviar
	 * 0-->correcto 1-->codigo de funcion incorrecto o no soportado
	 * 2-->direccion invalida o no doportada 3-->datos invalidos o no soportados
	 * 4-->fallo en la ejecuci贸n del movimiento 5-->movimiento en ejecuci贸n(la
	 * operacion puede esperar) 6-->el equipo esta ocupado no puede ejecutar la
	 * orden de momento 8-->error de comprobacion de archivo 10-->ruta a puerta
	 * de acceso invalida 11-->el dispoditivo objetivo no responde 224-->error
	 * de transmicion o marco de datos de modbus invalido 255-->equipo tarda
	 * mucho en responder(dispositivo no responde o no esta conectado a la red)
	 * 225-->Movimiento no definido 226-->fallo en la apertura del archivo
	 * 227-->Error de direccion de base
	 */
	public final int NO_ERR = 0;
	public final int MODBUS_SND_FAIL = -1;
	public final int MODBUS_INVALID_FUNC = 1;
	public final int MODBUS_INVALID_ADDR = 2;
	public final int MODBUS_INVALID_DATA = 3;
	public final int MODBUS_PERFORM_FAIL = 4;
	public final int MODBUS_DEVICE_ACK = 5;
	public final int MODBUS_DEVICE_BUSY = 6;
	public final int MODBUS_MEM_PARITY_ERR = 8;
	public final int MODBUS_INVALID_GATEWAY_PATH = 10;
	public final int MODBUS_DEVICE_NO_RESPOND = 11;
	public final int MODBUS_FRAME_ERR = 224;
	public final int MODBUS_TIMEOUT_ERR = 255;
	public final int MODBUS_PFM_UNDEFINED = 225;
	public final int MODBUS_OPEN_FILE_FAIL = 226;
	public final int MODBUS_BASEADDR_ERR = 227;
	// variables de comunicaci贸n
	private String ip;
	private Short port;
	Task<Integer> task;
	Thread th;
	// Variables de estado del equipo
	public BooleanProperty ocupado = new SimpleBooleanProperty(false);
	// //////////////////////////////////////////////////////
	public SimpleBooleanProperty conected = new SimpleBooleanProperty(false);
	// Temporizador de actualizacion de datos de posici贸n y estado
	Timeline actualizador = new Timeline(new KeyFrame(Duration.millis(500),
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					// Temporizador para control de posicion
					// implementar funcion de actualizacion de datos
					actualizar();

					if (!ocupado.get()) {
						actualizador.stop();
					}
				}
			}));
	// Gestion de posicionamiento
	public GestPos gestpos;
	// Posicion de la punta del agitador
	public Double posX; // centimetros
	public Double posY; // centimetros
	public Double posZ; // centimetros
	public Double posR; // grados de inclinacion del agitador
	public Double nR_act; // numero de reactor actual
	public Double nR_des; // numero de reactor actual
	public Double sec; // sector del vagon actual
	// Posici髇 de la punta del agitador como propiedades para muestra en
	// interfaz de usuario
	public StringProperty show_posX = new SimpleStringProperty("Desconocido");
	public StringProperty show_posY = new SimpleStringProperty("Desconocido");
	public StringProperty show_posZ = new SimpleStringProperty("Desconocido");
	public StringProperty show_posR = new SimpleStringProperty("Desconocido");
	// Velocidades de los diferentes ejes
	public Double velX; // m/s
	public Double velY; // m/s
	public Double velZ; // m/s
	public Double velR; // grados/s
	// Posicion de la velocidad del agitador como propiedades para muestra en
	// interfaz de usuario
	public StringProperty show_velX = new SimpleStringProperty("Desconocido");
	public StringProperty show_velY = new SimpleStringProperty("Desconocido");
	public StringProperty show_velZ = new SimpleStringProperty("Desconocido");
	public StringProperty show_velR = new SimpleStringProperty("Desconocido");
	// Posicion de los diferentes elementos del equipo en metros o grados seg煤n
	// aplique
	public Double posPortico;
	public Double posCarro;
	public Double posElevador;
	public Double posRotador;
	// Posicion de los diferentes ejes del equipo como propiedades para muestra
	// de datos
	public StringProperty show_posPortico = new SimpleStringProperty(
			"Desconocido");
	public StringProperty show_posCarro = new SimpleStringProperty(
			"Desconocido");
	public StringProperty show_posElevador = new SimpleStringProperty(
			"Desconocido");
	public StringProperty show_posRotador = new SimpleStringProperty(
			"Desconocido");
	// Posicion del equipo en pulsos
	public Integer pX;
	public Integer pY;
	public Integer pZ;
	public Integer pR;

	public Integer pXentrada;
	public Integer PYentrada;

	public Integer pXvertical;
	// Ubicacion del equipo en el reactor actual
	private Integer rActual = 0;
	private Integer sActual = 0;

	public SimpleStringProperty show_rActual = new SimpleStringProperty(
			"Desconocido");
	public SimpleStringProperty show_sActual = new SimpleStringProperty(
			"Desconocido");

	public Double xRel;
	public Double yRel;

	public SimpleStringProperty show_xRel = new SimpleStringProperty(
			"Desconocido");
	public SimpleStringProperty show_yRel = new SimpleStringProperty(
			"Desconocido");

	// Constantes del equipo del equipo
	public final Double x0; // distancia entre el punto de origen y el eje de
							// inclinacion del cabezal en x
	public final Double y0; // distancia entre el punto de origen y el eje de
							// inclinacion del cabezal en y
	public final Double z0; // distancia entre el punto de origen y el eje de
							// inclinacion del cabezal en z
	public final Double la; // longitud del agitador
	public final Double ra; // radio agitador
	public final Double de; // distancia entre el eje de inclinacion de la
							// herramienta y el centro de la herramienta
	public final Double Rx; // Relacion revoluciones motor vs desplazamiento en
							// metros metros en x
	public final Double Ry; // Relacion revoluciones motor vs desplazamiento en
							// metros metros en y
	public final Double Rz; // Relacion revoluciones motor vs desplazamiento en
							// metros del elevador
	public final Double Rr; // Relacion revoluciones motor vs Grados actuador
	public final Double Rp; // Relacion Pulsos/Revoluci贸n
	public final Double Rv; // Relacion vmaquina
	public final Double xj; // distancia entre el punto de origen y la jabalina
							// en x
	public final Double yj; // distancia entre el punto de origen y la jabalina
							// en y

	// los siguientes valores son los maximos y minimos en las coordenadas del
	// controlador cnc
	public final Double zMax;
	public final Double zMin;
	public final Double xMax;
	public final Double xMin;
	public final Double yMax;
	public final Double yMin;

	// Constantes de posicion y dimenciones de cada vagon
	public final Double dr; // Distancia resgtringida al borde = es+ra
	public Double es; // espacio de seguridad
	public final Double x_v1;// distancia entre el primer reactor y el eje de
								// referencia en x
	public final Double y_v1;// distancia entre el primer reactor y el eje de
								// referencia en y
	public final Double ar; // ancho del reactor (interno)
	public final Double ai; // ancho del intercambiador de calor
	public final Double lr; // largo del reactor
	public final Double lv_r; // largo de la parte rectangular del vagon (antes
								// de llegar a la zona inclinada)
	public final Double derX;// distancia entre vagones en x
	public final Double derY;// distancia entre vagones en y
	// ///////////////////////////////////////////////////////////////////////////////
	//
	DecimalFormat df = new DecimalFormat("0.00");

	// Constructor, de momento solo se asegura de inicializar las constantes
	// propias del equipo relacionadas con su dimesionamiento
	public CopyOfGruaCncAntiguo(Double x_0, Double y_0, Double z_0, Double l_a, Double r_a,
			Double d_e, Double r_x, Double r_y, Double r_z, Double r_r,
			Double r_p, Double r_v, Double x_max, Double x_j, Double y_j,
			Double x_min, Double y_max, Double y_min, Double z_max,
			Double z_min, Double dr, Double x_v1, Double y_v1, Double ar,
			Double ai, Double lr, // largo del reactor
			Double lv_r, // largo de la parte rectangular del vagon (antes de
							// llegar a la zona inclinada)
			Double derX,// distancia entre vagones en x
			Double derY) {

		control.init();
		this.x0 = x_0;
		this.y0 = y_0;
		this.z0 = z_0;
		this.la = l_a;
		this.ra = r_a;
		this.de = d_e;
		this.Rx = r_x;
		this.Ry = r_y;
		this.Rz = r_z;
		this.Rr = r_r;
		this.Rp = r_p;
		this.Rv = r_v;
		this.xMax = x_max;
		this.xMin = x_min;
		this.yMax = y_max;
		this.yMin = y_min;
		this.zMax = z_max;
		this.zMin = z_min;
		this.xj = x_j;
		this.yj = y_j;
		this.dr = dr;
		this.x_v1 = x_v1;
		this.y_v1 = y_v1;
		this.ar = ar;
		this.ai = ai;
		this.lr = lr;
		this.lv_r = lv_r;
		this.derX = derX;
		this.derY = derY;

		actualizador.setCycleCount(Timeline.INDEFINITE);
		gestpos = new GestPos(x_v1, y_v1, dr, lr, derX, derY, ai, ar);
	}

	// Metodos

	public void conectar_asinc(String dir, int puerto) {

		ip = dir;
		port = (short) puerto;
		task = new Task<Integer>() {
			@Override
			protected Integer call() throws Exception {
				int conect = control.modbus_connect(ip, port);
				System.out.println("Resultado Coneccion : " + conect);
				return conect;

			}

		};

		task.onSucceededProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue ov, Object t, Object t1) {
				System.out.println("Fin del intento de coneccion :");
				actualizar();
				try {
					switch (task.get()) {
					case COM_NO_ERR: {
						conListener.OnConnectionSucces();
						break;
					}
					case COM_ERR_CON_EXISTS: {
						conListener.OnConnectionExistsErr();
						break;
					}
					case COM_ERR_MAX_CON: {
						conListener.OnConnectionMaxLimitErr();
						break;
					}
					case COM_ERR_SOCKET: {
						conListener.OnConnectionSocketErr();
						break;
					}
					case COM_ERR_TCP: {
						conListener.OnConnectionTcpErr();
						break;
					}

					}
				} catch (InterruptedException ex) {
					Logger.getLogger(CopyOfGruaCncAntiguo.class.getName()).log(Level.SEVERE,
							null, ex);
				} catch (ExecutionException ex) {
					Logger.getLogger(CopyOfGruaCncAntiguo.class.getName()).log(Level.SEVERE,
							null, ex);
				}
			}
		});

		th = new Thread(task);
		th.setDaemon(true);
		th.start();
	}

	/**
	 * Funcion para movimientos libres del equipo (se comanda directamente al
	 * cnc no hay conversion a coordenadas de la punta de la herramienta)
	 * 
	 * @param x
	 *            posicion absoluta del eje x del equipo
	 * @param y
	 *            posicion absoluta del eje y del equipo
	 * @param z
	 *            posicion absoluta del eje z del equipo
	 * @param r
	 *            posicion absoluta del eje r del equipo
	 */
	public void free_move(Double x, Double y, Double z, Double r) {
		if (!ocupado.get()) {
			int pulsos1;
			int pulsos2;
			int pulsos3;
			int pulsos4;
			int pulsos5;

			pulsos1 = (int) (x * Rx * Rp);
			pulsos2 = pulsos1;
			pulsos3 = (int) (y * Ry * Rp);
			pulsos4 = (int) (z * Rz * Rp);
			pulsos5 = (int) (r * Rr * Rp);

			short[] ejes = { 1, 2, 3, 4, 5 };
			int[] pulsos = { pulsos1, pulsos2, pulsos3, pulsos4, pulsos5 };
			int res = control.move(ejes, pulsos, (short) 5);

			if (res == 0) {
				System.out.println("Comando enviado con exito");
				actualizador.play();
				ocupado.set(true);
				mov = FREEMOVE;
			} else {
				System.out.println("Fallo : " + res);
			}

		} else {
			movListener.OnDeviceIsBussy();
		}

	}

	/**
	 * Funci髇 de posicionamiento de la herramienta (punta horizontal) en
	 * coordenadas relativas, al sector del reactor requerido.
	 * 
	 * @param r
	 *            Numero de reactor requerido
	 * @param s
	 *            Sector requerido
	 * @param x
	 *            Posicion de destino en X respecto a la referencia del sector
	 *            [cm]
	 * @param y
	 *            Posicion de destino en Y respecto a la referencia del sector
	 *            [cm]
	 */
	public void gotoRtool(int r, int s, Double x, Double y) {
		Double[] pref = gestpos.getRefPoint(r, s);
		Double[] p_abs = { x + pref[0], y + pref[1] };
		if ((r == rActual && s == sActual) || (posR < 5 && posZ >= (zMax - 1))) {
			try {
				gestpos.getReactor(p_abs[0], p_abs[1]);
				movXY_tool(p_abs[0], p_abs[1]);
				mov = GOTO_R_TOOL;
			} catch (Bounds e) {
				movListener.OnOutOfBounds();
				movListener.OnCommandFailed("Equipo fuera de rango posible");
				e.printStackTrace();
			}
		} else {
			System.out
					.println("Imposible Cambiar de Sector o Reactor sin replegar la herramienta");
			movListener.OnCommandFailed("Imposible Cambiar de Sector o Reactor sin replegar la herramienta o elevedor no elevado");
		}

	}

	/**
	 * Funci髇 de posicionamiento de la jabalina en coordenadas relativas al
	 * sector del reactor requerido.
	 * 
	 * @param r
	 *            Numero de reactor requerido
	 * @param s
	 *            Sector requerido
	 * @param x
	 *            Posicion de destino en X respecto a la referencia del sector
	 *            [cm]
	 * @param y
	 *            Posicion de destino en Y respecto a la referencia del sector
	 *            [cm]
	 */
	public void gotoRjav(int r, int s, Double x, Double y) {
		Double[] pref = gestpos.getRefPoint(r, s);
		Double[] p_abs = { x + pref[0], y + pref[1] };
		try {
			gestpos.getReactor(p_abs[0], p_abs[1]);
			movXY_jav(p_abs[0], p_abs[1]);
			mov = GOTO_R_JAV;
		} catch (Bounds e) {
			movListener.OnOutOfBounds();
			e.printStackTrace();
		}

	}

	/**
	 * Funci髇 para avance con mezclado dentro del vagon y sector actuales, en
	 * coordenadas relativas al sector del reactor requerido
	 * 
	 * @param x
	 *            Posici髇 de destino en X [cm]
	 * @param y
	 *            Posici髇 de destino en Y [cm]
	 * @param vel
	 *            velocidad de giro
	 * @param s
	 *            sentido de giro de la herramienta
	 */
	public void mix(Double x, Double y, int vel, int s) {

	}

	/**
	 * Funci髇 para insertar la herramienta dentro del vagon y sector actuales,
	 * en las coordenadas actuales
	 */
	public void insrtTool() {
		if (posR < 1 && !ocupado.get() && posZ >= (zMax - 1)) {
			try {

				Double xmaq = posX - x0 - de;
				Double ymaq = posY - y0;
				Double zmaq = zMin;
				Double rmaq = 60.0;
				Double rmaq2 = 90.0;
				int pulsos1 = (int) (xmaq * Rx * Rp);
				int pulsos2 = pulsos1;
				int pulsos3 = (int) (ymaq * Ry * Rp);
				int pulsos4 = (int) (zmaq * Rz * Rp);
				int pulsos5 = (int) (rmaq * Rr * Rp);

				int pulsos4_2 = (int) ((zmaq + 15) * Rz * Rp);
				int pulsos5_2 = (int) (rmaq2 * Rr * Rp);

				if (xmaq >= xMin && xmaq <= xMax && ymaq >= yMin
						&& ymaq <= yMax) {
					gestpos.getReactor(posX, posY);
					pXvertical = pulsos1;
					pXentrada = pX;
					int res = control.fifo(pX, pX, pulsos3, pZ, pulsos5);
					int res1 = control.fifo(pX, pX, pulsos3, pulsos4, pulsos5);
					int res2 = control.fifo(pulsos1, pulsos1, pulsos3,
							pulsos4_2, pulsos5_2);
					if (res == 0) {
						System.out.println("Comando enviado con exito");
						actualizador.play();
						ocupado.set(true);
						mov = INSRT_TOOL;
					}

				}

			} catch (Bounds e) {
				// TODO Auto-generated catch block
				movListener.OnCommandFailed("Equipo fuera de rango posible");
				e.printStackTrace();
			}
		}
		else{
			movListener.OnCommandFailed("Herramienta no replegada o elevedor no elevado");
		}
	}

	/**
	 * Funci髇 para retirar la herramienta del vagon y sector actuales, en las
	 * coordenadas actuales
	 */
	public void rtrtTool() {
		if (!ocupado.get()) {
			try {
				// Double
				// xmaq=posX-x0-de*Math.sin(-17.0)-la*Math.cos(Math.toRadians(-17.0));
				Double xmaq = posX - x0 - de;
				Double ymaq = posY - y0;
				Double zmaq = zMax;
				Double rmaq = 60.0;
				Double rmaq2 = 0.0;

				int pulsos1 = (int) (xmaq * Rx * Rp);
				int pulsos2 = pulsos1;
				int pulsos3 = (int) (ymaq * Ry * Rp);
				int pulsos4 = (int) (zmaq * Rz * Rp);
				int pulsos5 = (int) (rmaq * Rr * Rp);

				int pulsos5_2 = (int) (rmaq2 * Rr * Rp);

				gestpos.getReactor(posX, posY);
				if (xmaq >= xMin && xmaq <= xMax && ymaq >= yMin
						&& ymaq <= yMax) {
					// int res=control.line(pulsos1, pulsos2, pulsos3,
					// pulsos4,pulsos5);

					int res = control.fifo(pXvertical, pXvertical, pY, -pZ, pR);
					int res1 = control.fifo(pXentrada, pXentrada, pY, -pZ,
							pulsos5);
					int res2 = control.fifo(pXentrada, pXentrada, pY, pulsos4,
							pulsos5);
					int res3 = control.fifo(pXentrada, pXentrada, pY, pulsos4,
							pulsos5_2);

					if (res == 0) {
						System.out.println("Comando enviado con exito");
						actualizador.play();
						ocupado.set(true);
						mov = RTRT_TOOL;
					}
				}
			} catch (Bounds e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				movListener.OnCommandFailed("Equipo fuera de rango posible");
			}
		}
	}

	/**
	 * Funci髇 para insertar la jabalina dentro del vagon y sector actuales, en
	 * las coordenadas actuales
	 */
	public void insrtJav() {

	}

	/**
	 * Funci髇 para retirar la jabalina del vagon y sector actuales, en las
	 * coordenadas actuales
	 */
	public void rtrtJav() {

	}

	/**
	 * Funci髇 para colocar la punta del equipo(solo si esta en posicion
	 * horizontal) en la posicion requerida en el plano xy repecto a la
	 * referencia del equipo
	 * 
	 * @param x
	 *            posici髇 en X deseada
	 * @param y
	 *            posici髇 en Y deseada
	 */
	public void movXY_tool(Double x, Double y) {
		if (!ocupado.get()) {
			int pulsos1;
			int pulsos2;
			int pulsos3;

			Double xmaq = x - x0 - de * Math.sin(Math.toRadians(posR)) - la
					* Math.cos(Math.toRadians(posR));
			Double ymaq = y - y0;

			if ((xmaq <= xMax) && (xmaq >= xMin) && (ymaq >= yMin)
					&& (ymaq <= yMax)) {
				pulsos1 = (int) (xmaq * Rx * Rp);
				pulsos2 = pulsos1;
				pulsos3 = (int) (ymaq * Ry * Rp);

				short[] ejes = { 1, 2, 3 };
				int[] pulsos = { pulsos1, pulsos2, pulsos3 };
				int res = control.move(ejes, pulsos, (short) 3);

				if (res == 0) {

					System.out.println("Comando enviado con exito");
					actualizador.play();
					ocupado.set(true);
				} else {
					System.out.println("Fallo : " + res);
				}
			} else {
				movListener.OnOutOfBounds();
				movListener.OnCommandFailed("Equipo fuera de rango posible");
				System.out.println("Movimiento fuera de rango");
			}
			if (mov != GOTO_R_TOOL)
				mov = MOV_XY_TOOL;

		} else {
			movListener.OnDeviceIsBussy();
		}
	}

	public void bombaOn(String ip,Integer port){
		Socket cliente =new Socket();
        DataOutputStream dos;
        SocketAddress dir;
        
        dir = new InetSocketAddress(ip,port);
        int tmout=2000;  //tiene que ser modificable!! ojo!!
        try {
			cliente.connect(dir, tmout);
			dos = new DataOutputStream(cliente.getOutputStream());
			dos.writeBytes("X");
			dos.close();
            cliente.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void bombaOff(String ip,Integer port){
		Socket cliente =new Socket();
        DataOutputStream dos;
        SocketAddress dir;
        
        dir = new InetSocketAddress(ip,port);
        int tmout=2000;  //tiene que ser modificable!! ojo!!
        try {
			cliente.connect(dir, tmout);
			dos = new DataOutputStream(cliente.getOutputStream());
			dos.writeBytes("Y");
			dos.close();
            cliente.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * Funci髇 para colocar la punta de la jabalina(solo si esta retraida) en la
	 * posicion requerida en el plano xy repecto a la referencia del equipo
	 * 
	 * @param x
	 *            posici髇 en X deseada
	 * @param y
	 *            posici髇 en Y deseada
	 */
	public void movXY_jav(Double x, Double y) {
		if (!ocupado.get()) {
			int pulsos1;
			int pulsos2;
			int pulsos3;

			Double xmaq = x - xj;
			Double ymaq = y - yj;

			if ((xmaq <= xMax) && (xmaq >= xMin) && (ymaq >= yMin)
					&& (ymaq <= yMax)) {
				pulsos1 = (int) (xmaq * Rx * Rp);
				pulsos2 = pulsos1;
				pulsos3 = (int) (ymaq * Ry * Rp);

				short[] ejes = { 1, 2, 3 };
				int[] pulsos = { pulsos1, pulsos2, pulsos3 };
				int res = control.move(ejes, pulsos, (short) 3);

				if (res == 0) {
					System.out.println("Comando enviado con exito");
					actualizador.play();
					ocupado.set(true);
				} else {
					System.out.println("Fallo : " + res);
				}
			} else {
				movListener.OnOutOfBounds();
			}
			if (mov != GOTO_R_JAV)
				mov = MOV_XY_JAV;
		} else {
			movListener.OnDeviceIsBussy();
		}
	}

	void configX(Double vi, Double vm, Double ac) {
		short[] axis = new short[] { 1, 2 };
		int isp = (int) (vi * 166.6667);
		int sp = (int) (vm * 166.6667);
		int aacc = (int) (ac * 166.6667);
		int[] init_speed = new int[] { isp, isp };
		int[] speed = new int[] { sp, sp };
		int[] acc = new int[] { aacc, aacc };
		int[] pmm = new int[] { 100, 100 };
		short num = 2;
		control.mov_parameter(axis, init_speed, speed, acc, pmm, num);
	}

	void configY(Double vi, Double vm, Double ac) {
		control.mov_parameter((short) 3, (int) (vi * 166.667),
				(int) (vm * 166.667), (int) (ac * 166.6667), 100);
	}

	void configZ(Double vi, Double vm, Double ac) {
		control.mov_parameter((short) 4, (int) (vi * 166.667),
				(int) (vm * 166.6067), (int) (ac * 166.6667), 100);
	}

	void configR(Double vi, Double vm, Double ac) {
		control.mov_parameter((short) 5, (int) (vi * 166.667),
				(int) (vm * 166.6067), (int) (ac * 166.6667), 100);
	}

	void configAll(Double vi, Double vm, Double ac) {
		short[] axis = new short[] { 1, 2, 3, 4, 5 };
		int isp = (int) (vi * 166.6667);
		int sp = (int) (vm * 166.6667);
		int aacc = (int) (ac * 166.6667);
		int[] init_speed = new int[] { isp, isp, isp, isp, isp };
		int[] speed = new int[] { sp, sp, sp, sp, sp };
		int[] acc = new int[] { aacc, aacc, aacc, aacc, aacc };
		int[] pmm = new int[] { 100, 100, 100, 100, 100 };
		short num = 5;
		control.mov_parameter(axis, init_speed, speed, acc, pmm, num);
	}

	public void toolOn() {
		control.output_on((short) 0);
	}

	public void toolOff() {
		control.output_off((short) 0);
	}

	// ////////////////////////
	// Supervisor de movimiento
	private void actualizar() {
		int res = control.is_working();
		if (res != 2) {
			if (res == 1) {
				ocupado.set(true);
			} else {
				ocupado.set(false);
			}
		}
		if (control.get_axis_pos() == 0) {
			int pulsos1 = control.get_pos1();
			int pulsos2 = control.get_pos2();
			int pulsos3 = control.get_pos3();
			int pulsos4 = control.get_pos4();
			int pulsos5 = control.get_pos5();

			if (Math.abs(pulsos1 - pulsos2) > 300) {// se usa para garantizar
													// que no haya un desfase
													// grande entre los dos
													// motores del eje x
				control.stop(); // TODO Verificar que no traiga problemas
				movListener.OnCommandFailed("Fallo en eje X");
				System.out.println();
				System.out.println("Desfase entre motores eje X :"
						+ Math.abs(pulsos1 - pulsos2));
			}

			pX = -pulsos1;
			pY = -pulsos3;
			pZ = -pulsos4;
			pR = -pulsos5;

			posPortico = pX.doubleValue() / (Rx * Rp);
			posCarro = pY.doubleValue() / (Ry * Rp);
			posElevador = pZ.doubleValue() / (Rz * Rp);
			posRotador = pR.doubleValue() / (Rr * Rp);

			posR = posRotador - 0;// TODO el 17 es el angulo inicial sobre la
									// horizontal, verificar comportamiento y
									// volver variable
			posX = posPortico + x0 + de * Math.sin(Math.toRadians(posR)) + la
					* Math.cos(Math.toRadians(posR));
			posY = posCarro + y0;
			posZ = posElevador + z0 + de * Math.cos(Math.toRadians(posR)) - la
					* Math.sin(Math.toRadians(posR));

			try {
				Integer[] reactor_act = gestpos.getReactor(posX, posY);
				rActual = reactor_act[0];
				sActual = reactor_act[1];
				show_rActual.set(rActual.toString());
				show_sActual.set(sActual.toString());
				Double[] prefAct = gestpos.getRefPoint(rActual, sActual);
				xRel = posX - prefAct[0];
				yRel = posY - prefAct[1];
				show_xRel.set(xRel.toString());
				show_yRel.set(yRel.toString());

			} catch (Bounds e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}

			movListener.OnPositionUpdate(posPortico, posCarro, posElevador,
					posRotador);

			show_posX.set(df.format(posX));
			show_posY.set(df.format(posY));
			show_posZ.set(df.format(posZ));
			show_posR.set(df.format(posR));

			show_posPortico.set(df.format(posPortico));
			show_posCarro.set(df.format(posCarro));
			show_posElevador.set(df.format(posElevador));
			show_posRotador.set(df.format(posRotador));

			// actualizar estado del equipo, si libre u ocupado

			int reswk;
			reswk = control.is_working();
			if (reswk != 2) {
				if (reswk == 1) {
					ocupado.set(true);
				} else {
					ocupado.set(false);
					movListener.OnMovementSucces(mov);
					// filtrar los casos de error.....
				}
			}

		} else {
			System.out.println("Fallo al actualizar");
		}
	}

}
