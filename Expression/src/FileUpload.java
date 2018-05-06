import com.alfa.MathsLib.Expression;

import org.json.simple.JSONObject;

import net.sourceforge.javaocr.gui.meanSquareOCR.TrainingImageSpec;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.CharacterRange;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.OCRScanner;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.TrainingImage;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.TrainingImageLoader;

import java.awt.Canvas;

import java.awt.image.BufferedImage;
// Import required java libraries
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
//import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;





/**
 * Servlet implementation class FileUpload
 */
//@WebServlet("/FileUpload")
public class FileUpload extends HttpServlet {
	static { 
	      System.setProperty("java.awt.headless", "true");
	      System.out.println("java.awt.headless: "+java.awt.GraphicsEnvironment.isHeadless());
    }
	private static final long serialVersionUID = 8433660580659064601L;

	private boolean isMultipart;
	private String filePath;
	private int maxFileSize = 10 * 1024 * 1024;
	private int maxMemSize = 4 * 1024;
	private File file ;
	private File uploadRepo;
	JSONObject jsonObj;

	private String trainPath;
	public void init( ){

		
		jsonObj=new JSONObject();
		
		String path="data";
		trainPath=path+"/trainData";
		File trainPathDir=new File(trainPath);
		if(!trainPathDir.exists()){
			System.out.println("trainPath directory doesn't exist...Creating One!");
			if(!trainPathDir.mkdirs()){
				System.out.println("Error Creating trainPath directory");
			}
			else{
				System.out.println("Created Directory for trainPath: "+trainPathDir.getAbsolutePath());
				System.out.println("\n\nPlease upload training data...\n\n");
			}
		}
		else{
			System.out.println("Directory for Existing trainPath: "+trainPathDir.getAbsoluteFile());
		}
		
		uploadRepo=new File("uploadPictures/");
		if(!uploadRepo.exists()){
			System.out.println("uplodRepo directory doesn't exist...Creating One!");
			if(!uploadRepo.mkdirs()){
				System.out.println("Error Creating uploadRepo directory");
			}
			else{
				System.out.println("Creating Directory for uplodaRepo: "+uploadRepo.getAbsoluteFile());
			}
		}
		else{
			System.out.println("Directory for Existing uploadRepo: "+uploadRepo.getAbsoluteFile());
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	public void doPost(HttpServletRequest request,HttpServletResponse response)throws ServletException, java.io.IOException {
	   
	  filePath = getServletContext().getRealPath("/")+"pix/";
	  
      // Check that we have a file upload request
      isMultipart = ServletFileUpload.isMultipartContent(request);
      response.setContentType("text/html");
      java.io.PrintWriter out = response.getWriter( );
      if( !isMultipart ){
         out.println("No file uploaded"); 
         return;
      }
      DiskFileItemFactory factory = new DiskFileItemFactory();
      // maximum size that will be stored in memory
      factory.setSizeThreshold(maxMemSize);
      // Location to save data that is larger than maxMemSize.
      
      factory.setRepository(uploadRepo);

      // Create a new file upload handler
      ServletFileUpload upload = new ServletFileUpload(factory);
      // maximum file size to be uploaded.
      upload.setSizeMax( maxFileSize );

      try{ 
    	  // Parse the request to get file items.
    	  List fileItems = upload.parseRequest(request);
	
	      // Process the uploaded file items
	      Iterator i = fileItems.iterator();
	
	      while ( i.hasNext () ) 
	      {
	         FileItem fi = (FileItem)i.next();
	         if ( !fi.isFormField () )	
	         {
	            // Get the uploaded file parameters
	            String fieldName = fi.getFieldName();
	            String fileName = fi.getName();
	            String contentType = fi.getContentType();
	            boolean isInMemory = fi.isInMemory();
	            long sizeInBytes = fi.getSize();
	            
	            File dir=new File(filePath);
	            if (!dir.exists()){
	            	System.out.println("image save directory doesn't exists...creating one!");
	            	if(!dir.mkdirs()){
	            		System.out.println("Error creating image save directory...");
	            	}
	            	else{
	            		System.out.println("Image save directory successfully created: "+dir.getAbsolutePath());
	            	}
	            }
	            // Write the file
	            if( fileName.lastIndexOf("\\") >= 0 ){
	               file = new File( filePath + 
	               fileName.substring( fileName.lastIndexOf("\\"))) ;
	            }else{
	               file = new File( filePath + 
	               fileName.substring(fileName.lastIndexOf("\\")+1)) ;
	            }
	            fi.write( file ) ;

	            System.out.println("File Uploaded: "+file.toPath().toString());
	            

	            String expressionString=javaOCR(file.toPath().toString());
	            System.out.println("OCR Result: "+expressionString);
	            
	            String answer=(calculate(expressionString).toPlainString()).trim();
	            System.out.println("Expression Evaluated: "+answer);
	            
	            jsonObj.put("expression", expressionString);
	            jsonObj.put("answer", answer);
	            jsonObj.put("returnValue", 0);
	            
	            System.out.println(jsonObj.toJSONString()+"");
	            
	            out.println(jsonObj.toJSONString());
	            out.close();
	         }
	      }
      }
      catch(Exception ex) {
       System.out.println(ex);
       //out.print("Error in Server..."+ex.getMessage().toString());
       jsonObj.put("answer", "Error in Server...");
       jsonObj.put("returnValue", 1);
      }
	}
	
	public void doGet(HttpServletRequest request,HttpServletResponse response)throws ServletException, java.io.IOException {
	   doPost(request,response);
	} 
	
	public BigDecimal calculate(String expr){
		BigDecimal result = null;
		
		if(expr.isEmpty()||expr==null){
			expr="0";
		}
		
		Expression expression = new Expression(expr);
		expression.setPrecision(10);
		result = expression.eval();
		
		return result;
	}
	

	public String javaOCR(String targImageLoc){
		String ocrStringOut = null;
		ArrayList<TrainingImageSpec> imgs=new ArrayList<TrainingImageSpec>();
		CharacterRange cr=new CharacterRange(' ','~');
		
		File folder = new File(trainPath);
		File[] listOfFiles = folder.listFiles();
		if(listOfFiles.length<=0){
			System.out.println("Error retriving training data: No training images found...Please Upload training images...");
		}
	    for (int i = 0; i < listOfFiles.length; i++) {
	    	if (listOfFiles[i].isFile()) {
	    		TrainingImageSpec tis=new TrainingImageSpec();
	    		tis.setCharRange(cr);
	    		System.out.println("Getting Training File: "+listOfFiles[i].getAbsolutePath());
	    		tis.setFileLocation(listOfFiles[i].getAbsolutePath());    		
	    		imgs.add(tis);
		      } 
		}
		try {
			ocrStringOut=(performMSEOCR(imgs, targImageLoc));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return ocrStringOut;

	
	}
	
	private  String performMSEOCR(ArrayList<TrainingImageSpec> imgs, String targImageLoc) throws Exception{
        OCRScanner ocrScanner = new OCRScanner();
        HashMap<Character, ArrayList<TrainingImage>> trainingImages = getTrainingImageHashMap(imgs);
        ocrScanner.addTrainingImages(trainingImages);
        BufferedImage targetImage = ImageIO.read(new File(targImageLoc));
        String text = ocrScanner.scan(targetImage, 0, 0, 0, 0, null);
        return text;
    }
	
	private HashMap<Character, ArrayList<TrainingImage>> getTrainingImageHashMap(ArrayList<TrainingImageSpec> imgs) throws Exception{
        TrainingImageLoader loader = new TrainingImageLoader();
        HashMap<Character, ArrayList<TrainingImage>> trainingImages = new HashMap<Character, ArrayList<TrainingImage>>();
        Canvas frame = new Canvas();

        for (int i = 0; i < imgs.size(); i++){
            loader.load(frame,imgs.get(i).getFileLocation(),imgs.get(i).getCharRange(),trainingImages);
        }

        return trainingImages;
    }
}
