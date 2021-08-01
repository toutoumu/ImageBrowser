package com.library.media;

import java.io.Serializable;
import java.util.Objects;

/**
 * 图片数据
 */
public class Picture implements Serializable, IPicture {
    private String albumName;//所属相册名称 Album
    private String fileName;//文件名,不包括路径 xxx.jpg
    private String filePath;//文件路径 /data/data/xxx.jpg
    private boolean isSelected = false;//是否选中
    private long length;//文件大小

    @Override
    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    /**
     * 所属相册名称 如:Album
     *
     * @return 所属相册名称
     */
    public String getAlbumName() {
        return albumName;
    }

    /**
     * 所属相册名称 data
     *
     * @param albumName 所属相册名称如:Album
     */
    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    /**
     * 文件名,不包括路径 xxx.jpg
     *
     * @return 文件名, 不包括路径 xxx.jpg
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 文件名,不包括路径 xxx.jpg
     *
     * @param fileName 文件名,不包括路径 xxx.jpg
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * 文件路径 /data/data/xxx.jpg
     *
     * @return 文件路径 /data/data/xxx.jpg
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 文件路径 /data/data/xxx.jpg
     *
     * @param filePath 文件路径 /data/data/xxx.jpg
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * 是否被选择
     *
     * @return 是否被选择
     */
    public boolean isSelected() {
        return isSelected;
    }

    /**
     * 设置是否被选择
     *
     * @param selected 是否被选择
     */
    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Picture picture = (Picture) o;

        return Objects.equals(fileName, picture.fileName);
    }

    @Override
    public int hashCode() {
        return fileName != null ? fileName.hashCode() : 0;
    }
}
