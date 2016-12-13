package strollmuseum.iot.zhjy.com.mp3seekbar;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.SeekBar;

import com.example.zhanyang.seekbarmp3.IMp3Aidl;

import java.io.File;

import strollmuseum.iot.zhjy.com.mp3seekbar.service.Mp3PlayService;

/**
 * 进行播放音乐：
 *    seekBar的拖拽形式的播放形式
 *      通过thumb形式:播放时:点击thumb进行暂停,暂停时:点击thumb时继续播放
 *      应用了seekBar的onPageChangeListener的监听器
 */

public class MainActivity extends AppCompatActivity{

    SeekBar seekBar;
    Mp3PlayService.MP3PlayService mp3PlayService;
    MyServiceConnection myServiceConnection;

    //数字标识
    public final static int MSG_DOING=1001;
    public final static int MSG_DONE=1002;
    //mp3文件的长度
    public static final String MP3_DURATION="MP3_DURATION";
    //mp3当前进度
    public static final String MP3_CURRATION="MP3_CURRATION";

    public  boolean iscanPlay =true;//是否要播放
    int currentProgress=0;//当前的进度

    public  Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //进行处理message
            switch (msg.what) {
                case MSG_DOING:
                    if(!iscanPlay){
                        //不进行播放
                        currentProgress=msg.getData().getInt(MP3_CURRATION);
                        System.out.println("MSG_DOING ...currentProgress:"+currentProgress);
                        seekBar.setProgress(currentProgress);
                        seekBar.setMax(msg.getData().getInt(MP3_DURATION));
                        seekBar.setSelected(iscanPlay);
                    }
                    break;

                //正常情况下停止
                case MSG_DONE:
                    iscanPlay =true;
                    seekBar.setProgress(0);
                    seekBar.setMax(msg.getData().getInt(MP3_DURATION));
                    seekBar.setSelected(iscanPlay);
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        seekBar=(SeekBar)findViewById(R.id.seekBar);
        seekBar.setProgress(0);
        seekBar.setSelected(true);
        //进行绑定对应的服务
        bind();

        //对seekBar进行设置监听事件
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            long startTime;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                System.out.println("seekBar onProgressChanged ...progress:"+progress+"...fromUser:"+fromUser);
                //seekBar事件改变过程中
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //开始改变
                System.out.println("seekBar onStartTrackingTouch ...:");
                startTime=System.currentTimeMillis();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                System.out.println("seekBar onStopTrackingTouch ...time delagate:"+(System.currentTimeMillis()-startTime));
                //停止改变
                if (System.currentTimeMillis()-startTime<120) {
                    //短时间进行按下,暂停...
                    if(iscanPlay){
                        //进行开始启动
                        try {
                            iscanPlay =false;
                            seekBar.setProgress(currentProgress);
                            seekBar.setSelected(iscanPlay);
                            mp3PlayService.start();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }else{
                        //短时间的暂停
                        try {
                            mp3PlayService.pause();
                            iscanPlay =true;
                            seekBar.setProgress(currentProgress);
                            mp3PlayService.seekTo(currentProgress);
                            seekBar.setSelected(iscanPlay);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    //长时间拖动
                    if((float)currentProgress/(float)seekBar.getMax()>0.999){
                        //如果拖动的进度大于0.99
                        try {
                            iscanPlay =true;
                            seekBar.setProgress(0);
                            mp3PlayService.stop();
                            seekBar.setSelected(iscanPlay);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }else{
                        try {
                            currentProgress=seekBar.getProgress();
                            seekBar.setProgress(currentProgress);
                            mp3PlayService.seekTo(currentProgress);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void bind() {
        //进行绑定setvice
        myServiceConnection=new MyServiceConnection();
        Messenger messenger=new Messenger(handler);
        Intent intent=new Intent(this, Mp3PlayService.class);
        //指定对应的语音文件路径
        String filePath= Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator+"music"+File.separator+"Avril-Lavigne"+File.separator+"take me away.mp3";
        intent.putExtra(Mp3PlayService.MP3_FILE_PATH_KEY,filePath);
        intent.putExtra(Mp3PlayService.MESSENGER_KEY,messenger);
        bindService(intent,myServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(myServiceConnection!=null){
            unbindService(myServiceConnection);
            mp3PlayService=null;
        }
    }

    class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mp3PlayService=(Mp3PlayService.MP3PlayService) IMp3Aidl.Stub.asInterface(service);
            System.out.println("MyServiceConnection onServiceConnected mp3PlayService is null:"+(mp3PlayService==null));
            try {
                service.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        //绑定服务
                        bind();
                    }
                },-1);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }
}

