import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;

import javax.imageio.ImageIO;

public class MicLocTeensy31 {

	//MICLOC TEST CODE TO INTERFACE WITH THE TEENSY
	
	private CommPortIdentifier	portIdentifier;
	private SerialPort			serialPort;
	private BufferedReader		serialIn;
	private BufferedWriter		serialOut;

	public void serialConnect(String port) {

		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(port);
			if (portIdentifier.isCurrentlyOwned()) {
				System.err.println("Error: Port is currently in use");
				System.exit(-1);
			} else {
				CommPort commPort = portIdentifier.open("MicLoc", 2000);
				if (commPort instanceof SerialPort) {
					serialPort = (SerialPort) commPort;
					serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
					serialPort.disableReceiveTimeout();
					serialPort.setInputBufferSize(8192);
					serialIn = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
					serialOut = new BufferedWriter(new OutputStreamWriter(serialPort.getOutputStream()));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	public static void main(String[] args) {
		String fileprefix = "";
		int fpc = 0;
		
		MicLocTeensy31 micloc = new MicLocTeensy31();

		ArrayList<MicInfo> mics = new ArrayList<MicInfo>();
		MicInfo mic1 = new MicInfo();
		MicInfo mic2 = new MicInfo();
		MicInfo mic3 = new MicInfo();
		MicInfo mic4 = new MicInfo();

		//SETUP THE MIC COORDINATES
		mic1.location = new Point(0, 0);
		mic2.location = new Point(0, 239);
		mic3.location = new Point(239, 239);
		mic4.location = new Point(239, 0);

		mics.add(mic1);
		mics.add(mic2);
		mics.add(mic3);
		mics.add(mic4);

		double soundSpeed;

		LocationTable table = null;
		int scale = 10;

		micloc.serialConnect("/dev/ttyACM0");
		
		System.out.println("Sync...");
		try {
			micloc.serialOut.write("p");
			micloc.serialOut.flush();
			String aux = micloc.serialIn.readLine();
			System.out.println(aux);

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String tempData, micData, bufferData;
		int nSamples, timeSamplesUSec, threshold;
		double samplesPerUsec, temperature, humidity;

		while (true) {
			try {
				String fields[];

				// PARSE SAMPLE INFO AND TEMPERATURE DATA
				tempData = micloc.serialIn.readLine();
				System.out.print("|tempData|:\t" + tempData);
				fields = tempData.split(" ");
				timeSamplesUSec = Integer.parseInt(fields[1]);
				nSamples = Integer.parseInt(fields[3]);
				samplesPerUsec = Double.parseDouble(fields[5]);
				temperature = Double.parseDouble(fields[7]);
				humidity = Double.parseDouble(fields[9]);
				threshold = Integer.parseInt(fields[11]);
				
				//CALC SOUND SPEED
				soundSpeed = SoundUtils.soundSpeed(temperature, humidity);
				System.out.println(" SoundSpeed: "+soundSpeed);
				
				
				//PARSE BUFFER DATA
				bufferData = micloc.serialIn.readLine();
				System.out.println("|bufferData:|\t" + bufferData);
				System.out.flush();

				//PARSE SOUND BUFFER DATA
				fields = bufferData.split(" ");
				int dsize = Integer.parseInt(fields[1]);
				int event = Integer.parseInt(fields[3]) - (nSamples - dsize);

				int[] data1 = new int[dsize];
				int[] data2 = new int[dsize];
				int[] data3 = new int[dsize];
				int[] data4 = new int[dsize];

				String mic = micloc.serialIn.readLine();
				fields = mic.split(" ");
				System.out.println("Read " + fields.length);
				readData(fields, data1, nSamples - dsize);

				mic = micloc.serialIn.readLine();
				fields = mic.split(" ");
				System.out.println("Read " + fields.length);
				readData(fields, data2, nSamples - dsize);

				mic = micloc.serialIn.readLine();
				fields = mic.split(" ");
				System.out.println("Read " + fields.length);
				readData(fields, data3, nSamples - dsize);

				mic = micloc.serialIn.readLine();
				fields = mic.split(" ");
				System.out.println("Read " + fields.length);
				readData(fields, data4, nSamples - dsize);
				System.out.flush();


				
				//SIMPLE TEMP FILE PREFIX FOR IMAGES
				fpc++;
				fileprefix = "AA"+fpc;
				
				// SHOW RAW SIGNALS
				createImage(fileprefix,data1, data2, data3, data4, event, 0, 0, 0, 0);
				
				// SIGNAL TREATMENT
				// NOISE CUTTING, NORMALIZATION AND WEIGHTING
				
				SoundUtils.normalize(data1);
				SoundUtils.normalize(data2);
				SoundUtils.normalize(data3);
				SoundUtils.normalize(data4);
				
				//CROSSCORRELATE THE SIGNALS FOR DTOA 
				CrossCorrelation cc12 = new CrossCorrelation(data1,data2);
				CrossCorrelation cc13 = new CrossCorrelation(data1,data3);
				CrossCorrelation cc14 = new CrossCorrelation(data1,data4);
				
				System.out.println("\nPost normalization (MIC1 basis is sample 0)\nCC21@ "+cc12.maxindex);
				System.out.println("CC31@ "+cc13.maxindex);
				System.out.println("CC41@ "+cc14.maxindex);
					
				fpc++;
				fileprefix = "AA"+fpc;
				// SHOW POST NORMALIZATION SIGNALS
				createImage(fileprefix,data1, data2, data3, data4, event, event, event+cc12.maxindex, event+cc13.maxindex, event+cc14.maxindex);
					
				
				//LOCATE THE SOUND EVENT
				LocationTableEntry bestLte = new LocationTableEntry();
				bestLte = new LocationTableEntry();
				mic1.sample = 0;
				mic2.sample = cc12.maxindex;
				mic3.sample = cc13.maxindex;
				mic4.sample = cc14.maxindex;

				bestLte.calcTds(mics, samplesPerUsec);

				//TO DO Perform validation on DTOAs
				//double maxdiff = (mic1.location.distance(mic3.location) / soundSpeed) * 1.01;
				
				boolean valid = true;
				if (valid) {

					if (table == null) {
						System.out.println("Generating table...");
						table = new LocationTable(scale);
						table.generateTable(mics, soundSpeed, -4000 / scale, -4000 / scale, 8000 / scale, 8000 / scale);
						System.out.println("Done. Tablesize: " + table.table.size());
					}

					BufferedImage image = table.heatMap(bestLte, LocationTable.LOCMETHOD_2MICCROSSOVER, samplesPerUsec);
					System.out.println("Best probable location: "+table.bestLocation);
					try {
						fpc++;
						fileprefix = "AA"+fpc;
						File file = File.createTempFile(fileprefix+"-", ".png");
						String result = file.getAbsolutePath();
						ImageIO.write(image, "PNG", file);
						Runtime.getRuntime().exec("eog -f -w " + result);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				} else {
					System.out.println("Invalid sample.");
				}
				
			} catch (Exception e) {

				e.printStackTrace();
				// ERROR OCURRED, TRY TO RESYNC
				// THIS MUST BE SOLVED IN TEENSY AND HERE, NEEDS BETTER COMMUNICATIONS CODE.
				// SOME FLOW CONTROL SHOULD BE IMPLEMENTED
				System.out.println("RESync...");
				try {
					Thread.sleep(200);
					micloc.serialOut.write("p");
					micloc.serialOut.flush();
					String aux = micloc.serialIn.readLine();
					System.out.println(aux);
					System.out.println("Done...");
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}


	private static void readData(String[] fields, int[] data1, int i) {
		int k = 0;
		for (int j = i; k < data1.length; j++, k++) {
			if (j >= fields.length)
				j = 0;
			data1[k] = Integer.parseInt(fields[j], 16) - 128;
		}
	}

	private static void createImage(String fileprefix,int data1[], int data2[], int data3[], int data4[], int event, int sample1, int sample2, int sample3, int sample4) {
		int dsize = data1.length;
		BufferedImage image1 = new BufferedImage(dsize, 256 * 4, BufferedImage.TYPE_INT_RGB);
		image1.createGraphics();
		Graphics2D g2d = (Graphics2D) image1.getGraphics();
		g2d.setColor(Color.WHITE);
		g2d.drawRect(0, 0, dsize - 1, 256 * 4 - 1);
		g2d.setColor(Color.GRAY);
		g2d.drawLine(event, 0, event, 256 * 4 - 1);
		g2d.setColor(Color.RED);
		g2d.drawLine(sample1, 0, sample1, 256 * 4 - 1);
		g2d.setColor(Color.GREEN);
		g2d.drawLine(sample2, 0, sample2, 256 * 4 - 1);
		g2d.setColor(Color.BLUE);
		g2d.drawLine(sample3, 0, sample3, 256 * 4 - 1);
		g2d.setColor(Color.YELLOW);
		g2d.drawLine(sample4, 0, sample4, 256 * 4 - 1);
		g2d.setColor(Color.GRAY);
		g2d.drawLine(0, 128, dsize, 128);
		g2d.drawLine(0, 128 + 256, dsize, 128 + 256);
		g2d.drawLine(0, 128 + 256 * 2, dsize, 128 + 256 * 2);
		g2d.drawLine(0, 128 + 256 * 3, dsize, 128 + 256 * 3);
		g2d.setColor(Color.WHITE);

		for (int i = 0; i < data1.length; i++) {
			if (i > 0) {
				g2d.setColor(Color.RED);
				g2d.drawLine(i - 1, -data1[i - 1] + 128, i, -data1[i] + 128);
				g2d.setColor(Color.GREEN);
				g2d.drawLine(i - 1, -data2[i - 1] + 128 + 256, i, -data2[i] + 128 + 256);
				g2d.setColor(Color.BLUE);
				g2d.drawLine(i - 1, -data3[i - 1] + 128 + 256 + 256, i, -data3[i] + 128 + 256 + 256);
				g2d.setColor(Color.YELLOW);
				g2d.drawLine(i - 1, -data4[i - 1] + 128 + 256 + 256 + 256, i, -data4[i] + 128 + 256 + 256 + 256);
			}
		}
		File file1;
		try {
			file1 = File.createTempFile(fileprefix+"-", ".png");
			String result1 = file1.getAbsolutePath();
			ImageIO.write(image1, "PNG", file1);
			Runtime.getRuntime().exec("eog -f -w " + result1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	

}
