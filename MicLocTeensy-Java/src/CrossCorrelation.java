//THIS CLASS PERFORMS THE CROSSCORRELATION BETWEEN TWO DATA ARRAYS
//IT ALSO DOES TIME-DELAY ANALYSIS FOR POSITIVE CORRELATED SIGNALS, SAVING THE MAXIMUM VALUE AND OFFSET
//SHOULD BE OPTIMIZED,FOR EXAMPLE, VIA FFT CROSSCORRELATION METHODS

public class CrossCorrelation {

	public double table[];
	public double max;
	public int maxindex;
	
	public CrossCorrelation (int[] data1, int[] data2) {
		
		table = new double[data2.length*2];
		max = Double.MIN_VALUE;
		maxindex=-1;
		
		double average1 = 0;
		double average2 = 0;
		for (int j = 0; j < data1.length; j++) {
			average1 += data1[j];
		}
		for (int j = 0; j < data2.length; j++) {
			average2 += data2[j];
		}
		average1 /= 1.0*data1.length;
		average2 /= 1.0*data2.length;
		int c=0;
		for (int j = -data2.length; j < data2.length;j++) {
			for (int i = 0; i < data1.length; i++) {
				if ((i+j < data2.length) && (i+j >= 0)) {
					table[c] += ((data1[i]-average1) * (data2[i+j]-average2));
				}
			}
			if (table[c] > max) {
				max = table[c];
				maxindex = c-data2.length;
			}
			c++;
		}
	}
}
