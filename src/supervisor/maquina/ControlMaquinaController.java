package supervisor.maquina;

import Bases.TabletCMD;
import Bases.cnc;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.util.calendar.LocalGregorianCalendar;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class ControlMaquinaController implements Initializable {
	//control de tiempo
	
	@FXML
	private TextArea txtAreaLog;
	
	String hactual;
	
	int ha[]={10, 16, 10, 10, 10, 10, 10};//hora para rutina
	int ma[]={00, 00, 00, 00, 00, 00, 00};//minuto para rutina
	int hi[]={23, 4, 9, 12, 15, 18, 23};//hora para intercambiador
	int mi[]={55, 55, 55, 55, 55, 55, 55};//minuto para intercambiador
	
	private Path pathRegistro;
	PrintWriter oStReg=null;
	
	boolean alarma=false;
	private final SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);
	Timeline actualizador = new Timeline(new KeyFrame(Duration.millis(1000), new EventHandler<ActionEvent>() {
	    @Override
	    public void handle(ActionEvent event) {
	    //Temporizador para control de posicion
	    	hactual=date.format(new GregorianCalendar().getTime());
	    	hora=new GregorianCalendar().getTime();
	    	//txtAreaLog.appendText(hora.getHours()+":"+hora.getMinutes()+":"+hora.getSeconds()+"\n");
	    	
//	    	if(((hora.getHours()==hi[0]&&hora.getMinutes()==mi[0])||
//	    			(hora.getHours()==hi[1]&&hora.getMinutes()==mi[1])||
//	    			(hora.getHours()==hi[2]&&hora.getMinutes()==mi[2])||
//	    			(hora.getHours()==hi[3]&&hora.getMinutes()==mi[3]))&&alarma){
//	    		txtAreaLog.appendText("Encendido Bomba : "+hora.getHours()+":"+hora.getMinutes()+"\n");
//	    		grua.bombaOn("20.0.0.8",2000);
//	    		
//	    	}
	    	
	    	if(((hora.getHours() == ha[0] && hora.getMinutes() == ma[0])||
	    			(hora.getHours() == ha[1] && hora.getMinutes() == ma[1])||
	    			(hora.getHours() == ha[2] && hora.getMinutes() ==ma[2])||
	    			(hora.getHours() == ha[3] && hora.getMinutes() ==ma[3])||
	    			(hora.getHours() == ha[4] && hora.getMinutes() ==ma[4])||
	    			(hora.getHours() == ha[5] && hora.getMinutes() ==ma[5])||
	    			(hora.getHours() == ha[6] && hora.getMinutes() ==ma[6]))&& alarma){
	    		txtAreaLog.appendText("Inicio de rutina : "+hora.getHours()+":"+hora.getMinutes()+"\n");
	    		rutina=true;
	    		paso_rut=0;
	    		alarma=false;
	    		grua.movListener.OnMovementSucces(1);

	    	}
	    	
	    }
	    }));
	
	//
	Double relx = 30.0 / 17.8;// verificar relaciones de reduccion
	Double rely = 40.0 / 17.8;// /18;
	Double relz = 40.0 / 17.8;// /18;
	Double relr = 40.0 / 360 * 2.4;
	Double relv = 166.666667;
	Double relp = 10000.0;
	Date hora;
	
	int rAm = 0; //TODO reactor a mezclar
	
	Double xmax = 300.0;
	Double xmin = 0.0;

	Double ymax = 900.0;
	Double ymin = 0.0;

	Double zmax = 0.0;
	Double zmin = -52.0;

	Double rmax = 109.0;
	Double rmin = 0.0;

	Double dis_res = 11.25 + 10; // distancia restringida en el sistema

	Double xv1 = 325.0;
	Double yv1 = 100.0;

	Double ar = 397.0;
	Double ai = 10.0;
	Double lr = 203.0;

	Double derX = 90.0;
	Double derY = 70.0;
	// variables usadas para generacion de rutina
	Boolean rutina = false;
	int paso_rut = 0;
	//

	Double temperaturas[] = new Double[4];
	int nMuestras = 20;
	// variables para muestra de temperaturas
	CategoryAxis ejeX = new CategoryAxis();
	NumberAxis ejeY = new NumberAxis("Temperatura", 0, 40, 1);
	public static XYChart.Series sensor1 = new XYChart.Series();
	public static XYChart.Series sensor2 = new XYChart.Series();
	public static XYChart.Series sensor3 = new XYChart.Series();
	public static XYChart.Series sensor4 = new XYChart.Series();
	// manejo de datos
	@FXML
	BarChart<String, Number> bcjavalinas = new BarChart<>(ejeX, ejeY);
	// coso sebas
	TabletCMD tabletcmd;
	// variables de control
	Boolean ctablet = false;
	// TODO medir y llenarGruaCnc grua=new GruaCnc(x_0, y_0, z_0, l_a, r_a, d_e,
	// r_x, r_y, r_z, r_r, r_p, r_v, x_max, x_j, y_j, x_min, y_max, y_min,
	// z_max, z_min, dr, x_v1, y_v1, ar, ai, lr, lv_r, derX, derY)
	// TODO corregir las distancias de la javalina
	GruaCnc grua = new GruaCnc(217.0, 87.5, 211.0, 192.0, 11.25, 15.0, relx,
			rely, relz, relr, relp, relv, xmax, 0.0, 0.0, xmin, ymax, ymin,
			zmax, zmin, dis_res, xv1, yv1, ar, ai, lr, lr, derX, derY);

	//cnc controlador = new cnc();
	// //////////////////

	TabletCMD remoto;

	@FXML
	private Label lblConeccion;

	// labels de muestra de datos de posicion
	// posiciones del cnc
	@FXML
	private Label PX;
	@FXML
	private Label PY;
	@FXML
	private Label PZ;
	@FXML
	private Label PR;
	// posiciones de la punta
	@FXML
	private Label PosX;
	@FXML
	private Label PosY;
	@FXML
	private Label PosZ;
	@FXML
	private Label PosR;
	@FXML
	private Label PosRY;
	@FXML
	private Label PosRX;
	@FXML
	private Label rAct;
	@FXML
	private Label sAct;

	// //cuadros de texto para movimiento
	@FXML
	private TextField txtFldPosX;
	@FXML
	private TextField txtFldPosY;
	@FXML
	private TextField txtFldPosZ;
	@FXML
	private TextField txtFldPosR;
	// MOVIEMIENTO EN REACTOR
	@FXML
	private TextField txtFldPosXrel;
	@FXML
	private TextField txtFldPosYrel;
	@FXML
	private TextField txtFldReactor;
	@FXML
	private TextField txtFldSector;

	// cuadros de texto para configuraciOn

	// configuración eje x
	@FXML
	private TextField txtFldViX;
	@FXML
	private TextField txtFldVmX;
	@FXML
	private TextField txtFldAccX;
	@FXML
	private TextField txtFldPdX;
	// configurtacion eje y
	@FXML
	private TextField txtFldViY;
	@FXML
	private TextField txtFldVmY;
	@FXML
	private TextField txtFldAccY;
	@FXML
	private TextField txtFldPdY;
	// configuracion eje z
	@FXML
	private TextField txtFldViZ;
	@FXML
	private TextField txtFldVmZ;
	@FXML
	private TextField txtFldAccZ;
	@FXML
	private TextField txtFldPdZ;
	// configuracion eje R
	@FXML
	private TextField txtFldViR;
	@FXML
	private TextField txtFldVmR;
	@FXML
	private TextField txtFldAccR;
	@FXML
	private TextField txtFldPdR;

	// Comando del variador de frecuencia
	@FXML
	private TextField txtFldVelVar;
	@FXML
	private TextField txtFldSentVar;

	@FXML
	private void handleMoverAgitador(ActionEvent e) {
		grua.toolOn();
	}

	@FXML
	private void handleDetenerAgitador(ActionEvent e) {
		grua.toolOff();
	}

	@FXML
	// coneccion con cnc
	private TextField txtFldPuerto;
	@FXML
	private TextField txtFldIP;

	@FXML
	private ChoiceBox<Short> chBoxEjes;

	// botones
	@FXML
	private Button btnConfigurar;

	// eventos para temperatura
	Boolean adq = false;

	@FXML
	private void handleIniciarTemp(ActionEvent e) {

	}

	@FXML
	private void handleDetenerTemp(ActionEvent e) {

	}

	// EVENTOS DE COMANDO DE MOVIMIENTO
	@FXML
	private void handleButtonMoverLibre(ActionEvent e) {
		grua.free_move(Double.parseDouble(txtFldPosX.getText()),
				Double.parseDouble(txtFldPosY.getText()),
				Double.parseDouble(txtFldPosZ.getText()),
				Double.parseDouble(txtFldPosR.getText()));
	}

	@FXML
	private void handleButtonMoverHerramienta(ActionEvent e) {
		grua.movXY_tool(Double.parseDouble(txtFldPosX.getText()),
				Double.parseDouble(txtFldPosY.getText()));
	}

	@FXML
	private void handleButtonIrReactor(ActionEvent e) {
//		grua.gotoRtool(Integer.parseInt(txtFldReactor.getText()),
//				Integer.parseInt(txtFldSector.getText()),
//				Double.parseDouble(txtFldPosXrel.getText()),
//				Double.parseDouble(txtFldPosYrel.getText()));
		if(!rutina){
			rutina=true;
			paso_rut=0;
			rAm=Integer.parseInt(txtFldReactor.getText());
			grua.movListener.OnMovementSucces(1);
			//grua.free_move(0.0, 0.0, 0.0, 0.0);
		}
		
		
	}

	@FXML
	private void handleButtonIntroHerramienta(ActionEvent e) {
		grua.insrtTool();
	}

	@FXML
	private void handleButtonRetHerramienta(ActionEvent e) {
		grua.rtrtTool();
	}

	// eventos de botones de configuracion

	Temperaturas adqTemp = new Temperaturas();
	Double coso = 0.000;

	// eventos de botones de configruacion
	@FXML
	private void handleButtonConnect(ActionEvent e) {
		grua.conectar_asinc(txtFldIP.getText(),
				Integer.parseInt(txtFldPuerto.getText()));
	}
	@FXML
	private void handleButtonDisconnect(ActionEvent e) {
		grua.desconectar();
	}
	
	@FXML
	private void handleButtonConfigAll(ActionEvent e) {

		// controlador.mov_parameter(axis, init_speed, speed, acc, pmm, num);
	}

	@FXML
	private void handleButtonConfigX(ActionEvent e) {
		grua.configX(Double.parseDouble(txtFldViX.getText()),
				Double.parseDouble(txtFldVmX.getText()),
				Double.parseDouble(txtFldAccX.getText()));
	}

	@FXML
	private void handleButtonConfigY(ActionEvent e) {
		grua.configY(Double.parseDouble(txtFldViY.getText()),
				Double.parseDouble(txtFldVmY.getText()),
				Double.parseDouble(txtFldAccY.getText()));
	}

	@FXML
	private void handleButtonConfigZ(ActionEvent e) {
		grua.configZ(Double.parseDouble(txtFldViZ.getText()),
				Double.parseDouble(txtFldVmZ.getText()),
				Double.parseDouble(txtFldAccZ.getText()));
	}

	@FXML
	private void handleButtonConfigR(ActionEvent e) {
		grua.configR(Double.parseDouble(txtFldViR.getText()),
				Double.parseDouble(txtFldVmR.getText()),
				Double.parseDouble(txtFldAccR.getText()));
	}

	// EVENTOS DE BOTONES DE COMANDOS BASICOS
	@FXML
	void handleButtonDetener(ActionEvent e) {
		ctablet = false;
		rutina=false;
		paso_rut=0;
		grua.detener();
		grua.toolOff();

	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {

		actualizador.setCycleCount(Timeline.INDEFINITE);
		actualizador.play();
		
		
		// tabletcmd=new TabletCMD(2000);

		bcjavalinas.getXAxis().setLabel("Sensores");
		bcjavalinas.getYAxis().setLabel("Temperatura [°C]");

		bcjavalinas.getYAxis().autoRangingProperty().set(false);
		bcjavalinas.setTitle("SENSORES");

		// sensor1.getData().add(new XYChart.Data(javalina1,0));

		temperaturas[0] = 0.00;
		temperaturas[1] = 0.00;
		temperaturas[2] = 0.00;
		temperaturas[3] = 0.00;

		sensor1.setName("Sensor-1");
		sensor1.getData().add(
				new XYChart.Data("Sensor-1", temperaturas[0] = 0.00));
		sensor2.setName("Sensor-2");
		sensor2.getData().add(
				new XYChart.Data("Sensor-2", temperaturas[0] = 0.00));
		sensor3.setName("Sensor-3");
		sensor3.getData().add(
				new XYChart.Data("Sensor-3", temperaturas[0] = 0.00));
		sensor4.setName("Sensor-4");
		sensor4.getData().add(
				new XYChart.Data("Sensor-4", temperaturas[0] = 0.00));

		bcjavalinas.getData().addAll(sensor1, sensor2, sensor3, sensor4);
		bcjavalinas.setAnimated(false);

		// AdquisiciÃ³n de temperaturas finalizada
		adqTemp.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent event) {
				if (adq) {
					temperaturas = adqTemp.getValue();
					adqTemp.reset();
					adqTemp.iniciarMuestreo(nMuestras);
					adqTemp.start();
					int c = 0;
					for (XYChart.Series<String, Number> series : bcjavalinas
							.getData()) {
						for (XYChart.Data<String, Number> data : series
								.getData()) {
							data.setYValue(temperaturas[c]);
							c++;
						}

					}
				}
			}
		});
		adqTemp.setOnFailed(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent event) {
				if (adq) {
					System.out.println("Error en el hilo reiniciando hilo");
					adqTemp.reset();
					adqTemp.iniciarMuestreo(nMuestras);
					adqTemp.start();
				}

			}
		});
		adqTemp.setOnCancelled(new EventHandler<WorkerStateEvent>() {

			@Override
			public void handle(WorkerStateEvent event) {
				if (adq) {
					System.out.println("Timeout excedido reiniciando hilo");
					adqTemp.reset();
					adqTemp.iniciarMuestreo(nMuestras);
					adqTemp.start();
				}

			}
		});

		PX.textProperty().bind(grua.show_posPortico);
		PY.textProperty().bind(grua.show_posCarro);
		PZ.textProperty().bind(grua.show_posElevador);
		PR.textProperty().bind(grua.show_posRotador);

		PosRX.textProperty().bind(grua.show_xRel);
		PosRY.textProperty().bind(grua.show_yRel);

		rAct.textProperty().bind(grua.show_rActual);
		sAct.textProperty().bind(grua.show_sActual);

		PosX.textProperty().bind(grua.show_posX);
		PosY.textProperty().bind(grua.show_posY);
		PosZ.textProperty().bind(grua.show_posZ);
		PosR.textProperty().bind(grua.show_posR);

		txtFldAccX.setText("1000");
		txtFldViX.setText("0");
		txtFldVmX.setText("1200");

		txtFldAccY.setText("1000");
		txtFldViY.setText("0");
		txtFldVmY.setText("1700");

		txtFldAccZ.setText("800");
		txtFldViZ.setText("0");
		txtFldVmZ.setText("1200");

		txtFldAccR.setText("50");
		txtFldViR.setText("0");
		txtFldVmR.setText("100");

		txtFldIP.setText("20.0.0.88");
		txtFldPuerto.setText("502");

		txtFldPosX.setText("0");
		txtFldPosY.setText("0");
		txtFldPosZ.setText("0");
		txtFldPosR.setText("0");
		
		txtFldReactor.setText("31");
		
		grua.setOnConnectionListener(new GruaCnc.OnConnectionListener() {

			@Override
			public void OnConnectionSucces() {
				lblConeccion.setText("Conectado");
			}

			@Override
			public void OnConnectionExistsErr() {
				throw new UnsupportedOperationException("Not supported yet."); // To
																				// change
																				// body
																				// of
																				// generated
																				// methods,
																				// choose
																				// Tools
																				// |
																				// Templates.
			}

			@Override
			public void OnConnectionMaxLimitErr() {
				throw new UnsupportedOperationException("Not supported yet."); // To
																				// change
																				// body
																				// of
																				// generated
																				// methods,
																				// choose
																				// Tools
																				// |
																				// Templates.
			}

			@Override
			public void OnConnectionSocketErr() {
				throw new UnsupportedOperationException("Not supported yet."); // To
																				// change
																				// body
																				// of
																				// generated
																				// methods,
																				// choose
																				// Tools
																				// |
																				// Templates.
			}

			@Override
			public void OnConnectionTcpErr() {
				throw new UnsupportedOperationException("Not supported yet."); // To
																				// change
																				// body
																				// of
																				// generated
																				// methods,
																				// choose
																				// Tools
																				// |
																				// Templates.
			}
		});

		grua.setOnOnMovementListener(new GruaCnc.OnMovementListener() {

			@Override
			public void OnPositionUpdate(Double x, Double y, Double z, Double r) {
				// TODO Auto-generated method stub

			}

			@Override
			public void OnOutOfBounds() {
				// TODO Auto-generated method stub

			}

			@Override
			public void OnMovementSucces(int mov) {
				// TODO Auto-generated method stub
				System.out.println("Termine de moverme!!! " + mov);
				String reporte="";
//					if (rutina) {
//						switch (paso_rut) {
//						case 0: {
//							grua.gotoRtool(31, 1, 60.0, 150.0);
//							break;
//						}
//						case 1: {
//							grua.insrtTool();
//							break;
//						}
//						case 2: {
//							grua.gotoRtool(31, 1, 160.0, 150.0);
//							break;
//						}
//						case 3: {
//							grua.gotoRtool(31, 1, 160.0, 120.0);
//							break;
//						}
//						case 4: {
//							grua.gotoRtool(31, 1, 0.0, 120.0);
//							break;
//						}
//						case 5: {
//							grua.gotoRtool(31, 1, 160.0, 0.01);
//							break;
//						}
//						case 6: {
//							grua.gotoRtool(31, 1, 0.0, 0.01);
//							break;
//						}
//						case 7: {
//							grua.gotoRtool(31, 1, 160.0, 120.0);
//							break;
//						}
//						case 8: {
//							grua.gotoRtool(31, 1, 160.0, 150.0);
//							break;
//						}
//						case 9: {
//							grua.rtrtTool();
//							break;
//						}
//						case 10: {
//							grua.gotoRtool(31, 2, 60.0, 8.0);
//							break;
//						}
//						case 11: {
//							grua.insrtTool();
//							break;
//						}
//						case 12: {
//							grua.gotoRtool(31, 2, 160.0, 8.0);
//							break;
//						}
//						case 13: {
//							grua.gotoRtool(31, 2, 160.0, 38.0);
//							break;
//						}
//						case 14: {
//							grua.gotoRtool(31, 2, 0.0, 38.0);
//							break;
//						}
//						case 15: {
//							grua.gotoRtool(31, 2, 160.0, 150.0);
//							break;
//						}
//						case 16: {
//							grua.gotoRtool(31, 2, 0.0, 150.0);
//							break;
//						}
//						case 17: {
//							grua.gotoRtool(31, 2, 160.0, 38.0);
//							break;
//						}
//						case 18: {
//							grua.gotoRtool(31, 2, 160.0, 8.0);
//							break;
//						}
//						case 19: {
//							grua.rtrtTool();
//							break;
//						}
//						
//						case 20: {
//							grua.free_move(0.0, 0.0, 0.0, 0.0);
//							paso_rut=10;
//							rutina=false;
//							break;
//						}
//						
//						}
//						paso_rut++;
//						
//					}
				
				if (rutina) {
					//D:\Documents\Compost\log
					pathRegistro=Paths.get("D://Documents//Compost//log");
					if(Files.notExists(pathRegistro)){
		                try {
		                	txtAreaLog.appendText(hactual+": Creando archivo de log.... \n");
		                    Files.createFile(pathRegistro);
		                    txtAreaLog.appendText(hactual+": Archivo de log creado \n");
		                } catch (IOException ex) {
		                    System.out.println(pathRegistro.toString());
		                    
		                }
		            }
					 // se crean los writers con append en true
					hactual=date.format(new GregorianCalendar().getTime());
					switch (paso_rut) {
					case 0: {
						grua.toolOff();
						grua.gotoRtool(rAm, 1, 80.0, 150.0);
						txtAreaLog.appendText(hactual+": Equipo inicia movimiento a reactor : "+rAm+" sector : 1 \n");
						reporte=hactual+": Equipo inicia movimiento a reactor : "+rAm+" sector : 1";
						break;
					}
					case 1: {
						grua.toolOn();
						grua.insrtTool();
						txtAreaLog.appendText(hactual+": Equipo inicia introduccion de herramienta \n");
						reporte=hactual+": Equipo inicia introduccion de herramienta";
						break;
					}
					case 2: {
						grua.gotoRtool(rAm, 1, 160.0, 150.0);
						txtAreaLog.appendText(hactual+": Equipo inicia mezclado: pasada 1 \n");
						reporte=hactual+": Equipo inicia mezclado: pasada 1 ";
						break;
					}
					case 3: {
						grua.gotoRtool(rAm, 1, 160.0, 115.0);
						break;
					}
					case 4: {
						grua.gotoRtool(rAm, 1, 0.0, 115.0);
						txtAreaLog.appendText(hactual+": pasada 2 \n");
						reporte=hactual+": pasada 2 ";
						break;
					}
					case 5: {
						grua.gotoRtool(rAm, 1, 0.0, 80.0);
						break;
					}
					case 6: {
						grua.gotoRtool(rAm, 1, 160.0, 80.0);
						txtAreaLog.appendText(hactual+": pasada 3 \n");
						reporte=hactual+": pasada 3 \n";
						break;
					}
					case 7: {
						grua.gotoRtool(rAm, 1, 160.0, 45.0);
						break;
					}
					case 8: {
						grua.gotoRtool(rAm, 1, 0.0, 45.0);
						txtAreaLog.appendText(hactual+": pasada 4 \n");
						reporte=hactual+": pasada 4";
						break;
					}
					case 9: {
						grua.gotoRtool(rAm, 1, 0.0, 10.0);
						
						break;
					}
					case 10: {
						grua.gotoRtool(rAm, 1, 160.0, 10.0);
						txtAreaLog.appendText(hactual+": pasada 5 \n");
						reporte=hactual+": pasada 5";
						break;
					}
					case 11: {
						grua.gotoRtool(rAm, 1, 0.0, 150.0);
						txtAreaLog.appendText(hactual+": diagonal 1 \n");
						reporte=hactual+": diagonal 1";
						break;
					}
					case 12: {
						grua.gotoRtool(rAm, 1, 160.0, 150.0);
						txtAreaLog.appendText(hactual+": pasada 6 \n");
						reporte=hactual+": pasada 6";
						break;
					}
					case 13: {
						grua.gotoRtool(rAm, 1, 0.1, 0.1);
						txtAreaLog.appendText(hactual+": diagonal 2 \n");
						reporte=hactual+": diagonal 2";
						break;
					}
					case 14: {
						grua.rtrtTool();
						txtAreaLog.appendText(hactual+": Se retrae herramienta \n");
						reporte=hactual+": Se retrae herramienta";
						break;
					}
					case 15: {
						//grua.toolOff();
						grua.gotoRtool(rAm, 2, 80.0, 8.0);
						txtAreaLog.appendText(hactual+": Equipo inicia movimiento a reactor : "+rAm+" sector : 2 \n");
						reporte=hactual+": Equipo inicia movimiento a reactor : "+rAm+" sector : 2";
						break;
					}
					case 16: {
						grua.toolOn();
						grua.insrtTool();
						txtAreaLog.appendText(hactual+": Equipo inicia introduccion de herramienta \n");
						reporte=": Equipo inicia introduccion de herramienta ";
						break;
					}
					case 17: {
						grua.gotoRtool(rAm, 2, 160.0, 8.0);
						txtAreaLog.appendText(hactual+": Equipo inicia mezclado: pasada 1 \n");
						reporte=hactual+": Equipo inicia mezclado: pasada 1";
						break;
					}
					case 18: {
						grua.gotoRtool(rAm, 2, 160.0, 43.0);
						break;
					}
					case 19: {
						grua.gotoRtool(rAm, 2, 0.0, 43.0);
						txtAreaLog.appendText(hactual+": pasada 2 \n");
						reporte=hactual+": pasada 2 ";
						break;
					}
					case 20: {
						grua.gotoRtool(rAm, 2, 0.0, 78.0);
						break;
					}
					case 21: {
						grua.gotoRtool(rAm, 2, 160.0, 78.0);
						txtAreaLog.appendText(hactual+": pasada 3 \n");
						reporte=hactual+": pasada 3 ";
						break;
					}
					case 22: {
						grua.gotoRtool(rAm, 2, 160.0, 113.0);
						break;
					}
					case 23: {
						grua.gotoRtool(rAm, 2, 0.0, 113.0);
						txtAreaLog.appendText(hactual+": pasada 4 \n");
						reporte=hactual+": pasada 4 ";
						break;
					}
					case 24: {
						grua.gotoRtool(rAm, 2, 0.0, 148.0);
						break;
					}
					case 25: {
						grua.gotoRtool(rAm, 2, 160.0, 148.0);
						txtAreaLog.appendText(hactual+": pasada 5 \n");
						reporte=hactual+": pasada 5 ";
						break;
					}
					case 26: {
						grua.gotoRtool(rAm, 2, 0.0, 8.0);
						txtAreaLog.appendText(hactual+": diagonal 1 \n");
						reporte=hactual+": diagonal 1 ";
						break;
					}
					case 27: {
						grua.gotoRtool(rAm, 2, 160.0, 8.0);
						txtAreaLog.appendText(hactual+": pasada 6 \n");
						reporte=hactual+": pasada 6 ";
						break;
					}
					case 28: {
						grua.gotoRtool(rAm, 2, 0.1, 148.0);
						txtAreaLog.appendText(hactual+": diagonal 2 \n");
						reporte=hactual+": diagonal 2 ";
						break;
					}
					case 29: {
						grua.rtrtTool();
						txtAreaLog.appendText(hactual+": Equipo retrae herramienta \n");
						reporte=hactual+": Equipo retrae herramienta ";
						break;
					}
					
					case 30: {
						grua.toolOff();
						if(grua.posR<1){
							grua.free_move(0.0, 0.0, 0.0, 0.0);
							txtAreaLog.appendText(hactual+": Equipo vuelve a home \n");
							reporte=hactual+": Equipo vuelve a home ";
							grua.bombaOff("20.0.0.8",2000);
						}
						
						break;
					}
					case 31:{
						txtAreaLog.appendText(hactual+": Rutina Exitosa \n");
						reporte=hactual+": Rutina Exitosa";
						grua.bombaOff("20.0.0.8",2000);
						paso_rut=0;
						rutina=false;
						alarma=true;
						break;
					}
					
					}
					try {
						oStReg=new PrintWriter(new FileWriter("D://Documents//Compost//log//log.rep",true));
						oStReg.println(reporte);
						oStReg.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						txtAreaLog.appendText(hactual+": Error al acceder al archivo de registro en: D://Documents//Compost//log//log.rep\n");
					} 
					paso_rut++;
					
				}
				
			}

			@Override
			public void OnDeviceIsBussy() {
				// TODO Auto-generated method stub

			}

			@Override
			public void OnCommandFailed(String fuente) {
				txtAreaLog.appendText(hactual+": Error : "+fuente+ " rutina detenida\n");
				rutina=false;
				grua.toolOff();
				grua.bombaOff("20.0.0.8",2000);
				paso_rut=99;
			}
		});

	}

}
