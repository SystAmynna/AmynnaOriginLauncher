package tests;

import com.amynna.Tools.FileManager;
import org.junit.jupiter.api.Test;

import java.io.File;

public class testDownload {


    @Test
    public void testDownload() {

        File f = FileManager.downloadFile(
                "https://external-content.duckduckgo.com/iu/?u=https%3A%2F%2Ftse1.mm.bing.net%2Fth%2Fid%2FOIP.7cRYFyLoDEDh4sRtM73vvwHaDg%3Fpid%3DApi&f=1&ipt=cf8b18b36cfb5f2a505ce165bd836e51f9e5d70f937095502c4df84b4d4cbebf&ipo=images",
                "/home/SystAmynna/Téléchargements/test/" // peut être "downloads/" ou "downloads/image.png"
        );
        System.out.println("Fichier dispo : " + f.getAbsolutePath());

    }



}
