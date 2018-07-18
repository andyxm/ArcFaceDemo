package com;
import com.face.lib.FaceDataHelper;
import com.model.StudentModel;
public class FaceHelper extends FaceDataHelper<StudentModel>{
	public static String appid = "DvyLb4FDPPY8SqZcz67QUxwfF2h2UxXb9i8z7561np3p";
	public static String ft_key = "6ufZxr4Y7KH9uzLShaXdmFuo2ooj5gaa6Tq5QXrLAp6T";
	public static String fd_key = "6ufZxr4Y7KH9uzLShaXdmFuvCD4toevs1YWdGF9Tsvns";
	public static String fr_key = "6ufZxr4Y7KH9uzLShaXdmFvHgQrNnmynxM7mhwbRwrMn";
	public static String age_key = "6ufZxr4Y7KH9uzLShaXdmFvfAcdufhbxea8jNu5fJPYz";
	public static String gender_key = "6ufZxr4Y7KH9uzLShaXdmFvnL1u46zVpW5wn7XAuxotS";
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
