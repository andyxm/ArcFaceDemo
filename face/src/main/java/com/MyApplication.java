package com;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import com.alibaba.android.arouter.launcher.ARouter;
import com.blankj.utilcode.utils.Utils;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheEntity;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.cookie.CookieJarImpl;
import com.lzy.okgo.cookie.store.DBCookieStore;
import com.lzy.okgo.https.HttpsUtils;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class MyApplication extends Application {
	private final String TAG = this.getClass().toString();
	public FaceDB mFaceDB;
	public FaceHelper mFaceHelperDB;
	//  ARouter 调试开关
	private boolean isDebugARouter = true;
//    AFR_FSDKFace      保存人脸特征信息
//	  AFD_FSDKError     报错错误信息
//    AFR_FSDKMatching  两个人脸信息的分数0-1.0
//	  AFR_FSDKEngine    实现了人脸识别的功能
	@Override
	public void onCreate() {
		super.onCreate();
		//SDCard/Android/data/com.arcsoft.sdk_demo/cache
//		mFaceDB = new FaceDB(this.getExternalCacheDir().getPath());
		mFaceHelperDB=new FaceHelper(this.getExternalCacheDir().getPath());
		OkGo.getInstance().init(this);
		initOkGo();
		Utils.init(this);
		if (isDebugARouter) {
			// 下面两行必须写在init之前，否则这些配置在init过程中将无效
			ARouter.openLog();     // 打印日志
			// 开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！
			// 线上版本需要关闭,否则有安全风险)
			ARouter.openDebug();
		}
		// 官方建议推荐在Application中初始化
		ARouter.init(this);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		ARouter.getInstance().destroy();
	}

	public static Bitmap decodeImage(String path) {
		Bitmap res;
		try {
			ExifInterface exif = new ExifInterface(path);
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			BitmapFactory.Options op = new BitmapFactory.Options();
			op.inSampleSize = 1;
			op.inJustDecodeBounds = false;
			//op.inMutable = true;
			res = BitmapFactory.decodeFile(path, op);
			//rotate and scale.
			Matrix matrix = new Matrix();

			if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
				matrix.postRotate(90);
			} else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
				matrix.postRotate(180);
			} else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
				matrix.postRotate(270);
			}
			Bitmap temp = Bitmap.createBitmap(res, 0, 0, res.getWidth(), res.getHeight(), matrix, true);
			Log.d("com.arcsoft", "check target Image:" + temp.getWidth() + "X" + temp.getHeight());

			if (!temp.equals(res)) {
				res.recycle();
			}
			return temp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	private boolean isShowHttpLog=true;
	protected void initOkGo() {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		if (isShowHttpLog) {
			HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");
			loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);        //log打印级别，决定了log显示的详细程度
			loggingInterceptor.setColorLevel(Level.INFO);                               //log颜色级别，决定了log在控制台显示的颜色
			builder.addInterceptor(loggingInterceptor);                                 //添加OkGo默认debug日志
		}
		//第三方的开源库，使用通知显示当前请求的log，不过在做文件下载的时候，这个库好像有问题，对文件判断不准确
		//builder.addInterceptor(new ChuckInterceptor(this));

		//超时时间设置，默认60秒
		builder.readTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);      //全局的读取超时时间
		builder.writeTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);     //全局的写入超时时间
		builder.connectTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);   //全局的连接超时时间

		//自动管理cookie（或者叫session的保持），以下几种任选其一就行
		//builder.cookieJar(new CookieJarImpl(new SPCookieStore(this)));            //使用sp保持cookie，如果cookie不过期，则一直有效
		builder.cookieJar(new CookieJarImpl(new DBCookieStore(this)));              //使用数据库保持cookie，如果cookie不过期，则一直有效
		//builder.cookieJar(new CookieJarImpl(new MemoryCookieStore()));            //使用内存保持cookie，app退出后，cookie消失

		//https相关设置，以下几种方案根据需要自己设置
		//方法一：信任所有证书,不安全有风险
		HttpsUtils.SSLParams sslParams1 = HttpsUtils.getSslSocketFactory();
		//方法二：自定义信任规则，校验服务端证书
		HttpsUtils.SSLParams sslParams2 = HttpsUtils.getSslSocketFactory(new SafeTrustManager());
		//方法三：使用预埋证书，校验服务端证书（自签名证书）
		//HttpsUtils.SSLParams sslParams3 = HttpsUtils.getSslSocketFactory(getAssets().open("srca.cer"));
		//方法四：使用bks证书和密码管理客户端证书（双向认证），使用预埋证书，校验服务端证书（自签名证书）
		//HttpsUtils.SSLParams sslParams4 = HttpsUtils.getSslSocketFactory(getAssets().open("xxx.bks"), "123456", getAssets().open("yyy.cer"));
		builder.sslSocketFactory(sslParams1.sSLSocketFactory, sslParams1.trustManager);
		//配置https的域名匹配规则，详细看demo的初始化介绍，不需要就不要加入，使用不当会导致https握手失败
		builder.hostnameVerifier(new SafeHostnameVerifier());
		// 其他统一的配置
		// 详细说明看GitHub文档：https://github.com/jeasonlzy/
		OkGo.getInstance().init(this)                           //必须调用初始化
				.setOkHttpClient(builder.build())               //建议设置OkHttpClient，不设置会使用默认的
				.setCacheMode(CacheMode.NO_CACHE)               //全局统一缓存模式，默认不使用缓存，可以不传
				.setCacheTime(CacheEntity.CACHE_NEVER_EXPIRE)   //全局统一缓存时间，默认永不过期，可以不传
				.setRetryCount(3);                              //全局统一超时重连次数，默认为三次，那么最差的情况会请求4次(一次原始请求，三次重连请求)，不需要可以设置为0
//                .addCommonHeaders(headers)                      //全局公共头
//                .addCommonParams(params);                       //全局公共参数
	}
	private class SafeTrustManager implements X509TrustManager {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			try {
				for (X509Certificate certificate : chain) {
					certificate.checkValidity(); //检查证书是否过期，签名是否通过等
				}
			} catch (Exception e) {
				throw new CertificateException(e);
			}
		}
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	}
	private class SafeHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			//验证主机名是否匹配
			//return hostname.equals("server.jeasonlzy.com");
			return true;
		}
	}
}
