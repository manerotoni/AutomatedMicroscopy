/***
 * @author Christian Tischer
 * 
 * remove entries from Results Table and ROI Manager that do not fall within a selected (min, max) measurement range
 * 
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


public class Filter_particles implements PlugIn {
	
	String plugin_name = "Filter particles";
	
	public void run(String arg) {
		
		IJ.log(" ");
		IJ.log(""+plugin_name+": started.");
 		
		String[] colnames = get_colnames_of_ResultsTable();
		String[] methods = {"threshold"};
		
		//static String action	
        	GenericDialog gd = new GenericDialog("Filter particles");
        	gd.addChoice("filter method: ", methods, methods[0]);
        	gd.addChoice("measurement: ", colnames, colnames[0]);
        	gd.addNumericField("threshold_Min: ", 0, 0);
        	gd.addNumericField("threshold_Max: ", 255, 0);
      	        gd.showDialog();
        	if(gd.wasCanceled()) return;
        	String method = (String)gd.getNextChoice();
        	String colname = (String)gd.getNextChoice();
        	float th_min = (float)gd.getNextNumber();
        	float th_max = (float)gd.getNextNumber();

        	filterParticles(colname, method, th_min, th_max);
		
    	}

	public void filterParticles(String colname, String method, float th_min, float th_max) {
		
		int i;
		int iCol;
		double v;

		ResultsTable rt = ResultsTable.getResultsTable();
		
		iCol = rt.getColumnIndex(colname); if(iCol == -1) return;
		int n = rt.getCounter();

		RoiManager manager = RoiManager.getInstance();
		manager.runCommand("Deselect");
		manager.runCommand("Show None");
		ImagePlus imp = IJ.getImage();
		imp.hide();
		
		if (method.equals("threshold")) {
			for (i=n-1; i>=0; i--) {
				v = rt.getValue(colname,i);	
				if ( (v > th_max) || (v < th_min) ) {  // particle is no good
					manager.select(i); 
					manager.runCommand("Delete");
					rt.deleteRow(i);
				}
			}	
			rt.updateResults();
			rt.show("Results");		
		}
		imp.show();
		
		
	}

	private String[] get_colnames_of_ResultsTable() {
		
		IJ.log(""+plugin_name+": getting colnames of results table...");
		ResultsTable rt = ResultsTable.getResultsTable();
		String temp = (String)rt.getColumnHeadings().trim();
		String[] colnames = temp.split("\t");
		return colnames;
		
	}

}


