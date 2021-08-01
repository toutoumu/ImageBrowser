package com.library.media;

public interface IPicture {
    /**
     * 图片文件路径
     *
     * @return 图片文件路径 eg: /data/files/xxx.jpg
     */
    String getFilePath();

    /**
     * 获取文件的大小
     *
     * @return 文件大小
     */
    long getLength();
}
