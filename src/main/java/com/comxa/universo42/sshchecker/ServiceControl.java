package com.comxa.universo42.sshchecker;

import com.comxa.universo42.sshchecker.modelo.SSH;
import com.comxa.universo42.sshchecker.modelo.SSHchecker;

import java.util.List;

public interface ServiceControl {
    public SSHchecker getChecker();
    public void setChecker(List<SSH> sshs, int qtdThreads);
}
