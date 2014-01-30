/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  ImageJ macro:
//  Detection and filtering of spot-like structures in fluorescence images
//  The code works with single input image files or can automatically retrieve images from a VB Macro controlling Zeiss 780 confocal microscope
//  In addition one can batch analyse all .lsm files in one folder.
//
//  Author: Christian Tischer; e-mail: tischitischer@gmail.com
//  Date: 25.Januar 2014
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



// Set-up of parameters via a GUI

imageSources = newArray("LSM780 VB Macro","Single File","All .lsm files in one directory"); 

Dialog.create("Online Spot detection");
Dialog.addChoice("Image source:",imageSources);
Dialog.addCheckbox("Save documentation images", true);
Dialog.addNumber("Spot area MIN (pixels)", 5);
Dialog.addNumber("Spot area MAX (pixels)", 20);
Dialog.addNumber("Next spot distance MIN (pixels)", 15);
Dialog.addNumber("Next spot distance MAX (pixels)", 200);
Dialog.addNumber("Spot intensity MIN", 120);
Dialog.addNumber("Spot intensity MAX", 230);
Dialog.addNumber("Spot distance to image center MIN (pixels)", 0);
Dialog.addNumber("Spot distance to image center MAX", 220);
Dialog.addNumber("Spot neighborhood intensity MIN", 5);
Dialog.addNumber("Spot neighborhood intensity MAX", 100);
Dialog.show;

imageSource = Dialog.getChoice();
saveDocu =  Dialog.getCheckbox();
eres_size_min = Dialog.getNumber();
eres_size_max = Dialog.getNumber();
eres_dist_min =  Dialog.getNumber();
eres_dist_max =  Dialog.getNumber();
eres_intens_min = Dialog.getNumber();
eres_intens_max = Dialog.getNumber();
eres_imCenterDist_min = Dialog.getNumber();
eres_imCenterDist_max = Dialog.getNumber();
eres_nb_min = Dialog.getNumber();
eres_nb_max = Dialog.getNumber();

// Handle file input

if(imageSource == "Single File") {
	
	// choose 
	path = File.openDialog("Choose image file...") ;

	//open
	run("Close all forced");
	open(path);

	// analyse
	findBestSpot(eres_size_min, eres_size_max, eres_dist_min, eres_dist_max, eres_intens_min, eres_intens_max, eres_imCenterDist_min, eres_imCenterDist_max, eres_nb_min, eres_nb_max);
	
	// save documentation image
	if(saveDocu) {
		saveDocumentationImage(path);
	}
		
}


if(imageSource == "All .lsm files in one directory") {
	dir = getDirectory("");
	list = getFileList(dir);
	for (i = 0; i < list.length; i++) {
		if (endsWith(list[i], ".lsm")) {
			run("Close all forced");
			path = dir+list[i];
			print(path);
			open(path);
			findBestSpot(eres_size_min, eres_size_max, eres_dist_min, eres_dist_max, eres_intens_min, eres_intens_max, eres_imCenterDist_min, eres_imCenterDist_max, eres_nb_min, eres_nb_max);
			if(saveDocu) {
				saveDocumentationImage(path);
			}
		}
	}
	run("Close all forced");
}


if(imageSource == "LSM780 VB Macro") {
	do {
		
		run("Close all forced");
	
		run("Microscope Communicator", "microscope=[LSM780] action=[obtain image] command=[do nothing]");
		path = getInfo("image.directory")+File.separator+getInfo("image.filename");
		findBestSpot(eres_size_min, eres_size_max, eres_dist_min, eres_dist_max, eres_intens_min, eres_intens_max, eres_imCenterDist_min, eres_imCenterDist_max, eres_nb_min, eres_nb_max);
		wait(100);
		
		
		// report result back to microscope
		if (selectionType() > -1) {
			run("Microscope Communicator", "microscope=[LSM780] action=[submit command] command=[image selected particle] object_x=0 object_y=0");
		} else {
			run("Microscope Communicator", "microscope=[LSM780] action=[submit command] command=[do nothing] object_x=0 object_y=0");
		}

		// save documentation image
		if(saveDocu) {
			saveDocumentationImage(path);
		}
		wait(500);
		
		
	} while (1);

}

