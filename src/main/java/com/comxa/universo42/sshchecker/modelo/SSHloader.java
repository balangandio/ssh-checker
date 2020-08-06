package com.comxa.universo42.sshchecker.modelo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SSHloader {
	public static final String DEFAULT_DELIMITER = "|";
	
	private File file;
	private String data;
	private String delimiter = DEFAULT_DELIMITER;
	
	private List<SSH> sshs;
	
	public SSHloader(File filePath) {
		this.file = filePath;
		
		if (file.isDirectory())
			throw new IllegalArgumentException("The file is a directory!");		
	}
	
	public SSHloader(String data) {
		this.data = data;
	}
	
	public List<SSH> getSSHs() {
		return this.sshs;
	}
	
	public void setSshs(List<SSH> sshs) {
		this.sshs = sshs;
	}
	
	public String getDelimiter() {
		return this.delimiter;
	}
	
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}


	public void load() throws IOException {
		this.sshs = new ArrayList<SSH>();
		Scanner scanner = null;
		
		try {
			if (this.file != null)
				scanner = new Scanner(this.file);
			else
				scanner = new Scanner(this.data);
			
			while (scanner.hasNextLine()) {
			
				String linha = scanner.nextLine();
				
				if (linha.length() == 0)
					continue;

				int aux = linha.indexOf(delimiter);
				
				if (aux == -1)
					continue;
				
				String host = linha.substring(0, aux);
				int port = SSHbean.DEFAULT_PORT;
				
				if (host.contains(":")) {
					String hostPort[] = host.split(":");
					
					if (hostPort != null && hostPort.length == 2) {
						host = hostPort[0];
						try {
							port = Integer.valueOf(hostPort[1]);
						} catch(NumberFormatException e){}
					}
				}
				
				aux++;
				
				int aux2 = linha.indexOf(delimiter, aux);
				
				if (aux2 == -1)
					continue;
				
				String user = linha.substring(aux, aux2);
				
				aux2 = linha.indexOf(delimiter, aux);
				
				if (aux2 == -1)
					continue;
				
				aux = aux2 + 1;
				
				aux2 = linha.indexOf(delimiter, aux);
				
				String pass = (aux2 != -1) ? linha.substring(aux, aux2) : linha.substring(aux);
				
				this.sshs.add(new SSH(host.trim(), port, user.trim(), pass.trim(), linha));
			}
		} finally {
			if (scanner != null)
				scanner.close();
		}
	}

	public void loadFixed(String user, String pass, int port) throws IOException {
		this.sshs = new ArrayList<SSH>();

		if (data == null && file != null)
			data = getFileStr(file);
		else if (data == null)
			return;

		Pattern ipPattern = Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");
		Matcher matcher = ipPattern.matcher(data);

		while (matcher.find()) {
			String ip = data.substring(matcher.start(), matcher.end());
			int p = findPort(ip, data, 100);

			if (p == -1)
				p = port;

			this.sshs.add(new SSH(ip, p, user, pass));
		}
	}
	
	public void save() throws IOException {
		PrintWriter pw = null; 
		
		try {
			pw = new PrintWriter(this.file);
			
			for (SSH ssh : this.sshs) { 
				if (ssh.getLinha() != null)
					pw.println(ssh.getLinha());
				else
					pw.println(String.format("%s:%d|%s|%s", ssh.getHost(), ssh.getPort(), ssh.getUser(), ssh.getPass()));
			}
		} finally {
			if (pw != null)
				pw.close();
		}
	}

	private int findPort(String ip, String data, int maxRange) {
		int aux = data.indexOf(ip);

		if (aux == -1)
			return -1;

		aux += ip.length();

		if (aux >= data.length()-1)
			return -1;

		if (data.charAt(aux) == ':') {
			aux++;
			int i;
			for (i = aux; i < data.length() && i-aux < 5 && Character.isDigit(data.charAt(i)); i++);

			if (i != aux)
				return Integer.parseInt(data.substring(aux, i));
		}

		return -1;
	}

	private String getFileStr(File file) throws IOException {
		StringBuilder builder = new StringBuilder();
		FileInputStream fileIn = null;
		try {
			fileIn = new FileInputStream(file);

			byte[] buffer = new byte[1024 * 32];
			int len;
			while ((len = fileIn.read(buffer)) != -1)
				builder.append(new String(buffer, 0, len));

		} finally {
			if (fileIn != null)
				fileIn.close();
		}

		return builder.toString();
	}
}
