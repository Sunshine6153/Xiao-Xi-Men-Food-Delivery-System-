package com.sky.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.UUID;

public class FileUploadUtils {

    public static String upload(MultipartFile file, String basePath) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID() + extension;
        file.transferTo(new File(basePath, newFileName));
        return newFileName;
    }

    public static byte[] download(String filePath) throws IOException {
        File file = new File(filePath);
        InputStream in = new FileInputStream(file);
        byte[] bytes = in.readAllBytes();
        in.close();
        return bytes;
    }
}
