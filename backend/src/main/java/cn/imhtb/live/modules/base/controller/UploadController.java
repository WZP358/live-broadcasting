package cn.imhtb.live.modules.base.controller;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.IdUtil;
import cn.imhtb.live.common.ApiResponse;
import cn.imhtb.live.common.exception.BusinessException;
import cn.imhtb.live.modules.user.service.IUserService;
import cn.imhtb.live.service.IFileUploadService;
import cn.imhtb.live.service.IRoomService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * 资源上传接口。
 */
@Api(tags = "资源上传接口")
@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UploadController {

    private static final long IMAGE_TARGET_SIZE = 2 * 1024 * 1024L;
    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    private final IUserService userService;
    private final IRoomService roomService;
    private final FileStorageService fileStorageService;
    private final IFileUploadService fileUploadService;

    @ApiOperation("上传文件")
    @PostMapping("/file")
    public ApiResponse<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = uploadImage(file, "图片");
        return ApiResponse.ofSuccess(fileUrl);
    }

    @ApiOperation("上传头像")
    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = uploadImage(file, "头像");
        userService.updateAvatar(avatarUrl);
        return ApiResponse.ofSuccess(avatarUrl);
    }

    @ApiOperation("上传直播间封面")
    @PostMapping("/room/cover")
    public ApiResponse<String> uploadRoomCover(@RequestParam("file") MultipartFile file) {
        String coverUrl = uploadImage(file, "直播间封面");
        roomService.updateCover(coverUrl);
        return ApiResponse.ofSuccess(coverUrl);
    }

    public ApiResponse<String> compressAndUploadFile(MultipartFile file) {
        validateImage(file, "图片");
        FileInfo fileInfo = fileStorageService.of(file)
                .image(img -> img.size(1000, 1000))
                .thumbnail(th -> th.size(200, 200))
                .upload();

        String url = fileInfo.getUrl();
        return ApiResponse.ofSuccess(url);
    }

    private String uploadImage(MultipartFile file, String sceneName) {
        validateImage(file, sceneName);
        String newFilename = buildFilename(file.getOriginalFilename());
        byte[] uploadBytes = readImageBytes(file, sceneName);

        try {
            return fileUploadService.uploadFileToMinio(new ByteArrayInputStream(uploadBytes), newFilename);
        } catch (RuntimeException e) {
            log.error("{}保存失败, filename={}", sceneName, newFilename, e);
            throw new BusinessException(sceneName + "保存失败，请检查 MinIO 或本地 uploads 目录权限");
        }
    }

    private byte[] readImageBytes(MultipartFile file, String sceneName) {
        try {
            if (file.getSize() <= IMAGE_TARGET_SIZE) {
                return file.getBytes();
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImgUtil.scale(file.getInputStream(), outputStream, calculateScaleFactor(file.getSize(), IMAGE_TARGET_SIZE));
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.warn("{}压缩失败，将保留原图上传, filename={}", sceneName, file.getOriginalFilename(), e);
            try {
                return file.getBytes();
            } catch (IOException ioException) {
                throw new BusinessException(sceneName + "读取失败，请重新选择图片", ioException);
            }
        }
    }

    private void validateImage(MultipartFile file, String sceneName) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的" + sceneName);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = normalizeExtension(originalFilename);
        String contentType = StringUtils.defaultString(file.getContentType()).toLowerCase(Locale.ROOT);
        boolean extensionSupported = SUPPORTED_IMAGE_EXTENSIONS.contains(extension);
        boolean typeSupported = SUPPORTED_IMAGE_TYPES.contains(contentType);
        if (!extensionSupported || (!StringUtils.isBlank(contentType) && !typeSupported)) {
            throw new BusinessException(sceneName + "仅支持 JPG、PNG 或 WEBP 图片");
        }
    }

    private String buildFilename(String originalFilename) {
        String extension = normalizeExtension(originalFilename);
        long now = System.currentTimeMillis() / 1000;
        return String.format("%s_%s.%s", IdUtil.simpleUUID(), now, extension);
    }

    private String normalizeExtension(String originalFilename) {
        String extension = FileNameUtils.getExtension(originalFilename);
        if (StringUtils.isBlank(extension)) {
            return "";
        }
        extension = extension.toLowerCase(Locale.ROOT);
        return "jpeg".equals(extension) ? "jpg" : extension;
    }

    public static float calculateScaleFactor(long originalSize, long targetSize) {
        if (targetSize <= 0 || originalSize <= 0) {
            throw new IllegalArgumentException("文件大小必须为正数");
        }
        if (targetSize >= originalSize) {
            return 1.0f;
        }
        double scaleFactor = Math.sqrt((double) targetSize / originalSize);
        return (float) Math.max(0.1, scaleFactor);
    }

}
