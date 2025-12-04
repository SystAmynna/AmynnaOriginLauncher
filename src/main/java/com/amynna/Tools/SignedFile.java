package com.amynna.Tools;

import java.io.File;

public record SignedFile(File file, File signature) {

    public boolean exists() {
        return file.exists() && signature.exists();
    }

    public boolean valid() {
        return exists() && KeyUtil.validateSignature(this);
    }

    public void delete() {
        FileManager.deleteFileIfExists(file);
        FileManager.deleteFileIfExists(signature);
    }

}
