/***
 * @author Christian Tischer
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
public class Enlarge_all_ROIs implements PlugIn {
	
	String plugin_name = "Enlarge all ROIs";
	
	public void run(String arg) {
		
		IJ.log(" ");
		IJ.log(""+plugin_name+": started");
 	
		GenericDialog gd = new GenericDialog(plugin_name);
        	gd.addNumericField("number of pixels (negative for shrink): ", 0, 0);
      	        gd.showDialog();
        	if(gd.wasCanceled()) return;
        	int numpix = (int)gd.getNextNumber();

		enlargeAllRois(numpix);
		
    	}

	public void enlargeAllRois(int numpix) {
			
		RoiManager manager = RoiManager.getInstance();
		
		for (int i = 0; i < manager.getCount(); i++) {
			IJ.log(""+plugin_name+": enlarging ROI "+i);
			manager.select(i);
			IJ.run("Enlarge...", "enlarge="+numpix+" pixel");
			manager.runCommand("Update");
		} 
				
	}

}


