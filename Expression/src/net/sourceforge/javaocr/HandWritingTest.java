package net.sourceforge.javaocr;

import java.io.File;

import com.statics.MyClass;

import net.sourceforge.javaocr.gui.GUIController;
import net.sourceforge.javaocr.gui.handwritingRecognizer.HandWritingFormProcessor;


public class HandWritingTest {

	public static void main(String[] args) throws Exception {
		File sourceImage=new File("E:\\javaOCR\\handwritingTests\\TrainingImages\\0-9anda-z.jpg");//E:\\javaOCR\\handwritingTests\\TrainingImages\\0-9anda-z.jpg
		File targetImage=new File("E:\\aa.png");//E:\\javaOCR\\handwritingTests\\0-9anda-zTarget.jpg
		GUIController gui=new GUIController();
		
		
		HandWritingFormProcessor processor = new HandWritingFormProcessor(gui);

		MyClass m=new MyClass();
		System.out.println(gui.processHandwriting(sourceImage, targetImage, m));
	}

}
