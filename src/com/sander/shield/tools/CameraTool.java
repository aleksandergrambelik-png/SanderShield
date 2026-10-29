package com.sander.shield.tools;

import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;

public class CameraTool {
    public static Intent getCameraIntent() {
        return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    }
}
