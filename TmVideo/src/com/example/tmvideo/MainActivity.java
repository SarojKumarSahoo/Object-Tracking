package com.example.tmvideo;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;



public class MainActivity extends Activity {

	VideoView videoView ;
	MediaMetadataRetriever mRetriever;
	Mat input,template,result,thres,inter,submat,image,temp;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status){
			switch(status){
			case LoaderCallbackInterface.SUCCESS:
			{
				Log.d("testing", "Loading successful " );
				init();
				break;
			}
			default:{
				super.onManagerConnected(status);
			}
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
       
				   
	}
	
	public void init(){
		

		videoView =(VideoView)findViewById(R.id.videoview);
	    
		MediaController mediaController= new MediaController(this);
	    mediaController.setAnchorView(videoView);        
	    
	    //File videoFile=new File("/storage/sdcard1/video/video.mp4");
	    File videoFile=new File("/storage/emulated/0/DCIM/Camera/VID_20150622_103908.3gp");

        Uri videoFileUri=Uri.parse(videoFile.toString());
	   
	    //Uri uri=Uri.parse("/storage/sdcard1/video/video.mp4"); 
	    
        mRetriever = new MediaMetadataRetriever();
        mRetriever.setDataSource(videoFile.getAbsolutePath());

	    videoView.setMediaController(mediaController);
	    videoView.setVideoURI(videoFileUri);        
	    
	    videoView.setOnCompletionListener(myVideoViewCompletionListener);
	    videoView.setOnPreparedListener(MyVideoViewPreparedListener);
	    videoView.setOnErrorListener(myVideoViewErrorListener);
	    
	    videoView.requestFocus();
	    videoView.start();
	} 
	
	public void onResume(){
	    	super.onResume();
	    	OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
	    }
	    
	public void onDestroy(){
	    	super.onDestroy();

	    }
	
	public void image() throws IOException{
		if(input.empty())
			return;
		if(template == null){
			
			temp = Utils.loadResource(MainActivity.this, R.drawable.temp, Highgui.CV_LOAD_IMAGE_COLOR);
			template = new Mat(temp.size(), CvType.CV_32F);
			Imgproc.cvtColor(temp, template, Imgproc.COLOR_BGR2GRAY);
			Imgproc.threshold(template, template, 128, 255, Imgproc.THRESH_OTSU);

			
		}
	}
	
	public Bitmap tempMatch(Bitmap b){
		
		input = new Mat(new Size(768 , 1280), CvType.CV_32F);
		Utils.bitmapToMat(b, input);
		thres = new Mat(input.size(), input.type());
		
		input.copyTo(thres);
		
		//Imgproc.resize(thres, thres, new Size(thres.width()/2 , thres.height()/2));
		Log.d("testing", "thres size = " + thres.size());
		Imgproc.cvtColor(thres, thres, Imgproc.COLOR_BGR2GRAY);
		Imgproc.GaussianBlur(thres, thres, new Size(5,5), 2.2 , 2);
		Imgproc.threshold(thres, thres, 128, 255, Imgproc.THRESH_OTSU);
		
		try{
			image();
		}catch (IOException e){
			e.printStackTrace();
		}	
		
		int match_method = Imgproc.TM_CCOEFF_NORMED;
		
		int result_cols = thres.cols() - template.cols() + 1;
		int result_rows = thres.rows() - template.rows() + 1;
		Mat result = new Mat(result_rows, result_cols, CvType.CV_32F);
		//Log.d("testing", "result size = " + result.size());
		
		Imgproc.matchTemplate(thres, template, result, match_method);
		
		MinMaxLocResult mmr = Core.minMaxLoc(result);
		
		Point matchLoc;
		
		matchLoc = mmr.maxLoc;
		//Log.d(TAG , "maxval" + mmr.maxLoc);
		
		//if(mmr.maxVal >= thresholdMax)
		//{
			Rect roi = new Rect((int) matchLoc.x, (int) matchLoc.y, template.cols() , template.rows());
			Core.rectangle(input, matchLoc, new Point(matchLoc.x + template.cols(), matchLoc.y + template.rows()), new Scalar(255, 0 ,0),2,8,0);
			submat = input.submat(roi);
			
			try{
				
				image = Utils.loadResource(MainActivity.this, R.drawable.temp, Highgui.CV_LOAD_IMAGE_COLOR);
			}catch (IOException e){
				e.printStackTrace();
			//}
			
			Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2BGRA);
			image.copyTo(submat);
		
		}
		Utils.matToBitmap(input, b);
		
		return b;
	}
	
	public void saveFrames(ArrayList<Bitmap> saveBitmapList) throws IOException{
	    Random r = new Random();
	    int folder_id = r.nextInt(1000) + 1;

	    String folder = "/storage/sdcard1/videos/frames/"+folder_id+"/";
	    File saveFolder=new File(folder);
	    if(!saveFolder.exists()){
	       saveFolder.mkdirs();
	    }

	    int i=1;
	    for (Bitmap b : saveBitmapList){
	       ByteArrayOutputStream bytes = new ByteArrayOutputStream();
	        b.compress(Bitmap.CompressFormat.JPEG, 40, bytes);

	        File f = new File(saveFolder,("frame"+i+".jpg"));

	        f.createNewFile();

	        FileOutputStream fo = new FileOutputStream(f);
	        fo.write(bytes.toByteArray());

	           fo.flush();
	           fo.close();

	        i++;
	    }
	    Toast.makeText(getApplicationContext(),"Folder id : "+folder_id, Toast.LENGTH_LONG).show();

	}
	
	 
    MediaPlayer.OnCompletionListener myVideoViewCompletionListener = 
    		   new MediaPlayer.OnCompletionListener() {

    		  @Override
    		  public void onCompletion(MediaPlayer arg0) {
    		   Toast.makeText(MainActivity.this, "End of Video",
    		     Toast.LENGTH_LONG).show();
    		   
    		  }
    		 };

    		 MediaPlayer.OnPreparedListener MyVideoViewPreparedListener = 
    		   new MediaPlayer.OnPreparedListener() {

    		  @Override
    		  public void onPrepared(MediaPlayer mp) {
    		   
    		   long duration = videoView.getDuration(); 
    		   Toast.makeText(MainActivity.this,
    		     "Duration: " + duration + " (ms)", 
    		     Toast.LENGTH_LONG).show();
    		   
    		   ArrayList<Bitmap> mFrames=new ArrayList<Bitmap>();
    		   for(int i=1000000;i<duration*1000;i+=1000000)
    	        {
    	           Bitmap bitmap = mRetriever.getFrameAtTime(i,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
    	           mFrames.add(tempMatch(bitmap));
    	        }
    		   
    		   try {
    			   saveFrames(mFrames);
    		   } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
    		   }

    		  }
    		 };

    		 MediaPlayer.OnErrorListener myVideoViewErrorListener = 
    		   new MediaPlayer.OnErrorListener() {

    		  @Override
    		  public boolean onError(MediaPlayer mp, int what, int extra) {
    		   
    		   Toast.makeText(MainActivity.this, 
    		     "Error!!!",
    		     Toast.LENGTH_LONG).show();
    		   return true;
    		  }
    	};
}
