package com.superman.comebaby.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.faceplusplus.api.FaceDetecter;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;
import com.superman.comebaby.R;
import com.superman.comebaby.util.FaceMask;
import com.superman.comebaby.util.IsFaceInfo;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class loginActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    EditText edtTxt_password;
    Button button_login;
    Button button_switch;
    TextView textView_log;
    TextView textView_admin;
    JSONObject result;
    String verifyConfidence = "";
    String verifyIsSamePerson = "";
    int intVerifyConfidence;
    String log;
    SurfaceView surfaceView_preview;
    SurfaceHolder surfaceHolder;
    Camera camera;
    Handler handler;
    HandlerThread handlerThread;
    Handler handlerText;
    HandlerThread handlerThreadText;
    Handler handlerAdmin;
    HandlerThread handlerThreadAdmin;
    SurfaceHolder holder;
    JSONObject jsonFace;
    FaceDetecter faceDetecter = null;
    FaceDetecter faceDetecter2 = null;
    FaceMask faceMask;
    private int width;
    private int height;
    HttpRequests requests;
    int BACK_CAMERA = 0;
    int FRONT_CAMERA = 1;
    int cameraState = FRONT_CAMERA;//1为前置摄像头，0为后置摄像头
    ImageView imageView_display;
    Bitmap tem;
    int i = 0;
    String race = "";
    String gender = "";
    String smiling = "";
    String age = "";
    int intSmiling;
    String raceConfidence = "";
    JSONObject c;
    String faceId;
    PostParameters postParameters;
    byte[] ori;
    byte[] ori2;
    IsFaceInfo isFaceInfo;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        requests = new HttpRequests(MainActivity.APIKey, MainActivity.APISecret);
        Log.w("cameraac", FindFrontCamera() + "");
        isFaceInfo = new IsFaceInfo();
        handlerThread = new HandlerThread("login");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
//上面那个handler用来显示人脸的框下面那个用来与在线API互动去获取具体数据；
        handlerThreadText = new HandlerThread("viewText");
        handlerThreadText.start();
        handlerText = new Handler(handlerThreadText.getLooper());
        handlerThreadAdmin = new HandlerThread("admin");
        handlerThreadAdmin.start();
        handlerAdmin = new Handler(handlerThreadAdmin.getLooper());
        faceMask = (FaceMask) findViewById(R.id.faceMask_mask);
        button_login = (Button) findViewById(R.id.button_login);
        button_switch = (Button) findViewById(R.id.button_switch);
        textView_log = (TextView) findViewById(R.id.textView_log);
        textView_admin = (TextView) findViewById(R.id.textView_admin);
        imageView_display = (ImageView) findViewById(R.id.imageView_display);
        edtTxt_password = (EditText) findViewById(R.id.edtTxt_password);
        surfaceView_preview = (SurfaceView) findViewById(R.id.surfaceView_preview);
        surfaceHolder = surfaceView_preview.getHolder();
        surfaceHolder.addCallback(this);
        surfaceView_preview.setKeepScreenOn(true);
//        初始化检测器
        faceDetecter = new FaceDetecter();
        faceDetecter2 = new FaceDetecter();
        if (!faceDetecter.init(this, MainActivity.APIKey)) {
            Toast.makeText(loginActivity.this, "APIKey错误，请联系作者", Toast.LENGTH_LONG).show();
            Log.d("检测器初始化", "有错误");
        }
        if (!faceDetecter2.init(this, MainActivity.APIKey)) {
            Toast.makeText(loginActivity.this, "APIKey错误，请联系作者", Toast.LENGTH_LONG).show();
            Log.d("检测器初始化", "有错误");
        }
        faceDetecter.setTrackingMode(true);

        button_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (cameraState == FRONT_CAMERA) {
                    // TODO: 2016/4/18 打开后置摄像头
                    Log.d("switch", "1");
                    cameraState = BACK_CAMERA;
                    camera.setPreviewCallback(null);
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                    Log.d("switch", "2");
                    initCamera();
                    Log.d("switch", "3");

                    try {
                        camera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        Log.d("switch", "4");
                        camera.setDisplayOrientation(90);
                        Log.d("switch", "5");
                        camera.startPreview();
                        Log.d("switch", "6");
                        camera.setPreviewCallback(loginActivity.this);
                        Log.d("switch", "7");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    // TODO: 2016/4/18 打开前置摄像头
                    cameraState = FRONT_CAMERA;
                    camera.setPreviewCallback(null);
                    camera.stopPreview();
                    camera.release();
                    camera = null;

                    initCamera();

                    try {
                        camera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    camera.setDisplayOrientation(90);
                    camera.startPreview();
                    camera.setPreviewCallback(loginActivity.this);
                }
            }
        });


