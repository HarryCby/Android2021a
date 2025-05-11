//package com.harry.a2021competition;
//
//import android.content.Context;
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.List;
//
//public class CsvReader {
//    // 读取 CSV 文件并返回 float[100][4] 类型的二维数组
//    public static float[][] readCsvFile(Context context, String fileName) {
//        InputStream inputStream = null;
//        BufferedReader bufferedReader = null;
//        ArrayList<float[]> resultList = new ArrayList<>();
//
//        try {
//            // 从 assets 文件夹中获取文件输入流
//            inputStream = context.getAssets().open(fileName);
//            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                // 使用逗号分隔每一行的值
//                String[] values = line.split(",");
//                float[] row = new float[values.length];
//                for (int i = 0; i < values.length; i++) {
//                    // 将每一行的字符串转换为 float 类型
//                    row[i] = Float.parseFloat(values[i].trim());
//                }
//                resultList.add(row);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            // 关闭流
//            if (bufferedReader != null) {
//                try {
//                    bufferedReader.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            if (inputStream != null) {
//                try {
//                    inputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        // 将 ArrayList 转换为 float[100][4] 数组
//        float[][] floatArray = new float[resultList.size()][4];
//        for (int i = 0; i < resultList.size(); i++) {
//            System.arraycopy(resultList.get(i), 0, floatArray[i], 0, Math.min(4, resultList.get(i).length));
//        }
//        return floatArray;
//    }
//}