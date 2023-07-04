package Astrocytes_InOut_Vessels_Tools;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import ij.plugin.RGBStackMerge;
import ij.process.AutoThresholder;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.ImageIcon;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom2.BoundingBox;
import mcib3d.geom2.Object3DComputation;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Object3DPlane;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.VoxelInt;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageLabeller;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import org.apache.commons.io.FilenameUtils;



/**
 * @author ORION-CIRB
 */
public class Tools {
    
    private final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));
    private final String urlHelp = "https://github.com/orion-cirb/Astrocytes_InOut_Vessels.git";
    private CLIJ2 clij2 = CLIJ2.getInstance();
    
    public String[] channelNames = {"Vessels", "Microglia", "Astrocytes"};
    public Calibration cal;
    public double pixVol;
    
    // Microglia
    private String microThMethod = "Moments";

    // Vessels
    private String vesselThMethod = "Triangle";
    private double minVesselVol = 100;
    private int dilVessel = 2;
    
    // Astrocytes
    private String astroThMethod = "Li";
    private double minAstroVol = 0.2;

    
    /**
     * Display a message in the ImageJ console and status bar
     */
    public void print(String log) {
        System.out.println(log);
        IJ.showStatus(log);
    }
    
    
    /**
     * Check that needed modules are installed
     */
    public boolean checkInstalledModules() {
        ClassLoader loader = IJ.getClassLoader();
        try {
            loader.loadClass("net.haesleinhuepf.clij2.CLIJ2");
        } catch (ClassNotFoundException e) {
            IJ.showMessage("Error", "CLIJ not installed, please install from update site");
            return false;
        }
        try {
            loader.loadClass("mcib3d.geom2.Object3DInt");
        } catch (ClassNotFoundException e) {
            IJ.showMessage("Error", "3D ImageJ Suite not installed, please install from update site");
            return false;
        }
        return true;
    }
    
    
    /**
     * Find images extension
     */
    public String findImageType(File imagesFolder) {
        String ext = "";
        String[] files = imagesFolder.list();
        for (String name : files) {
            String fileExt = FilenameUtils.getExtension(name);
            switch (fileExt) {
               case "nd" :
                   ext = fileExt;
                   break;
                case "czi" :
                   ext = fileExt;
                   break;
                case "lif"  :
                    ext = fileExt;
                    break;
                case "ics" :
                    ext = fileExt;
                    break;
                case "ics2" :
                    ext = fileExt;
                    break;
                case "lsm" :
                    ext = fileExt;
                    break;
                case "tif" :
                    ext = fileExt;
                    break;
                case "tiff" :
                    ext = fileExt;
                    break;
            }
        }
        return(ext);
    }
    
    
    /**
     * Find images in folder
     */
    public ArrayList<String> findImages(String imagesFolder, String imageExt) {
        File inDir = new File(imagesFolder);
        String[] files = inDir.list();
        if (files == null) {
            System.out.println("No image found in "+imagesFolder);
            return null;
        }
        ArrayList<String> images = new ArrayList();
        for (String f : files) {
            // Find images with extension
            String fileExt = FilenameUtils.getExtension(f);
            if (fileExt.equals(imageExt))
                images.add(imagesFolder + File.separator + f);
        }
        Collections.sort(images);
        return(images);
    }
    
    
    /**
     * Find image calibration
     */
    public void findImageCalib(IMetadata meta) {
        cal = new Calibration();
        cal.pixelWidth = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
        cal.pixelHeight = cal.pixelWidth;
        if (meta.getPixelsPhysicalSizeZ(0) != null)
            cal.pixelDepth = meta.getPixelsPhysicalSizeZ(0).value().doubleValue();
        else
            cal.pixelDepth = 1;
        cal.setUnit("microns");
        System.out.println("XY calibration = " + cal.pixelWidth + ", Z calibration = " + cal.pixelDepth);
    }
    
    
    /**
     * Find channels name
     * @throws loci.common.services.DependencyException
     * @throws loci.common.services.ServiceException
     * @throws loci.formats.FormatException
     * @throws java.io.IOException
     */
    public String[] findChannels (String imageName, IMetadata meta, ImageProcessorReader reader) throws loci.common.services.DependencyException, ServiceException, FormatException, IOException {
        int chs = reader.getSizeC();
        String[] channels = new String[chs];
        String imageExt =  FilenameUtils.getExtension(imageName);
        switch (imageExt) {
            case "nd" :
                for (int n = 0; n < chs; n++) 
                {
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelName(0, n);
                }
                break;
            case "nd2" :
                for (int n = 0; n < chs; n++) 
                {
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelName(0, n);
                }
                break;
            case "lif" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null || meta.getChannelName(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelName(0, n);
                break;
            case "czi" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelFluor(0, n);
                break;
            case "ics" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelExcitationWavelength(0, n).value().toString();
                break;
            case "ics2" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelExcitationWavelength(0, n).value().toString();
                break;   
            default :
                for (int n = 0; n < chs; n++)
                    channels[n] = Integer.toString(n);
        }
        return(channels);         
    }
    
    
    /**
     * Generate dialog box
     */
    public String[] dialog(String[] chs) { 
        GenericDialogPlus gd = new GenericDialogPlus("Parameters");
        gd.setInsets​(0, 100, 0);
        gd.addImage(icon);
        
        gd.addMessage("Channels", Font.getFont("Monospace"), Color.blue);
        int index = 0;
        for (String chNames : channelNames) {
            gd.addChoice(chNames+" : ", chs, chs[index]);
            index++;
        }
        
        String[] thMethods = AutoThresholder.getMethods();
        gd.addMessage("Vessels detection", Font.getFont("Monospace"), Color.blue);
        gd.addChoice("Threshold method: ",thMethods, vesselThMethod);
        gd.addNumericField("Min vessel volume (µm3): ", minVesselVol);
        gd.addNumericField("Vessel dilation (µm): ", dilVessel);
        
        gd.addMessage("Astrocytes detection", Font.getFont("Monospace"), Color.blue);
        gd.addChoice("Threshold method: ",thMethods, astroThMethod);
        gd.addNumericField("Min astrocytic object volume (µm3): ", minAstroVol);
        
        gd.addMessage("Image calibration", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("XY pixel size (µm): ", cal.pixelWidth);
        gd.addNumericField("Z pixel size (µm): ", cal.pixelDepth);
        gd.addHelp(urlHelp);
        gd.showDialog();
        
        String[] chChoices = new String[channelNames.length];
        for (int n = 0; n < chChoices.length; n++) 
            chChoices[n] = gd.getNextChoice();
        
        vesselThMethod = gd.getNextChoice();
        minVesselVol = gd.getNextNumber();
        dilVessel = (int) gd.getNextNumber();
        
        astroThMethod = gd.getNextChoice();
        minAstroVol = gd.getNextNumber();
        
        cal.pixelWidth = cal.pixelHeight = gd.getNextNumber();
        cal.pixelDepth = gd.getNextNumber();
        pixVol = cal.pixelWidth*cal.pixelWidth*cal.pixelDepth;
        
        if (gd.wasCanceled())
            chChoices = null; 
        
        return(chChoices);
    }
    
    
    /**
     * Flush and close an image
     */
    public void flushCloseImg(ImagePlus img) {
        img.flush();
        img.close();
    }
    
    
    /**
     * Find population of vessels
     */
    public Objects3DIntPopulation findVessels(ImagePlus imgVessel, ImagePlus imgMicro, ArrayList<Roi> rois) {
        // Detect microglia
        ImagePlus imgMicroMed = new Duplicator().run(imgMicro);
        IJ.run(imgMicroMed, "Median...", "radius=8 stack");
        ImagePlus imgMicroBin = threshold(imgMicroMed, microThMethod);
        Objects3DIntPopulation microgliaPop = new Objects3DIntPopulation(ImageHandler.wrap(imgMicroBin));
        
        // Detect vessels
        ImagePlus imgVesselLOG = imgVessel.duplicate();
        IJ.run(imgVesselLOG, "Laplacian of Gaussian", "sigma=14 scale_normalised negate stack");
        ImagePlus imgVesselBin = threshold(imgVesselLOG, vesselThMethod);
        imgVesselBin.setCalibration(cal);
               
        // Fill ROIs in black in vessels image
        if (!rois.isEmpty())
            fillImg(imgVesselBin, rois);
        
        // Fill microglia in black in vessels image
        ImageHandler imgVesselBinH = ImageHandler.wrap(new Duplicator().run(imgVesselBin));
        for (Object3DInt microglia: microgliaPop.getObjects3DInt()) 
            microglia.drawObject(imgVesselBinH, 0);
        
        // Vessels size filtering
        Objects3DIntPopulation vesselsPop = getPopFromImage(imgVesselBinH.getImagePlus());
        System.out.println("Nb vessels detected:"+vesselsPop.getNbObjects());
        popFilterSize(vesselsPop, minVesselVol, Double.MAX_VALUE);
        System.out.println("Nb vessels remaining after size filtering: "+ vesselsPop.getNbObjects());
        
        flushCloseImg(imgMicroMed);
        flushCloseImg(imgMicroBin);
        flushCloseImg(imgVesselBinH.getImagePlus());
        flushCloseImg(imgVesselLOG);
        flushCloseImg(imgVesselBin);
        return(vesselsPop);
    }
    
    
    /**
     * Threshold using CLIJ2
     */
    public ImagePlus threshold(ImagePlus img, String thMed) {
        ClearCLBuffer imgCL = clij2.push(img);
        ClearCLBuffer imgCLBin = clij2.create(imgCL);
        clij2.automaticThreshold(imgCL, imgCLBin, thMed);
        ImagePlus imgBin = clij2.pull(imgCLBin);
        clij2.release(imgCL);
        clij2.release(imgCLBin);
        return(imgBin);
    }
    
        
    /**
     * Fill ROIs in black in image
     */
    private ImagePlus fillImg(ImagePlus img, ArrayList<Roi> rois) {
        img.getProcessor().setColor(Color.BLACK);
        for (int s = 1; s <= img.getNSlices(); s++) {
            img.setSlice(s);
            for (Roi r : rois) {
                img.setRoi(r);
                img.getProcessor().fill(img.getRoi());
            }
        }
        img.deleteRoi();
        return(img);
    } 
    
    
    /**
     * Return population of 3D objects population from binary image
     */
    private Objects3DIntPopulation getPopFromImage(ImagePlus img) {
        ImageLabeller labeller = new ImageLabeller();
        ImageInt labels = labeller.getLabels(ImageHandler.wrap(img));
        Objects3DIntPopulation pop = new Objects3DIntPopulation(labels);
        return pop;
    } 
    
    
    /**
     * Remove objects in population with size < min and size > max
     */
    public void popFilterSize(Objects3DIntPopulation pop, double min, double max) {
        pop.getObjects3DInt().removeIf(p -> (new MeasureVolume(p).getVolumeUnit() < min) || (new MeasureVolume(p).getVolumeUnit() > max));
        pop.resetLabels();
    }

    
    /**
     * Find population of astrocytes
     */
    public Objects3DIntPopulation findAstrocytes(ImagePlus imgAstro, ArrayList<Roi> rois) {
        // Detect astrocytes
        ImagePlus imgBin = new Duplicator().run(imgAstro);
        IJ.run(imgBin, "Median...", "radius=2 stack");
        IJ.run(imgBin, "Convert to Mask", "method="+astroThMethod+" background=Dark calculate black");
        imgBin.setCalibration(cal);

        // Fill ROIs in black
        if (!rois.isEmpty()) {
            fillImg(imgBin, rois);
        }
        
        // Astrocytes size filtering
        Objects3DIntPopulation astroPop = getPopFromImage(imgBin);
        System.out.println("Nb astrocytic objects detected:"+astroPop.getNbObjects());
        popFilterSize(astroPop, minAstroVol, Double.MAX_VALUE);
        System.out.println("Nb astrocytic objects remaining after size filtering: "+ astroPop.getNbObjects());
        
        flushCloseImg(imgBin);
        return(astroPop);
    }
       
    
    /**
     * Find astrocytes into and out of vessels
     */
    public List<Objects3DIntPopulation> findAstroInOutVessels(Objects3DIntPopulation astrocytesPop, Objects3DIntPopulation vesselsPop, ImagePlus imgAstro) {
        ImageHandler imh = ImageHandler.wrap(imgAstro).createSameDimensions();
        astrocytesPop.drawInImage(imh);
        ImageHandler imhDup = imh.duplicate();
        
        for (Object3DInt vessel: vesselsPop.getObjects3DInt()) {
            Object3DInt vesselDil = dilateObj(vessel, imgAstro, dilVessel);
            vesselDil.drawObject(imh, 0);
        }

        Objects3DIntPopulation popOut = new Objects3DIntPopulation(imh);
        
        ImagePlus imgSub = new ImageCalculator().run("subtract stack create", imhDup.getImagePlus(), imh.getImagePlus());
        Objects3DIntPopulation popIn = new Objects3DIntPopulation(ImageHandler.wrap(imgSub));
        
        imh.closeImagePlus();
        imhDup.closeImagePlus();
        return(Arrays.asList(popIn, popOut));  
    }

    
    /**
     * Return dilated object restricted to image borders
     */
    public Object3DInt dilateObj(Object3DInt obj, ImagePlus img, double dilSize) {
        Object3DInt objDil = new Object3DComputation(obj).getObjectDilated((float)(dilSize/cal.pixelWidth), (float)(dilSize/cal.pixelHeight),(float)(dilSize/cal.pixelDepth));
        
        // Check if object goes over image borders
        BoundingBox bbox = objDil.getBoundingBox();
        BoundingBox imgBbox = new BoundingBox(ImageHandler.wrap(img));
        int[] box = {imgBbox.xmin, imgBbox.xmax, imgBbox.ymin, imgBbox.ymax, imgBbox.zmin, imgBbox.zmax};
        if (bbox.xmin < 0 || bbox.xmax > imgBbox.xmax || bbox.ymin < 0 || bbox.ymax > imgBbox.ymax || bbox.zmin < 0 || bbox.zmax > imgBbox.zmax) {
            Object3DInt objDilInImg = new Object3DInt();
            for (Object3DPlane p: objDil.getObject3DPlanes()) {
                for (VoxelInt v: p.getVoxels()) {
                    if (v.isInsideBoundingBox(box))
                        objDilInImg.addVoxel(v);
                }
            }
            return(objDilInImg);
        } else {
            return(objDil);
        }
    }

  
    /**
     * Draw results
     */
    public void drawResults(ImagePlus imgAstro, Objects3DIntPopulation vesselsPop, Objects3DIntPopulation astroIn, Objects3DIntPopulation astroOut,
            String outDirResults, String rootName) {
        ImageHandler imgVessels = ImageHandler.wrap(imgAstro).createSameDimensions();
        ImageHandler imgAstroIn = ImageHandler.wrap(imgAstro).createSameDimensions();
        ImageHandler imgAstroOut = ImageHandler.wrap(imgAstro).createSameDimensions();
        
        // Draw vessels pop in blue, astroIn pop in red and astroOut pop in green
        for (Object3DInt vessel: vesselsPop.getObjects3DInt()) 
                vessel.drawObject(imgVessels, 255);
        for (Object3DInt ob : astroIn.getObjects3DInt()) 
                ob.drawObject(imgAstroIn, 255);
        for (Object3DInt ob : astroOut.getObjects3DInt()) 
                ob.drawObject(imgAstroOut, 255);
        ImagePlus[] imgColors = {imgAstroIn.getImagePlus(), imgAstroOut.getImagePlus(), imgVessels.getImagePlus()};
        ImagePlus imgObjects = new RGBStackMerge().mergeHyperstacks(imgColors, false);
        imgObjects.setCalibration(cal);
        IJ.run(imgObjects, "Enhance Contrast", "saturated=0.35");

        // Save resulting image
        FileSaver ImgObjectsFile = new FileSaver(imgObjects);
        ImgObjectsFile.saveAsTiff(outDirResults + rootName + ".tif");
        
        imgVessels.closeImagePlus();
        imgAstroIn.closeImagePlus();
        imgAstroOut.closeImagePlus();
    }
    
    
    /**
     * Write results
     */
    public void writeResults(BufferedWriter results, Objects3DIntPopulation vesselsPop, Objects3DIntPopulation astroIn, Objects3DIntPopulation astroOut, 
            ImagePlus imgAstro, ArrayList<Roi> rois, String imgName) throws IOException {
        
        double imgVol = imgAstro.getWidth() * imgAstro.getHeight() * imgAstro.getNSlices() * pixVol;
        double roisVol = getRoisVolume(rois, imgAstro);
        double vesselsVol = findPopVolume(vesselsPop);
        
        double astroInVol = findPopVolume(astroIn);
        double astroOutVol = findPopVolume(astroOut);
        
        results.write(imgName+"\t"+imgVol+"\t"+(imgVol-roisVol)+"\t"+vesselsVol+"\t"+astroInVol+"\t"+astroOutVol+"\t"+"\n");
        results.flush();
    }
    
    
    /**
     * Compute ROIs total volume
     */
    public double getRoisVolume(ArrayList<Roi> rois, ImagePlus img) {
        double roisVol = 0;
        for(Roi roi: rois) {
            PolygonRoi poly = new PolygonRoi(roi.getFloatPolygon(), Roi.FREEROI);
            poly.setLocation(0, 0);
            
            img.resetRoi();
            img.setRoi(poly);

            ResultsTable rt = new ResultsTable();
            Analyzer analyzer = new Analyzer(img, Analyzer.AREA, rt);
            analyzer.measure();
            roisVol += rt.getValue("Area", 0);
        }

        return(roisVol * img.getNSlices() * cal.pixelDepth);
    }
    

    /**
     * Find total volume of objects in population
     */
    public double findPopVolume(Objects3DIntPopulation pop) {
        double totalVol = 0;
        for(Object3DInt obj: pop.getObjects3DInt())
            totalVol += new MeasureVolume(obj).getVolumeUnit();
        return(totalVol);
    }
    
}
