/***
 * @author Christian Tischer
 * 
 * adds a column to the Results Table containg the nearest neighbor distances based on the XM and YM entries in the Results Table
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

// todo rename to "select_best_particle"
public class Measure_nearest_neighbour_distance implements PlugIn {
	
	String plugin_name = "Measure nearest neighbour distance";
	
	public void run(String arg) {
		
		IJ.log(" ");
		IJ.log(""+plugin_name+": started");
 		
		measure_nn_dist();	
    	}

	public void measure_nn_dist() {
		double minDist,dx2,dy2;
			
		ResultsTable rt = ResultsTable.getResultsTable();	
		float[] x = (float[])rt.getColumn(rt.getColumnIndex("XM"));
		float[] y = (float[])rt.getColumn(rt.getColumnIndex("YM"));
		
		for (int i = 0; i < x.length; i++) {
			minDist = Float.MAX_VALUE;
			for (int j = 0; j < x.length; j++) {
				if (i!=j) {
					//IJ.log("i"+i+" j"+j);
			
					dx2 = Math.pow(x[i]-x[j],2);
					dy2 = Math.pow(y[i]-y[j],2);
					if (Math.sqrt(dx2+dy2) < minDist) {
						minDist = Math.sqrt(dx2+dy2);
					}		
				}
			}
		
			rt.setValue("nn_distance", i, minDist);
			
		}
	
	rt.updateResults();
	rt.show("Results");
	
	}
	
	
}


