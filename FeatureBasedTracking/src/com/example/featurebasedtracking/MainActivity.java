package com.example.featurebasedtracking;
//Feature Based Matching.
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;



public class MainActivity extends Activity{
	
	private static final String TAG = "test";
	Mat input,gray,thres,inter,template,output,object,scene;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status){
			switch(status){
			case LoaderCallbackInterface.SUCCESS:
			{
				try {
					image();
				} catch (IOException e) {
					e.printStackTrace();
				}
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

    
    public void onResume(){
    	super.onResume();
    	OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }
    
    public void onDestroy(){
    	super.onDestroy();
    	
    }
    
    public void image() throws IOException{
    	
    	
    	if(object == null){
			
    		object = Utils.loadResource(MainActivity.this, R.drawable.box, Highgui.CV_LOAD_IMAGE_COLOR);

		}
		
		if(scene == null){
			
			scene = Utils.loadResource(MainActivity.this, R.drawable.scene, Highgui.CV_LOAD_IMAGE_COLOR);

		}
		
		FeatureDetector orbDetector = FeatureDetector.create(FeatureDetector.ORB);
	    DescriptorExtractor orbextractor = DescriptorExtractor.create(DescriptorExtractor.ORB);

	    MatOfKeyPoint keypoints_object = new MatOfKeyPoint();
	    MatOfKeyPoint keypoints_scene = new MatOfKeyPoint();

	    Mat descriptors_object = new Mat();
	    Mat descriptors_scene = new Mat();

	    //Getting the keypoints
	    orbDetector.detect( object, keypoints_object );
	    orbDetector.detect( scene, keypoints_scene );

	    //Compute descriptors
	    orbextractor.compute( object, keypoints_object, descriptors_object );
	    orbextractor.compute( scene, keypoints_scene, descriptors_scene );

	    //Match with Brute Force
	    MatOfDMatch matches = new MatOfDMatch();
	    DescriptorMatcher matcher;
	    matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
	    matcher.match( descriptors_object, descriptors_scene, matches );

	    double max_dist = 0;
	    double min_dist = 100;

	    List<DMatch> matchesList = matches.toList();

	    //calculation of max and min distances between keypoints
	      /*for( int i = 0; i < descriptors_object.rows(); i++ )
	      { double dist = matchesList.get(i).distance;
	        if( dist < min_dist ) min_dist = dist;
	        if( dist > max_dist ) max_dist = dist;
	      }*/
	      
	    for(final DMatch match : matchesList){
	    	  final double dist = match.distance;
	            if (dist < min_dist) {
	                min_dist = dist;
	            }
	            if (dist > max_dist) {
	                max_dist = dist;
	            }
	      
	     } 
	     Log.d(TAG , "min Dist = " + min_dist);
	     LinkedList<DMatch> good_matches = new LinkedList<DMatch>();

	     for( int i = 0; i < descriptors_object.rows(); i++ )
	      { 
	    	 Log.d(TAG , "MatchList dist = " + matchesList.get(i).distance);
	    	 if( matchesList.get(i).distance <= 2.5 * min_dist ) 
	         { good_matches.addLast( matchesList.get(i));
	        }
	      }

	     MatOfDMatch goodMatches = new MatOfDMatch();
	     goodMatches.fromList(good_matches);
	     
	     Mat outImg = new Mat(scene.size() , scene.type());
	     scene.copyTo(outImg);
	     //Features2d.drawMatches(object, keypoints_object, scene, keypoints_scene, goodMatches, outImg, new Scalar(0,255,0), new Scalar(255,0,0), drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
	     
	     LinkedList<Point> objList = new LinkedList<Point>();
	     LinkedList<Point> sceneList = new LinkedList<Point>();
	     List<DMatch> good_matches_list = goodMatches.toList();

	     List<KeyPoint> keypoints_objectList = keypoints_object.toList();
	     List<KeyPoint> keypoints_sceneList = keypoints_scene.toList();

	     for(int i = 0; i<good_matches_list.size(); i++)
	     {
	         objList.addLast(keypoints_objectList.get(good_matches_list.get(i).queryIdx).pt);
	         sceneList.addLast(keypoints_sceneList.get(good_matches_list.get(i).trainIdx).pt);
	     }
	     
	     MatOfPoint2f obj = new MatOfPoint2f();
	     obj.fromList(objList);

	     MatOfPoint2f sce = new MatOfPoint2f();

	     sce.fromList(sceneList);
	     
	     if(objList.size() < 4 || sceneList.size() < 4)
	     {
	    	 return;
	     }
	     //findHomography 
	     Mat hg = Calib3d.findHomography(obj, sce, Calib3d.RANSAC, 10);

	     Mat obj_corners = new Mat(4,1,CvType.CV_32FC2);
	     Mat scene_corners = new Mat(4,1,CvType.CV_32FC2);

	     obj_corners.put(0, 0, new double[] {0,0});
	     obj_corners.put(1, 0, new double[] {object.cols(),0});
	     obj_corners.put(2, 0, new double[] {object.cols(),object.rows()});
	     obj_corners.put(3, 0, new double[] {0,object.rows()});
	     
	     Core.perspectiveTransform(obj_corners, scene_corners, hg);

	     Core.line(outImg, new Point(scene_corners.get(0,0)), new Point(scene_corners.get(1,0)), new Scalar(0, 0, 255),4);
	     Core.line(outImg, new Point(scene_corners.get(1,0)), new Point(scene_corners.get(2,0)), new Scalar(0, 0, 255),4);
	     Core.line(outImg, new Point(scene_corners.get(2,0)), new Point(scene_corners.get(3,0)), new Scalar(0, 0, 255),4);
	     Core.line(outImg, new Point(scene_corners.get(3,0)), new Point(scene_corners.get(0,0)), new Scalar(0, 0, 255),4);
	     //Log.d(TAG, "Corners: " + scene_corners.get(0, 0) + "," + scene_corners.get(1, 0) + "," + scene_corners.get(2, 0) + "," + scene_corners.get(3, 0) );
	     
	     ImageView iv = (ImageView) findViewById(R.id.image);
	     Bitmap imageMatched = Bitmap.createBitmap(outImg.cols(), outImg.rows(), Bitmap.Config.RGB_565);//need to save bitmap
	     Utils.matToBitmap(outImg, imageMatched);
	     iv.setImageBitmap(imageMatched);
	     
    }

}
