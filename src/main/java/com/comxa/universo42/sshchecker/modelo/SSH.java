package com.comxa.universo42.sshchecker.modelo;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.HTTPProxyData;

import java.io.IOException;

public class SSH extends SSHbean implements Runnable {
	public static final int TIMEOUT_TCP_CONNECTION = 10000;
	public static final int TIMEOUT_SSH_CONNECTION = 15000;
	
	private String proxyHost;
	private int proxyPort;

	private boolean isStopped;
	private boolean isComplete;
	private boolean isOn;
	private Connection conexao;
	private Exception e;

	public SSH(String serial) {
		super(serial);
	}
	
	public SSH(String host, int port, String user, String pass) {
		super(host, port, user, pass);
	}
	
	public SSH(String host, int port, String user, String pass, String linha) {
		super(host, port, user, pass, linha);
	}
	
	public SSH(String host, int port, String user, String pass, String linha, String proxyHost, int proxyPort) {
		this(host, port, user, pass, linha);
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
	}

	
	public void check(boolean blocking) {
		if (blocking)
			run();
		else
			new Thread(this).start();
	}

	public void stopCheck() {
		isStopped = true;
		if (this.conexao != null)
			this.conexao.closeHard();
	}

	public boolean isStopped() {
		return isStopped;
	}

	public boolean isOn() throws IllegalStateException {
		if (!this.isComplete)
			throw new IllegalStateException("SSH not checked yet!");
		
		return this.isOn;
	}
	
	public boolean isComplete() {
		return this.isComplete;
	}
	
	public Exception getException() {
		return this.e;
	}
	
    @Override
    public void run() {
        this.conexao = new Connection(getHost(), getPort());
        
        if (proxyHost != null && proxyHost.length() > 0)
            conexao.setProxyData(new HTTPProxyData(this.proxyHost, this.proxyPort));
                
        try {
            conexao.connect(null, TIMEOUT_TCP_CONNECTION, TIMEOUT_SSH_CONNECTION);

            if (conexao.authenticateWithPassword(getUser(), getPass()))
            	isOn = true;
        } catch (IOException e) {
            this.e = e;
        } finally {
			conexao.close();
		}

        this.isComplete = true;
        onComplete();
	}
    
    public void onComplete() {}
}
