package com.example.projectone;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.nfc.Tag;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;

public class PictureHandle {
    //分割图片时投影分割的参数约束
    private static final int min_tresh = 2; //波峰最小幅度
    private static final int min_range = 2;//波峰最小间隔
    //private static int min_wscnt = 110;

    //分割图片时的分割模式标记
    private static final int VPRO = 0;
    private static final int HPRO = 1;

    private static final int BLACK = 0;
    private static final int WITHE = 255;

    private static final int SWidth = 28;//standard
    private static final int SHeight = 28;

    private static final String TAG = "Picture Handle";
    private static final String filepath = Environment.getExternalStorageDirectory().getPath()+ "/HmwkCheck";
    private static int dcntrow = 0, dcntcol = 0;
    private static int cnt = 0;


    //灰度化->双边滤波降噪->二值化
    public static Bitmap BinarizationWithDenoising_Opencv(Bitmap bitmap,int d){
        Log.i(TAG,"begin to binarizaiton");
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Mat origin = new Mat();
        Mat gray = new Mat();
        Mat bf = new Mat();
        Mat out = new Mat();
        Utils.bitmapToMat(bitmap, origin);
        Imgproc.cvtColor(origin, gray, Imgproc.COLOR_RGBA2GRAY);
        Log.i(TAG,"After cvtColor");

        Imgproc.bilateralFilter(gray, bf, d, (double) (d * 2), (double) (d / 2));
        Log.i(TAG,"After denoising");
        Imgproc.adaptiveThreshold(bf, out, 255.0D, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 7, 2.5D);
        Log.i(TAG,"After adaptiveThreshold");

        Utils.matToBitmap(out, result);
        origin.release();
        gray.release();
        bf.release();
        out.release();
        return result;
    }

    //在图片四周填充白色背景使得图片居于正中间，然后缩放到ssize
    public static Bitmap resize_Opencv(Bitmap bitmap, int ssize) {
        Bitmap res = Bitmap.createBitmap(ssize,ssize,Bitmap.Config.ARGB_8888);
        Mat origin = new Mat();
        Mat out  = new Mat();
        Size size = new Size(ssize, ssize);
        Utils.bitmapToMat(bitmap,origin);
        //先扩充成正方形
        Mat smat = new Mat();
        int width = origin.cols(),height = origin.rows();
        Scalar value = new Scalar(WITHE,WITHE,WITHE,WITHE);
        //计算填充后的大小
        int Flength = (int)(max(width,height)*2);
        Core.copyMakeBorder(origin,smat,(Flength-height)/2,(Flength-height)/2,(Flength-width)/2,(Flength-width)/2, Core.BORDER_CONSTANT,value);
        //saveImg(filepath+"/afterseg/expand"+(cnt++)+".png",smat);
        //缩放
        Imgproc.resize(smat,out,size,0,0,Imgproc.INTER_AREA);
        Utils.matToBitmap(out,res);
        origin.release();
        out.release();
        return res;
    }

    //不填充直接缩放到28*28
    public static Bitmap resize_28_Opencv_withoutfill(Bitmap bitmap) {
        Bitmap res = Bitmap.createBitmap(SWidth,SHeight,Bitmap.Config.ARGB_8888);
        Mat origin = new Mat();
        Mat out  = new Mat();
        Size size = new Size(SWidth,SHeight);
        Utils.bitmapToMat(bitmap,origin);
        //缩放
        Imgproc.resize(origin,out,size,0,0,Imgproc.INTER_AREA);
        Utils.matToBitmap(out,res);
        origin.release();
        out.release();
        return res;
    }

