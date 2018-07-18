package com;
import com.face.lib.FaceDataHelper;
import com.model.StudentModel;
public class FaceHelper extends FaceDataHelper<StudentModel>{
	public static String appid = "xxxxxxxxxxxxxxxxxxx";
	public static String ft_key = "xxxxxxxxxxxxxxxxxxx";
	public static String fd_key = "xxxxxxxxxxxxxxxxxxx";
	public static String fr_key = "xxxxxxxxxxxxxxxxxxx";
	public static String age_key = "xxxxxxxxxxxxxxxxxxx";
	public static String gender_key = "xxxxxxxxxxxxxxxxxxx";
	public FaceHelper(String path) {
		super(path);
	}

	@Override
	protected String appid() {
		return appid;
	}
	@Override
	protected String ft_key() {
		return ft_key;
	}
	@Override
	protected String fd_key() {
		return fd_key;
	}
	@Override
	protected String fr_key() {
		return fr_key;
	}
	@Override
	protected String age_key() {
		return age_key;
	}
	@Override
	protected String gender_key() {
		return gender_key;
	}

	@Override
	protected boolean isSaveSdcard() {
		return true;
	}

	@Override
	protected StudentModel getT(String card) {
		return new StudentModel();
	}
}
