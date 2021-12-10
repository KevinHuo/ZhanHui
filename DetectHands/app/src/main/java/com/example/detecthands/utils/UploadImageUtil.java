package com.example.detecthands.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class UploadImageUtil {

    public static String uploadImage(String imagePath) {
        String url = "https://niucube-api.qiniu.com/v1/upload";
        Map<String, String> textMap = new HashMap<>();
        //设置file的name，路径
        Map<String, String> fileMap = new HashMap<>();
        fileMap.put("file", imagePath);
        String contentType = "image/jpeg";
        String result = formUpload(url, textMap, fileMap, contentType);
        return result;
    }

    private static String formUpload(String urlStr, Map<String, String> textMap,
                                    Map<String, String> fileMap, String contentType) {
        String res = "";
        HttpURLConnection conn = null;
        // boundary 就是 request 头和上传文件内容的分隔符
        String BOUNDARY = "---------------------------123821742118716";
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            OutputStream out = new DataOutputStream(conn.getOutputStream());
            // text
            if (textMap != null) {
                StringBuilder strBuf = new StringBuilder();
                for (Map.Entry<String, String> stringStringEntry : textMap.entrySet()) {
                    String inputName = (String) ((Map.Entry) stringStringEntry).getKey();
                    String inputValue = (String) ((Map.Entry) stringStringEntry).getValue();
                    if (inputValue == null) {
                        continue;
                    }
                    strBuf.append("\r\n").append("--").append(BOUNDARY).append("\r\n");
                    strBuf.append("Content-Disposition: form-data; name=\"").append(inputName).append("\"\r\n\r\n");
                    strBuf.append(inputValue);
                }
                out.write(strBuf.toString().getBytes());
            }
            // file
            if (fileMap != null) {
                for (Map.Entry<String, String> stringStringEntry : fileMap.entrySet()) {
                    String inputName = (String) ((Map.Entry) stringStringEntry).getKey();
                    String inputValue = (String) ((Map.Entry) stringStringEntry).getValue();
                    if (inputValue == null) {
                        continue;
                    }
                    File file = new File(inputValue);
                    String filename = file.getName();
                    String strBuf = "\r\n" + "--" + BOUNDARY + "\r\n" +
                            "Content-Disposition: form-data; name=\"" + inputName + "\"; filename=\"" + filename + "\"\r\n" +
                            "Content-Type:" + contentType + "\r\n\r\n";
                    out.write(strBuf.getBytes());
                    DataInputStream in = new DataInputStream(new FileInputStream(file));
                    int bytes = 0;
                    byte[] bufferOut = new byte[1024];
                    while ((bytes = in.read(bufferOut)) != -1) {
                        out.write(bufferOut, 0, bytes);
                    }
                    in.close();
                }
            }
            byte[] endData = ("\r\n--" + BOUNDARY + "--\r\n").getBytes();
            out.write(endData);
            out.flush();
            out.close();
            // 读取返回数据
            StringBuffer strBuf = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                strBuf.append(line).append("\n");
            }
            res = strBuf.toString();
            reader.close();
        } catch (Exception e) {
            System.out.println("发送POST请求出错。" + urlStr);
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return res;
    }
}
