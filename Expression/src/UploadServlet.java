import com.alfa.MathsLib.Expression;

import org.json.simple.JSONObject;
import java.util.ArrayList;

import net.sourceforge.javaocr.gui.GUIController;
import net.sourceforge.javaocr.gui.meanSquareOCR.TrainingImageSpec;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.CharacterRange;

// Import required java libraries
import java.io.*;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;


//@WebServlet("/UploadServlet")
public class UploadServlet extends HttpServlet {
   
   /**
	 * 
	 */
	private static final long serialVersionUID = 8433660580659064601L;

	private boolean isMultipart;
	private String filePath;
	private int maxFileSize = 10 * 1024 * 1024;
	private int maxMemSize = 4 * 1024;
	private File file ;
	private File uploadRepo;
	JSONObject jsonObj;
	private String dataPath;
	public void init( ){
		dataPath=getServletContext().getInitParameter("dataPath");
		jsonObj=new JSONObject();
		String path="data";
		File dirTessData=new File(path+"/tessdata");
		if(!dirTessData.exists()){
			System.out.println("tessData directory doesn't exist...Creating One!");
			if(!dirTessData.mkdirs()){
				System.out.println("Error Creating tessData directory");
			}
			else{
				System.out.println("Creating Directory for tessData: "+dirTessData.getAbsoluteFile());
			}
		}
		else{
			System.out.println("Directory for Existing tessData: "+dirTessData.getAbsoluteFile());
		}
		String engDataDest=(path+"/tessdata/eng.traineddata");
		if(!(new File(engDataDest).exists())){			
			System.out.println("eng.trainedata doesn't exists...\nPlease Upload trainData at:"+engDataDest);
		}
		else{
			System.out.println("eng.trainedata exists: "+engDataDest);
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
	            /*out.print("path: "+file.toPath().toString());
	            out.println("Uploaded Filename: " + fileName + "<br>");
	            out.println("Uploaded Filename: " + file.getAbsolutePath()+ "<br>");
	            out.println("<img src=\""+request.getContextPath()+"/"+fileName+"\" >");*/
	            //out.println("Success... File Uploaded: "+fileName);
	            System.out.println("File Uploaded: "+file.toPath().toString());
	            
	            //ApplyTesseract
	            //String expressionString=tesseract(file.toPath().toString());
	            String expressionString=javaOCR(file.toPath().toString());
	            
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
	        
		   /*throw new ServletException("GET method used with " +
	                getClass( ).getName( )+": POST method required.");*/
	   
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
	
	public static void saveFileFromUrlWithCommonsIO(String fileName,String fileUrl) throws MalformedURLException, IOException {
		FileUtils.copyURLToFile(new URL(fileUrl), new File(fileName));
	}
	public String javaOCR(String targImageLoc){
		String ocrStringOut = null;
		//String targImageLoc="/home/alfa/Downloads/javaocr-20100605/ocrTests/asciiSentence.png";
		GUIController guiController = new GUIController();
		CharacterRange cr=new CharacterRange(' ','~');
		TrainingImageSpec tis=new TrainingImageSpec();
		tis.setCharRange(cr);
		tis.setFileLocation("hpljPica.jpg");
		
		
		ArrayList<TrainingImageSpec> imgs=new ArrayList<TrainingImageSpec>();
		imgs.add(tis);
		try {
			ocrStringOut=(guiController.performMSEOCR(imgs, targImageLoc));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ocrStringOut;

	
	}
}