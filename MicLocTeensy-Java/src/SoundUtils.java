
public class SoundUtils {

	public static double soundSpeed(double T, double Rh){
		// Atmosferic pressure does not influence soundspeed much, use sea-level pressure
		return soundSpeed(T,101.325,Rh);
	}
	
	//Based on algoritm found in http://resource.npl.co.uk/acoustics/techguides/speedair/
	public static double soundSpeed(double T,double P, double Rh){
	    double C;		// speed
	    double Xc, Xw;		// Mole fraction of carbon dioxide and water vapour respectively
	    double H;			// molecular concentration of water vapour

	    double C1;		// Intermediate calculations
	    double C2;
	    double C3;

	    double ENH;
	    double PSV;
	    double PSV1;
	    double PSV2;

	    double T_kel; 		// ambient temperature (Kelvin)

	    double Kelvin = 273.15;	//For converting to Kelvin

		P = P*1000.0;
	    T_kel = Kelvin + T;      //Measured ambient temp

		//Molecular concentration of water vapour calculated from Rh
		//using Giacomos method by Davis (1991) as implemented in DTU report 11b-1997
		ENH = 3.14*Math.pow(10,-8)*P + 1.00062 + Math.pow(T,2)*5.6*Math.pow(10,-7);
		//These commented lines correspond to values used in Cramer (Appendix)
		//PSV1 = sqr(T_kel)*1.2811805*Math.pow(10,-5)-1.9509874*Math.pow(10,-2)*T_kel ;
		//PSV2 = 34.04926034-6.3536311*Math.pow(10,3)/T_kel;	
		PSV1 = Math.pow(T_kel,2)*1.2378847*Math.pow(10,-5)-1.9121316*Math.pow(10,-2)*T_kel;
		PSV2 = 33.93711047-6.3431645*Math.pow(10,3)/T_kel;
		PSV = Math.pow(Math.E,PSV1)*Math.pow(Math.E,PSV2);
		H = Rh*ENH*PSV/P;
		Xw = H/100.0;
		//Xc = 314.0*Math.pow(10,-6);
		Xc = 400.0*Math.pow(10,-6);

	    //Speed calculated using the method of Cramer from
		//JASA vol 93 pg 2510
		C1 = 0.603055*T + 331.5024 - Math.pow(T,2)*5.28*Math.pow(10,-4) + (0.1495874*T + 51.471935 - Math.pow(T,2)*7.82*Math.pow(10,-4))*Xw;
		C2 = (-1.82*Math.pow(10,-7)+3.73*Math.pow(10,-8)*T- Math.pow(T,2)*2.93*Math.pow(10,-10))*P+(-85.20931-0.228525*T+ Math.pow(T,2)*5.91*Math.pow(10,-5))*Xc;
		C3 =  Math.pow(Xw,2)*2.835149 +  Math.pow(P,2)*2.15*Math.pow(10,-13) -  Math.pow(Xc,2)*29.179762 - 4.86*Math.pow(10,-4)*Xw*P*Xc;
		C = C1 + C2 - C3;
		return C;
		}	


	//NORMALIZATION, NOISE REDUCTION AND WEIGHTING 
	public static void normalize(int[] data) {
		int max = 0;
		int targetMax = 128;
		int firstMax = -1;
		
		// FIND MAX VALUE
		for (int i = 0; i < data.length; i++) {
			int abs = Math.abs(data[i]);
			if (abs > max)
				max = abs;
		}
		double maxReduce = 1 - targetMax / (double) max;

		for (int i = 0; i < data.length; i++) {
			int abs = Math.abs(data[i]);
			
			// NOISE CUTTING
			//if (abs <= targetMax / 6) data[i] = 0;
			
			// NORMALIZATION
			double factor = (maxReduce * abs / (double) max);
			int dat = (int) ((1 - factor) * data[i]);
			int absdat = Math.abs(dat);
			if (absdat <= targetMax) {
				data[i] = dat;
			}
			
			// NOISE REDUCTION
			if (absdat < targetMax / 2) data[i] = (int) (data[i] * absdat / (targetMax / 2.0));
			
			
			// WEIGHTING FROM FIRST MAXIMUM
			if (absdat >= (int)(targetMax*0.75)  && firstMax == -1) {
				firstMax = i;
			}
			if (firstMax >= 0) {
				data[i] = (int) (data[i] * (1-Math.min(((i-firstMax)*1.0/(data.length/3)),1)));
			}

		}
	}

	
	//SOUND DETECTION, FIRST ATTEMPT. NOT SO GOOD RESULTS WITH NOISE, USE CrossCorrelation CLASS INSTEAD
	private static int detectSound(int[] data, int event) {
		int start = event - 512;
		int lookAhead = 25;
		double average;
		double variance;

		boolean found = false;
		
		if (start < 0)
			start = 0;
		int i;
		for (i = 0; i < data.length - lookAhead; i++) {
			average = 0;
			variance = 0;
			for (int j = i; j < i + lookAhead; j++) {
				average += data[j];
			}
			average /= lookAhead;
			for (int j = i; j < i + lookAhead; j++) {
				variance += Math.pow((average - data[j]), 2);
			}
			if (variance > 8000 && data[i] != 0) {
				found = true;
				break;
			}
		}
		if (found) {
			//go back until silence
			for (int k=i;k>0;k--) {
				if (data[k] == 0) return k;
			}
		}
		return -1;
	}

	
}
