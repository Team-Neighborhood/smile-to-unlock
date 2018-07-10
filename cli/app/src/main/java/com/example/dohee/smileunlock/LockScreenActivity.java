package com.example.dohee.smileunlock;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class LockScreenActivity extends Activity implements SurfaceHolder.Callback{
    /* 소요시간 측정을 위한 변수들 */
    private long nStart = 0, nEnd = 0;

    // 파이어베이스 부분
    private FirebaseStorage storage;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private static FirebaseDatabase database;

    private String unlockSignal;
    private List<String> inform= new ArrayList<>();

    // 카메라 부분
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    boolean previewing = false;
    private Context context = this;
    //private LayoutInflater controlInflater = null;

    // 화면 구성
    private Button buttonTakePicture;

    static{

        database = FirebaseDatabase.getInstance();
        //database.setPersistenceEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lockscreen);

        //storage 이용
        storage = FirebaseStorage.getInstance();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        getWindow().setFormat(PixelFormat.UNKNOWN);

        surfaceView = (SurfaceView)findViewById(R.id.cameraPreivew);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        buttonTakePicture = (Button)findViewById(R.id.takePicture);
        buttonTakePicture.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                //database.getReference().child("unlock").child("signal").setValue("true");
                //ActivityCompat.finishAffinity(LockScreenActivity.this);
                camera.takePicture(null, null, myPictureCallback_JPG);

            }});

        database.getReference().child("unlock")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            unlockSignal = snapshot.getValue().toString();
//                            Log.i(TAG,"dd"+snapshot.getValue().toString());

                            if(unlockSignal.equals("true")){
                                ActivityCompat.finishAffinity(LockScreenActivity.this);
                                database.getReference().child("unlock").child("signal").setValue("false");
                            }

                            //Toast.makeText(context, unlockSignal, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        database.getReference().child("tryUnlock")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            inform.add(snapshot.getValue().toString());
                            database.getReference().child("tryUnlock").removeValue();

                            //Toast.makeText(context, unlockSignal, Toast.LENGTH_SHORT).show();
                        }

                        Toast toast;

                        if(!inform.isEmpty()) {
                            if (inform.size() == 1) {
                                toast = Toast.makeText(context, "등록되지 않은 사용자입니다.", Toast.LENGTH_SHORT);
                                toast.setGravity(Gravity.CENTER, 0, 200);
                                ViewGroup group = (ViewGroup) toast.getView();
                                TextView messageTextView = (TextView) group.getChildAt(0);
                                messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
//                                messageTextView.setTextColor(Color.BLACK);
                                toast.show();
                            }
                            else if (!inform.get(1).equals("Smile")) {
                                toast = Toast.makeText(context, inform.get(0) + "님 웃어주세요:)", Toast.LENGTH_SHORT);
                                toast.setGravity(Gravity.CENTER, 0, 200);
                                ViewGroup group = (ViewGroup) toast.getView();
                                TextView messageTextView = (TextView) group.getChildAt(0);
                                messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
//                                messageTextView.setTextColor(Color.BLACK);
                                toast.show();
                            }
                            else {
                                toast = Toast.makeText(context, inform.get(0) + "님 반갑습니다:)", Toast.LENGTH_SHORT);
                                toast.setGravity(Gravity.CENTER, 0, 200);
                                ViewGroup group = (ViewGroup) toast.getView();
                                TextView messageTextView = (TextView) group.getChildAt(0);
                                messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
//                                messageTextView.setTextColor(Color.BLACK);
                                toast.show();
                            }

                            if (camera != null){
                                try {
                                    camera.setPreviewDisplay(surfaceHolder);
                                    camera.startPreview();
                                    camera.setDisplayOrientation(90);
                                    previewing = true;
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }

                            inform.clear();
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }



    Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback(){

        @Override
        public void onAutoFocus(boolean arg0, Camera arg1) {
            // TODO Auto-generated method stub
            buttonTakePicture.setEnabled(true);
        }};

    Camera.ShutterCallback myShutterCallback = new Camera.ShutterCallback(){

        @Override
        public void onShutter() {
            // TODO Auto-generated method stub

        }};

    Camera.PictureCallback myPictureCallback_RAW = new Camera.PictureCallback(){

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub

        }};

    public Bitmap byteArrayToBitmap(byte[] byteArray ) {
        Bitmap bitmap = BitmapFactory.decodeByteArray( byteArray, 0, byteArray.length ) ;
        return bitmap ;
    }

    Camera.PictureCallback myPictureCallback_JPG = new Camera.PictureCallback(){

        @Override
        public void onPictureTaken(byte[] data, Camera cam) {
            nStart = System.currentTimeMillis();
            if (data != null)
                Log.i(TAG, "JPEG 사진 찍었음!");
//            Bitmap bitmap = byteArrayToBitmap(data);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length,options);

            Matrix matrix = new Matrix();
            matrix.postRotate(-90);
            matrix.preScale(0.5f, -0.5f);

            bitmap= Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            String title= System.currentTimeMillis()+".jpg";
            String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, null, null);
            Uri imgUri = Uri.parse(path);


//            Bitmap bm = BitmapFactory.decodeFile(ab_path);
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
//            byte[] b = baos.toByteArray();
//
//            //socket
//            Runnable runner = new TCPFileSender(ab_path);
//            Thread worker = new Thread(runner);
//            worker.start();  //onResume()에서 실행.

//            Client cli = new Client(path);

            StorageReference storageRef = storage.getReference("TryToUnlock");

            StorageReference imgRef = storageRef.child(imgUri.getLastPathSegment()+".jpg");
            UploadTask uploadTask = imgRef.putFile(imgUri);

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    System.out.println("fail");
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                    //Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    System.out.println("success");

                    nEnd = System.currentTimeMillis();
                    Log.i(TAG, "unlock upload time: " + (nEnd - nStart) + "ms");
                }
            });

