# TestForceUpdate
Android 6.0运行时权限，强制更新app 和 非强制更新app

    网上关于 6.0 运行时权限的文章不少，我做这个项目的时候也是从这些文章中一步一步了解的，现在整理分享给大家：

    [CSDN:鸿阳大神的 Android 6.0 运行时权限处理完全解析](http://blog.csdn.net/lmj623565791/article/details/50709663)

    [博客园：谈谈Android 6.0运行时权限理解](http://www.cnblogs.com/cr330326/p/5181283.html)

    [http://www.cnblogs.com/Fndroid/p/5542526.html](http://www.cnblogs.com/Fndroid/p/5542526.html)

    [https://blog.coding.net/blog/understanding-marshmallow-runtime-permission](https://blog.coding.net/blog/understanding-marshmallow-runtime-permission)
    暴躁如我，先放特效（最后崩溃是因为，apk签名不一致。）
    第一张是强制更新的
    ![这里写图片描述](https://github.com/SuperChandler/TestForceUpdate/tree/master/readme_img/forceupdate.gif)
    第二张是非强制更新：
    ![这里写图片描述](http://img.blog.csdn.net/20170428182607620?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvc2luYXRfMjY3MTA3MDE=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)
    首先，说下Android 6.0 版本之前的权限，在安装app的时候就默认授予的，用户没有办法去修改权限设置。但是6.0版本出现后，为了提高Android app 的安全箱，Google增生了一个运行时权限的机制。
    总的来说，就是，用户在安装应用的时候并不会默认开启所有权限，只有当用户的操作行为触及到某个权限时，系统会给出：“是否允许开启权限”的安全警告。

    随着Android版本的不断升级，市场上的手机系统也基本上都已经处于6.0了（还有少量5.x和更少量4.x）。
    那么作为Android 开发者，如何在代码中去设置运行时权限呢？（说实话，稍微有点麻烦，但做一遍基本就没啥问题了。）

    言归正传，一起暴躁起来吧。这里我们举个在实际项目中的栗子：（app检测更新）
    *并不是所有的权限都需要配置这些，只有危险性的权限才需要配置运行时权限代码，比如，调用摄像头啊，读取sd卡啊，打电话啊，获取地理位置啊，这些容易侵犯到隐私的权限等等。*


    **1. 配置权限：**
    在AndroidManifest文件中添加需要的权限。更新下载安装app，需要读写sd卡数据
    因此权限：

    ```
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"></uses-permission>
    ```

    **2.检测是否开启了权限：（这个其实可以不写）**

    ```
    if (ContextCompat.checkSelfPermission(thisActivity,
    Manifest.permission.READ_EXTERNAL_STORAGE)
    != PackageManager.PERMISSION_GRANTED) {
    }else{
    //
    }
    ```
    这里只需要注意下 checkSelfPermission()这个是判断权限的方法的返回值
    看源码很简单就可以知道大概的意思
    ![这里写图片描述](http://img.blog.csdn.net/20170428112441844?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvc2luYXRfMjY3MTA3MDE=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)
    参数二就是你要检查的权限
    返回值就是检查的结果：

    ```
    已授予权限：android.content.pm.PackageManager.PERMISSION_GRANTED
    未授予权限：android.content.pm.PackageManager.PERMISSION_DENIED
    ```
    **3.请求获取权限：**

    ```
    @TargetApi(23)
    protected void askPermissions() {
    String[] permissions = {
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE"
    };
    mContext.requestPermissions(permissions, JHConstants.REQ_PERMISSION_CODE);
    }
    ```
    这里注意，requestPermissions（）方法
    参数一：是一个权限的数组
    参数二：是一个请求权限的请求码（就像是startActivityForResult方法中的requestCode一样一样的），这个请求码，在后面的回调方法中会用到。
    ![这里写图片描述](http://img.blog.csdn.net/20170428114810762?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvc2luYXRfMjY3MTA3MDE=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

    **4.处理权限的回调**

    ```
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == JHConstants.REQ_PERMISSION_CODE) {
    boolean isAllow = true;
    for (int res: grantResults) {
    if (res != PackageManager.PERMISSION_GRANTED){
    isAllow = false;
    break;
    }
    }
    if (isAllow) {
    updateManager.showDownloadDialog(updateManager.getServiceUrl(), false);
    }else {
    MySystemUtils.goToSetPermission(activity,"在设置-应用-权限中打开 SD卡存储 权限，以保证功能的正常使用",JHConstants.REQ_PERMISSION_CODE);
    }
    }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == JHConstants.REQ_PERMISSION_CODE || requestCode == JHConstants.REQ_INSTALL_CODE ){//在进入安装界面和设置权限界面后，返回到activity时再进行一次更新检测处理
    checkVersion();
    }
    }
    ```
    此方法分析：
    参数一requestCode：就是步骤3的请求码（暴躁的你是不是感觉这很像onActivityResult中的方法）
    参数二permissions：就是步骤3中的请求权限数组（看这回调，写的多周到）
    参数三int[] grantResults **重点**：如果你懂英文，也不难理解--授权结果，为什么是个int数组呢？
    因为 参数二是请求权限数组，多少个请求权限，就有多少个授权结果喽。

    那么，授权结果为什么是个int数组捏？
    这里就用到步骤二了。**PackageManager.PERMISSION_GRANTED呀！** 通过判断数组元素是否等于PackageManager.PERMISSION_GRANTED就行了呀。

    当然，为了使app更加人性化，我们在app未通过授权，又未显示授权框的时候，引导用户去**设置**中修改app权限：
    ` MySystemUtils.goToSetPermission(activity,"在设置-应用-权限中打开SD卡存储权限，以保证功能的正常使用",JHConstants.REQ_PERMISSION_CODE);`

    ```
    /**
    * 对于其他的权限，其实申请的逻辑是类似的；唯一不同的肯定就是参数,
    * 我们需要3个参数:Activity|Fragment、权限字符串数组、int型申请码。
    * 去设置权限
    * @param activity
    * @param message  显示的信息
    * @param req_permission  请求权限的码
    */
    public static void goToSetPermission(final Activity activity, String message, final int req_permission){
    new AlertDialog.Builder(activity)
    .setTitle("权限申请")
    .setMessage(message)
    .setPositiveButton("去开启", new DialogInterface.OnClickListener() {
    @Override
    public void onClick(DialogInterface dialog, int which) {
    //根据包名跳转到系统自带的应用程序信息界面
    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
    intent.setData(uri);
    activity.startActivityForResult(intent,req_permission);
    dialog.dismiss();
    }
    })
    .setCancelable(false)
    .show();
    }
    ```

    为什么要加一个req_permission的参数呢？
    因为，如果是强制更新的话，用户进入到设置页面，啥都不干，又点击返回按钮，就回来了，这时候，界面跟本没有任何回调，但是加了一个req_permission，可以在onActivityResult中获取到请求码，然后再继续进行弹窗，从而实现流氓级的强制更新（说实话，也不算是流氓，这个也是被逼无奈，如果版本里面有重大bug，用户又不更新，那咋办？用户会一直使用bug级的app。）

    当然，伟大的google为我们想的很周全，还提供了一个**shouldShowRequestPermissionRationale**的方法
    这个方法的作用是为了给 **对话框的频繁弹出** 做说明处理。
    比如：
    用户操作app，第一次时候弹出了一个对话框，要申请读取sd卡权限，然后用户也不知道干啥的，就直接给禁止了。
    当用户第二次做同样的操作时候，由于上一次没有选择“禁止后不在询问”，所以这次，又弹出了对话框。这时候，用户肯定会很郁闷啊，这玩意是干啥的，怎么老给我弹出对话框啊？？
    这个时候，**shouldShowRequestPermissionRationale**就可以派上用场了，通过判断此方法的返回值，
    如果是true，就可以在里面给用户一个提示处理的逻辑，告诉用户为什么要弹出对话框，应该怎么选择，巴拉巴拉巴的。。。。（不过好多app 似乎都没有做这个处理，其实个人感觉，这个方法很鸡肋）

    下班了， 写的有些仓促，有啥问题，直接密我吧

    项目我给放到git上了，欢迎来watch。
    [https://github.com/SuperChandler/TestForceUpdate](https://github.com/SuperChandler/TestForceUpdate)
    附赠怎么向github上传项目http://blog.csdn.net/var_rain/article/details/48736795