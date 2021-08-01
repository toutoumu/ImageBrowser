package com.library.media;

import java.io.Serializable;

/**
 * 相册数据
 */
public class Album implements Serializable {
    /** 相册路径,包括相册名称 /data/data/albumName */
    private String path;
    /** 相册名称 */
    private String name;
    /** 封面照片 */
    private Picture cover;

    /**
     * 封面照片
     *
     * @return {@link Picture}
     */
    public Picture getCover() {
        return cover;
    }

    /**
     * 封面照片
     *
     * @param cover {@link Picture}
     */
    public void setCover(Picture cover) {
        this.cover = cover;
    }

    /**
     * 相册路径
     *
     * @return 相册路径 /data/data/albumName
     */
    public String getPath() {
        return path;
    }

    /**
     * 相册路径
     *
     * @param path 相册路径 /data/data/albumName
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 相册名称
     *
     * @return 相册名称 "albumName"
     */
    public String getName() {
        return name;
    }

    /**
     * 相册名称
     *
     * @param name 相册名称 "albumName"
     */
    public void setName(String name) {
        this.name = name;
    }
}
