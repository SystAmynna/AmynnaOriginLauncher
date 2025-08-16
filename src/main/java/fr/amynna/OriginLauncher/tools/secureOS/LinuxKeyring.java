package fr.amynna.OriginLauncher.tools.secureOS;

import fr.amynna.OriginLauncher.Proprieties;

import java.io.*;

public class LinuxKeyring {
    private static final String SERVICE = Proprieties.NAME + "MsAuth";

    protected static void store(String token) throws Exception {
        Process p = new ProcessBuilder("secret-tool", "store", "--label=MyLauncher", "service", SERVICE).start();
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()))) {
            w.write(token);
        }
        p.waitFor();
    }

    protected static String load() throws Exception {
        Process p = new ProcessBuilder("secret-tool", "lookup", "service", SERVICE).start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        return br.readLine();
    }

}
