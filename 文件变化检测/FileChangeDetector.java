package com.onion.file;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author lc
 * @date 2020-02-21 09:33
 */
public class FileChangeDetector {

    public static final String LOG_FILE = ".LOG";//记录文件

    public static void main(String[] args) {
        try {
            String currentDir = new File(".").getCanonicalPath();
            System.out.println("[" + currentDir + "] 请输入    记录: 1    检测: 2");
            Scanner scanner = new Scanner(System.in);
            int queryNum = scanner.nextInt();
            if (1 == queryNum) {
                record(currentDir);
            } else if (2 == queryNum) {
                detect(currentDir);
            }
            System.out.println("执行成功");
        } catch (Exception e) {
            System.out.println("执行失败: " + e.getMessage());
        } finally {
            System.out.println("bye~");
        }
    }

    /**
     * 在directory目录下记录.LOG文件
     * @param directory
     */
    public static void record(String directory) {
        try(
                FileWriter fileWriter = new FileWriter(directory + File.separator + LOG_FILE);
                PrintWriter writer = new PrintWriter(fileWriter))
        {
            traverse(new File(directory), writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 在directory目录下根据.LOG文件对比当前目录下文件情况
     * @param directory
     */
    public static void detect(String directory) {
        try{
            String logFile = directory + File.separator + LOG_FILE;
            //旧的
            Map<String, String> previous = processLog(logFile);

            //当前的
            File temp = File.createTempFile(LOG_FILE, ".temp");
            temp.deleteOnExit();
            try(
                    FileWriter fileWriter = new FileWriter(temp);
                    PrintWriter writer = new PrintWriter(fileWriter))
            {
                traverse(new File(directory), writer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Map<String, String> current = processLog(temp.getAbsolutePath());

            //比较
            Set<String> delete = new TreeSet<>();
            Set<String> add = new TreeSet<>();
            Set<String> update = new TreeSet<>();
            if(0 == previous.size())
                current.keySet().forEach(add::add);
            if(0 == current.size())
                previous.keySet().forEach(delete::add);
            if(0!=previous.size() && 0!=current.size()) {
                Set<String> keys = previous.keySet();
                for (String k: keys) {
                    String v = previous.get(k);
                    String v2 = current.get(k);
                    if (null == v2)
                        delete.add(k);
                    if (null != v2 && !v.equals(v2))
                        update.add(k);
                    current.remove(k);
                }
                for (String k: current.keySet())
                    add.add(k);
            }
            System.out.println("****修改****");
            update.forEach(System.out::println);
            System.out.println("****新增****");
            add.forEach(System.out::println);
            System.out.println("****删除****");
            delete.forEach(System.out::println);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    //遍历
    public static void traverse(File startDir, PrintWriter writer) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA);
        File[] files = startDir.listFiles();
        List<File> directoryList = new ArrayList<>();
        writer.println("[" + startDir.getAbsolutePath() + "]");
        for(File f: files){
            if(f.isDirectory()) {
                directoryList.add(f);
            }else {
                long lastModified = f.lastModified();
                Date date = new Date(lastModified);
                LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                String fileMessage = f.getName() + "=" + localDateTime.format(formatter);
                writer.println(fileMessage);
            }
        }

        for(int i = 0; i < directoryList.size(); i++)
            traverse(directoryList.get(i), writer);
    }

    /**
     * 解析logFile文件，返回一个文件名映射时间的Map
     * @param logFile
     * @return
     * @throws FileNotFoundException
     */
    public static Map<String, String> processLog(String logFile) throws FileNotFoundException {
        Map<String, String> all = new HashMap<>();//所有的文件名映射时间
        File file = new File(logFile);
        Scanner scanner = new Scanner(file);
        String directoryName = null;
        while(scanner.hasNextLine()){
            String lineMessage = scanner.nextLine();
            if(lineMessage.startsWith("[") && lineMessage.endsWith("]")) {
                directoryName = lineMessage;
                continue;
            }
            String[] split = lineMessage.split("=");
            if(LOG_FILE.equals(split[0]))
                continue;
            all.put(directoryName+split[0], split[1]);
        }
        return all;
    }

}
