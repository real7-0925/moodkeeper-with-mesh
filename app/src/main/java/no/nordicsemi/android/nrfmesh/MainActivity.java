/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.nrfmesh;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.predictions.aws.AWSPredictionsPlugin;
import com.amplifyframework.predictions.models.IdentifyActionType;
import com.amplifyframework.predictions.result.IdentifyEntitiesResult;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.slider.Slider;
import com.google.android.play.core.tasks.Task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import dagger.hilt.android.AndroidEntryPoint;
import no.nordicsemi.android.nrfmesh.databinding.ActivityMainBinding;
import no.nordicsemi.android.nrfmesh.utils.Utils;
import no.nordicsemi.android.nrfmesh.viewmodels.SharedViewModel;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements
        BottomNavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnNavigationItemReselectedListener {

    private static final String CURRENT_FRAGMENT = "CURRENT_FRAGMENT";

    private SharedViewModel mViewModel;

    private NetworkFragment mNetworkFragment;
    private GroupsFragment mGroupsFragment;
    private ProxyFilterFragment mProxyFilterFragment;
    private Fragment mSettingsFragment;

    /*add by myself*/
    private static final String TAG = "Faces";

    private String mPath = "/storage/emulated/0/Detectface";//設置的照片位址
    public static final int CAMERA_PERMISSION = 100;//檢測相機權限用
    public static final int REQUEST_HIGH_IMAGE = 101;//檢測相機回傳

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        mNetworkFragment = (NetworkFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_network);
        mGroupsFragment = (GroupsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_groups);
        mProxyFilterFragment = (ProxyFilterFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_proxy);
        mSettingsFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_settings);
        final BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_view);

        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        bottomNavigationView.setOnNavigationItemReselectedListener(this);

        if (savedInstanceState == null) {
            onNavigationItemSelected(bottomNavigationView.getMenu().findItem(R.id.action_network));
        } else {
            bottomNavigationView.setSelectedItemId(savedInstanceState.getInt(CURRENT_FRAGMENT));
        }

        /*add by myself*/
        try {
            Amplify.addPlugin(new AWSCognitoAuthPlugin());//without credential log in
            Amplify.addPlugin(new AWSPredictionsPlugin());//rekognition translate polly high level client

            AmplifyConfiguration config = AmplifyConfiguration.builder(getApplicationContext())
                    .devMenuEnabled(false)
                    .build();
            Amplify.configure(config, getApplicationContext());
        } catch (AmplifyException e) {
            Log.e("Tutorial", "Could not initialize Amplify", e);
        }

        //create DetectFace folder in android for picture
        String DetectFacedir = "/DetectFace/";
        File PrimaryStorage = Environment.getExternalStorageDirectory();
        File PICDir = new File("/storage/emulated/0/DetectFace/");
        File ReadyPath = new File("/storage/emulated/0/DetectFace/" + "Ready.txt");
        Log.e("str", String.valueOf(PrimaryStorage));
        try {
            Log.i("test", "delete CMD");
            String deleteCmd = "rm -r " + ReadyPath;
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(deleteCmd);


        } catch (FileNotFoundException e) {
            Log.e("NOTFOUND", "file notfound");
        } catch (IOException e) {
            Log.e("IOERROR", "some IO error");
        }

        Task task = new Task();

        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(0);
        executor.scheduleWithFixedDelay(task, 1, 300, TimeUnit.SECONDS);

        //相機
        Button btHigh = findViewById(R.id.buttonHigh);
        //取得相機權限
        if (checkSelfPermission(Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);
        /**按下照相之拍攝按鈕*/
        btHigh.setOnClickListener(v->{
            Intent highIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //檢查是否已取得權限
            if (highIntent.resolveActivity(getPackageManager()) == null) return;
            //取得相片檔案的URI位址及設定檔案名稱
            File imageFile = getImageFile();
            if (imageFile == null) return;
            //取得相片檔案的URI位址
            Uri imageUri = FileProvider.getUriForFile(
                    this,
                    "com.jetec.cameraexample.CameraEx",//記得要跟AndroidManifest.xml中的authorities 一致
                    imageFile
            );
            highIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
            startActivityForResult(highIntent,REQUEST_HIGH_IMAGE);//開啟相機
        });
    }
    /**取得相片檔案的URI位址及設定檔案名稱*/
    private File getImageFile()  {
        Date date = new Date();

        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String time = sdf.format(date);
        String fileName = time;
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File f = new File(mPath);
        try {
            //給予檔案命名及檔案格式
            File imageFile = File.createTempFile(fileName,".jpg", f);
            //給予全域變數中的照片檔案位置，方便後面取得
            Log.e("aa", "imageFile = " + imageFile.getAbsolutePath());
            mPath = imageFile.getAbsolutePath();
            return imageFile;
        } catch (IOException e) {
            return null;
        }
    }

    //end


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final Boolean isConnectedToNetwork = mViewModel.isConnectedToProxy().getValue();
        if (isConnectedToNetwork != null && isConnectedToNetwork) {
            getMenuInflater().inflate(R.menu.disconnect, menu);
        } else {
            getMenuInflater().inflate(R.menu.connect, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.action_connect) {
            mViewModel.navigateToScannerActivity(this, false, Utils.CONNECT_TO_NETWORK, false);
            return true;
        } else if (id == R.id.action_disconnect) {
            mViewModel.disconnect();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //add by myself
        /**可在此檢視回傳為哪個相片，requestCode為上述自定義，resultCode為-1就是有拍照，0則是使用者沒拍照*/
        Log.d(TAG, "onActivityResult: requestCode: "+requestCode+", resultCode "+resultCode);
        /**如果是的相片回傳*/
        if (requestCode == REQUEST_HIGH_IMAGE && resultCode == -1) {
            ImageView imageHigh = findViewById(R.id.imageViewHigh);
            new Thread(() -> {
                //在BitmapFactory中以檔案URI路徑取得相片檔案，並處理為AtomicReference<Bitmap>，方便後續旋轉圖片
                AtomicReference<Bitmap> getHighImage = new AtomicReference<>(BitmapFactory.decodeFile(mPath));
                Matrix matrix = new Matrix();
                matrix.setRotate(90f);//轉90度
                getHighImage.set(Bitmap.createBitmap(getHighImage.get()
                        , 0, 0
                        , getHighImage.get().getWidth()
                        , getHighImage.get().getHeight()
                        , matrix, true));
                runOnUiThread(() -> {
                    //以Glide設置圖片(因為旋轉圖片屬於耗時處理，故會LAG一下，且必須使用Thread執行緒)
                    Glide.with(this)
                            .load(getHighImage.get())
                            .centerCrop()
                            .into(imageHigh);
                });
            }).start();
        }
        else{
            Toast.makeText(this, "未作任何拍攝", Toast.LENGTH_SHORT).show();
        }



        //滑桿
        Slider slider1 = (Slider) findViewById(R.id.seekbar_Red);
        Slider slider2 = (Slider) findViewById(R.id.seekbar_Green);
        Slider slider3 = (Slider) findViewById(R.id.seekbar_Blue);

//        slider1.showContextMenu();// 设置推动时显示指示器
//        slider2.showContextMenu();
//        slider3.showContextMenu();

        slider1.setTrackTintList(ColorStateList.valueOf(0xFF881515));// 滑軌底色
        slider2.setTrackTintList(ColorStateList.valueOf(0xFF308014));
        slider3.setTrackTintList(ColorStateList.valueOf(0xFF3D59AB));

        slider1.setValue(0);// 设定初始进度
        slider2.setValue(0);
        slider3.setValue(0);

        slider1.setValueTo(255);// 设定最终进度
        slider2.setValueTo(255);
        slider3.setValueTo(255);

        slider1.setHaloTintList(ColorStateList.valueOf(0xFFFF0000));// 设定光環顔色
        slider2.setHaloTintList(ColorStateList.valueOf(0xFF00FF00));
        slider3.setHaloTintList(ColorStateList.valueOf(0xFF0000FF));

        slider1.setThumbTintList(ColorStateList.valueOf(0xFFFFFAFA));// 设定滑塊顔色
        slider2.setThumbTintList(ColorStateList.valueOf(0xFFFFFAFA));
        slider3.setThumbTintList(ColorStateList.valueOf(0xFFFFFAFA));

        slider1.setTrackActiveTintList(ColorStateList.valueOf(0xFFE3170D));//設置軌道活動部分顔色
        slider2.setTrackActiveTintList(ColorStateList.valueOf(0xFF32CD32));
        slider3.setTrackActiveTintList(ColorStateList.valueOf(0xFF1E90FF));

        slider1.setTrackHeight(20);//設置軌道寬度
        slider2.setTrackHeight(20);
        slider3.setTrackHeight(20);
//        slider1.setBackgroundColor(getResources().getColor(R.color.red));// 背景颜色// 监听进度
//        slider1.setOnValueChangedListener(new OnValueChangedListener() {
//
//            @Override
//            public void onValueChanged(int value) {
//                // TODO 自动生成的方法存根
//                System.out.println("now value = "+ value);
//            }
//        });

        slider1.setThumbElevation(30);// 设置滑块的影子大小
        slider2.setThumbElevation(30);// 设置滑块的影子大小
        slider3.setThumbElevation(30);// 设置滑块的影子大小
        //end
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (id == R.id.action_network) {
            ft.show(mNetworkFragment).hide(mGroupsFragment).hide(mProxyFilterFragment).hide(mSettingsFragment);
        } else if (id == R.id.action_groups) {
            ft.hide(mNetworkFragment).show(mGroupsFragment).hide(mProxyFilterFragment).hide(mSettingsFragment);
        } else if (id == R.id.action_proxy) {
            ft.hide(mNetworkFragment).hide(mGroupsFragment).show(mProxyFilterFragment).hide(mSettingsFragment);
        } else if (id == R.id.action_settings) {
            ft.hide(mNetworkFragment).hide(mGroupsFragment).hide(mProxyFilterFragment).show(mSettingsFragment);
        }
        ft.commit();
        invalidateOptionsMenu();
        return true;
    }

    @Override
    public void onNavigationItemReselected(@NonNull MenuItem item) {
    }

    class Task implements Runnable {
        public void run() {
            Log.i("test", "run started");
//             File PrimaryStorage = Environment.getExternalStorageDirectory();
//             //Log.e("str", String.valueOf(PrimaryStorage));
//             String Facedir = "/DetectFace/";
//             String ReadyFil = "READY.txt";
            File imageFile = new File("/storage/emulated/0/Detectface/yyMMdd.jpeg");
            imageFile.mkdir();
//             //Log.i("test","create file");
//             //File imageFile = new File(System.currentTimeMillis() + ".jpg");
//             File ReadyPath = new File("/storage/emulated/0/Detectface2/" + ReadyFil);


//            File file = new File(ReadyPath, ReadyFil);
//            FileOutputStream outputStream = null;
//            try {
//                outputStream = new FileOutputStream(ReadyFil);
//                outputStream.write("0".getBytes());
//                outputStream.close();
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            //if (ReadyPath.exists()) {
            //Log.e("try","ReadyPath exists");
//                try {
//                    String deleteCmd = "rm -r " + ReadyPath;
//                    Runtime runtime = Runtime.getRuntime();
//                    runtime.exec(deleteCmd);
//
//                } catch (FileNotFoundException e) {
//                    Log.e("NOTFOUND", "file notfound");
//                } catch (IOException e) {
//                    Log.e("IOERROR", "some IO error");
//                }


            try {
                Log.i("try", "DetectEntities");
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Bitmap image = BitmapFactory.decodeFile(String.valueOf(imageFile), bmOptions); //將照片轉為jpg
                DetectEntities(image); //將照片上傳至AWS
            } catch (Exception e) {
                Log.e("DETECT", "detect error" + e.getMessage());
            }
            //}
        }
    }


    private void DetectEntities(Bitmap image) {
        try {
            Log.i("DETECTENTITIES", "started");

            Amplify.Predictions.identify(  //啟用臉部辨識系統
                    IdentifyActionType.DETECT_ENTITIES,
                    image,
                    result -> LabelDataHold((IdentifyEntitiesResult) result, image),
                    error -> Log.e("AmplifyQuickstart", "Identify failed ", error)// + error.getMessage())
            );
            Log.i("DETECTENTITIES", "finished");

        } catch (Exception e) {
            Log.e("DETECT", "DetectEntities error "); //+ e.getMessage());
        }
    }


    private void LabelDataHold(IdentifyEntitiesResult result, Bitmap image) {
        final String[] printout = new String[result.getEntities().size()];
        double[][] Xnumber = new double[result.getEntities().size()][];
        int max = result.getEntities().size();

        for (int m = 0; m < max; m++) {
            printout[m] = String.valueOf(result.getEntities().get(m).getEmotions().get(m).getValue());
            printout[m] = String.valueOf(result.getEntities().get(m).getBox());
            printout[m] = String.valueOf(result.getEntities().get(m).getAgeRange());
            printout[m] = String.valueOf(result.getEntities().get(m).getGender());
            printout[m] = String.valueOf(result.getEntities().get(m).getLandmarks());
            printout[m] = String.valueOf(result.getEntities().get(m).getPolygon());
            printout[m] = String.valueOf(result.getEntities().get(m).getPose());


            //result.getEntities().get(0).getAgeRange().getLow();

            //Log.i("result", result.toString());
            Log.i("Emotions  Result", result.getEntities().get(m).getEmotions().get(m).getValue()
                    + ", Confidence: " + result.getEntities().get(m).getEmotions().get(m).getConfidence());

            Log.i("AgeRange  Result", "Age: " + result.getEntities().get(0).getAgeRange().getLow()
                    + " - " + result.getEntities().get(0).getAgeRange().getHigh());

            Log.i("Gender    Result", result.getEntities().get(0).getGender().getValue()
                    + ", Confidence: " + result.getEntities().get(0).getGender().getConfidence());

//         Log.i("Try           Result", result.getEntities().get(0).
//                 + ", Confidence: " + result.getEntities().get(0).getEmotions().get(0).getConfidence());

            //Log.i("Landmarks Result", String.valueOf(result.getEntities().get(0).getLandmarks()));
        }

    }
}
