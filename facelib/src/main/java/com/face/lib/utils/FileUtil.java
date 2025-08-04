package com.face.lib.utils;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;


import com.blankj.utilcode.util.FileUtils;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtil {
	private static final  String TAG = "FileUtil";
	public static final String IMAGE_FOLDER_PATH = Environment.getExternalStorageDirectory().getPath() + "/Face/Images";//图片保存目录
	public static final String IMAGE_REGIST_PATH = Environment.getExternalStorageDirectory().getPath() + "/Face/Regist/Images";//图片保存目录
	public static void saveBitmap(Bitmap b, String IMGE_NAME,boolean AutoRegist){
		if (AutoRegist){
			evenTypeSave(b,IMGE_NAME,IMAGE_REGIST_PATH);
		}else{
			evenTypeSave(b,IMGE_NAME,IMAGE_FOLDER_PATH);
		}
	}
	public static void evenTypeSave(Bitmap b, String IMGE_NAME,String path){
		boolean orExistsDir = FileUtils.createOrExistsDir(path);
		if(orExistsDir){
			String jpegName = path + "/" + IMGE_NAME;
			Log.i(TAG, "pic_name_all=" + jpegName);
			try {
				FileOutputStream fout = new FileOutputStream(jpegName);
				BufferedOutputStream bos = new BufferedOutputStream(fout);
				b.compress(Bitmap.CompressFormat.JPEG, 60, bos);
				bos.flush();
				bos.close();
				Log.i(TAG, "success..");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.i(TAG, "fail..");
				e.printStackTrace();
			}
		}else{
			Log.e(TAG,path+" create fail");
		}
	}
}
