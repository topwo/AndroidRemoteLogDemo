package wang.catunclue.wslog;

import android.util.Log;

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
    private OkHttpClient mOkHttpClient;
    private WebSocket mWebSocket;
    private String wsUrl;
    private volatile boolean closing;

    public static WSLog getInstance() {
        return WSLog.SingletonHolder.sInstance;
    }

    public void init(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    public void setEnable(boolean b) {
        if (b) {
            connectWS();
            MonitorLogcat.getInstance().start(new MonitorLogcat.LogcatOutputCallback() {
                @Override
                public void onReaderLine(String line) {
                    if (mWebSocket != null && !closing) {
                        mWebSocket.send(line);
                    }
                }
            });
        } else {
            MonitorLogcat.getInstance().stop();
            disconnectWS();
        }
    }

    private void connectWS() {
        if (mOkHttpClient == null) {
            mOkHttpClient = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .build();
        }

        mOkHttpClient.dispatcher().cancelAll();

        mOkHttpClient.newWebSocket(
                new Request
                        .Builder()
                        .url(wsUrl)
                        .build(),
                new WebSocketListener() {

                    @Override
                    public void onOpen(WebSocket webSocket, final Response response) {
                        mWebSocket = webSocket;
                        closing = false;
                        Log.i(TAG, "onOpen---------------------------");
                    }

                    @Override
                    public void onMessage(WebSocket webSocket, String text) {
                        Log.i(TAG, "onMessage---------------------------"+text);
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
                        Log.i(TAG, "onFailure---------------------------"+response, t);
                    }
                });
    }

    private void disconnectWS() {
        closing = true;
        if (mOkHttpClient != null) {
            mOkHttpClient.dispatcher().cancelAll();
        }
        if (mWebSocket != null) {
//            mWebSocket.cancel();
            mWebSocket.close(1000, "normal");
            mWebSocket = null;
        }
    }

    private static class SingletonHolder {
        private static final WSLog sInstance = new WSLog();
    }

    private WSLog() {
    }
}
