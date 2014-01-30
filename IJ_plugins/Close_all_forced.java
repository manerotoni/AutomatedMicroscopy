/***
 * @author Christian Tischer
 * class for closing all open windows and resetting the ROI manager
 */

import ij.*;
import ij.plugin.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.measure.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.frame.RoiManager;

// todo rename to "select_best_particle"
public class Close_all_forced implements PlugIn {
	
	String plugin_name = "Close all forced";

	// todo: make a GUI for also clearing the ROI manager and whether you want to force closing
	public void run(String arg) {
		
		IJ.log(" ");
		IJ.log(""+plugin_name+": started");
 	
		int[] list = WindowManager.getIDList();
		if (list==null)
			return;
		
		for (int i=0; i<list.length; i++) {
			//IJ.log(""+i);
			ImagePlus img = WindowManager.getImage(list[i]);
			if (img!=null) {
				img.changes = false;
				ImageWindow win = img.getWindow();
				if (win!=null)
					win.close();
			}
		}
		
		if (RoiManager.getInstance() != null)  RoiManager.getInstance().runCommand("reset");
	
		
    	}

}


