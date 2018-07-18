本应用来源于虹软中国,人脸识别技术工程如何使用？
//首先先说句道歉，因为我发现上一期写的demo发布到git上没有完整的文件。这是我的疏忽

1.下载代码 git clone https://github.com/andyxm/ArcFace.git

2.下载虹软人脸识别库 http://www.arcsoft.com.cn/ai/sdk/apply-1002-4.html 注册成功后，获取对应的key下载对应的android SDK,将对应平台的so和jar都放入demo中的libs里 https://pan.baidu.com/s/1H3H1VxGpEv7-8ODyV8G4dw 密码tq2w下载的libs库后放入facelib下Module下(如果有key可以直接网盘下载)

3.FaceHelper中实现了FaceDataHelper，填写你的所有对应的key,和你将要查询的StudentModel,其中isSaveSdcard如果要保存到sd卡一定要设置true

4.FaceFRAbsLoop中设置你要设置的分数值

5.图片的存储路径在->/sdcard/Face/Images,人脸数据存储路径->/sdcard/Android/data/com.face/cache andorid studio 的Device File Explorer查看即可 写的不是很好,请多多指教
注意：部分开发板可能会有问题，去找你的厂商协调下 使用摄像头时，请自行调节就可以了
由于能力有限，程序如有更好改进的地方,请大家留言