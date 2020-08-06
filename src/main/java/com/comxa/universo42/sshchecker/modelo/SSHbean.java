package com.comxa.universo42.sshchecker.modelo;

public class SSHbean {
	public static final int DEFAULT_PORT = 22;
	
	private String linha;
	
	private String host;
	private int port;
	private String user;
	private String pass;

	public SSHbean(String serial) {
		deserializar(serial);
	}
	
	public SSHbean(String host, int port, String user, String pass) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.pass = pass;
	}
	
	public SSHbean(String host, int port, String user, String pass, String linha) {
		this(host, port, user, pass);
		this.linha = linha;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}
	
	public String getLinha() {
		return this.linha;
	}
	
	public void setLinha(String linha) {
		this.linha = linha;
	}


	public String serializar() {
		StringBuilder builder = new StringBuilder();

		builder.append(this.host + "|");
		builder.append(this.port + "|");
		builder.append(this.user + "|");
		builder.append(this.pass + "|");

		if (this.linha != null)
			builder.append(this.linha);

		builder.append("|");

		return builder.toString();
	}

	public void deserializar(String str) {
		int aux = str.indexOf('|');

		this.host = str.substring(0, aux);
		aux += 1;

		int aux2 = str.indexOf(aux, '|');

		this.port = Integer.parseInt(str.substring(aux, aux2));
		aux2 += 1;

		aux = str.indexOf(aux2, '|');

		this.user = str.substring(aux2, aux);
		aux += 1;

		aux2 = str.indexOf(aux, '|');

		this.pass = str.substring(aux, aux2);
		aux2 += 1;

		aux = str.indexOf(aux2, '|');

		if (aux != aux2)
			this.linha = str.substring(aux2, aux);
	}

	@Override
	public String toString() {
		return getLinha() != null ? getLinha() : String.format("%s:%d|%s|%s", host, port, user, pass);
	}
}
