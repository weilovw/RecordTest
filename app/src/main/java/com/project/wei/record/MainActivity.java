package com.project.wei.record;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int GET_RECODE_AUDIO = 1;
    private static String[] PERMISSION_AUDIO = {
            Manifest.permission.RECORD_AUDIO
    };
    private EditText et_input;
    private TextView text2;
    private Button btn_startspeech;



    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

    @SuppressLint("NewApi") @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy=new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=5cc29188");//初始化SDK
        initView();//初始化布局
    }
     //文字转语音
    private void speekText(String str) {
        //1. 创建 SpeechSynthesizer 对象 , 第二个参数： 本地合成时传 InitListener
        SpeechSynthesizer mTts = SpeechSynthesizer.createSynthesizer(this, null);
//2.合成参数设置，详见《 MSC Reference Manual》 SpeechSynthesizer 类
//设置发音人（更多在线发音人，用户可参见 附录 13.2
        mTts.setParameter(SpeechConstant.VOICE_NAME, "vixyun"); // 设置发音人
        mTts.setParameter(SpeechConstant.SPEED, "50");// 设置语速
        mTts.setParameter(SpeechConstant.VOLUME, "100");// 设置音量，范围 0~100
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端
//设置合成音频保存位置（可自定义保存位置），保存在 “./sdcard/iflytek.pcm”
//保存在 SD 卡需要在 AndroidManifest.xml 添加写 SD 卡权限
//仅支持保存为 pcm 和 wav 格式， 如果不需要保存合成音频，注释该行代码
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, "./sdcard/iflytek.pcm");
//3.开始合成

        ///.......从JSON中解析出来的answer放这里
       // str=et_input.getText().toString();
        mTts.startSpeaking(str, new MySynthesizerListener());

    }

    class MySynthesizerListener implements SynthesizerListener {

        @Override
        public void onSpeakBegin() {
            showTip(" 开始播放 ");
        }

        @Override
        public void onSpeakPaused() {
            showTip(" 暂停播放 ");
        }

        @Override
        public void onSpeakResumed() {
            showTip(" 继续播放 ");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                showTip("播放完成 ");
            } else if (error != null) {
                showTip(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话 id，当业务出错时将会话 id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话 id为null
            //if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //     String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //     Log.d(TAG, "session id =" + sid);
            //}
        }
    }

    private void startSpeechDialog() {
        //1. 创建RecognizerDialog对象
        RecognizerDialog mDialog = new RecognizerDialog(this, new MyInitListener());
        //2. 设置accent、 language等参数
        mDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");// 设置中文
        mDialog.setParameter(SpeechConstant.ACCENT, "mandarin");
        // 若要将UI控件用于语义理解，必须添加以下参数设置，设置之后 onResult回调返回将是语义理解
        // 结果
        // mDialog.setParameter("asr_sch", "1");
        // mDialog.setParameter("nlp_version", "2.0");
        //3.设置回调接口
        mDialog.setListener(new MyRecognizerDialogListener());
        //4. 显示dialog，接收语音输入
        mDialog.show();
    }

    class MyRecognizerDialogListener implements RecognizerDialogListener {
        String  OKhttSTR="I am Iron man";
        private static final String TAG = "MyRecognizerDialogListe";

        /**
         * @param results
         * @param isLast  是否说完了
         */
        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            String result = results.getResultString(); //为解析的
            showTip(result);
            System.out.println(" 没有解析的 :" + result);

            String text = JsonParser.parseIatResult(result);//解析过后的
            System.out.println(" 解析后的 :" + text);

            String sn = null;
            // 读取json结果中的 sn字段
            try {
                JSONObject resultJson = new JSONObject(results.getResultString());
                sn = resultJson.optString("sn");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mIatResults.put(sn, text);//没有得到一句，添加到

            StringBuffer resultBuffer = new StringBuffer();
            for (String key : mIatResults.keySet()) {
                resultBuffer.append(mIatResults.get(key));
            }
            Log.d("resultBuffer",resultBuffer.toString());
           // 得到的 resultBuffer即需要传入的JSON
            JSONObject body = new JSONObject();
            try{
                body.put("txt", resultBuffer.toString());
            } catch (Exception e){
            };

                //String GETresult = sendPostMessage(body, "utf-8");

            MediaType jsonType = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(jsonType,body.toString());
            System.out.println("传入的是"+body.toString());
            HttpUtil.sendOkHttpResponse("http://soulcode.cn/txtrobo/api/chat",requestBody, new okhttp3.Callback() {

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // 在这里根据返回内容执行具体的逻辑

                     OKhttSTR=response.body().string();
                    System.out.println("返回的是"+response.body().string());
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    // 在这里对异常情况进行处理
                }
            });
            //System.out.println(" 没有解析的小智返回文本 :" + GETresult);

            //et_input.setText(GETresult);// 设置输入框的文本
            //et_input.setSelection(et_input.length());//把光标定位末尾
            //speekText(GETresult);
            speekText(OKhttSTR);

        }

        public  String sendPostMessage(JSONObject body,String encode){
            StringBuffer buffer = new StringBuffer();
             String PATH ="http://soulcode.cn/txtrobo/api/chat";

            try {
                URL url = new URL(PATH);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
               // System.out.println(" 连接错误代码 :" + connection.getResponseCode());
                Log.d(TAG, "sendPostMessage: connection.getResponseCode()="+connection.getResponseCode());
                connection.setConnectTimeout(3000);
                connection.setDoInput(true);//表示从服务器获取数据
                connection.setDoOutput(true);//表示向服务器写数据

                connection.setRequestMethod("POST");
                //是否使用缓存
                connection.setUseCaches(false);
                //表示设置请求体的类型是文本类型
                connection.setRequestProperty("Content-Type", "application/json");

                DataOutputStream os = new DataOutputStream( connection.getOutputStream());
                String content = String.valueOf(body);
                os.writeBytes(content);
                os.flush();
                os.close();

                if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                    InputStreamReader in = new InputStreamReader(connection.getInputStream());
                    BufferedReader bf = new BufferedReader(in);
                    String recieveData = null;
                    String result = "";
                    while ((recieveData = bf.readLine()) != null){
                        result += recieveData + "\n";
                    }
                    in.close();
                    connection.disconnect();
                    return result;

                }

            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return "我是傻子";
        }

        @Override
        public void onError(SpeechError speechError) {

        }
    }

    class MyInitListener implements InitListener {
        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败 ");
            }

        }
    }

    /**
     * 语音识别
     */
    private void startSpeech() {
        //1. 创建SpeechRecognizer对象，第二个参数： 本地识别时传 InitListener
        SpeechRecognizer mIat = SpeechRecognizer.createRecognizer(this, null); //语音识别器
        //2. 设置听写参数，详见《 MSC Reference Manual》 SpeechConstant类
        mIat.setParameter(SpeechConstant.DOMAIN, "iat");// 短信和日常用语： iat (默认)
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");// 设置中文
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");// 设置普通话
        //3. 开始听写
        mIat.startListening(mRecoListener);
    }


    // 听写监听器
    private RecognizerListener mRecoListener = new RecognizerListener() {
        // 听写结果回调接口 (返回Json 格式结果，用户可参见附录 13.1)；
//一般情况下会通过onResults接口多次返回结果，完整的识别内容是多次结果的累加；
//关于解析Json的代码可参见 Demo中JsonParser 类；
//isLast等于true 时会话结束。
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.e("yyyy", results.getResultString());
            System.out.println(results.getResultString());
            showTip(results.getResultString());
        }

        // 会话发生错误回调接口
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
            // 获取错误码描述
            Log.e("yyyyy", "error.getPlainDescription(true)==" + error.getPlainDescription(true));
        }

        // 开始录音
        public void onBeginOfSpeech() {
            showTip(" 开始录音 ");
        }

        //volume 音量值0~30， data音频数据
        public void onVolumeChanged(int volume, byte[] data) {
            showTip(" 声音改变了 ");
        }

        // 结束录音
        public void onEndOfSpeech() {
            showTip(" 结束录音 ");
        }

        // 扩展用接口
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };

//吐司提示信息
    private void showTip(String data) {
        Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
    }

//那权限
    public static void verifyAudioPermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSION_AUDIO,
                    GET_RECODE_AUDIO);
        }
    }

    private void initView() {

        et_input = (EditText) findViewById(R.id.et_input);
        text2 = (TextView) findViewById(R.id.text2);
        btn_startspeech = (Button) findViewById(R.id.btn_startspeech);
        //btn_startspeektext = (Button) findViewById(R.id.btn_startspeektext);
        verifyAudioPermissions(this);
        btn_startspeech.setOnClickListener(this);
       // btn_startspeektext.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_startspeech:
                //语音识别（把声音转文字）
                startSpeechDialog();
                break;
        }
    }
    private void submit() {
        // validate
        String input = et_input.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            Toast.makeText(this, "请输入文本信息 ...", Toast.LENGTH_SHORT).show();
            return;
        }

    }

}
