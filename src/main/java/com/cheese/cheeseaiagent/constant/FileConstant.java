package com.cheese.cheeseaiagent.constant;

/**
 * 文件常量
 * <p>
 * 集中管理项目中与文件路径相关的常量，便于统一修改与维护。
 */
public interface FileConstant {

    /**
     * 文件保存目录
     * <p>
     * 使用 user.dir（项目运行目录）下的 /tmp 作为通用文件保存目录，
     * 工具类生成/下载的文件默认输出到此目录。
     */
    String FILE_SAVE_DIR = System.getProperty("user.dir") + "/tmp";
}
