package cn.imhtb.live.service.impl;

import cn.imhtb.live.common.utils.MinioUtil;
import cn.imhtb.live.service.IFileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author PinTeh
 * @date 2020/4/15
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements IFileUploadService {

    private static final String LOCAL_UPLOAD_DIR = "uploads";

    @Override
    public String uploadFileToMinio(InputStream inputStream, String newFilename) {
        byte[] bytes = readAllBytes(inputStream);
        try {
            return MinioUtil.uploadObjectWithInputStream(newFilename, new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            log.warn("minio upload unavailable, save file locally instead, file={}", newFilename, e);
            return saveLocal(bytes, newFilename);
        }
    }

    @Override
    public String uploadFileToMinioStrict(InputStream inputStream, String newFilename, long size, String contentType) {
        try {
            return MinioUtil.uploadObjectWithInputStream(newFilename, inputStream, size, contentType);
        } catch (Exception e) {
            log.error("minio upload failed, file={}", newFilename, e);
            throw new IllegalStateException("MinIO 上传失败", e);
        }
    }

    private byte[] readAllBytes(InputStream inputStream) {
        try {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("读取上传文件失败", e);
        }
    }

    private String saveLocal(byte[] bytes, String newFilename) {
        try {
            Path uploadDir = Paths.get(System.getProperty("user.dir"), LOCAL_UPLOAD_DIR);
            Files.createDirectories(uploadDir);
            Files.write(uploadDir.resolve(newFilename), bytes);
            return "/" + LOCAL_UPLOAD_DIR + "/" + newFilename;
        } catch (IOException e) {
            throw new IllegalStateException("保存上传文件失败", e);
        }
    }

}