//            camera.startPreview();


//            if (data != null)
//                Log.i(TAG, "JPEG 사진 찍었음!");
//            Bitmap bitmap = byteArrayToBitmap(data);
//            Matrix matrix = new Matrix();
//            matrix.postRotate(-90);
//            matrix.preScale(0.5f, -0.5f); // 좌우반전
//            bitmap= Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//
//            // 파일 저장
//            String sd = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
//            File myDir = new File(sd+"/TryToUnlock");
//            if (!myDir.isDirectory()) myDir.mkdir();
//            String path = myDir.getPath()+'/' + (int) System.currentTimeMillis() + ".png";
//
//            File file = new File(path);
//            OutputStream out;
//
//            try {
//                file.createNewFile();
//                out = new FileOutputStream(file);
//                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
//                //path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, null, null);
//                out.flush();
//                out.close();
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//            Log.i(TAG, "주소:"+path);
//
//            //Uri imgUri = Uri.parse(path);
//            Uri imgUri = Uri.fromFile(file);
//
//            StorageReference storageRef = storage.getReferenceFromUrl("gs://unlock-app-6c0a4.appspot.com/");
//            StorageReference imgRef = storageRef.child(firebaseUser.getUid()).child("TryToUnlock").child(imgUri.getLastPathSegment());
//
//            UploadTask uploadTask = imgRef.putFile(imgUri);
//            // Register observers to listen for when the download is done or if it fails
//            uploadTask.addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception exception) {
//                    // Handle unsuccessful uploads
//                    System.out.println("fail");
//                }
//            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                @Override
//                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
//                    //Uri downloadUrl = taskSnapshot.getDownloadUrl();
//                    System.out.println("success");
//                }
//            });
//
//            Runnable runner = new TCPClient(data);
//            Thread worker = new Thread(runner);
//            worker.start();  //onResume()에서 실행.
//            nEnd = System.currentTimeMillis();
//            Log.i(TAG, "upload time: " + (nEnd - nStart) + "ms");
        }};

    private String getRealPathFromURI(Uri contentUri) {
        int column_index=0;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(cursor.moveToFirst()){
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        }

        return cursor.getString(column_index);
    }


    public void unlockScreen(View view) {
        //Instead of using finish(), this totally destroys the process
        //android.os.Process.killProcess(android.os.Process.myPid());
        ActivityCompat.finishAffinity(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub
        if(previewing){
            camera.stopPreview();
            previewing = false;
        }

        if (camera != null){
            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                camera.setDisplayOrientation(90);
                previewing = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        int cameraId = 0;
        /* 카메라가 여러개 일 경우 그 수를 가져옴  */
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for(int i=0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);

            /* 전면 카메라를 쓸 것인지 후면 카메라를 쓸것인지 설정 시 */
            /* 전면카메라 사용시 CAMERA_FACING_FRONT 로 조건절 */
            if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                cameraId = i;
        }

        camera = Camera.open(cameraId);

        Log.i(TAG, "카메라 미리보기 활성");

        try {
            camera.setPreviewDisplay(holder);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
        Log.i(TAG, "카메라 기능 해제");
    }



}
