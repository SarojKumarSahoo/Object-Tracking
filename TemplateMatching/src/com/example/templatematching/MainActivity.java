package com.example.templatematching;
//Template Matching on Camera Frames.
import java.io.IOException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
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
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;


public class MainActivity extends Activity implements CvCameraViewListener2 {

	Mat input,template,result,thres,inter,submat,image,temp;
	double thresholdMax = 0.35;
	private static final String TAG = "TEST";
	
	//Loading of OpenCV library
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status){
			switch(status){
			case LoaderCallbackInterface.SUCCESS:
			{
				mOpenCvCameraView.enableView();
				break;
			}
			default:{
				super.onManagerConnected(status);
			}
			}
		}
	};
	private CameraBridgeViewBase mOpenCvCameraView;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //Setting CameraView
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.MainActivityCameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }
    
    //Async Initiallization
    public void onResume(){
    	super.onResume();
    	OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }
    
    public void onDestroy(){
    	super.onDestroy();
    	if(mOpenCvCameraView != null)
    		mOpenCvCameraView.disableView();
    }
    
	@Override
	public void onCameraViewStarted(int width, int height) {
		
	}

	@Override
	public void onCameraViewStopped() {
		
	}
	
	//Loading the template image from the drawable folder and binarizing it.
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

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		
		//Current Input Frame
		input = inputFrame.rgba();
		thres = new Mat(input.size(), input.type());
		
		input.copyTo(thres);
		
		//Binarizing the input frame.
		Imgproc.cvtColor(thres, thres, Imgproc.COLOR_BGR2GRAY);
		Imgproc.GaussianBlur(thres, thres, new Size(5,5), 2.2 , 2);
		Imgproc.threshold(thres, thres, 128, 255, Imgproc.THRESH_OTSU);
		
		//Loading the template image
		try{
			image();
		}catch (IOException e){
			e.printStackTrace();
		}	
		
		//Template match method
		int match_method = Imgproc.TM_CCOEFF_NORMED;
		
		//Defining result Mat
		int result_cols = thres.cols() - template.cols() + 1;
		int result_rows = thres.rows() - template.rows() + 1;
		Mat result = new Mat(result_rows, result_cols, CvType.CV_32F);
		//Log.d("testing", "result size = " + result.size());
		
		//Matching
		Imgproc.matchTemplate(thres, template, result, match_method);
		
		//Localizing the best match
		MinMaxLocResult mmr = Core.minMaxLoc(result);
		Point matchLoc;
		matchLoc = mmr.maxLoc;
		Log.d(TAG , "maxval" + mmr.maxLoc);
		
		if(mmr.maxVal >= thresholdMax)
		{
			//Region of Interest for Image Augmentation
			Rect roi = new Rect((int) matchLoc.x, (int) matchLoc.y, template.cols() , template.rows());
			
			//Drawing rectangle over the best match found
			Core.rectangle(input, matchLoc, new Point(matchLoc.x + template.cols(), matchLoc.y + template.rows()), new Scalar(255, 0 ,0));
			
			//Overlaying 2D image
			submat = input.submat(roi);
			
			try{
				image = Utils.loadResource(MainActivity.this, R.drawable.temp, Highgui.CV_LOAD_IMAGE_COLOR);
			}catch (IOException e){
				e.printStackTrace();
			}
			
			Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2BGRA);
			image.copyTo(submat);
		
		}
		
		return input;
	}

}