    //腐蚀和膨胀
    public static Bitmap erodeAnddialte_Opencv(Bitmap bitmap,int type){
        Bitmap res = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(),Bitmap.Config.ARGB_8888);
        Mat origin = new Mat();
        Mat out = new Mat();
        Mat structImg = Imgproc.getStructuringElement(Imgproc.MARKER_CROSS,new Size(2,2));
        Utils.bitmapToMat(bitmap,origin);
        switch (type){
            case 0:
                Imgproc.morphologyEx(origin,out,Imgproc.MORPH_CLOSE,structImg);
                break;
            case 1:
                Imgproc.morphologyEx(origin,out,Imgproc.MORPH_OPEN,structImg);
                break;
            case 2:
                Imgproc.dilate(origin,out,structImg);
                break;
            case 3:
                Imgproc.erode(origin,out,structImg);
                break;
            default:
                throw new AssertionError("No such a type");
        }
        Utils.matToBitmap(out,res);
        return res;
    }

    //根据bitmap保存图片
    public static void savebitmap(String path, Bitmap bitmap){
        Mat origin = new Mat();
        Utils.bitmapToMat(bitmap,origin);
        try{
            Imgcodecs.imwrite(path,origin);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG,"save failed.");
        }
    }

    //根据Mat保存图片
    public static void saveImg(String path,Mat img){
        try{
            Imgcodecs.imwrite(path,img);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG,"save failed.");
        }
    }

    //绘制投影(会在投影分割时调用，主要作用是保存投影图，方便使用时测试)
    @RequiresApi(api = Build.VERSION_CODES.N)//android 7
    public static void Drawprojection(int[] pos, int mode){
        if(mode == HPRO){
            int width = Arrays.stream(pos).max().getAsInt();
            int height = pos.length;
            Mat project = Mat.zeros(height,width, CvType.CV_8SC1);//黑底图
            for(int i = 0; i < project.rows(); ++i){
                for(int j = 0; j < pos[i]; ++j){
                    project.put(i,j,WITHE);
                }
            }
            saveImg(filepath+"/pic/row"+(dcntrow++)+".png",project);
        }
        else if(mode == VPRO){
            int width = pos.length;
            int height = Arrays.stream(pos).max().getAsInt();
            Mat project = Mat.zeros(height,width,CvType.CV_8SC1);
            for(int i = 0; i < project.cols(); ++i){
                for(int j = 0; j < pos[i]; ++j){
                    project.put(j,i, WITHE);
                }
            }
            saveImg(filepath+"/pic/col"+(dcntcol++)+".png",project);
        }
    }

    /*不同分割方法*/

