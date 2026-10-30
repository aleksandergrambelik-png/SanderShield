package com.sander.shield.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class HexViewer {
    public interface HexCallback {
        void onResult(String hexOutput);
        void onError(String error);
    }

    public void readFile(final String filePath, final HexCallback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File file = new File(filePath);
                    if (!file.exists()) {
                        cb.onError("File not found: " + filePath);
                        return;
                    }

                    // Baca maksimal 1KB aja biar gak berat di terminal
                    int maxBytes = 1024; 
                    byte[] buffer = new byte[maxBytes];
                    InputStream is = new FileInputStream(file);
                    int bytesRead = is.read(buffer);
                    is.close();

                    if (bytesRead <= 0) {
                        cb.onError("File is empty.");
                        return;
                    }

                    StringBuilder hex = new StringBuilder();
                    hex.append("{\"event\":\"hex_dump\",\"file\":\"").append(filePath).append("\",\"data\":\"\\n");

                    for (int i = 0; i < bytesRead; i++) {
                        // Format Hex (2 digit)
                        hex.append(String.format("%02X ", buffer[i]));
                        
                        // Bikin baris baru tiap 16 byte (standar hex editor)
                        if ((i + 1) % 16 == 0) {
                            hex.append("\\n");
                        }
                    }
                    
                    hex.append("\"}\n");
                    cb.onResult(hex.toString());

                } catch (Exception e) {
                    cb.onError(e.getMessage());
                }
            }
        }).start();
    }
}
