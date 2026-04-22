package cn.imhtb.live.common.config;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal in-memory file recorder to satisfy x-file-storage when no persistent
 * recorder is configured in local development.
 */
@Component
public class InMemoryFileRecorder implements FileRecorder {

    private final Map<String, FileInfo> fileInfoByUrl = new ConcurrentHashMap<>();
    private final Map<String, FilePartInfo> filePartById = new ConcurrentHashMap<>();

    @Override
    public boolean save(FileInfo fileInfo) {
        if (fileInfo == null || fileInfo.getUrl() == null) {
            return false;
        }
        fileInfoByUrl.put(fileInfo.getUrl(), fileInfo);
        return true;
    }

    @Override
    public void update(FileInfo fileInfo) {
        save(fileInfo);
    }

    @Override
    public FileInfo getByUrl(String url) {
        return fileInfoByUrl.get(url);
    }

    @Override
    public boolean delete(String url) {
        return fileInfoByUrl.remove(url) != null;
    }

    @Override
    public void saveFilePart(FilePartInfo filePartInfo) {
        if (filePartInfo == null || filePartInfo.getId() == null) {
            return;
        }
        filePartById.put(filePartInfo.getId(), filePartInfo);
    }

    @Override
    public void deleteFilePartByUploadId(String uploadId) {
        if (uploadId == null) {
            return;
        }
        filePartById.entrySet().removeIf(entry -> uploadId.equals(entry.getValue().getUploadId()));
    }
}
