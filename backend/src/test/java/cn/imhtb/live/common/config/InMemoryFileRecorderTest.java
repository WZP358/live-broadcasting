package cn.imhtb.live.common.config;

import org.dromara.x.file.storage.core.FileInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryFileRecorderTest {

    @Test
    void savesUpdatesAndDeletesFileInfoByUrl() {
        InMemoryFileRecorder recorder = new InMemoryFileRecorder();
        FileInfo initialFile = new FileInfo().setUrl("https://cdn.example.com/live/a.png");
        FileInfo updatedFile = new FileInfo().setUrl("https://cdn.example.com/live/a.png");

        assertTrue(recorder.save(initialFile));
        assertSame(initialFile, recorder.getByUrl(initialFile.getUrl()));

        recorder.update(updatedFile);
        assertSame(updatedFile, recorder.getByUrl(updatedFile.getUrl()));

        assertTrue(recorder.delete(updatedFile.getUrl()));
        assertNull(recorder.getByUrl(updatedFile.getUrl()));
        assertFalse(recorder.delete(updatedFile.getUrl()));
    }

    @Test
    void rejectsMissingFileInfoOrUrl() {
        InMemoryFileRecorder recorder = new InMemoryFileRecorder();

        assertFalse(recorder.save(null));
        assertFalse(recorder.save(new FileInfo()));
    }
}
