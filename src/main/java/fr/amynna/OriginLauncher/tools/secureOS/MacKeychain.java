package fr.amynna.OriginLauncher.tools.secureOS;

import fr.amynna.OriginLauncher.Proprieties;

import java.io.*;

public class MacKeychain {
    private static final String SERVICE = Proprieties.NAME + "MsAuth";
    private static final String ACCOUNT = "refreshToken";

    protected static void store(String token) throws Exception {
        new ProcessBuilder("security", "add-generic-password",
                "-a", ACCOUNT, "-s", SERVICE, "-w", token, "-U").start().waitFor();
    }

    protected static String load() throws Exception {
        Process p = new ProcessBuilder("security", "find-generic-password",
                "-a", ACCOUNT, "-s", SERVICE, "-w").start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        return br.readLine();
    }

}
