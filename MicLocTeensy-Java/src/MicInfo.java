import java.awt.Point;


public class MicInfo {
	
	public Point location;
	public int sample;
	public int value;
	
	public String toString() {
		return  "["+location.x+","+location.y+"] S: "+sample+ " V: "+value;
	}

}
