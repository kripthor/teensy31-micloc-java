import java.awt.Point;
import java.util.ArrayList;


public class LocationTableEntry {

	Point p;
	
	public ArrayList<Double> dMics;
	public ArrayList<Double> tMics;
	public ArrayList<Double> tdMics;
	
	
	public LocationTableEntry() {

	}

	public LocationTableEntry(int x, int y) {
		p = new Point(x,y);
	}

	
	public String toString() {
		return "["+p.x+","+p.y+"]";
	}

	public void calc(ArrayList<MicInfo> mics, double soundSpeed) {
		dMics = new ArrayList<Double>();
		tMics = new ArrayList<Double>();
		tdMics = new ArrayList<Double>();
		
		for (int k = 0; k < mics.size();k++) {
			double dmic = (p.distance(mics.get(k).location));
			double tmic = dmic/soundSpeed;
			dMics.add(dmic);
			tMics.add(tmic);
		}
		
		for (int k = 0; k < tMics.size()-1;k++) {
			for (int kk = k+1; kk < tMics.size();kk++) {
				tdMics.add(tMics.get(kk)-tMics.get(k));
			}
		}
	}
	
	public void calcTds(ArrayList<MicInfo> mics, double samplesPerUSec) {
		dMics = new ArrayList<Double>();
		tMics = new ArrayList<Double>();
		tdMics = new ArrayList<Double>();
		
		for (int k = 0; k < mics.size();k++) {
			double tmic = mics.get(k).sample/samplesPerUSec;
			tMics.add(tmic);
		}
		
		for (int k = 0; k < tMics.size()-1;k++) {
			for (int kk = k+1; kk < tMics.size();kk++) {
				tdMics.add(tMics.get(kk)-tMics.get(k));
			}
		}
	}
}
