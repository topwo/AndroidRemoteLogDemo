package wang.catunclue.wslog;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * the manager for ws remote log
 *
 * @author 喵叔catuncle
 * Created at 2018/12/2 16:00
 */
public class WSLog {
    private static final String TAG = WSLog.class.getSimpleName();

    private static class SingletonHolder {
        private static final WSLog sInstance = new WSLog();
    }

    private interface Msg {
        int OPEN = 1;
        int CLOSE = 2;
    }


    private String wsUrlStr;
    private HandlerThread workerThread;
    private Handler workHandler;
    private OkHttpClient mOkHttpClient;
    private WebSocket mWebSocket;
    private volatile boolean closing;

    private WSLog() {
        workerThread = new HandlerThread("WSLog-thread");
        workerThread.start();
        workHandler = new Handler(workerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == Msg.OPEN) {
                    connectWS();
                    MonitorLogcat.getInstance().start(new MonitorLogcat.LogcatOutputCallback() {
                        @Override
                        public void onReaderLine(String line) {
                            if (mWebSocket != null && !closing) {
                                mWebSocket.send(line);
                            }
                        }
                    });
                } else if (msg.what == Msg.CLOSE) {
                    MonitorLogcat.getInstance().stop();
                    disconnectWS();
                }
            }
        };
    }

    public static WSLog getInstance() {
        return WSLog.SingletonHolder.sInstance;
    }

    public void initUrlRoutePath(String urlStr, String identity) {
        wsUrlStr = (TextUtils.isEmpty(urlStr) ? "ws://log.frp.topwo.com:8999" : urlStr) + (TextUtils.isEmpty(identity) ? "" : "/" + identity);
    }

    public void setEnable(boolean b) {
        if (b) {
            workHandler.sendEmptyMessage(Msg.OPEN);
        } else {
            workHandler.sendEmptyMessage(Msg.CLOSE);
        }
    }

    private synchronized void connectWS() {
        synchronized (WSLog.class) {

            if (mOkHttpClient == null) {
                mOkHttpClient = new OkHttpClient.Builder()
                        .retryOnConnectionFailure(true)
                        .build();
            }

            mOkHttpClient.dispatcher().cancelAll();

            mOkHttpClient.newWebSocket(
				new Request.Builder()
                    .url(wsUrlStr + "/" + new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss.SSS").format(new Date()))
                    .build(), new WebSocketListener() {

                @Override
                public void onOpen(WebSocket webSocket, final Response response) {
                    mWebSocket = webSocket;
                    closing = false;
                    Log.i(TAG, "onOpen---------------------------");
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    Log.i(TAG, "onMessage---------------------------" + text);
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    Log.i(TAG, "onClosing---------------------------");
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    Log.i(TAG, "onClosed---------------------------");
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    Log.i(TAG, "onFailure---------------------------" + response, t);
                }
            });
        }
    }

    private void disconnectWS() {
        closing = true;
        if (mOkHttpClient != null) {
            mOkHttpClient.dispatcher().cancelAll();
        }
        if (mWebSocket != null) {
//            mWebSocket.cancel();
            boolean b = mWebSocket.close(1000, "NORMAL");
            Log.i(TAG, "mWebSocket.close = " + b);
            mWebSocket = null;
        }
    }
}
