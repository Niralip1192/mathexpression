package com.expression.aanal.m_expression;


import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private String url;
    private Button CameraButton;
    private Button GalleryButton;
    private EditText urlPath;
    private ProgressDialog progress;
    private String returnFromServer;
    private TextView displayResult;
    private String imageName;
    private String imagePath;
    private String saveDir=Environment.getExternalStorageDirectory()+File.separator +"expression"+File.separator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File imageSaveDir=new File(saveDir);
        if(!(imageSaveDir.exists())){
            try{
                imageSaveDir.mkdirs();
                imageSaveDir.canWrite();
                imageSaveDir.canRead();
                imageSaveDir.canExecute();
            }
            catch(Exception x){
                Toast.makeText(MainActivity.this, "Unable to make directory...\nExiting...", Toast.LENGTH_LONG).show();
                Log.e("HAPPY",x.getLocalizedMessage());
                return;
            }
        }
        urlPath= (EditText) findViewById(R.id.urlText);
        displayResult = (TextView) findViewById(R.id.ResultDisplay);
        CameraButton = (Button) findViewById(R.id.cameraBtn);
        GalleryButton = (Button) findViewById(R.id.galleryUpload);

        CameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                url=urlPath.getText().toString();
                if(url.isEmpty() ||     url==null){
                    url="http://expression-ruling.rhcloud.com/Expression/FileUpload";
                }
                displayResult.setText("");

                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                imageName = UUID.randomUUID().toString() +".jpg";
                imagePath=saveDir+ imageName;
                File file = new File(imagePath);
                Log.e("ALLOUT","created file to store img: "+file.getAbsolutePath());

                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));

                startActivityForResult(intent, 1);


            }
        });
        GalleryButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                url=urlPath.getText().toString();
                if(url.isEmpty() ||     url==null){
                    url="http://expression-ruling.rhcloud.com/Expression/FileUpload";
                }
                loadImagefromGallery();
            }
        });
        displayResult.setText("");
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("ALLOUT", "requestCode:" + requestCode);
        if(requestCode==1) {
            File file = new File(imagePath);
            Log.e("ALLOUT","Got File after capture:"+file.getAbsolutePath());

            //Crop the captured image using an other intent
            try {
                Log.e("ALLOUT","Cropping");
                /*the user's device may not support cropping*/
                cropCapturedImage(Uri.fromFile(file));
                Log.e("ALLOUT","Done Cropping");
                Log.e("ALLOUT","Showing img");
                Bundle extras=new Bundle();
                //Create an instance of bundle and get the returned data
                if(data!=null) {
                    Log.e("ALLOUT","requestCode==1 data!=null");
                    extras = data.getExtras();
                }
                else{
                    Log.e("ALLOUT","requestCode==1 data==null");
                }
                //get the cropped bitmap from extras
                Bitmap thePic = extras.getParcelable("data");

            }
            catch(ActivityNotFoundException aNFE){
                //display an error message if user device doesn't support
                String errorMessage = "Sorry - your device doesn't support the crop action!";
                Toast toast = Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        else if (requestCode == 2) {
            Log.e("ALLOUT", "Showing img");
            //Create an instance of bundle and get the returned data
            Bundle extras = new Bundle();
            if (data != null) {
                Log.e("ALLOUT", "requestCode==2 data!=null");
                extras = data.getExtras();
            } else {
                Log.e("ALLOUT", "requestCode==2 data==null");
            }
            //get the cropped bitmap from extras
            Bitmap thePic = extras.getParcelable("data");
            //set image bitmap to image view
            Log.e("ALLOUT", "Setting iv");

            if(!saveCropImg(thePic)){
                Log.e("ALLOUT","Error Saving the image");
            }

            new LongOperation().execute("");

        }
        else if(requestCode==3 && resultCode == RESULT_OK && data != null && data.getData() != null){
            Uri selectedImage = data.getData();
            String[] projection = { MediaStore.Images.Media.DATA };
            // Get the cursor
            Cursor cursor = getContentResolver().query(selectedImage,projection, null, null, null);


            // Move to first row
            cursor.moveToFirst();


            int columnIndexx = cursor.getColumnIndex(projection[0]);
            String picturePath = cursor.getString(columnIndexx);
            cursor.close();
            imagePath=picturePath;
            int x=imagePath.lastIndexOf("/");
            imageName=imagePath.substring(x+1,imagePath.length());

            Log.e("HAPPY","imagePath: "+imagePath);
            Log.e("HAPPY","imageName: "+imageName);
            cropCapturedImage(Uri.fromFile(new File(imagePath)));
        }
    }

    //create helping method cropCapturedImage(Uri picUri)
    public void cropCapturedImage(Uri picUri){
        //call the standard crop action intent
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        //indicate image type and Uri of image
        cropIntent.setDataAndType(picUri, "image/*");
        //set crop properties
        cropIntent.putExtra("crop", "true");
        //indicate aspect of desired crop
        cropIntent.putExtra("aspectX", 1);
        cropIntent.putExtra("aspectY", 1);
        //indicate output X and Y
        cropIntent.putExtra("outputX", 256);
        cropIntent.putExtra("outputY", 256);
        //retrieve data on return
        cropIntent.putExtra("return-data", true);
        //start the activity - we handle returning in onActivityResult
        Log.e("ALLOUT","returning cropped img");
        startActivityForResult(cropIntent, 2);
    }

    public boolean saveCropImg(Bitmap bmp){
        FileOutputStream out = null;
        Bitmap bitmap=null;
        try {
            Log.e("ALLOUT","getting instance of original file");
            imageName = UUID.randomUUID().toString() +".jpg";
            imagePath=saveDir + imageName;
            File fi=new File(imagePath);
            while(fi.exists()){
                imageName = UUID.randomUUID().toString() +".jpg";
                imagePath=saveDir + imageName;
            }
            fi=null;
            out = new FileOutputStream(imagePath);

            Log.e("ALLOUT", "Saved cropped img:" + imagePath);
            if(bmp!=null) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ALLOUT", "Error saving cropped img" + e.getMessage());
            return false;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }
    }

    public void loadImagefromGallery() {
        // Create intent to Open Image applications like Gallery, Google Photos
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, 3);
    }

    //Network operation Class
    private class LongOperation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            HttpFileUploader uploader=new HttpFileUploader(url,"param",imageName);
            try{
                File imageFile=new File(imagePath);
                if(!imageFile.exists()){
                    returnFromServer="Image Not Found on local media";
                    return "Error";
                }
                Thread.sleep(1000);
                returnFromServer=uploader.doStart( new FileInputStream(imageFile));
                if(returnFromServer.isEmpty() || returnFromServer==null){
                    returnFromServer="Server Error";
                }

            }
            catch(Exception x){
                Log.e("Happy",x.toString());
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
            if(returnFromServer!=null && !returnFromServer.isEmpty() ) {
                doJsonParse(returnFromServer);
            }
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(MainActivity.this, "Uploading...","Please Wait", true);
        }

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    protected void doJsonParse(String strJson){
        try {
            JSONObject jsonRootObject = new JSONObject(strJson);

            int returnValue = Integer.parseInt(jsonRootObject.optString("returnValue").toString());
            String answer=jsonRootObject.optString("answer").toString();
            if(returnValue==0){
                String expr=jsonRootObject.optString("expression").toString();
                displayResult.setText(expr+" = "+answer);
            }
            else{
                displayResult.setText("ERROR: "+answer);
                Toast.makeText(MainActivity.this, "Some Error Occurred", Toast.LENGTH_LONG).show();
            }
        } catch (JSONException e) {e.printStackTrace();}
    }
}
