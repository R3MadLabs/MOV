package com.example.mov;

import java.io.*;

public class VideoSegmentProcessor {

    public void video(String inputVideoPath, String tempDir,String combinedVideoPath ){
        int segmentDurationSeconds = 100; // Duration of each segment in seconds
        // Step 1: Split the video into segments
        splitVideo(inputVideoPath, tempDir, segmentDurationSeconds);
        // Step 2: Process and convert each segment
        processSegments(tempDir);
        // Step 3: Combine processed segments
        combineSegments(tempDir, combinedVideoPath);
        // Step 4: Clean up temporary files
        cleanUp(tempDir);
    }

    private static void splitVideo(String inputPath, String tempDir, int segmentDurationSeconds) {
        // FFmpeg command to split the video into segments
        String ffmpegSplitCommand = String.format("ffmpeg -i %s -c copy -map 0 -segment_time %d -f segment %ssegment_%%03d.mp4",
                inputPath, segmentDurationSeconds, tempDir);

        executeCommand(ffmpegSplitCommand);
    }

    private static void processSegments(String tempDir) {
        File[] segmentFiles = new File(tempDir).listFiles();
        if (segmentFiles != null) {
            for (File segmentFile : segmentFiles) {
                // FFmpeg command to process and convert each segment
                String ffmpegProcessCommand = String.format("ffmpeg -i %s -c:v libx264 -c:a aac %s_processed.mp4",
                        segmentFile.getAbsolutePath(), segmentFile.getAbsolutePath());

                executeCommand(ffmpegProcessCommand);
            }
        }
    }

    private static void combineSegments(String tempDir, String outputPath) {
        // List all processed segment files
        File[] processedSegmentFiles = new File(tempDir).listFiles((dir, name) -> name.endsWith("_processed.mp4"));

        // Create a text file listing the processed segments for FFmpeg concat demuxer
        String concatListFilePath = tempDir + "concat_list.txt";
        StringBuilder concatList = new StringBuilder();
        for (File processedSegmentFile : processedSegmentFiles) {
            concatList.append("file '").append(processedSegmentFile.getAbsolutePath()).append("'\n");
        }
        try {
            FileWriter concatListWriter = new FileWriter(concatListFilePath);
            concatListWriter.write(concatList.toString());
            concatListWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // FFmpeg command to combine processed segments
        String ffmpegCombineCommand = String.format("ffmpeg -f concat -safe 0 -i %s -c copy %s",
                concatListFilePath, outputPath);

        executeCommand(ffmpegCombineCommand);

        // Clean up the concat list file
        new File(concatListFilePath).delete();
    }

    private static void cleanUp(String tempDir) {
        File[] tempFiles = new File(tempDir).listFiles();
        if (tempFiles != null) {
            for (File tempFile : tempFiles) {
                tempFile.delete();
            }
        }
    }

    private static void executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

