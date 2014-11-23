package Bases;

import org.omg.CORBA.Bounds;

public class GestPos {
    
    public final Double x0;
    public final Double y0;
    public final Double dr;
    public final Double lr;
    public final Double derX;
    public final Double derY;
    public final Double ai;
    public final Double aR;
    
    /**
     * 
     * @param x0 distancia desde el punto de fererencia a la esquina del reactor
     * @param y0 
     * @param dr espacio restringido entre centro de la herramienta y borde del reactor
     * @param lr largo del reactor
     * @param derX distancia entre filas de reactores
     * @param derY distancia entre columnas de reactores
     * @param ai ancho del intercambiador de calor
     * @param aR ancho del reactor
     */
    public GestPos(
            Double x0,
            Double y0,
            Double dr,
            Double lr,
            Double derX,
            Double derY,
            Double ai,
            Double aR){
        this.x0=x0;
        this.y0=y0;
        this.dr=dr;
        this.lr=lr;
        this.derX=derX;
        this.derY=derY;
        this.ai=ai;
        this.aR=aR;
    }
    
    /**
     * Devuelve el punto de referencia para el sector del reactor requerido
     * @param nr numero de reactor  
     * @param s sector
     * @return  arreglo con las coordeandas x e y
     */
    
    public Double[] getRefPoint(int nr,int s){
        Double posRef[]={0.0,0.0};
        if(nr<16){
            posRef[0]=x0+dr+(lr+derX)*nr;            
            posRef[1]=y0+dr;
            if(s==2){
                posRef[1]+=aR/2+ai/2;
            }
        }else  if(nr<32){
            posRef[0]=x0+dr+(lr+derX)*(31-nr);
            posRef[1]=y0+dr+aR+derY;
            if(s==2){
                posRef[1]+=aR/2+ai/2;
            }
        }
        return posRef;
    }
    /**
     * Define en que reactor se encuentra la punta del equipo
     * @param x coordenada en el eje x
     * @param y coordenada en el eje y
     * @return un arreglo con el reactor y el sector del mismo
     * @throws Bounds en caso de que la punta no se encuentre en una zona permitida del equipo
     */
    public Integer[] getReactor(Double x,Double y)throws Bounds{
        int columna=99;
        Integer[] reactor={0,0};
        Double[] pref1;
        Double[] pref2;
        Double[] pmax1;
        Double[] pmax2;
        int rnum=99;
        if((y>y0)&&(y<y0+aR)){
            columna=0;
        }
        if((y>y0+aR+derY)&&(y<y0+2*aR+derY)){
            columna=1;
        }
        if(columna<2){
            rnum=(int)((x-x0-dr)/(lr+derX));
            rnum=31*columna+(1-2*columna)*rnum;
            pref1=getRefPoint(rnum, 1);
            pref2=getRefPoint(rnum, 2);
            pmax1=getMaxCord(rnum, 1);
            pmax2=getMaxCord(rnum, 2);
            if((x>=pref1[0]&&
                    y>=pref1[1]&&
                    x<=pmax1[0]&&
                    y<=pmax1[1])
                    ||
                    (x>=pref2[0]&&
                    y>=pref2[1]&&
                    x<=pmax2[0]&&
                    y<=pmax2[1])){
                int sec=1;
                if(y>pmax1[1])
                    sec=2;
                reactor[0]=rnum;
                reactor[1]=sec;
            }
            else{
                throw new Bounds("Fuera de Rango");
            }
        }else{
            throw new Bounds("Fuera de Rango");
        }
        return reactor;
    }
    /**
     * Calcula el punto maximo en x e y al que se puede llegar dentro de un sector de reactor
     * @param r reactor
     * @param s sector
     * @return arreglo con las coordenadas xmax e ymax
     */
    public Double[] getMaxCord(int r,int s){
        Double[] pref=getRefPoint(r, s);
        Double[] pmax={
            pref[0]+lr-2*dr,
            pref[1]+aR/2-ai/2-2*dr            
        };
        return pmax;
    }
    
}
