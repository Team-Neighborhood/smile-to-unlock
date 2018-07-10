package com.example.dohee.smileunlock;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    //firebase auth object
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private static FirebaseDatabase database;

    //view objects
    private TextView textViewUserEmail;
    private TextView textViewSignout;
    private TextView textViewDelete;
    private Button buttonRegistration;
    private ListView listView;
    private Switch switch1;
    private String switchState;

    Context context = this;


    private List<String> userList = new ArrayList<>();
    private List<String> key = new ArrayList<>();

    static{

        database = FirebaseDatabase.getInstance();
        //database.setPersistenceEnabled(true);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initializing views
        textViewUserEmail = (TextView) findViewById(R.id.textviewUserEmail);
        textViewSignout = (TextView) findViewById(R.id.textviewSignout);
        textViewDelete = (TextView) findViewById(R.id.textviewDelete);
        buttonRegistration = (Button) findViewById(R.id.buttonRegestration);
        listView = (ListView) findViewById(R.id.listView);
        switch1 = (Switch) findViewById(R.id.switch1);

        //initializing firebase authentication object
        firebaseAuth = FirebaseAuth.getInstance();
        //유저가 로그인 하지 않은 상태라면 null 상태이고 이 액티비티를 종료하고 로그인 액티비티를 연다.
        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            startActivity(new Intent(this, LoginActivity.class));
        }

        //유저가 있다면, null이 아니면 계속 진행
        user = firebaseAuth.getCurrentUser();

        //textViewUserEmail의 내용을 변경해 준다.
        textViewUserEmail.setText("반갑습니다.\n" + user.getEmail() + "으로 로그인 하였습니다.");

        //switch
        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked==true){
                    if(database.getReference().child("userInformation").child(user.getUid())!=null) {
                        Intent intent = new Intent(MainActivity.this, ScreenService.class);
                        startService(intent);
                        database.getReference().child("switch").child("state")
                                .setValue(isChecked);
                    }else{
                        switch1.setChecked(false);
                        Toast.makeText(context, "사용자를 등록 해 주세요", Toast.LENGTH_SHORT).show();
                        database.getReference().child("switch").child("state")
                                .setValue(false);
                    }
                }else{
                    Intent intent = new Intent(MainActivity.this, ScreenService.class);
                    stopService(intent);
                    database.getReference().child("switch").child("state")
                            .setValue(isChecked);
                }

            }

        });

        database.getReference().child("switch")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            switchState = snapshot.getValue().toString();

                            if(switchState=="true"){
                                switch1.setChecked(true);
                            }else{
                                switch1.setChecked(false);
                            }

                            //Toast.makeText(context, switchState, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        //logout textview event
        textViewSignout.setOnClickListener(this);
        textViewDelete.setOnClickListener(this);

        //사용자 등록 이벤트
        buttonRegistration.setOnClickListener(this);

        final ListViewAdapter adapter = new ListViewAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener(new ListViewItemLongClickListener());

        database.getReference().child("userInformation").child(user.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        userList.clear();
                        dataSnapshot.getChildrenCount();
                        for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                            ImageDTO imageDTO = snapshot.getValue(ImageDTO.class);
                            userList.add(imageDTO.getName());
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

//        database.getReference().child("userInformation").child(user.getUid())
//                .addChildEventListener(new ChildEventListener() {
//                    @Override
//                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
//                        ImageDTO imageDTO = dataSnapshot.getValue(ImageDTO.class);
//                        userList.add(imageDTO.getName());
//                        key.add(dataSnapshot.getKey());
//                        adapter.notifyDataSetChanged();
//                    }
//
//                    @Override
//                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
//
//                    }
//
//                    @Override
//                    public void onChildRemoved(DataSnapshot dataSnapshot) {
//                        ImageDTO imageDTO = dataSnapshot.getValue(ImageDTO.class);
//                        userList.remove(imageDTO.getName());
//                        key.remove(dataSnapshot.getKey());
//                        adapter.notifyDataSetChanged();
//                    }
//
//                    @Override
//                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
//
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError databaseError) {
//
//                    }
//                });

        // 권한 설정
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            checkVerify();
        }
        else
        {
            startApp();
        }
    }

    @Override
    public void onClick(View view) {
        if (view == textViewSignout) {
            firebaseAuth.signOut();
            finish();
            startActivity(new Intent(this, LoginActivity.class));
        }
        //회원탈퇴를 클릭하면 회원정보를 삭제한다. 삭제전에 컨펌창을 하나 띄워야 겠다.
        if (view == textViewDelete) {
            AlertDialog.Builder alert_confirm = new AlertDialog.Builder(MainActivity.this);
            alert_confirm.setMessage("정말 계정을 삭제 할까요?").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            user.delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Toast.makeText(MainActivity.this, "계정이 삭제 되었습니다.", Toast.LENGTH_LONG).show();
                                            finish();
                                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                        }
                                    });
                        }
                    }
            );
            alert_confirm.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(MainActivity.this, "취소", Toast.LENGTH_LONG).show();
                }
            });
            alert_confirm.show();
        }

        if (view == buttonRegistration){
            startActivity(new Intent(this, UserRegistrationActivity.class));
        }

    }

    private class ListViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return userList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.item_usrlist, viewGroup, false);
            }
            ((TextView) view.findViewById(R.id.text_name)).setText(userList.get(i).toString());
            return view;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void checkVerify()
    {
        if (
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                )
        {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                // ...
            }
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                    1);
        }
        else
        {
            startApp();
        }
    }

    public void startApp()
    {
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String strDir = file.getAbsolutePath();

        File oFile = new File(strDir+"/abc.txt");
        FileOutputStream oFos;
        try
        {
            oFos = new FileOutputStream(oFile);
            oFos.write("abc".getBytes(), 0, 3);
            oFos.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    private class ListViewItemLongClickListener implements AdapterView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            final String selectedKey = key.get(position);
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setMessage("삭제 하시겠습니까?");

            // add positive button and event listener
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    database.getReference().child("userInformation").child(user.getUid()).child(selectedKey).removeValue();
                } });
            // add negative button and event listener
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                } });

            builder.setCancelable(false);

            // create alertdialog and show dialog
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1)
        {
            if (grantResults.length > 0)
            {
                for (int i=0; i<grantResults.length; ++i)
                {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                    {
                        // 하나라도 거부한다면.
                        new AlertDialog.Builder(this).setTitle("알림").setMessage("권한을 허용해주셔야 앱을 이용할 수 있습니다.")
                                .setPositiveButton("종료", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                }).setNegativeButton("권한 설정", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                                getApplicationContext().startActivity(intent);
                            }
                        }).setCancelable(false).show();

                        return;
                    }
                }
                startApp();
            }
        }
    }
}
