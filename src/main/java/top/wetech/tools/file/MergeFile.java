package top.wetech.tools.file;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * Created by Chen on 2018/03/21.
 * 合并两个结构相同的文件
 */
public class MergeFile {
    private static final int BUFSIZE = 1024 * 8;
    private static Logger logger = Logger.getLogger(MergeFile.class);

    /**
     * Description: 合并files到outFile中
     * Param: [outFile, files]
     * return: void
     * Author: CHEN ZUOLI
     * Date: 2018/3/21
     * Time: 10:53
     */
    private static void mergeFiles(String outFile, ArrayList<String> files) {
        FileChannel outChannel = null;
        logger.info("Merge " + files + " into " + outFile);
        try {
            outChannel = new FileOutputStream(outFile).getChannel();
            for (String f : files) {
                FileChannel fc = new FileInputStream(f).getChannel();
                ByteBuffer bb = ByteBuffer.allocate(BUFSIZE);
                while (fc.read(bb) != -1) {
                    bb.flip();
                    outChannel.write(bb);
                    bb.clear();
                }
                fc.close();
            }
            logger.info("Merged!! ");
        } catch (Exception e) {
            logger.error("merge files " + files + " exception!", e);
        } finally {
            try {
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (IOException ignore) {
                ignore.printStackTrace();
            }
        }
    }

    /**
     * Description: 合并文件夹下所有part开头的文件
     * Param: [fileDir]
     * return: void
     * Author: CHEN ZUOLI
     * Date: 2018/3/21
     * Time: 11:12
     */
    public static void mergeDirFiles(String fileDir) {
        File file = new File(fileDir);
        if (!file.exists()) {
            logger.error("传入的文件夹路径不存在：" + fileDir);
            return;
        }
        if (!file.isDirectory()) {
            logger.error("传入的非文件夹路径：" + fileDir);
            return;
        }
        File[] files = file.listFiles();
        ArrayList<String> pathList = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().indexOf("part") == 0) {
                String path = files[i].getAbsolutePath();
                pathList.add(path);
            }
        }
        mergeFiles(fileDir + File.separator + "merged", pathList);
    }

}
