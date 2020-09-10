package com.libaolu.nmon.utils;

import org.apache.jmeter.util.JMeterUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2020/4/29 14:41
 **/
public class LeeUtils {

    /**
     * 获取license
     * @return
     */
    public String getLicenseKey() {
        InputStream in = getClass().getClassLoader().getResourceAsStream("raw/libaolu.lic");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        String key = null;
        try {
            while ((line = reader.readLine()) != null) {
                key = line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return key;
    }

    /**
     * 打印banner
     */
    public String displayAsciiArt() {
        String wel = "";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("banner/banner.txt")) {
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                String key = "";
                try {
                    while (( line = reader.readLine()) != null) {
                        if (line.indexOf("___")>0){
                            key += System.lineSeparator()+line+System.lineSeparator();
                        } else if (line.indexOf("JMeter")>0){
                            key += line;
                        } else {
                            key += line+System.lineSeparator();
                        }
                    }
                    wel = key;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        reader.close();
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wel;
    }

    public void writeContent(String content,String name) {
        String dir = JMeterUtils.getJMeterBinDir()+File.separator+name;
        FileWriter fw = null;
        BufferedWriter out = null;
        try {
            fw = new FileWriter(dir,false);
            out = new BufferedWriter(fw);
            out.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                assert out != null;
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String readContent(String name) {
        String fileDir = JMeterUtils.getJMeterBinDir()+File.separator+name;
        File file = new File(fileDir);
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        BufferedReader reader = null;
        FileInputStream fs = null;
        try {
            fs = new FileInputStream(fileDir);
            reader = new BufferedReader(new InputStreamReader(fs, StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line;
        String key = null;
        try {
            while ((line = reader.readLine()) != null) {
                key = line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fs.close();
                reader.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return key;
    }

   /* public static void main(String[] args) {
        LeeUtils le = new LeeUtils();
        System.out.println(le.displayAsciiArt());
    }*/
}