//    //水平投影切割后连通域切割
//    @RequiresApi(api = Build.VERSION_CODES.N)
//    public static List<Mat> cutImg(Bitmap bitmap){
//        Mat origin = new Mat();
//        Utils.bitmapToMat(bitmap,origin);
//        List<Mat> ret = new ArrayList<>();
//        //水平投影分割为一行一行的图片，当模式为HPRO时粒度(granularity)没有影响。
//        List<Mat> ycutpoint = cutImginmode(origin,HPRO,"characters");
//        //每一行进行连通域分割。
//        for(int i = 0; i < ycutpoint.size(); ++i){
//            Bitmap temp = Bitmap.createBitmap(ycutpoint.get(i).cols(),ycutpoint.get(i).rows(),Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(ycutpoint.get(i),temp);
//            //连通域分割
//            List<Mat> ss = cutImgbyseedfill(temp);
//            ret.addAll(ss);
//        }
//        return ret;
//    }
//
//    //投影切割
//    @RequiresApi(api = Build.VERSION_CODES.N)
//    public static List<Mat> cutImgbyprojection(Bitmap bitmap,String granularity){
//       Mat origin = new Mat();
//       Utils.bitmapToMat(bitmap,origin);
//       List<Mat> ret = new ArrayList<>();
//        //得到了每一行图片的矩阵。
//       List<Mat> ycutpoint = cutImginmode(origin,HPRO, granularity);
//       for(int i = 0; i < ycutpoint.size(); ++i){
//           //得到了单位列矩阵
//           List<Mat> xcutpoint = cutImginmode(ycutpoint.get(i), VPRO, granularity);
//          for(int j = 0; j < xcutpoint.size(); ++j){
//               Mat tmp = RemoveWS_Vertical(xcutpoint.get(j));
//               List<Mat> finalcutpoint = cutImginmode(tmp,HPRO, granularity);
//               for(int k = 0; k < finalcutpoint.size(); ++k){
//                   ret.add(finalcutpoint.get(k));
//               }
//          }
//       }
//       origin.release();
//       return ret;
//    }
//
//    //全连通域切割
//    public static List<Mat> cutImgbyseedfill(Bitmap bitmap){
//        Mat origin  = new Mat();
//        Utils.bitmapToMat(bitmap,origin);
//        int label = 0;
//        ArrayList<BOCD> bocds = new ArrayList<>();
//        int[][] mmap = new int[origin.rows()][origin.cols()];
//        Stack<Point> stack = new Stack<>();
//        stack.clear();
//
//        for(int j = 0; j < origin.cols(); ++j){
//            for (int i = 0; i < origin.rows(); ++i){
//                if(BLACK == (int)origin.get(i, j)[0] && mmap[i][j] == 0){
//                    mmap[i][j] = ++label;
//                    stack.push(new Point(j,i));
//                    bocds.add(new BOCD(label,j,j,i,i));
//                    //get the whole connected domain
//                    while(!stack.empty()){
//                        Point seed = stack.pop();
//                        int c = (int)seed.x, r = (int) seed.y;
//                        //top
//                        if(r - 1 >= 0 && mmap[r-1][c] == 0 && BLACK == (int)origin.get(r-1,c)[0]){
//                            mmap[r-1][c] = label;
//                            stack.push(new Point(c,r-1));
//                            //update top bound
//                            BOCD bocd = bocds.get(label-1);//from zero
//                            bocd.setTop(min(r-1,bocd.getTop()));
//                            bocds.set(label-1,bocd);
//                        }
//                        //bottom
//                        if(r + 1 < origin.rows() && mmap[r+1][c] == 0 && BLACK == (int)origin.get(r+1,c)[0]){
//                            mmap[r+1][c] = label;
//                            stack.push(new Point(c,r+1));
//                            //update bottom bound
//                            BOCD bocd = bocds.get(label-1);
//                            bocd.setBottom(max(r+1,bocd.getBottom()));
//                            bocds.set(label-1,bocd);
//                        }
//                        //left
//                        if(c - 1 >= 0 && mmap[r][c-1] == 0 && BLACK == (int)origin.get(r,c-1)[0]){
//                            mmap[r][c-1] = label;
//                            stack.push(new Point(c-1,r));
//                            //update left bound
//                            BOCD bocd = bocds.get(label-1);
//                            bocd.setLeft(min(c-1,bocd.getLeft()));
//                            bocds.set(label-1,bocd);
//                        }
//                        //right
//                        if(c + 1 < origin.cols() && mmap[r][c+1] == 0 && BLACK == (int)origin.get(r,c+1)[0]){
//                            mmap[r][c+1] = label;
//                            stack.push(new Point(c+1,r));
//                            //update right bound
//                            BOCD bocd = bocds.get(label-1);
//                            bocd.setRight(max(c+1,bocd.getRight()));
//                            bocds.set(label-1,bocd);
//                        }
//                        //left-top
//                        if(c - 1 >= 0 && r - 1 >= 0 && mmap[r-1][c-1] == 0 && BLACK == (int)origin.get(r-1,c-1)[0]){
//                            mmap[r-1][c-1] = label;
//                            stack.push(new Point(c-1,r-1));
//                            //update left and top bound
//                            BOCD bocd = bocds.get(label-1);
//                            bocd.setLeft(min(c-1,bocd.getLeft()));
//                            bocd.setTop(min(r-1,bocd.getTop()));
//                            bocds.set(label-1,bocd);
//                        }
//                        //left-bottom
//                        if(c-1 >= 0 && r+1 < origin.rows() && mmap[r+1][c-1] == 0 && BLACK == (int)origin.get(r+1,c-1)[0]){
//                            mmap[r+1][c-1] = label;
//                            stack.push(new Point(c-1, r+1));
//                            //update left and bottom bound
//                            BOCD bocd = bocds.get(label-1);
//                            bocd.setLeft(min(c-1,bocd.getLeft()));
//                            bocd.setBottom(max(r+1,bocd.getBottom()));
//                            bocds.set(label-1,bocd);
//                        }
//                        //right-top
//                        if(c+1 < origin.cols() && r-1 >= 0 && mmap[r-1][c+1] == 0 && BLACK == (int)origin.get(r-1,c+1)[0]){
//                            mmap[r-1][c+1] = label;
//                            stack.push(new Point(c+1,r-1));
//                            //update right and top bound
//                            BOCD bocd = bocds.get(label-1);
//                            bocd.setRight(max(c+1,bocd.getRight()));
//                            bocd.setTop(min(r-1,bocd.getTop()));
//                            bocds.set(label-1,bocd);
//                        }
//                        //right-bottom
//                        if(c+1 < origin.cols() && r+1 < origin.rows() && mmap[r+1][c+1] == 0 && BLACK == (int)origin.get(r+1,c+1)[0]){
//                            mmap[r+1][c+1] = label;
//                            stack.push(new Point(c+1,r+1));
//                            //update right and bottom bound
//                            BOCD bocd = bocds.get(label-1);
//                            bocd.setRight(max(c+1,bocd.getRight()));
//                            bocd.setBottom(max(r+1,bocd.getBottom()));
//                            bocds.set(label-1,bocd);
//                        }
//                    }
//                }
//            }
//        }
//
//        List<Mat> ret = new ArrayList<>();
//
//        for(int i = 0; i < bocds.size();++i){
//            BOCD bocd = bocds.get(i);
//            //System.out.println(i+" left: "+bocd.getLeft()+" right: "+bocd.getRight()+" top: "+bocd.getTop()+" bottom: "+bocd.getBottom());
//            int width = bocd.getRight()-bocd.getLeft()+1;
//            int height = bocd.getBottom()-bocd.getTop()+1;
//            Mat res = new Mat(origin,new Rect(bocd.getLeft(),bocd.getTop(),width,height));
//            ret.add(res);
//        }
//        return ret;
//    }

    public static Mat RemoveWS_Vertical(Mat origin){
        int nWidth = origin.cols(), nHeight = origin.rows() ;
        int up = nHeight - 1, bottom = 0;
        for (int i = 0; i < nHeight; ++i){
            for (int j = 0; j < nWidth; ++j){
                if (((int)origin.get(i,j)[0]) == BLACK) {
                    up = Math.min(i,up);
                    bottom = Math.max(i,bottom);
                }
            }
        }
        Mat ret = new Mat(origin,new Rect(0,up,nWidth,bottom - up));
        return ret;
    }

    //投影并分割
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static List<Mat> cutImginmode(Mat origin,int mode,String granularity){
        if(mode == HPRO){
            int nWidth = origin.cols(), nHeight = origin.rows();
            int[] xNum = new int[nHeight];
            // 统计出每行黑色像素点的个数
            for (int i = 0; i < nHeight; i++) {
                for (int j = 0; j < nWidth; j++) {
                    if (((int)origin.get(i,j)[0]) == BLACK) {
                        xNum[i]++;
                    }
                }
            }
            //画图
           //Drawprojection(xNum,mode);
            //分割
            List<Mat> YMat = new ArrayList<>();
            int begin =-1,end = 0;
            for(int i = 0; i < xNum.length; ++i){
                if(xNum[i] > min_tresh && begin == -1){
                    begin = i;
                }
                else if((xNum[i] < min_tresh && begin != -1)||(i == xNum.length-1 && begin != -1)){
                    end = i;
                    // 粒度大小无影响
                    if(end - begin >= min_range){//find a row
                         int height = end - begin;
                         Mat temp = new Mat(origin,new Rect(0,begin,nWidth,height));
                         Mat t = new Mat();
                         temp.copyTo(t);
                         YMat.add(t);
                    }
                    begin =-1;
                    end = 0;

                }
                else if(xNum[i] > min_tresh && begin !=-1){
                    continue;
                }
                else if(xNum[i] < min_tresh || begin == -1) {
                    continue;
                }
            }
            return YMat;
        }
        else if(mode == VPRO){
            int nWidth = origin.cols(), nHeight = origin.rows();
            int[] yNum = new int[nWidth];
            int[] bpNum = new int[nWidth];//统计每一列空白像素个数
            // 统计出每列黑色像素点的个数
            for (int i = 0; i < nWidth; i++) {
                for (int j = 0; j < nHeight; j++) {
                    if (((int)origin.get(j,i)[0]) == BLACK) {
                        yNum[i]++;
                    }
                    else
                        bpNum[i]++;
                }
            }
            //画图
            Drawprojection(yNum,mode);

            //分割
            List<Mat> XMat = new ArrayList<>();

            if(granularity.equals("characters")){
                int begin =-1,end = 0;
                for(int i = 0; i < yNum.length; ++i){
                    if(yNum[i] > min_tresh && begin == -1){
                        begin = i;
                    }
                    else if((yNum[i] < min_tresh && begin != -1)||(i == yNum.length-1 && begin != -1)){
                        end = i;
                        if(end - begin >= min_range){//find a column
                            int width = end - begin;
                            Mat temp = new Mat(origin,new Rect(begin,0,width,nHeight));
                            Mat t = new Mat();
                            temp.copyTo(t);
                            XMat.add(t);
                        }
                        begin=-1;
                        end = 0;
                    }
                    else if(yNum[i] > min_tresh && begin != -1){
                        continue;
                    }
                    else if(yNum[i] < min_tresh || begin == -1) {
                        continue;
                    }
                }
                return XMat;
            }
            else if(granularity.equals("pieces")){
                //计算阈值
                int total = 0, num = 0,average;
                int frontindex = 0,backindex = nWidth-1;
                while(bpNum[frontindex] == nHeight){
                    frontindex++;
                    if(frontindex == nWidth)
                        break;
                }
                while(bpNum[backindex] == nHeight){
                    backindex--;
                    if(backindex == 0)
                        break;
                }

                for(; frontindex <= backindex; ++frontindex){
                    int cnt = 0;
                    if(bpNum[frontindex] == nHeight){
                        num++;
                    }

                    while(bpNum[frontindex] == nHeight){
                        cnt++;
                        frontindex++;
                        if(frontindex == nWidth)
                            break;
                    }
                    total += cnt;
                }
                average = num == 0 ? total : total/num;
                Log.w("Average is ",String.valueOf(average));

                //分割
                int begin =-1,end = 0;
                int wscnt = 0;
                int curspcnt = 0, oldspcnt = 0;
                for(int i = 0; i < yNum.length; ++i){
                    if(yNum[i] > min_tresh && begin == -1){
                        begin = i;
                        wscnt = 0;
                        oldspcnt = curspcnt = 0;
                    }
                    else if(i == yNum.length-1 && begin != -1){//need before <min_tresh
                        end = i;
                        int width = end - begin - wscnt + 5;
                        Mat temp = new Mat(origin,new Rect(begin,0,width,nHeight));
                        Mat t = new Mat();
                        temp.copyTo(t);
                        XMat.add(t);
                        begin=-1;
                        end = 0;
                    }
                    else if(yNum[i] < min_tresh && begin != -1){
                        end = i;
                        if(wscnt > 4*average){//find a column //curspcnt - oldspcnt > 110
                            int width = end - begin - wscnt + 5;
                            Mat temp = new Mat(origin,new Rect(begin,0,width,nHeight));
                            Mat t = new Mat();
                            temp.copyTo(t);
                            XMat.add(t);
                            begin=-1;
                            end = 0;
                        }
                        else{
                            end = 0;
                            wscnt++;
                            curspcnt++;
                        }

                    }
                    else if(yNum[i] > min_tresh && begin != -1){
                        wscnt = 0;
                        if(curspcnt > 0){
                            oldspcnt = curspcnt;
                            curspcnt = 0;
                        }
                    }
                    else if(yNum[i] < min_tresh || begin == -1) {
                        wscnt = 0;
                    }
                }
                return XMat;
            }
            else{
                throw new AssertionError("No such a granularity");
            }
        }
        else
            return null;
    }

    //分割成单个算式
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static List<Mat> cutIntopieces(Bitmap bitmap){
        Mat origin = new Mat();
        Utils.bitmapToMat(bitmap,origin);
        List<Mat> ret = new ArrayList<>();
        //得到了每一行图片的矩阵。
        List<Mat> ycutpoint = cutImginmode(origin,HPRO,"pieces");
        int i = 0;
        for(Mat each : ycutpoint){
            saveImg(filepath+"/"+(i++)+".png",each);
            List<Mat>  tmp = cutImginmode(each,VPRO,"pieces");
            for (Mat res: tmp) {
                ret.add(res);
            }
        }
        return ret;
    }

    //分割每一个算式到单个字符
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static List<Mat> cuteachpieces(Mat origin){
        List<Mat> ret = new ArrayList<>();
        List<Mat> mats = cutImginmode(origin,VPRO,"characters");
        for(int i = 0; i < mats.size(); ++i){
            Mat tmp = RemoveWS_Vertical(mats.get(i));
            saveImg(filepath+"/afterseg/cut"+i+".png",tmp);
            ret.add(tmp);
        }
        return ret;
    }


    //旋转
    public  static Bitmap rotateBitmap(Bitmap bitmap, int degree) {
        Bitmap ret = null;
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            ret = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(ret == null){
            ret = bitmap;
        }
        if(ret != bitmap){
            bitmap.recycle();
        }
        return ret;
    }
    public  static int getBitmapdgree(String path) {
        int ret = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:
                    ret = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    ret = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    ret = 270;
                    break;
                default:
                    break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, String.valueOf(ret));
        return ret;
    }







}

//用于连通域分割
class BOCD{
    private int index;
    private int left,right,top,bottom;

    public BOCD(int index){
        this.index = index;
        left = right = top = bottom = 0;
    }
    public BOCD(int index,int left,int right,int top,int bottom){
        this.index = index;
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }
    public int getIndex(){
        return index;
    }
    public int getBottom() {
        return bottom;
    }
    public int getLeft() {
        return left;
    }
    public int getRight() {
        return right;
    }
    public int getTop() {
        return top;
    }
    public void setIndex(int index) {
        this.index = index;
    }
    public void setBottom(int bottom) {
        this.bottom = bottom;
    }
    public void setLeft(int left) {
        this.left = left;
    }
    public void setTop(int top) {
        this.top = top;
    }
    public void setRight(int right) {
        this.right = right;
    }
}