//创建一个数组，判断输入框里面的值在数组里面是否存在。
        final String[] passwordSet = {"123456", "258258"};

        button_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = edtTxt_password.getText().toString();
                boolean isPassword = Arrays.asList(passwordSet).contains(input);
                if (isPassword) {
                    Intent intent = new Intent(loginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    Log.d("isPassword", "密码正确，验证通过");
                } else {
                    Toast.makeText(loginActivity.this, "密码错误", Toast.LENGTH_LONG).show();
                    Log.d("isPassword", "密码错误，登录失败");
                }
            }
        });


        //TODO 在这里处理登录事宜，获取人脸图片（视频截取或拍照），获取faceId，调用/recognition/identify接口验证是否为管理员登录
        //TODO 或者直接用密码登录，密码存放于一个数组之中。

    }

    @Override
    protected void onResume() {
        super.onResume();
        initCamera();


    }

    private int FindFrontCamera() {
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }


    private void initCamera() {
        //       获取相机对象，设置摄像头参数
        camera = Camera.open(cameraState);
        Log.d("cameraNub", Camera.getNumberOfCameras() + "");
        Camera.getNumberOfCameras();


        Camera.Parameters pars = camera.getParameters();
        List<Camera.Size> supportedPreviewSizes = pars.getSupportedPreviewSizes();
        for (int ii = 0; ii < supportedPreviewSizes.size(); ii++) {
            Log.d("size", supportedPreviewSizes.get(ii).width + "宽" + supportedPreviewSizes.get(ii).height + "高");
        }

//        Log.d("尺寸", supportedPreviewSizes.get(1).width+""+supportedPreviewSizes.get(1).height);
//        width=supportedPreviewSizes.get(1).width;
//        height=supportedPreviewSizes.get(1).height;

//实际测试如果预览图像的分辨率太大，识别速度大大降低，所以手动设置了下

        width = 320;
        height = 240;
        pars.setPreviewSize(width, height);
        if (cameraState == BACK_CAMERA) {
            pars.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        camera.setParameters(pars);
    }


    @Override
    protected void onPause() {

        super.onPause();
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        faceDetecter.release(this);
        faceDetecter2.release(this);
        handlerThread.quit();
        handlerThreadText.quit();
        handlerThreadAdmin.quit();
        Log.d("onDestroy", "调用毁灭方法");
    }


    //Callback 的三个实现方法
    @Override
    public void surfaceCreated(SurfaceHolder holder) {


    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        this.holder = holder;

//       设置摄像头的预览参数
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        camera.setDisplayOrientation(90);
        camera.startPreview();
        camera.setPreviewCallback(this);
        Log.d("surfaceChanged", "运行中");
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    //   把yuv转换成bitmap
    public Bitmap yuv2bitmap(byte[] mData, Camera myCamera) {
        byte[] tmp;
        Bitmap bmp;
        Camera.Size size = myCamera.getParameters().getPreviewSize(); //获取预览大小
        final int w = size.width;  //宽度
        final int h = size.height;
        Log.d("size", w + ":" + h + "");
        final YuvImage image = new YuvImage(mData, ImageFormat.NV21, w, h, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream(mData.length);
        if (!image.compressToJpeg(new Rect(0, 0, w, h), 100, os)) {
            return null;
        }
        tmp = os.toByteArray();
        bmp = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
        return bmp;
    }

    //旋转bitmap方法2
    protected Bitmap bitmapRotate(float degrees, Bitmap baseBitmap) {
        // 创建一个和原图一样大小的图片
        Bitmap afterBitmap = Bitmap.createBitmap(baseBitmap.getWidth(),
                baseBitmap.getHeight(), baseBitmap.getConfig());
        Canvas canvas = new Canvas(afterBitmap);
        Paint paint = new Paint();
        Matrix matrix = new Matrix();
        // 根据原图的中心位置旋转
        matrix.setRotate(degrees, baseBitmap.getWidth() / 2,
                baseBitmap.getHeight() / 2);
        canvas.drawBitmap(baseBitmap, matrix, paint);
        return afterBitmap;
    }


    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        Log.d("test-onPreviewFrame", "运行中1");
        camera.setPreviewCallback(null);
        ori = new byte[width * height];
        ori2 = new byte[width * height * 3 / 2];

        handler.post(new Runnable() {
            FaceDetecter.Face[] faceinfo = null;


            @Override
            public void run() {
                Log.d("test-handler1", "运行中2");

                if (cameraState == FRONT_CAMERA) {
                    try {
                        int is = 0;
                        for (int x = width - 1; x >= 0; x--) {
                            for (int y = height - 1; y >= 0; y--) {
                                ori[is] = data[y * width + x];
                                is++;
                            }
                        }
                        faceinfo = faceDetecter.findFaces(ori, height,
                                width);
                        if (faceinfo != null) {
                            isFaceInfo.setFaceInfo(true);
                        } else {
                            isFaceInfo.setFaceInfo(false);
                        }
                        Log.d("faceinfo1", "" + isFaceInfo.getisFaceInfo() + "");
                        Log.d("detect", "前置摄像头检测人脸中");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    int delta;
                    int wh;
                    int ww;
                    int hh;
                    int tmpBack;
                    int tmp2;
                    //后置摄像头的预览帧转换，旋转并翻转，转换为灰度图
                    delta = 0;
                    for (int x = 0; x < width; x++) {
                        tmpBack = (height - 1) * width;
                        for (int y = height - 1; y >= 0; y--) {
                            ori2[delta] = data[tmpBack + x];
                            tmpBack -= width;
                            delta++;
                        }
                    }

                    wh = width * height;
                    ww = width / 2;
                    hh = height / 2;

                    for (int i = 0; i < ww; i++) {
                        tmp2 = width * (hh - 1);
                        for (int j = 0; j < hh; j++) {
                            ori2[delta] = data[wh + tmp2 + i];
                            ori2[delta + 1] = data[wh + tmp2 + i + 1];
                            delta += 2;
                            tmp2 -= width;
                        }
                    }
                    faceinfo = faceDetecter.findFaces(ori2, height,
                            width);
                    if (faceinfo != null) {
                        isFaceInfo.setFaceInfo(true);
                    } else {
                        isFaceInfo.setFaceInfo(false);
                    }
                    Log.d("detect", "后置摄像头检测人脸中");
                }
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Log.d("onpreview", "UI运行");
                        faceMask.setFaceInfo(faceinfo);
                    }
                });
//                loginActivity.this.camera.setPreviewCallback(loginActivity.this);
            }
        });
//        此处为第二个handler用来获取具体数据,然并卵,其实没这个必要，UI处理时还是post到主线程，该卡还是卡
        handlerText.post(new Runnable() {
            FaceDetecter.Face[] faceinfoBitmap = null;

            @Override
            public void run() {
                Log.d("test:handlerText", "运行中3");
                Log.d("测试值2", "" + faceDetecter.getImageByteArray() + "");
                try {
//                    与在线API互动，上传本地识别结果，获取详细的线上识别信息
                    Log.d("faceinfo2", "" + isFaceInfo.getisFaceInfo() + "");
//                    如果检测到人脸就执行下面语句
                    if (isFaceInfo.getisFaceInfo()) {
//                        把原始的YUV帧转换为bitmap
                        i = i + 1;
                        Log.d("i", "" + i + "");
//                        用i控制调用在线api的频率，频繁调用会导致服务器错误，对外表现为人脸检测/*
// 到存在多少毫秒后，开始进行图像转换，调用在线api*/
                        if (i >= 15) {
                            tem = yuv2bitmap(data, camera);
                            if (cameraState == FRONT_CAMERA) {
                                tem = bitmapRotate(-90, tem);
                            } else {
                                tem = bitmapRotate(90, tem);
                            }
                            i = 0;
                            Log.d("if--test:i", "" + i + "");
                            faceinfoBitmap = faceDetecter2.findFaces(tem);
                            Log.d("测试值3", "" + faceDetecter2.getResultJsonString() + "");
                            jsonFace = requests.offlineDetect(faceDetecter2.getImageByteArray(),
                                    faceDetecter2.getResultJsonString(), new PostParameters());
                            Log.d("jsonFace", "" + jsonFace + "");
                            /*手动对已经使用完的bitmap进行内存回收*实测效果不明显/
                            if(tem!=null&&!tem.isRecycled()){
                                tem.recycle();
                                tem=null;
                            }
                            if (bmp != null && !bmp.isRecycled()) {
                                bmp.recycle();
                                bmp=null;
                            }


//                    /*以上代码从线上JSON数据中解析出了faceId*/

//                        以下代码从线上JSON数据中解析出了人脸的各种属性，人种、性别、年龄等等。。

                            JSONObject attribute = c.getJSONObject("attribute");
                            JSONObject raceAttribute = attribute.getJSONObject("race");
                            JSONObject genderAttribute = attribute.getJSONObject("gender");
                            JSONObject smilingAttribute = attribute.getJSONObject("smiling");
                            JSONObject ageAttribute = attribute.getJSONObject("age");
                            race = raceAttribute.getString("value");
                            gender = genderAttribute.getString("value");
                            smiling = smilingAttribute.getString("value");
                            intSmiling = (int) ((Double.parseDouble(smiling)));
                            age = ageAttribute.getString("value");
                            raceConfidence = raceAttribute.getString("confidence");
                            Log.d("attribute_race", race + ":" + raceConfidence);
                            Log.d("attribute_gender", gender);
                            Log.d("attribute_smiling", smiling);
                            Log.d("attribute_age", age);

//手动运行垃圾回收器，会对整个内存进行扫描，理论上比较耗时,这里针对bitmap，实际内存的检测结果没有很大变化。。。可能自动回收也做的比较好吧
//                            System.gc();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        imageView_display.setImageBitmap(tem);
                        log = "识别结果：" + "\n" + "性别：" + gender + "\n" + "年龄:" + age + "\n" + "人种：" + race + "\n" +
                                "微笑度：" + intSmiling;
                        textView_log.setText(log);
                        Log.d("uiThread02", "运行中");
                    }
                });
            }
        });
        handlerAdmin.post(new Runnable() {
            @Override
            public void run() {
                Log.d("handlerAdmin", "判断管理员相似度线程运行1");
                ////                    以下代码开始设置参数为验证人脸做前期数据的准备
                try {
                    /*执行指令前先判断是否从图像处获取了faceId，这是从上个handler中获取的，因为已经做过
                    * 有无人脸判断，这次在这个基础上再做一个有无faceId判断即可*/
                    if (faceId != null) {
                        Log.d("handlerAdmin", "判断管理员相似度线程运行1");
                        postParameters = new PostParameters();
                        Log.d("handlerAdmin", "判断管理员相似度线程运行2");
                        postParameters.setPersonName("me");
                        Log.d("handlerAdmin", "判断管理员相似度线程运行3");
                        postParameters.setFaceId(faceId);
                        Log.d("handlerAdmin", "判断管理员相似度线程运行4");
////                   在线获取验证的识别数据，准备解析出人脸相似度数据
//                        JSONObject trainResult = requests.trainVerify(postParameters);
//                        Log.d("trainResult",trainResult+"");
                        result = requests.recognitionVerify(postParameters);
                        verifyConfidence = result.getString("confidence");
                        intVerifyConfidence = (int) (Double.parseDouble(verifyConfidence));
                        verifyIsSamePerson = result.getString("is_same_person");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
//
                Log.d("识别结果", "" + verifyConfidence + verifyIsSamePerson);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView_admin.setText("与管理员相似度：" + intVerifyConfidence + "%" + verifyIsSamePerson);
                    }
                });
            }
        });
        loginActivity.this.camera.setPreviewCallback(loginActivity.this);
    }
}
