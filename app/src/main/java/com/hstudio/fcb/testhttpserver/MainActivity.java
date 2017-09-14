package com.hstudio.fcb.testhttpserver;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
    HttpServiceThread httpServiceThread;

    TextView infoIp;
    ImageView iv;
    Button button;
    Uri selectedImageUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoIp = (TextView) findViewById(R.id.infoip);
        infoIp.setText(getIpAddress() + ":"
                + HttpServiceThread.HttpServerPORT + "\n");

        httpServiceThread = new HttpServiceThread();
        httpServiceThread.start();
        iv = (ImageView)findViewById(R.id.imageView);
        button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 999);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 999 && resultCode == RESULT_OK && data != null && data.getData() != null) {
             selectedImageUri = data.getData();
            getRealPathFromURI(selectedImageUri);
           iv.setImageBitmap(BitmapFactory.decodeFile(getRealPathFromURI(selectedImageUri)));
        }
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        httpServiceThread.stopServer();
    }
    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    private class HttpServiceThread extends Thread {

        ServerSocket serverSocket;
        Socket socket;
        HttpService httpService;
        BasicHttpContext basicHttpContext;
        static final int HttpServerPORT = 8080;
        boolean RUNNING = false;

        HttpServiceThread() {
            RUNNING = true;
            startHttpService();
        }

        @Override
        public void run() {

            try {
                serverSocket = new ServerSocket(HttpServerPORT);
                serverSocket.setReuseAddress(true);

                while (RUNNING) {
                    socket = serverSocket.accept();

                    DefaultHttpServerConnection httpServerConnection = new DefaultHttpServerConnection();
                    httpServerConnection.bind(socket, new BasicHttpParams());
                    httpService.handleRequest(httpServerConnection,
                            basicHttpContext);
                    httpServerConnection.shutdown();
                }
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (HttpException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        private synchronized void startHttpService() {
            BasicHttpProcessor basicHttpProcessor = new BasicHttpProcessor();
            basicHttpContext = new BasicHttpContext();

            basicHttpProcessor.addInterceptor(new ResponseDate());
            basicHttpProcessor.addInterceptor(new ResponseServer());
            basicHttpProcessor.addInterceptor(new ResponseContent());
            basicHttpProcessor.addInterceptor(new ResponseConnControl());

            httpService = new HttpService(basicHttpProcessor,
                    new DefaultConnectionReuseStrategy(),
                    new DefaultHttpResponseFactory());

            HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
            registry.register("/", new HomeCommandHandler());
            registry.register("/image", new ImageCommandHandler());
            httpService.setHandlerResolver(registry);
        }

        public synchronized void stopServer() {
            RUNNING = false;
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        class HomeCommandHandler implements HttpRequestHandler {

            @Override
            public void handle(HttpRequest request, HttpResponse response,
                               HttpContext httpContext) throws HttpException, IOException {

                HttpEntity httpEntity = new EntityTemplate(
                        new ContentProducer() {

                            public void writeTo(final OutputStream outstream)
                                    throws IOException {
                               // File file = new File(
                               //         Environment.getExternalStorageDirectory(),
                               //         "h.png");
                                String fileName = "h.png";
                                String completePath = Environment.getExternalStorageDirectory() + "/" + fileName;

                                File file = new File(completePath);
                                Uri imageUri = Uri.fromFile(file);

                                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                                        outstream, "UTF-8");
                                String response = "<html><head></head>" +
                                        "<body>" +
                                        "<h1>Hello<h1>" +
                                        "<img src='http://"+ getIpAddress()+":"+HttpServiceThread.HttpServerPORT +"/"+imageUri+"'>" +
                                        "</body></html>";
                                //+getIpAddress()+":"HttpServiceThread.HttpServerPORT +file+
                                outputStreamWriter.write(getRealPathFromURI(selectedImageUri));
                                outputStreamWriter.write(response);
                                outputStreamWriter.flush();
                            }
                        });
                response.setHeader("Content-Type", "text/html");
                response.setEntity(httpEntity);
            }

        }

        class ImageCommandHandler implements HttpRequestHandler {

            @Override
            public void handle(HttpRequest request, HttpResponse response,
                               HttpContext context) throws HttpException, IOException {

                File file = new File(
                        Environment.getExternalStorageDirectory(),
                        "h.png");

                FileEntity fileEntity = new FileEntity(file, "image/png");
                response.setHeader("Content-Type", "application/force-download");
                response.setHeader("Content-Disposition","attachment; filename=image.png");
                response.setHeader("Content-Type", "image/png");
                response.setEntity(fileEntity);
            }

        }

    }
}
