package caojx.learn.filecopy;

import java.io.File;

/**
 * 文件拷贝接口
 *
 * @author caojx created on 2020/6/15 12:34 下午
 */
public interface FileCopyRunner {

    /**
     * source文件拷贝到target文件中
     *
     * @param source
     * @param target
     */
    void copyFile(File source, File target);
}