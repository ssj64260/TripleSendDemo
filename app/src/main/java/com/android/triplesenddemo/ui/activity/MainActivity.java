package com.android.triplesenddemo.ui.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.triplesenddemo.R;
import com.android.triplesenddemo.app.BaseActivity;
import com.android.triplesenddemo.config.Constants;
import com.android.triplesenddemo.utils.AppManager;
import com.android.triplesenddemo.utils.DataCleanManager;
import com.android.triplesenddemo.utils.FastClick;
import com.android.triplesenddemo.utils.ImageUtils;
import com.android.triplesenddemo.utils.SDCardUtils;
import com.android.triplesenddemo.utils.ThreadPoolUtil;
import com.android.triplesenddemo.utils.ToastMaster;
import com.android.triplesenddemo.widget.imageloader.ImageLoaderFactory;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_TO_SETTING = 1000;//跳转到系统设置权限页面
    private static final int REQUEST_CODE_PICTURE1 = 1001;
    private static final int REQUEST_CODE_PICTURE2 = 1002;
    private static final int REQUEST_CODE_PICTURE3 = 1003;

    private CoordinatorLayout mRootView;
    private Toolbar mToolbar;
    private EditText etTitle;
    private ImageView ivPicture1;
    private ImageView ivPicture2;
    private ImageView ivPicture3;
    private EditText etName1;
    private EditText etName2;
    private EditText etName3;
    private TextView tvDoCreate;
    private TextView tvPath;
    private ImageView ivPreview;
    private TextView tvDelete;

    private int permissionPosition = 0;//当前请求权限位置
    private String[] permissions;
    private String[] errorTips;

    private AlertDialog mAlertDialog;

    private String mPath1;
    private String mPath2;
    private String mPath3;

    private String mBasePath;
    private String mCachePath;
    private File mCurrentImage;

    private View.OnClickListener mClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hideKeyboard();
            switch (v.getId()) {
                case R.id.iv_picture1:
                    selectPicture(REQUEST_CODE_PICTURE1);
                    break;
                case R.id.iv_picture2:
                    selectPicture(REQUEST_CODE_PICTURE2);
                    break;
                case R.id.iv_picture3:
                    selectPicture(REQUEST_CODE_PICTURE3);
                    break;
                case R.id.tv_do_create:
                    doCreatePicture();
                    break;
                case R.id.iv_preview:
                    doShare();
                    break;
                case R.id.tv_delete:
                    doDelete();
                    break;
            }
        }
    };

    private View.OnLongClickListener mLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            hideKeyboard();
            if (!TextUtils.isEmpty(mPath1)) {
                switch (v.getId()) {
                    case R.id.iv_picture2:
                        mPath2 = mPath1;
                        ImageLoaderFactory.getLoader().loadImage(MainActivity.this, ivPicture2, mPath2);
                        break;
                    case R.id.iv_picture3:
                        mPath3 = mPath1;
                        ImageLoaderFactory.getLoader().loadImage(MainActivity.this, ivPicture3, mPath3);
                        break;
                }
            } else {
                Snackbar.make(mRootView, "请先选择图片1", Snackbar.LENGTH_LONG).show();
            }
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        requestPermission();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        final File cacheDir = new File(mCachePath);
        if (cacheDir.exists()) {
            DataCleanManager.deleteAllFiles(cacheDir);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_TO_SETTING == requestCode) {
            if (permissionPosition < permissions.length) {
                if (ContextCompat.checkSelfPermission(this, permissions[permissionPosition]) != PackageManager.PERMISSION_GRANTED) {
                    finish();
                } else {
                    permissionPosition++;
                    requestPermission();
                }
            }
        } else if (resultCode == RESULT_OK) {
            final List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
            if (selectList != null && selectList.size() > 0) {
                final LocalMedia media = selectList.get(0);
                if (REQUEST_CODE_PICTURE1 == requestCode) {
                    mPath1 = media.getCompressPath();
                    ImageLoaderFactory.getLoader().loadImage(MainActivity.this, ivPicture1, mPath1);
                } else if (REQUEST_CODE_PICTURE2 == requestCode) {
                    mPath2 = media.getCompressPath();
                    ImageLoaderFactory.getLoader().loadImage(MainActivity.this, ivPicture2, mPath2);
                } else {
                    mPath3 = media.getCompressPath();
                    ImageLoaderFactory.getLoader().loadImage(MainActivity.this, ivPicture3, mPath3);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                if (permissionPosition < errorTips.length) {
                    showPermissionTipsDialog();
                } else {
                    finish();
                }
            } else {
                permissionPosition++;
                requestPermission();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!FastClick.isExitClick()) {
            ToastMaster.toast("再次点击退出程序");
        } else {
            super.onBackPressed();
        }
    }

    private void initData() {
        mBasePath = SDCardUtils.getSDCardDir() + Constants.PATH_BASE;
        mCachePath = SDCardUtils.getExternalCacheDir(this);

        final File baseDir = new File(mBasePath);
        final File tempDir = new File(mCachePath);
        if (!baseDir.exists()) {
            baseDir.mkdir();
        }
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }

        final String appName = getString(R.string.app_name);
        permissions = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };
        errorTips = new String[]{
                String.format("在设置-应用-%1$s-权限中开启存储权限，以正常使用该功能", appName),
                String.format("在设置-应用-%1$s-权限中开启相机权限，以正常使用该功能", appName),
                String.format("在设置-应用-%1$s-权限中开启麦克风权限，以正常使用该功能", appName)
        };

        final List<String> requestList = new ArrayList<>();
        final List<String> errorTipsList = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                String tips = this.errorTips[i];
                requestList.add(permission);
                errorTipsList.add(tips);
            }
        }
        permissions = requestList.toArray(new String[requestList.size()]);
        errorTips = errorTipsList.toArray(new String[errorTipsList.size()]);
    }

    private void initView() {
        mRootView = (CoordinatorLayout) findViewById(R.id.rootview);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        etTitle = (EditText) findViewById(R.id.et_title);
        ivPicture1 = (ImageView) findViewById(R.id.iv_picture1);
        ivPicture2 = (ImageView) findViewById(R.id.iv_picture2);
        ivPicture3 = (ImageView) findViewById(R.id.iv_picture3);
        etName1 = (EditText) findViewById(R.id.et_name1);
        etName2 = (EditText) findViewById(R.id.et_name2);
        etName3 = (EditText) findViewById(R.id.et_name3);
        tvDoCreate = (TextView) findViewById(R.id.tv_do_create);
        tvPath = (TextView) findViewById(R.id.tv_path);
        ivPreview = (ImageView) findViewById(R.id.iv_preview);
        tvDelete = (TextView) findViewById(R.id.tv_delete);

        mToolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(mToolbar);

        ivPicture1.setOnClickListener(mClick);
        ivPicture2.setOnClickListener(mClick);
        ivPicture3.setOnClickListener(mClick);
        tvDoCreate.setOnClickListener(mClick);
        ivPreview.setOnClickListener(mClick);
        tvDelete.setOnClickListener(mClick);

        ivPicture2.setOnLongClickListener(mLongClick);
        ivPicture3.setOnLongClickListener(mLongClick);
    }

    private void requestPermission() {
        if (permissionPosition < permissions.length) {
            ActivityCompat.requestPermissions(this, new String[]{permissions[permissionPosition]}, permissionPosition);
        } else {
            initView();
        }
    }

    private void showPermissionTipsDialog() {
        if (mAlertDialog == null) {
            mAlertDialog = new AlertDialog.Builder(this).create();
            mAlertDialog.setTitle("权限申请");
            mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "去设置", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    AppManager.showInstalledAppDetails(MainActivity.this, getPackageName(), REQUEST_TO_SETTING);
                }
            });
            mAlertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        }
        mAlertDialog.setMessage(errorTips[permissionPosition]);
        mAlertDialog.show();
    }

    private void selectPicture(int requestCode) {
        PictureSelector.create(this)
                .openGallery(PictureMimeType.ofImage())
                .imageSpanCount(3)
                .selectionMode(PictureConfig.SINGLE)
                .previewImage(false)
                .isCamera(false)
                .imageFormat(PictureMimeType.JPEG)
                .isZoomAnim(false)
                .glideOverride(200, 200)
                .enableCrop(true)
                .compress(true)
                .withAspectRatio(1, 1)
                .hideBottomControls(false)
                .isGif(false)
                .compressSavePath(mCachePath)
                .freeStyleCropEnabled(false)
                .circleDimmedLayer(false)
                .showCropFrame(true)
                .showCropGrid(false)
                .cropCompressQuality(90)
                .minimumCompressSize(100)
                .cropWH(200, 200)
                .rotateEnabled(false)
                .scaleEnabled(true)
                .forResult(requestCode);
    }

    private void doCreatePicture() {
        final String title = etTitle.getText().toString();
        final String name1 = etName1.getText().toString();
        final String name2 = etName2.getText().toString();
        final String name3 = etName3.getText().toString();
        if (TextUtils.isEmpty(title)) {
            Snackbar.make(mRootView, "请先输入标题", Snackbar.LENGTH_LONG).show();
        } else if (TextUtils.isEmpty(mPath1)) {
            Snackbar.make(mRootView, "请先选择图片1", Snackbar.LENGTH_LONG).show();
        } else if (TextUtils.isEmpty(mPath2)) {
            Snackbar.make(mRootView, "请先选择图片2", Snackbar.LENGTH_LONG).show();
        } else if (TextUtils.isEmpty(mPath3)) {
            Snackbar.make(mRootView, "请先选择图片3", Snackbar.LENGTH_LONG).show();
        } else if (TextUtils.isEmpty(name1)) {
            Snackbar.make(mRootView, "请输入图片1文字内容", Snackbar.LENGTH_LONG).show();
        } else if (TextUtils.isEmpty(name2)) {
            Snackbar.make(mRootView, "请输入图片2文字内容", Snackbar.LENGTH_LONG).show();
        } else if (TextUtils.isEmpty(name3)) {
            Snackbar.make(mRootView, "请输入图片3文字内容", Snackbar.LENGTH_LONG).show();
        } else {
            showProgress("图片处理中...");
            ThreadPoolUtil.getInstache().cachedExecute(new Runnable() {
                @Override
                public void run() {
                    mCurrentImage = ImageUtils.createExpression(title, mPath1, mPath2, mPath3, name1, name2, name3, mBasePath);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mCurrentImage.exists()) {
                                final String filePath = mCurrentImage.getAbsolutePath();
                                ImageLoaderFactory.getLoader().loadImage(MainActivity.this, ivPreview, filePath);
                                tvPath.setText("保存路径：" + filePath);
                            } else {
                                Snackbar.make(mRootView, "生成失败", Snackbar.LENGTH_LONG).show();
                            }
                            hideProgress();
                        }
                    });
                }
            });
        }
    }

    private void doShare() {
        if (mCurrentImage != null && mCurrentImage.exists() && mCurrentImage.isFile()) {
            final Uri uri = Uri.fromFile(mCurrentImage);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/jpg");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, ""));
        } else {
            Snackbar.make(mRootView, "文件不存在", Snackbar.LENGTH_LONG).show();
        }
    }

    private void doDelete() {
        if (mCurrentImage != null && mCurrentImage.exists()) {
            mCurrentImage.delete();
            tvPath.setText("");
            ivPreview.setImageResource(0);
        }
        Snackbar.make(mRootView, "文件已删除", Snackbar.LENGTH_LONG).show();
    }
}
