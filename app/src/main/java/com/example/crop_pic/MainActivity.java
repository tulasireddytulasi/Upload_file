package com.example.crop_pic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.androidnetworking.model.Progress;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static final  String   SHARED_PRE = "storage";
    private ImageView crop_imageView;
    private Button button, uploadbtn;
    private TextView textView;
    private ProgressDialog progressDialog;
    Bitmap bitmap;
    String text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AndroidNetworking.initialize(getApplicationContext());
        crop_imageView = findViewById(R.id.img);
        button = findViewById(R.id.cropImg);
        textView = findViewById(R.id.textview);
        uploadbtn = findViewById(R.id.uploadbtn);
        uploadbtn.setEnabled(false);
        //loadPic();
        loadPic2();
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Uploading Image, Please wait");
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dexter.withActivity(MainActivity.this)
                        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                CropImage.activity()
                                        .setGuidelines(CropImageView.Guidelines.ON)
                                        .start(MainActivity.this);
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                               if(response.isPermanentlyDenied()){
                                   AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                   builder.setTitle("Permision Required")
                                           .setMessage("Permission Required to access image")
                                           .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                               @Override
                                               public void onClick(DialogInterface dialog, int which) {
                                                   Intent intent = new Intent();
                                                   intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                   intent.setData(Uri.fromParts("pakage", getPackageName(), null));
                                                   startActivityForResult(intent, 51);
                                               }
                                           })
                                           .setNegativeButton("Cancel",null)
                                           .show();
                               }
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                               permissionToken.continuePermissionRequest();
                            }
                        }).check();
            }
        });

    }

    private void loadPic2() {
        SharedPreferences shareds = getSharedPreferences(SHARED_PRE, MODE_PRIVATE);
        text = shareds.getString("PIC","");
        if (text == "" || text == null){
            loadPic();
        }else {
            byte[] decodestring = Base64.decode(text, Base64.DEFAULT);
            Bitmap decode = BitmapFactory.decodeByteArray(decodestring,0, decodestring.length);
            crop_imageView.setImageBitmap(decode);
        }

    }

    private void loadPic() {
        AndroidNetworking.get("http://marvelwallpapers.000webhostapp.com/profile_pic.php")
                .setTag("test")
                .setPriority(Priority.HIGH)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        String imgurl = response.replaceAll("\\s", "");

                        Glide.with(MainActivity.this).
                                asBitmap()
                                .load(imgurl)
                                .into(new SimpleTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                        bitmap = resource;
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                        byte[] b = baos.toByteArray();
                                        String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);

                                        SharedPreferences shared = getSharedPreferences(SHARED_PRE, MODE_PRIVATE);
                                        SharedPreferences.Editor editor = shared.edit();
                                        editor.putString("PIC", imageEncoded);
                                        editor.commit();

                                        loadPic2();
                                    }
                                });
                    }
                    @Override
                    public void onError(ANError anError) {
                       Toast.makeText(getApplicationContext(),String.valueOf(anError),Toast.LENGTH_LONG).show();
                    }
                });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
               final Uri resultUri = result.getUri();
                //crop_imageView.setImageURI(resultUri);
                uploadbtn.setEnabled(true);
                uploadbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        new AsyncTask<Void, Void, String>(){
                            @Override
                            protected String doInBackground(Void... voids) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                byte[] b = baos.toByteArray();
                                String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);

                                Log.d("Image Log:", imageEncoded);
                                return imageEncoded;
                            }

                            @Override
                            protected void onPostExecute(String s) {
                                textView.setText(s);
                                SharedPreferences shared = getSharedPreferences(SHARED_PRE, MODE_PRIVATE);
                                SharedPreferences.Editor editor = shared.edit();
                                editor.putString("PIC", s);
                                editor.commit();

                                loadPic2();
                            }
                        }.execute();

                        // https://marvelwallpapers.000webhostapp.com/upload4.php
                        // http://hcapi.helocherry.com/?p=uploadImage
                        File imageFile = new File(resultUri.getPath());
                        progressDialog.show();
                        AndroidNetworking.upload("https://marvelwallpapers.000webhostapp.com/upload4.php")
                                .addMultipartFile("file", imageFile)
                                .addMultipartParameter("fb_id", "1621896367988994")
                                .setPriority(Priority.HIGH)
                                .build()
                                .setUploadProgressListener(new UploadProgressListener() {
                                    @Override
                                    public void onProgress(long bytesUploaded, long totalBytes) {
                                        float progress = (float) bytesUploaded / totalBytes * 100;
                                        progressDialog.setProgress((int)progress);
                                    }
                                })
                                .getAsString(new StringRequestListener() {
                                    @Override
                                    public void onResponse(String response) {
                                        progressDialog.dismiss();
                                        Toast.makeText(MainActivity.this, response, Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onError(ANError anError) {
                                        progressDialog.dismiss();
                                        anError.printStackTrace();
                                        Toast.makeText(MainActivity.this, anError.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });

                    }
                });
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}