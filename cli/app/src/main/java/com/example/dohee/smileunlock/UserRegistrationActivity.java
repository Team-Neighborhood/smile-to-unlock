package com.example.dohee.smileunlock;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Vector;

import static android.content.ContentValues.TAG;

public class UserRegistrationActivity extends AppCompatActivity {

    /* 소요시간 측정을 위한 변수들 */
    private long nStart = 0, nEnd = 0;

    private FirebaseStorage storage;
    private static FirebaseDatabase database;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private String[] imagePath = new String[6];
//    private Vector<String> imagePath = new Vector<>(6);
    private static Vector<String> imageDownloadPath = new Vector<>();
    //UI
    ImageView img1, img2, img3, img4, img5, img6;
    EditText editTextName;
    Button buttonImage;
    FloatingActionButton buttonSave;

    //constant
    final int PICTURE_REQUEST_CODE = 100;

    static{
        //database
        database = FirebaseDatabase.getInstance();
//        database.setPersistenceEnabled(true);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        //storage 이용
        storage = FirebaseStorage.getInstance();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        img1 = (ImageView) findViewById(R.id.img1);
        img2 = (ImageView) findViewById(R.id.img2);
        img3 = (ImageView) findViewById(R.id.img3);
        img4 = (ImageView) findViewById(R.id.img4);
        img5 = (ImageView) findViewById(R.id.img5);
        img6 = (ImageView) findViewById(R.id.img6);

        editTextName = (EditText) findViewById(R.id.editTextName);

        buttonImage = (Button) findViewById(R.id.buttonImage);

        buttonSave = (FloatingActionButton) findViewById(R.id.buttonSave);

        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 권한
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);

                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                //사진을 여러개 선택할수 있도록 한다
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "포토를 이용하여 사진을 선택해 주세요"), PICTURE_REQUEST_CODE);
            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                upload(imagePath);

            }
        });
    }

        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("onActivityResult", "CALL");
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICTURE_REQUEST_CODE:
//                System.out.println(data.getData());
//                System.out.println(getPath(data.getData()));
                Log.i("result", String.valueOf(resultCode));
                if (resultCode == Activity.RESULT_OK) {
                    ClipData clipData = data.getClipData();
                    for(int i=0;i<6;i++) {
                        Uri urione = clipData.getItemAt(i).getUri();
                        switch (i) {
                            case 0:
                                Glide.with(this).load(urione).override(500,500).into(img1);
                                imagePath[0]=getPath(urione);
                                //img1.setImageURI(urione);
                                break;
                            case 1:
                                Glide.with(this).load(urione).override(500,500).into(img2);
                                imagePath[1]=getPath(urione);
                                //img2.setImageURI(urione);
                                break;
                            case 2:
                                Glide.with(this).load(urione).override(500,500).into(img3);
                                imagePath[2]=getPath(urione);
                                //img3.setImageURI(urione);
                                break;
                            case 3:
                                Glide.with(this).load(urione).override(500,500).into(img4);
                                imagePath[3]=getPath(urione);
                                //                                img4.setImageURI(urione);
                                break;
                            case 4:
                                Glide.with(this).load(urione).override(500,500).into(img5);
                                imagePath[4]=getPath(urione);
//                                img5.setImageURI(urione);
                                break;
                            case 5:
                                Glide.with(this).load(urione).override(500,500).into(img6);
                                imagePath[5]=getPath(urione);
//                                img6.setImageURI(urione);
                                break;
                        }
                    }
                }
        }
        //Toast.makeText(UserRegistrationActivity.this, imageList.indexOf(0), Toast.LENGTH_SHORT).show();
//        Toast.makeText(UserRegistrationActivity.this, String.valueOf(c.getItemCount()), Toast.LENGTH_SHORT).show();

    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if(requestCode==PICTURE_REQUEST_CODE){
//
//            imagePath = getPath(data.getData());
//            File f = new File(imagePath);
//            img1.setImageURI(Uri.fromFile(f));
//        }
//    }

    // get image path
    public  String getPath(Uri uri){

        String [] proj = {MediaStore.Images.Media.DATA};
        CursorLoader cursorLoader = new CursorLoader(this,uri,proj,null,null,null);

        Cursor cursor = cursorLoader.loadInBackground();
        int index= cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        cursor.moveToFirst();

        return cursor.getString(index);
    }

    private void upload(String[] uri){
        //StorageReference storageRef = storage.getReferenceFromUrl("gs://smilelocker-203407.appspot.com/");
        ImageDTO imageDTO = new ImageDTO();
        imageDTO.setName(editTextName.getText().toString());
        imageDTO.setUid(firebaseUser.getUid());
        imageDTO.setUserID(firebaseUser.getEmail());

        nStart = System.currentTimeMillis();

        String fileName;
        String path;
        StorageReference imgRef;

//        Uri file;
//        UploadTask uploadTask;

//        fileName = editTextName.getText().toString()+"_"+System.currentTimeMillis();
//        path = "Users/" + fileName;
//        imgRef = storage.getReference(path);
//        file = Uri.fromFile(new File(uri[0]));
//        uploadTask = imgRef.putFile(file);
//
//        fileName = editTextName.getText().toString()+"_"+System.currentTimeMillis();
//        path = "Users/" + fileName;
//        imgRef = storage.getReference(path);
//        file = Uri.fromFile(new File(uri[1]));
//        uploadTask = imgRef.putFile(file);
//
//        fileName = editTextName.getText().toString()+"_"+System.currentTimeMillis();
//        path = "Users/" + fileName;
//        imgRef = storage.getReference(path);
//        file = Uri.fromFile(new File(uri[2]));
//        uploadTask = imgRef.putFile(file);
//
//        fileName = editTextName.getText().toString()+"_"+System.currentTimeMillis();
//        path = "Users/" + fileName;
//        imgRef = storage.getReference(path);
//        file = Uri.fromFile(new File(uri[3]));
//        uploadTask = imgRef.putFile(file);
//
//        fileName = editTextName.getText().toString()+"_"+System.currentTimeMillis();
//        path = "Users/" + fileName;
//        imgRef = storage.getReference(path);
//        file = Uri.fromFile(new File(uri[4]));
//        uploadTask = imgRef.putFile(file);
//
//        fileName = editTextName.getText().toString()+"_"+System.currentTimeMillis();
//        path = "Users/" + fileName;
//        imgRef = storage.getReference(path);
//        file = Uri.fromFile(new File(uri[5]));
//        uploadTask = imgRef.putFile(file);

        for(int i=0; i<6; i++) {

            Uri file = Uri.fromFile(new File(uri[i]));

            fileName = editTextName.getText().toString()+"_"+System.currentTimeMillis()+".jpg";
            path = "Users/" + fileName;
//        StorageReference imgRef = storageRef.child(firebaseUser.getUid()).child(file.getLastPathSegment());
            imgRef = storage.getReference(path);
            UploadTask uploadTask = imgRef.putFile(file);

            // Register observers to listen for when the download is done or if it fails
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
//
////                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
////                    Log.i("url", downloadUrl.toString());
//
//                    nEnd = System.currentTimeMillis();
//                    Log.i(TAG, "upload time: " + (nEnd - nStart) + "ms");
//                }
//            });

        }

        database.getReference()
                .child("userInformation")
                .child(firebaseUser.getUid())
                .push()
                .setValue(imageDTO);
        finish();
    }


}