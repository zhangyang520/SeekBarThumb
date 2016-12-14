package strollmuseum.iot.zhjy.com.mp3seekbar.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.example.zhanyang.seekbarmp3.IMp3Aidl;


import java.util.Timer;
import java.util.TimerTask;

import strollmuseum.iot.zhjy.com.mp3seekbar.MainActivity;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class Mp3PlayService extends IntentService {
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FOO = "com.example.zhanyang.seekbarmp3.service.action.FOO";
    private static final String ACTION_BAZ = "com.example.zhanyang.seekbarmp3.service.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.example.zhanyang.seekbarmp3.service.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.example.zhanyang.seekbarmp3.service.extra.PARAM2";

    //mp3传递的键
    public static String MP3_FILE_PATH_KEY="MP3_FILE_PATH_KEY";
    public static String MESSENGER_KEY="MESSENGER_KEY";
    private boolean hasPlayed=false;
    public Mp3PlayService() {
        super("Mp3PlayService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, Mp3PlayService.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, Mp3PlayService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FOO.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionFoo(param1, param2);
            } else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer=null;
        }

        if (timer!=null) {
            timer.cancel();
            timer=null;
        }
    }

    MediaPlayer mediaPlayer;//媒体播放器
    Timer timer;//定时器
    String filePath;
    Messenger messenger;
    boolean isPaused;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //进行通过intent进行传递路径的mp3
        filePath=intent.getStringExtra(MP3_FILE_PATH_KEY);
        messenger=intent.getParcelableExtra(MESSENGER_KEY);
        System.out.println("filePath:"+filePath+"..messenger:"+(messenger==null));
        return new MP3PlayService();
    }

    /**进行播放音乐
     * @param intent
     *
     */
    private void playMp3() {
        try {
            if(mediaPlayer!=null){
                //如果meadiaPlay不为空 进行资源的释放
                mediaPlayer.release();
                mediaPlayer=null;
            }
            mediaPlayer=MediaPlayer.create(getApplicationContext(), Uri.parse("file://" + filePath));
            mediaPlayer.setLooping(false);
            //进行准备播放
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    //准备完毕
                    mediaPlayer.start();
                    addTimer();
                }

                /**
                 * 进行创建定时器
                 */
                private void addTimer() {
                    if(timer!=null){
                        timer.cancel();
                        timer=null;
                    }
                    timer=new Timer();
                    hasPlayed=true;
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            //进行定时地发送信息
//                            System.out.println("timer.  isPaused..."+isPaused);
                            synchronized (Mp3PlayService.this) {
                                if (!isPaused) {
                                    if ((float)mediaPlayer.getCurrentPosition()/(float)mediaPlayer.getDuration()>=0.999){
                                        //已经停止了
                                        try {
                                            hasPlayed=false;
                                            Message message=Message.obtain();
                                            message.what= MainActivity.MSG_DONE;
                                            messenger.send(message);
                                            timer.cancel();
                                            timer=null;
                                            System.out.println("messenger.send MSG_DONE...");
                                        } catch (RemoteException e) {
                                            e.printStackTrace();
                                            System.out.println("messenger.send RemoteException...");
                                        }
                                    }else{
                                        //正在进行中.....
                                        try {
                                            Message message=Message.obtain();
                                            message.what= MainActivity.MSG_DOING;
                                            Bundle bundle=new Bundle();
                                            bundle.putInt(MainActivity.MP3_DURATION, mediaPlayer.getDuration());
                                            bundle.putInt(MainActivity.MP3_CURRATION,mediaPlayer.getCurrentPosition());
                                            message.setData(bundle);
                                            messenger.send(message);
                                            System.out.println("messenger.send MSG_DOING...");
                                        } catch (RemoteException e) {
                                            e.printStackTrace();
                                            System.out.println("messenger.send RemoteException...");
                                        }
                                    }
                                }
                            }
                        }
                    },0,200);
                }
            });

            /**
             * 进行播放完毕 注意魅族手机，该方法不会调用!
             * */
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    //进行通知停止播放
                    try {
                        hasPlayed=false;
                        Message message=Message.obtain();
                        message.what= MainActivity.MSG_DONE;
                        messenger.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        System.out.println("messenger.send RemoteException...");
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public class MP3PlayService extends IMp3Aidl.Stub{
        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

        }

        @Override
        public void start() throws RemoteException {
            System.out.println("service start...hasPlayed:"+hasPlayed+"...filepath:"+filePath);
            if (!hasPlayed) {
                //进行播放mp3
                playMp3();
            }else{
                //进行启动
                isPaused=false;
                mediaPlayer.start();
            }
        }

        @Override
        public void stop() throws RemoteException {
            isPaused=true;
            mediaPlayer.stop();
        }

        /**
         * 进行停止
         * @throws RemoteException
         */
        @Override
        public void pause() throws RemoteException {
            isPaused=true;
            System.out.println("pause..:"+isPaused);
            mediaPlayer.pause();
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public void seekTo(int progress) throws RemoteException {
            mediaPlayer.seekTo(progress);
        }

        @Override
        public boolean hasStart() throws RemoteException {
            return hasPlayed;
        }
    }
}
