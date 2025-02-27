package com.example.demo.config;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VideoSaver {
    private static final String FFMPEG_PATH = "C:\\ffmpeg-master-latest-win64-gpl-shared\\bin\\ffmpeg.exe"; 
    private static final String BASE_FOLDER_PATH = "C:/videos/";  

    public static void saveFramesToVideo(List<MultipartFile> frames, String cctvIdx, String eventType, String detectionTime) throws IOException {
        if (frames.isEmpty()) {
            System.out.println("❌ 저장할 프레임이 없습니다.");
            return;
        }

        // ✅ CCTV별 폴더 생성 (이벤트 타입 포함)
        String folderName = "CCTV_" + cctvIdx + "_" + eventType + "_" + detectionTime;
        File frameFolder = new File(BASE_FOLDER_PATH + folderName);
        if (!frameFolder.exists()) frameFolder.mkdirs();  

        // ✅ 프레임을 CCTV 폴더에 저장
        for (int i = 0; i < frames.size(); i++) {
            File frameFile = new File(frameFolder, String.format("frame_%04d.jpg", i));
            frames.get(i).transferTo(frameFile);
        }

        // ✅ 변환될 MP4 파일 경로
        String outputVideoPath = BASE_FOLDER_PATH + folderName + "/output.mp4";

        // ✅ FFmpeg 명령어 실행 (프레임 → MP4 변환)
        String command = String.format("%s -r 1 -framerate 1 -i %s/frame_%%04d.jpg -c:v libx264 -crf 23 -pix_fmt yuv420p %s",
                FFMPEG_PATH, frameFolder.getAbsolutePath(), outputVideoPath);

        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor(10, TimeUnit.SECONDS); 
            System.out.println("✅ MP4 영상 생성 완료: " + outputVideoPath);
        } catch (InterruptedException e) {
            System.err.println("❌ FFmpeg 실행 오류: " + e.getMessage());
        }
    }
}
