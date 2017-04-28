package com.fanhaichao.testforceupdate;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 版本更新
 */
public class UpdateManager2 {
    /**
     * 请求码：获取权限的activity——RequestCode请求码
     */
    public static final int REQ_PERMISSION_CODE = 200;
    /**
     * 请求码：安装应用的activity--RequestCode请求码
     */
    public static final int REQ_INSTALL_CODE = 201;

    private static final int DOWNLOAD_ING = 1;

    private static final int DOWNLOAD_FINISH = 2;

    protected static final String TAG = "UpdateManager";

    private String mSavePath;

    private int progress;

    private boolean cancelUpdate = false;

    private Activity mContext;

    private ProgressBar mProgress;
    private Dialog mDownloadDialog;

    private boolean isForce ;//是否是强制更新

    public void setForce(boolean force) {
        isForce = force;
    }

    public boolean isForce() {
        return isForce;
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DOWNLOAD_ING:

                    mProgress.setProgress(progress);
                    break;
                case DOWNLOAD_FINISH:
                    // 安装APK
                    installApk();
                    break;

                default:
                    break;
            }
        }

        ;
    };

    public UpdateManager2(Activity mContext) {
        this.mContext = mContext;
    }

    /**
     * 更新弹窗
     */
    public void showNoticeUpdateDialog(final String down_url) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("应用更新");
        builder.setMessage("发现新版本，请更新！");

        builder.setPositiveButton("确定更新", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //TODO android SDK >=23
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (mContext.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            && mContext.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)){
                        //do nothing  此方法只有在第一次拒绝，且下次还能显示时，第二次显示的时候才会调用，
                        // 主要目的就是为了 给第二次弹出权限选择对话框做解释处理
                        //一般不用处理，但是这个方法要知道
                    }
                    askPermissions();
                }else {
                    showDownloadDialog(down_url);
                }

            }
        });
        if (!isForce){
            builder.setNegativeButton("稍后更新",
                    new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
        }
        builder.setCancelable(!isForce);
        Dialog noticeDialog = builder.create();
        noticeDialog.show();
    }


    /**
     * 下载弹窗
     * @param down_url
     * @param isCancleable  是否允许取消弹窗
     */
    public void showDownloadDialog(String down_url) {
        cancelUpdate = false;
        Builder builder = new Builder(mContext);
        builder.setTitle("更新中...");

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.update, null);
        mProgress = (ProgressBar) v.findViewById(R.id.update_progress);
        builder.setView(v);
        if (!isForce){
            builder.setNegativeButton("取消更新",
                    new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            cancelUpdate = true;
                        }
                    });
        }

        builder.setCancelable(false);
        mDownloadDialog = builder.create();
        mDownloadDialog.show();

        //下载apk
        downloadApk(down_url);
    }

    private void downloadApk(String down_url) {

        new downloadApkThread(down_url).start();
    }

    private class downloadApkThread extends Thread {
        private String down_url;

        public downloadApkThread(String down_url) {
            this.down_url = down_url;
        }

        @Override
        public void run() {
            try {
                //判断SD卡是否存在，并且是否具有读写权限
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    // 获得存储卡的路径
                    String sdpath = Environment.getExternalStorageDirectory() + "/";
//					mSavePath = sdpath + "download";
                    String sdStatus = Environment.getExternalStorageState();
                    if (sdStatus.equals(Environment.MEDIA_MOUNTED)) {
                        mSavePath = sdpath + "download/";
                    } else {// 否则存储在手机中
                        mSavePath = "/udisk/download/";
                    }

                    URL url = new URL(down_url);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    //Accept-Encoding指定浏览器可以支持的web服务器返回内容压缩编码类型。
                    //要求http请求不要gzip压缩  否则下边int length = conn.getContentLength();返回值为-1 ,不能read
                    conn.setRequestProperty("Accept-Encoding", "identity");
                    conn.setRequestMethod("POST");
                    conn.connect();

                    int length = conn.getContentLength();
                    //报找不到文件 是因为window不识别 apk文件  在imme 里添加上.apk就可以了
                    InputStream is = conn.getInputStream();
                    File file = new File(mSavePath);
                    if (!file.exists()) {
                        file.mkdir();//创建目录
                    }

                    File apkFile = new File(mSavePath + "test.apk");
                    if (!apkFile.exists()) {
                        apkFile.createNewFile();
                    }
                    FileOutputStream fos = new FileOutputStream(apkFile);
                    int count = 0;
                    byte buf[] = new byte[1024];

                    do {
                        int numread = is.read(buf);
                        count += numread;

                        progress = (int) (((float) count / length) * 100);

                        mHandler.sendEmptyMessage(DOWNLOAD_ING);
                        if (numread <= 0) {
                            mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
                            break;
                        }

                        fos.write(buf, 0, numread);
                    } while (!cancelUpdate);
                    fos.close();
                    is.close();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();

                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        goToSetPermission(mContext, "在设置-应用-权限中打开 SD卡存储 权限，以保证功能的正常使用", REQ_PERMISSION_CODE);
                    }
                });
            }

            mDownloadDialog.dismiss();
        }
    }

    /**
     * 安装
     */
    private void installApk() {
        //如果安装不上可能是签名不一样
        File apkfile = new File(mSavePath, "test.apk");
        if (!apkfile.exists()) {
            Toast.makeText(mContext,"下载地址里面的apk和该应用的apk 签名不一致",Toast.LENGTH_LONG).show();
            return;
        }

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");
        if (isForce) {
            mContext.startActivityForResult(i, REQ_INSTALL_CODE);
        }else {
            mContext.startActivity(i);
        }
    }

    @TargetApi(23)
    protected void askPermissions() {
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
        mContext.requestPermissions(permissions, REQ_PERMISSION_CODE);

    }


    /**
     * 对于其他的权限，其实申请的逻辑是类似的；唯一不同的肯定就是参数,
     * 我们需要3个参数:Activity|Fragment、权限字符串数组、int型申请码。
     * 去设置权限
     * @param activity
     * @param message  显示的信息
     * @param req_permission  请求权限的码
     */
    public void goToSetPermission(final Activity activity, String message, final int req_permission){
        new Builder(activity)
                .setTitle("权限申请")
                .setMessage(message)
                .setPositiveButton("去开启", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //根据包名跳转到系统自带的应用程序信息界面
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                        intent.setData(uri);
                        if (isForce) {
                            activity.startActivityForResult(intent, req_permission);
                        }else {
                            activity.startActivity(intent);
                        }
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .show();
    }
}
