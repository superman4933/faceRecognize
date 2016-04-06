package com.superman.comebaby.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.view.LayoutInflater;
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
import com.superman.comebaby.db.Db;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    Button openImg;
    Button detectImg;
    Button addFace;
    Bitmap bitmap;
    ImageView imageView;
    TextView resultName;
    String dbFaceName;
    private final static int REQUEST_GET_IMG = 1;
    final String APIKey = "35a467be6126eda75a31818ddd9e483e";
    final String APISecret = "BRaGt8folSXNK6htil3410nMyejYkRyM";
    Handler handler = null;
    HandlerThread handlerThread = null;
    HttpRequests requests;
    FaceDetecter faceDetecter;
    ProgressDialog dialog;
    String faceId;
    int faceCount;
    private Db db;
    private SQLiteDatabase sqLiteDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultName = (TextView) findViewById(R.id.resultName);

        db = new Db(this, "face.db", null, 1);
        sqLiteDatabase = db.getReadableDatabase();

        dialog = new ProgressDialog(this);

        handlerThread = new HandlerThread("detect");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
//        离线sdk的检查器和http请求对象初始化

        faceDetecter = new FaceDetecter();
        faceDetecter.init(this, APIKey);
        requests = new HttpRequests(APIKey, APISecret);

//初始化，创建一个名为"firstFaceSet"的人脸集合(先从服务器获取数据判断，是否创建过这个人脸集合)
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("faceset", "1");
                try {
                    PostParameters postParameters = new PostParameters();
                    postParameters.setFacesetName("firstFaceSet");
//            JSONObject isCreateFaceSetResult=requests.facesetGetInfo(postParameters);
//            String faceSetName=isCreateFaceSetResult.getString("faceset_name");
//            if(faceSetName.equals("firstFaceSet")){
//                Log.d("faceset", "人脸已经创建过了："+faceSetName);
//                return;
//            }
                    requests.facesetCreate(postParameters);
                    Log.d("faceset", "人脸集合创建成功");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        imageView = (ImageView) findViewById(R.id.imageView);
        detectImg = (Button) findViewById(R.id.detectImg);
        openImg = (Button) findViewById(R.id.openImg);
//        添加到数据库按钮的方法
        addFace = (Button) findViewById(R.id.addFaceDb);
        addFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (faceCount != 1) {
                            Toast.makeText(MainActivity.this, "一次只能添加一张哦",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        final View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog, null);
                        dialog.setTitle("这是谁呢?");
//对话框的确定按钮方法
                        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        EditText inputName = (EditText) view.findViewById(R.id.inputName);
                                        String name = inputName.getText().toString();
                                        Log.d("123", name);

                                        ContentValues values = new ContentValues();
                                        values.put("name", name);
                                        values.put("face_id", faceId);
                                        sqLiteDatabase.insert("book", null, values);

                                        values.clear();
                                        Log.d("置入数据库成功", name);
                                        Log.d("置入数据库成功", faceId);

                                        try {
//                                  把faceid添加到服务器上面，并且运行训练模型，最后获取结果。
                                            PostParameters parameters = new PostParameters();
                                            parameters.setFaceId(faceId);
                                            parameters.setFacesetName("firstFaceSet");
//                                            添加到faceset
                                            requests.facesetAddFace(parameters);
//                                            运行训练模型
                                            PostParameters parameters2 = new PostParameters();
                                            parameters2.setFacesetName("firstFaceSet");
                                            JSONObject trainSession_id = requests.trainSearch(parameters2);
                                            String train_id = trainSession_id.getString("session_id");
                                            parameters2.setSessionId(train_id);
                                            JSONObject result = requests.infoGetSession(parameters2);
//                                    循环对结果分析，发现训练成功就停止
closeWaitDialog();

                                            Log.d("训练结果", result.toString());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }


                                    }
                                }

                        );

                        dialog.setNegativeButton("取消", new DialogInterface.OnClickListener()

                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }

                        );

                        dialog.setCancelable(false);
                        AlertDialog tmpdialog = dialog.create();
                        tmpdialog.setView(view);
                        tmpdialog.show();

                    }
                });
            }
        });

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
                            PostParameters parameters = new PostParameters();
                            parameters.setKeyFaceId(faceId);
                            parameters.setFacesetName("firstFaceSet");
                            parameters.setCount(3);
                            JSONObject faceResult = requests.recognitionSearch(parameters);
                            Log.d("识别的原始结果", faceResult+"");
                            String faceResult2 = faceResult.getString("candidate");
                            JSONArray faceResult3 = new JSONArray(faceResult2);
                            JSONObject faceResult4 = faceResult3.getJSONObject(0);
                            String faceResult5 = faceResult4.getString("face_id");
                            String similarity = faceResult4.getString("similarity");

                            double similarity2 = Double.parseDouble(similarity);

                            Log.d("识别结果的faceid", faceResult5);
                            Log.d("识别结果的相似度", similarity2+"");
                            Cursor cursor = sqLiteDatabase.query("Book", null, "face_id=?", new String[]{faceResult5}, null, null, null);
                            Log.d("本地数据库的查询结果为", cursor.moveToFirst() + "");

                            if (cursor.moveToFirst()&&similarity2>=80.0) {
                                dbFaceName = cursor.getString(cursor.getColumnIndex("name"));
                                Log.d("查询结果", dbFaceName);
                            }
                            if (cursor != null) {
                                cursor.close();
                            }
//                            为什么关闭dialog指令在子线程中不会报错
                            closeWaitDialog();
                        } catch (Exception e) {
                            closeWaitDialog();
                            Log.d("识别失败", "s");

                            Toast.makeText(MainActivity.this, "识别失败", Toast.LENGTH_SHORT);
                            // TODO 自动生成的 catch 块
                            e.printStackTrace();
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
