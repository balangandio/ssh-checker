package com.comxa.universo42.sshchecker;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class ConfigLoader {
    public static final int DEFAULT_QTD_THREADS = 10;

    private String file;
    private int qtdThreads = DEFAULT_QTD_THREADS;

    public ConfigLoader(String file) {
        this.file = file;
    }

    public int getQtdThreads() {
        return qtdThreads;
    }

    public void setQtdThreads(int qtdThreads) {
        this.qtdThreads = qtdThreads;
    }

    public void save() throws IOException {
        PrintWriter pw = new PrintWriter(new File(this.file));

        pw.println(this.qtdThreads);

        pw.close();
    }

    public void load() throws IOException {
        File f = new File(this.file);

        if (f.exists()) {
            Scanner scanner = new Scanner(f);

            if (scanner.hasNextLine())
                this.qtdThreads = Integer.parseInt(scanner.nextLine());

            scanner.close();
        }
    }
}
