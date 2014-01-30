/***
 * @author Christian Tischer
 * class communicating with the LSM 780 VB macro
 * using a (modified) class for Windows Registry communication, written by Oleg Ryaboy
 */


import ij.*;
import ij.io.*;
import ij.plugin.*;
import ij.gui.GenericDialog;
import ij.measure.*;
import ij.plugin.frame.RoiManager;

//import java.io.IOException;
//import java.io.InputStream;
//import java.io.StringWriter;
//import java.io.File;

import java.io.*;
import java.lang.*;
import java.util.*;


public class Microscope_Communicator implements PlugIn {
	
	String plugin_name = "Communicator";
	String microscope = "LSM780";
	String winreg_location = "HKCU\\SOFTWARE\\VB and VBA Program Settings\\OnlineImageAnalysis\\macro";; 
	String winreg_separator = "REG_SZ";
	
	public void run(String arg) {
		
		IJ.log(" ");
		IJ.log(""+plugin_name+": started");
	
		
		String[] microscopes = {"File system","LSM780"};
		String[] actions = {"read status", "submit command", "obtain image", "save current image"};
		String[] commands = {"do nothing", "image selected particle", "image at x, y"};
		//static String action	
        	GenericDialog gd = new GenericDialog("Microscope Communication");
        	gd.addChoice("microscope: ", microscopes, microscopes[0]);
        	gd.addChoice("action: ", actions, actions[1]);
      	        gd.addChoice("command: ", commands, commands[0]);
      	        //gd.addNumericField("x: ", 0, 0);
      	        //gd.addNumericField("y: ", 0, 0);
  	        gd.showDialog();
        	if(gd.wasCanceled()) return;
        	microscope = (String)gd.getNextChoice();
        	String action = (String)gd.getNextChoice();
        	String command = (String)gd.getNextChoice();
        	//int offsetx = (int)gd.getNextNumber();
        	//int offsety = (int)gd.getNextNumber();
        	// todo: maybe get rid of the offset variables
		int offsetx = 0;
		int offsety = 0;
						
		IJ.log(""+plugin_name+": user values retrieved");
		IJ.log(""+plugin_name+": microscope = "+microscope);
		IJ.log(""+plugin_name+": action = "+action);

		if (microscope.equals("LSM780")) {
			winreg_location = "HKCU\\SOFTWARE\\VB and VBA Program Settings\\OnlineImageAnalysis\\macro";
		} 

		// action choice
		if (action.equals("obtain image")) {
			obtainImage(microscope);
		}
		else if (action.equals("submit command")) {
			writeToMacro(command, offsetx, offsety);
		}
     		else if (action.equals("read status")) {
			readFromMacro();
     		}
     		else if (action.equals("save current image")) {
			saveCurrentImage();     			
     		}

		IJ.log(""+plugin_name+": done");
		
    	}

	public void obtainImage(String microscope) {
		
		String path;
		
		if (microscope.equals("File system")) {
			
			OpenDialog od = new OpenDialog("Choose a file", null);  
        		String dir = od.getDirectory();  
        		if (null == dir) {
        			path=""; // dialog was canceled  
        		} else {
        			dir = dir.replace('\\', '/'); // Windows safe  
        			if (!dir.endsWith("/")) dir += "/";  
        			path = dir + od.getFileName();  
        		}
    
			
		} else {
		
			IJ.log(""+plugin_name+": obtaining image...waiting for microscope...");
			
			String code = "do nothing";
		
			while ( ! code.equals("1")) {
	 			code = getTrimmedRegistryValue("Code");
 				// todo: nicer update of waiting => update command in same line
 				//IJ.log("Comm_ZeissConfocal: current code value = "+code);
 				try {
 				   Thread.sleep(100);
				} catch(InterruptedException ex) {
    				   Thread.currentThread().interrupt();
				}
 			}
	 		IJ.log(""+plugin_name+": microscope responded.");
	 		path = getTrimmedRegistryValue("filepath");
			writeToMacro("wait", 0, 0);
		
		}	
 		
		IJ.log(""+plugin_name+": loading image from "+path);
 		ImagePlus imp = new ImagePlus(path);
		imp.show();
 		
	}

	public void writeToMacro(String command, int offsetx, int offsety) {
		int x=0;
		int y=0;
		
		if (command.equals("image selected particle")) {
			IJ.log(""+plugin_name+": measuring location of of selected particle...");
			// measure currently selected particle
			// todo: make sure the center of mass coordinates are selected
			RoiManager manager = RoiManager.getInstance();
			manager.runCommand("Measure");
			// get x,y coordinates
			ResultsTable rt = ResultsTable.getResultsTable();
			int lastRow = rt.getCounter()-1;
			x = (int)rt.getValueAsDouble(rt.getColumnIndex("XM"),lastRow);
			y = (int)rt.getValueAsDouble(rt.getColumnIndex("YM"),lastRow);
			IJ.log(""+plugin_name+": XM, YM ="+x+", "+y);
			rt.deleteRow(lastRow);
			ImagePlus imp = IJ.getImage();
			int w = (int)imp.getWidth();
			int h = (int)imp.getHeight();
			int dx = (int)-(x-w/2);
			int dy = (int)-(y-h/2);
			
			if(microscope.equals("LSM510")) {
				IJ.log(""+plugin_name+": LSM510 => inverting dy");
				dy = -dy;
			}
	
			//dx =-500;
			//dy =-500;
			
			IJ.log(""+plugin_name+": dx, dy ="+dx+", "+dy);
			
			WindowsRegistry.writeRegistry(winreg_location, "offsetx", ""+dx);
			WindowsRegistry.writeRegistry(winreg_location, "offsety", ""+dy);
			WindowsRegistry.writeRegistry(winreg_location, "orientation", ""+0);
			
			try {
 			   Thread.sleep(500);
			} catch(InterruptedException ex) {
    			   Thread.currentThread().interrupt();
			}
 			WindowsRegistry.writeRegistry(winreg_location, "Code", "5");
	  
			IJ.log(""+plugin_name+": wrote to microscope");
		}
		
		if (command.equals("do nothing")) {
			IJ.log(""+plugin_name+": no good object found");
			WindowsRegistry.writeRegistry(winreg_location, "Code", "2");
		}
	}

