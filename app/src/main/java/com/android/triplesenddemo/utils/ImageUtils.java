package com.android.triplesenddemo.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 图片旋转处理工具类
 */

public class ImageUtils {

    public static File createExpression(final String title,
                                        final String path1,
                                        final String path2,
                                        final String path3,
                                        final String name1,
                                        final String name2,
                                        final String name3,
                                        final String savePath) {
        final int backgroundWidth = 600;
        final int backgroundHeight = 300;
        final int backgroundColor = 0xffffffff;
        final int textColor = 0xff000000;
        final int textSize = 30;
        final int pictureWidth = 200;
        final int pictureHeight = 200;
        final int pictureTop = 50;

        final Paint paint = new Paint();
        paint.setColor(backgroundColor);
        paint.setStyle(Paint.Style.FILL);

        final Bitmap picture = Bitmap.createBitmap(backgroundWidth, backgroundHeight, Bitmap.Config.ARGB_8888);
        final Rect background = new Rect(0, 0, backgroundWidth, backgroundHeight);

        final Canvas canvas = new Canvas(picture);
        canvas.drawRect(background, paint);

        final Bitmap bitmap1 = BitmapFactory.decodeFile(path1);
        final Rect pictureRect1 = new Rect(0, 0, pictureWidth, pictureHeight);
        final RectF dst1 = new RectF(0, pictureTop, pictureWidth, pictureHeight + pictureTop);
        canvas.drawBitmap(bitmap1, pictureRect1, dst1, null);
        bitmap1.recycle();

        final Bitmap bitmap2 = BitmapFactory.decodeFile(path2);
        final Rect pictureRect2 = new Rect(0, 0, pictureWidth, pictureHeight);
        final RectF dst2 = new RectF(pictureWidth, pictureTop, pictureWidth * 2, pictureHeight + pictureTop);
        canvas.drawBitmap(bitmap2, pictureRect2, dst2, null);
        bitmap2.recycle();

        final Bitmap bitmap3 = BitmapFactory.decodeFile(path3);
        final Rect pictureRect3 = new Rect(0, 0, pictureWidth, pictureHeight);
        final RectF dst3 = new RectF(pictureWidth * 2, pictureTop, pictureWidth * 3, pictureHeight + pictureTop);
        canvas.drawBitmap(bitmap3, pictureRect3, dst3, null);
        bitmap3.recycle();

        paint.reset();
        paint.setColor(textColor);
        paint.setTextSize(textSize);

        final Rect titleRect = new Rect();
        paint.getTextBounds(title, 0, title.length(), titleRect);
        final float titleLeft = (backgroundWidth - titleRect.right) / 2f;
        final float titleTop = (pictureTop - textSize) / 2f - titleRect.top;
        canvas.drawText(title, titleLeft, titleTop, paint);


        final String imageName = System.currentTimeMillis() + ".jpg";
        final File newFile = ImageUtils.saveBitmapToJpg(picture, savePath, imageName, 100);
        picture.recycle();

        return newFile;
    }

    public static File drawWaterMark(final String filePath,
                                     final String savePath,
                                     final Drawable timeDrawable,
                                     final Drawable addressDrawable,
                                     final String datetime,
                                     final String address,
                                     int quality) {
        final File file = new File(filePath);

        if (file.exists()) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inDither = false;//不进行图片抖动处理
            options.inPreferredConfig = null;//设置让解码器以最佳方式解码
            final Bitmap bitmap = BitmapFactory.decodeFile(filePath, options).copy(Bitmap.Config.ARGB_8888, true);
            if (bitmap != null) {
                final int angle = ImageUtils.readPictureDegree(filePath);
                final Bitmap newBitmap;
                if (angle != 0) {
                    newBitmap = ImageUtils.rotaingImageView(angle, bitmap);
                    bitmap.recycle();
                } else {
                    newBitmap = bitmap;
                }

                final int bitmapWidth = newBitmap.getWidth();
                final int bitmapHeight = newBitmap.getHeight();
                final int backgroundHeight = (int) (Math.min(bitmapWidth, bitmapHeight) * 0.15f);

                final Canvas canvas = new Canvas(newBitmap);
                final Paint paint = new Paint();
                paint.setColor(0x66000000);
                paint.setStyle(Paint.Style.FILL);

                final Rect backgroundRect = new Rect(0, bitmapHeight - backgroundHeight, bitmapWidth, bitmapHeight);
                canvas.drawRect(backgroundRect, paint);

                final float iconTextSize = backgroundHeight / 4f;
                final float iconLeft = iconTextSize / 2f;
                final float iconRight = iconLeft + iconTextSize;
                final float timeIconTop = backgroundRect.top + iconTextSize / 4f;
                final float timeIconBottom = timeIconTop + iconTextSize;
                final float addressIconTop = timeIconBottom + iconTextSize / 2f;
                final float addressIconBottom = addressIconTop + iconTextSize;

                timeDrawable.setBounds((int) iconLeft, (int) timeIconTop, (int) iconRight, (int) timeIconBottom);
                addressDrawable.setBounds((int) iconLeft, (int) addressIconTop, (int) iconRight, (int) addressIconBottom);
                timeDrawable.draw(canvas);
                addressDrawable.draw(canvas);

                final Rect datetimeRect = new Rect();
                final Rect addressRect = new Rect();

                paint.reset();
                paint.setColor(0xffffffff);
                paint.setTextSize(iconTextSize);
                paint.getTextBounds(datetime, 0, datetime.length(), datetimeRect);
                paint.getTextBounds(address, 0, address.length(), addressRect);

                final float datetimeTextX = iconRight + iconTextSize / 2f;
                final float addressTextX = iconRight + iconTextSize / 2f;
                final float datetimeTextY = timeIconTop - datetimeRect.top;
                final float addressTextY = addressIconTop - addressRect.top;

                canvas.drawText(datetime, datetimeTextX, datetimeTextY, paint);
                canvas.drawText(address, addressTextX, addressTextY, paint);

                paint.reset();

                final String imageName = "watermark_" + quality + "_" + System.currentTimeMillis() + ".jpg";
                final File newFile = ImageUtils.saveBitmapToJpg(newBitmap, savePath, imageName, quality);

                newBitmap.recycle();

                return newFile;
            }
        }
        return null;
    }

    /**
     * 获取图片角度
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 图片旋转
     */
    public static Bitmap rotaingImageView(int angle, Bitmap bitmap) {
        // 旋转图片 动作
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // 创建新的图片
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    //把Bitmap转换成jpg图片
    public static File saveBitmapToJpg(Bitmap bitmap, String path, String bitName, int quality) {
        File file = new File(path);

        try {
            if (!file.exists()) {
                file.mkdir();
            }

            file = new File(path, bitName);

//            if (!file.exists()) {
//                file.createNewFile();
//            }

            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }
}