function saveDocumentationImage(path) {

	selectWindow("raw");
	if (selectionType() > -1) {	
		// mark best ERES in the image
		run("Enlarge...", "enlarge=5");
		setForegroundColor(255, 255, 255); run("Line Width...", "line=1"); run("Draw");
	} 

	//run("Make Montage...", "columns=2 rows=1 scale=1 first=1 last=2 increment=1 border=0 font=12");
	saveAs("PNG",path+"--docu.png");

}

// the following function contains the actual code to automatically find the spots

function findBestSpot(eres_size_min, eres_size_max, eres_dist_min, eres_dist_max, eres_intens_min, eres_intens_max, eres_imCenterDist_min, eres_imCenterDist_max, eres_nb_min, eres_nb_max) {
	
	
	// Set image properties
	run("Properties...", "unit=pix pixel_width=1 pixel_height=1 voxel_depth=1 origin=0,0");
	rename("raw"); 
	selectWindow("raw"); run("Duplicate...", "title=eres");
	
	// smooth 
	selectWindow("eres");
	run("Duplicate...", "title=gs");
	run("Gaussian Blur...", "sigma=0.5 scaled");
	
	// threshold 
	selectWindow("gs"); run("Duplicate...", "title=gs_bw");
	local_threshold_radius = 10*sqrt(eres_size_max/PI);
	local_threshold_signalToNoise = 5;
	run("Auto Local Threshold", "method=Niblack radius="+local_threshold_radius+" parameter_1="+local_threshold_signalToNoise+" parameter_2=0 white");
	run("Convert to Mask");
	
	// segment ERES
	run("Analyze Particles...", "size="+eres_size_min+"-"+eres_size_max+" pixel circularity=0.00-1.00 show=Masks exclude clear add");
	rename("ERES_Masks");
	
	//  eres intensity and center 
	selectWindow("gs");
	run("Set Measurements...", "min center area redirect=None decimal=2");
	roiManager("Deselect"); run("Clear Results"); roiManager("Measure");
	
	// FILTER: minimal neighbor distance
	run("Measure nearest neighbour distance");
	run("Filter particles", "filter=threshold measurement=nn_distance threshold_min="+eres_dist_min+" threshold_max="+eres_dist_max);
	
	// FILTER: intensities
	run("Filter particles", "filter=threshold measurement=Max threshold_min="+eres_intens_min+" threshold_max="+eres_intens_max);
	
	// FILTER: minimal image center distance
	run("Measure image center distance");
	run("Filter particles", "filter=threshold measurement=imCenter_distance threshold_min="+eres_imCenterDist_min+" threshold_max="+eres_imCenterDist_max);
	
	// FILTER for unidentified objects in neighbourhood
	// in this step the local intensity in a neighborhood of the identified ERES is measured
	// if this intensity is too high the ERES are filtered, because this probably indicates other objects in the vicinity of the ERES that could interfere with the FRAP epxeriment
	selectWindow("ERES_Masks"); run("Dilate"); run("Dilate"); run("Invert"); run("Divide...", "value=255");
	imageCalculator("Multiply create", "gs","ERES_Masks");
	run("Gray Morphology", "radius="+eres_dist_min+" type=circle operator=[fast dilate]");
	rename("ERES_Neighborhood");
	roiManager("Deselect"); run("Clear Results"); roiManager("Measure");
	run("Filter particles", "filter=threshold measurement=Max threshold_min="+eres_nb_min+" threshold_max="+eres_nb_max);
		
	// Measure ERES intensity and center
	selectWindow("gs");
	run("Set Measurements...", "min center area redirect=None decimal=2");
	roiManager("Deselect"); run("Clear Results"); roiManager("Measure");

	// Select best ERES (here we chose a random one of all that "survived" the different filters
	selectWindow("raw");   
	run("Select None"); run("Select best particle", "filter=random measurement=Max threshold_min=0 threshold_max=0");

	// for testing only: add a fixed particle ROI
	//makeOval(112, 95, 20, 15);
	//roiManager("Add");
	
		
}