	public void readFromMacro() {
		
		String code = getTrimmedRegistryValue("Code");
		IJ.log(""+plugin_name+": read Code = "+ code);
						
	}


	// todo: move to Winreg class; parameter: winreg_separator
	public String getTrimmedRegistryValue(String key) {
		//IJ.log("reading from "+winreg_location);
		String temp1 = WindowsRegistry.readRegistry(winreg_location, key);
		//IJ.log("return="+temp1);
		String [] temp2 = temp1.split(winreg_separator); // extract only the value
		String value = temp2[1].trim(); // get rid of whitespaces
		return value;
	}

	public void saveCurrentImage() {
		IJ.log(""+plugin_name+": saving current image...");
		String path = getTrimmedRegistryValue("filepath");
		File f = new File(path);
    		IJ.log("Path-->" + f.getParent());
    		//IJ.log("file--->" + f.getName());       
    		//int idx = f.getName().lastIndexOf('.');
    		//IJ.log("extension--->" + ((idx > 0) ? f.getName().substring(idx) : "") );
		
		ImagePlus imp = IJ.getImage();
		//IJ.run(saveAs("Jpeg", "C:\\temp\\ERES-tmp.jpg");
		IJ.saveAs(imp, "Jpeg", f.getParent()+"\\"+f.getName()+"_"+imp.getTitle()+".jpeg"); 
		//path.split
		
	}

	public static String join(Collection s, String delimiter) {
    		StringBuffer buffer = new StringBuffer();
    		Iterator iter = s.iterator();
    		while (iter.hasNext()) {
        		buffer.append(iter.next());
		        if (iter.hasNext()) {
        	buffer.append(delimiter);
        }
        
    }
    return buffer.toString();
}


}



/**
 * @author Oleg Ryaboy, based on work by Miguel Enriquez 
 */
class WindowsRegistry {

    /**
     * 
     * @param location path in the registry
     * @param key registry key
     * @return registry value or null if not found
     */

    public static final String writeRegistry(String location, String key, String value){
    	try {
    		
    		String cmd = "reg add " + '"' + location + "\" /v " + key + " /d \"" + value  + '"' + " /f ";
    		//IJ.log(""+cmd);
    		Process process = Runtime.getRuntime().exec(cmd);
    		
            	StreamReader reader = new StreamReader(process.getInputStream());
            	reader.start();
            	process.waitFor();
            	reader.join();
            	String output = reader.getResult();
    		//IJ.log(""+output);
    		return "ok";
    	}
    	catch (Exception e) {
            return null;
        }
    }
    
    public static final String readRegistry(String location, String key){
        try {
            // Run reg query, then read output with StreamReader (internal class)
            String cmd = "reg query " + '"' + location + "\" /v " + key;
    	    //IJ.log(""+cmd);
            Process process = Runtime.getRuntime().exec(cmd);
    		
            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
            String output = reader.getResult();
            //IJ.log(""+output);
	    return output;	
	   	
	    // Output has the following format:
            // \n<Version information>\n\n<key>\t<registry type>\t<value>
            //if( !output.contains("\t")){
            //        return "WinReg Error: string does not contain tabs :-(";
            //}
            
            // Parse out the value
            //String[] parsed = output.split("\t");
            //return parsed[parsed.length-1];
        }
        catch (Exception e) {
            return "WinReg Error: something unknown went wrong :-(";
        }

    }

    static class StreamReader extends Thread {
        private InputStream is;
        private StringWriter sw= new StringWriter();

        public StreamReader(InputStream is) {
            this.is = is;
        }

        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1)
                    sw.write(c);
            }
            catch (IOException e) { 
        }
        }

        public String getResult() {
            return sw.toString();
        }
    }

	public static void checkImageJ(){
	       IJ.log("You successfully imported the WindowsRegistry java class to ImageJ.");
	}
    
   public static void main(String[] args) {

        // Sample usage
   	IJ.log("read: "+WindowsRegistry.readRegistry("HKLM\\SOFTWARE\\MyCo","Data"));
   	WindowsRegistry.writeRegistry("HKLM\\SOFTWARE\\MyCo","Data","suupi");
   	IJ.log("read: "+WindowsRegistry.readRegistry("HKLM\\SOFTWARE\\MyCo","Data"));
   	
   }
}