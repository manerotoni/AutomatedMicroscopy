/***
 * @author Christian Tischer
 * 
 * adds a column to the Results Table with the distances to the image center (based on XM and YM measurements in the Results Table)
 * Note: this requires the entries XM and YM to be present in the Results Table!
 */


import ij.*;
import ij.plugin.*;
import ij.measure.*;
import ij.plugin.frame.RoiManager;
import ij.gui.GenericDialog;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.*;
import java.util.*;

public class Measure_image_center_distance implements PlugIn {
	
	String plugin_name = "Measure image center distance";
	
	public void run(String arg) {
		
		IJ.log(" ");
		IJ.log(""+plugin_name+": started");
 		measure_imCenter_dist();
			
    	}

	public void measure_imCenter_dist() {
		double minDist,dx2,dy2;
	
		ResultsTable rt = ResultsTable.getResultsTable();	
		float[] x = (float[])rt.getColumn(rt.getColumnIndex("XM"));
		float[] y = (float[])rt.getColumn(rt.getColumnIndex("YM"));
		
		ImagePlus imp = IJ.getImage();
		int w = imp.getWidth();
		int h = imp.getHeight();
     
		
		minDist = Float.MAX_VALUE;	
		for (int i = 0; i < x.length; i++) {
			dx2 = Math.pow(x[i]-w/2,2);
			dy2 = Math.pow(y[i]-h/2,2);
			rt.setValue("imCenter_distance", i, Math.sqrt(dx2+dy2));
		}
	
		rt.updateResults();
		rt.show("Results");
	
	}
	
	
}


