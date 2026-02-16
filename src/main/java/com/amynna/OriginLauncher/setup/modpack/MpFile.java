package com.amynna.OriginLauncher.setup.modpack;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.SignedFile;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class MpFile {

   protected final String path;
   protected final File file;
   private final List<String> dependencies = new LinkedList<>();

   private String url;
   private String hash;
   private long size;

   private String name;
   private String description;


   protected MpFile(String path) {
       this.path = path;
       this.file = new File(AppProperties.MINECRAFT_DIR + File.separator + path);
   }

   protected MpFile(String path, String url, String hash, long size) {
       this(path);
       this.url = url;
       this.hash = hash;
       this.size = size;
   }

   private boolean isSigned() {
       return url != null && hash != null && size > 0;
   }

   public String getName() {
       return name != null ? name : path;
   }

   public String getDescription() {
       return description != null ? description : "Aucune description disponible.";
   }

   protected List<String> getDependencies() {
       return dependencies;
   }

   protected void setMetaData(String name, String description) {
       this.name = name;
       this.description = description;
   }

   protected void addDependency(String dependency) {
       dependencies.add(dependency);
   }

    protected void download() {
       if (isSigned()) downloadSignedFile();
       else downloadUnsignedFile();
    }

    private void downloadSignedFile() {
        String onServerUrl = AppProperties.MODPACK_DIR_ON_SERVER + path;
        FileManager.downloadAndValidateFile(onServerUrl, this.file.getAbsolutePath());
    }

    private void downloadUnsignedFile() {
        FileManager.downloadFileAndVerifySha(url, file.getAbsolutePath(), hash, FileManager.SHA512);
    }

    protected boolean lightCheck() {
        return isSigned() ? checkSignedFile() : lightCheckUnsignedFile();
    }

    private boolean lightCheckUnsignedFile() {
       return size == file.length();
    }

    protected boolean check() {
       return isSigned() ? checkSignedFile() : checkUnsignedFile();
    }

    private boolean checkSignedFile() {
        File signature = FileManager.searchFileInDirectory(AppProperties.SIGNATURE_DIR, file.getName() + AppProperties.SIGNATURE_FILE_EXTENSION);
        if (signature == null || !signature.exists()) {
            signature = FileManager.downloadSignatureFile(AppProperties.MODPACK_DIR_ON_SERVER + path);
        }
        if (signature == null || !signature.exists()) {
            return false;
        }

        SignedFile signedFile = new SignedFile(file, signature);
        return signedFile.valid();
    }

    private boolean checkUnsignedFile() {
        return hash.equals(FileManager.calculSHA(file, FileManager.SHA512));
    }

    protected OptionalMpFile convertToOptional() {
        OptionalMpFile optionalMpFile = new OptionalMpFile(this.path, this.url, this.size, this.hash);
        optionalMpFile.setMetaData(this.name, this.description);
        return optionalMpFile;
    }


}
