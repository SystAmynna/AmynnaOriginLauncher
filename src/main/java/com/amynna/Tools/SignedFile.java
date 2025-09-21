package com.amynna.Tools;

import java.io.File;

public class SignedFile {

    public final File file;
    public final File signature;

    public SignedFile(File file, File signature) {
        this.file = file;
        this.signature = signature;
    }

}
