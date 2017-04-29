package com.fanhaichao.testforceupdate;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button bt_check_force_update;
    private UpdateManager2 updateManager;
    private String downLoad_url = "http://www.jh1000.com/download/fileProcess.do?inputPath=jh1000.apk&fileName=jh1000.apk";

    private Activity activity;
    private boolean isForce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt_check_force_update = (Button) findViewById(R.id.bt_check_force_update);
        activity = this;

        updateManager = new UpdateManager2(MainActivity.this);
        bt_check_force_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isForce = true;
                updateManager.setForce(isForce);
                updateManager.showNoticeUpdateDialog(downLoad_url);
            }
        });

        findViewById(R.id.bt_check_update).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isForce = false;
                updateManager.setForce(isForce);
                updateManager.showNoticeUpdateDialog(downLoad_url);
            }
        });

        findViewById(R.id.bt_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == UpdateManager2.REQ_PERMISSION_CODE) {
            boolean isAllow = true;
            for (int res: grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED){
                    isAllow = false;
                    break;
                }
            }
            if (isAllow) {
                updateManager.showDownloadDialog(downLoad_url);
            }else {
                if (isForce) {
                    updateManager.goToSetPermission(this, "在设置-应用-权限中打开 SD卡存储 权限，以保证功能的正常使用", UpdateManager2.REQ_PERMISSION_CODE);
                }else {
                    // do nothing 非强制更新
                }
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UpdateManager2.REQ_PERMISSION_CODE || requestCode == UpdateManager2.REQ_INSTALL_CODE ){
            updateManager.showNoticeUpdateDialog(downLoad_url);

        }
    }
}
