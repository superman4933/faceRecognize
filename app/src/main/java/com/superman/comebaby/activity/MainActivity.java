package com.superman.comebaby.activity;

import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.faceplusplus.api.FaceDetecter;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;
import com.superman.comebaby.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    Button openImg;
    Button detectImg;
    Bitmap bitmap;
    ImageView imageView;
    TextView resultName;
    String dbFaceName;
    private final static int REQUEST_GET_IMG = 1;
    final public static String APIKey = "35a467be6126eda75a31818ddd9e483e";
    final public static String APISecret = "BRaGt8folSXNK6htil3410nMyejYkRyM";
    Handler handler = null;
    HandlerThread handlerThread = null;
    HttpRequests requests;
    FaceDetecter faceDetecter;
    ProgressDialog dialog;
    String faceId;
    int faceCount;
    String groupOne;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        groupOne = "groupOne";
        dialog = new ProgressDialog(this);
        handlerThread = new HandlerThread("detect");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
//        离线sdk的检查器和http请求对象初始化
        faceDetecter = new FaceDetecter();
        faceDetecter.init(this, APIKey);
        requests = new HttpRequests(APIKey, APISecret);
        imageView = (ImageView) findViewById(R.id.imageView);
        detectImg = (Button) findViewById(R.id.detectImg);
        openImg = (Button) findViewById(R.id.openImg);

//        打开图片按钮的方法
        openImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.action.GET_CONTENT");
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_GET_IMG);
            }
        });

//        检测图片中人脸
        detectImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWaitDialog();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        FaceDetecter.Face[] faceInfo = faceDetecter.findFaces(bitmap);
                        if (faceInfo == null) {
                            closeWaitDialog();
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, "检测不到人脸", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                            return;
                        }
//               如果检测到人脸则执行以下指令
                        final Bitmap bit = getFaceInfoBitmap(faceInfo, bitmap);
                        dbFaceName = "识别失败";

                        try {

                            JSONObject jsonFace = requests.offlineDetect(faceDetecter.getImageByteArray(),
                                    faceDetecter.getResultJsonString(), new PostParameters());
                            Log.d("打开图片的原始数据", jsonFace.toString());
                            String a = jsonFace.getString("face");
                            JSONArray b = new JSONArray(a);
                            faceCount = b.length();//检测出的人脸数量
                            Log.d("检测出的人脸数量：", "" + b.length());
                            JSONObject c = b.getJSONObject(0);//从数组里面再去获取json数据，只要第一组。
                            faceId = c.getString("face_id");//在这一组里去提取出face_id键值对形式
                            Log.d("从服务器获取的faceid", faceId);

//                            开始识别
                            PostParameters recoPerson = new PostParameters();
                            recoPerson.setKeyFaceId(faceId);
                            recoPerson.setGroupName(groupOne);
                            JSONObject faceResult = requests.recognitionIdentify(recoPerson);
                            Log.d("识别的原始结果", faceResult + "");

                            String faceResult2 = faceResult.getString("face");
                            JSONArray faceResult3 = new JSONArray(faceResult2);
                            JSONObject faceResult4 = faceResult3.getJSONObject(0);
                            String faceResult5 = faceResult4.getString("candidate");
                            JSONArray array2 = new JSONArray(faceResult5);
                            JSONObject faceResult6 = array2.getJSONObject(0);
                            String faceResult7 = faceResult6.getString("person_name");

                            dbFaceName = faceResult7;
                            String confidence = faceResult6.getString("confidence");

                            double confidence2 = Double.parseDouble(confidence);

                            Log.d("识别结果personName", faceResult5);
                            Log.d("识别结果的相似度", confidence2 + "");
//                            为什么关闭dialog指令在子线程中不会报错
                            closeWaitDialog();
                        } catch (Exception e) {
                            closeWaitDialog();
                            Log.d("识别失败", "s");

                            Toast.makeText(MainActivity.this, "识别失败", Toast.LENGTH_SHORT);
                            // TODO 自动生成的 catch 块
                            e.printStackTrace();
                        } finally {
                            System.gc();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                closeWaitDialog();
                                resultName.setText(dbFaceName);
                                imageView.setImageBitmap(bit);
                            }
                        });

                    }
                });


            }
        });

    }

    public void closeWaitDialog() {
        dialog.dismiss();
    }

    public void showWaitDialog() {
        dialog.setMessage("等我一会");
        dialog.setCancelable(false);
        dialog.setTitle("稍等");
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    //把人脸矩形框绘制到图片上
    public static Bitmap getFaceInfoBitmap(FaceDetecter.Face[] faceInfo, Bitmap newBit) {
        Bitmap tem = newBit.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(tem);
        Paint paint = new Paint();
        paint.setColor(0xffff0000);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);
        for (FaceDetecter.Face localFaceInfo : faceInfo) {
            RectF rectF = new RectF(newBit.getWidth() * localFaceInfo.left,
                    newBit.getHeight() * localFaceInfo.top, newBit.getWidth() * localFaceInfo.right
                    , newBit.getHeight() * localFaceInfo.bottom);
            canvas.drawRect(rectF, paint);
        }
        return tem;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_GET_IMG: {
                    if (data != null) {
                        Uri localUri = data.getData();
                        String imagePath = null;
                        if (DocumentsContract.isDocumentUri(this, localUri)) {
                            Log.d("activity", "1");
                            String docId = DocumentsContract.getDocumentId(localUri);
                            if ("com.android.providers.media.documents".equals(localUri.getAuthority())) {
                                Log.d("activity", "2");
                                String id = docId.split(":")[1];
                                String selection = MediaStore.Images.Media._ID + "=" + id;
                                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);

                            } else if ("com.android.providers.downloads.documents".equals(localUri.getAuthority())) {
                                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://" +
                                        "downloads/public_downloads"), Long.valueOf(docId));
                                imagePath = getImagePath(contentUri, null);
                                Log.d("activity", "3");
                            }
                        } else if ("content".equalsIgnoreCase(localUri.getScheme())) {
                            Log.d("activity", localUri + "");
                            imagePath = getImagePath(localUri, null);


                        }
                        displayImage(imagePath);
                        Log.d("activity", "5");
                    }
                    break;
                }
                default: {

                }
            }

        }


    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        Log.d("activity", "6");
        if (cursor != null) {
            Log.d("activity", "7");
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            Log.d("activity", "8");
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        if (imagePath != null) {
            bitmap = BitmapFactory.decodeFile(imagePath);
            imageView.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "加载失败了", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        faceDetecter.release(this);
    }
}
