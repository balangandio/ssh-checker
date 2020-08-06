package com.comxa.universo42.sshchecker.modelo;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SSHchecker implements Runnable {
	public static final int MILLISEGUNDOS_SLEEP = 1000;
	
	private List<SSH> sshs;
	private int qtdThreads;
	private int qtdThreadsDone;
	private int qtdChecked;

	private boolean isRunning;
	private ThreadChecker[] threads;
	private Iterator<SSH> sshIterator;
	
	private List<SSH> sshOns = Collections.synchronizedList(new LinkedList<SSH>());
	private List<SSH> sshOffs = Collections.synchronizedList(new LinkedList<SSH>());
	private List<SSH> sshError = Collections.synchronizedList(new LinkedList<SSH>());
	
	public SSHchecker(List<SSH> sshs, int qtdThreads) {
		this.sshs = sshs;
		this.qtdThreads = qtdThreads;
	}
	
	public void check(boolean blocking) {
		isRunning = true;
		sshIterator = this.sshs.iterator();
		threads = new ThreadChecker[this.qtdThreads];
		
		for (int i = 1; i <= threads.length; i++) {
			threads[i-1] = new ThreadChecker();
			threads[i-1].run(false);
		}

		if (blocking)
			run();
		else
			new Thread(this).start();
	}

	@Override
	public void run() {
		while (isRunning && !isComplete()) {
			try {
				Thread.sleep(MILLISEGUNDOS_SLEEP);
			} catch (InterruptedException e) {
				onLog("SSH checker: interrupted exception!");
			}
			onLog(String.format("Checked: %d/%d - Threads: %d/%d", qtdChecked, sshs.size(), qtdThreadsDone, qtdThreads));
		}

		this.isRunning = false;
		if (isComplete())
			onComplete();
	}
	
	public void stop() {
		this.isRunning = false;

		for (ThreadChecker t : threads)
			if (t.ssh != null)
				t.ssh.stopCheck();
	}

	public boolean isRunning() {
		return this.isRunning;
	}
	
	public boolean isComplete() {
		return (qtdThreadsDone == qtdThreads);
	}

	public List<SSH> getSshs() {
		return this.sshs;
	}

	public List<SSH> getSshOns() {
		return this.sshOns;
	}
	
	public List<SSH> getSshOffs() {
		return this.sshOffs;
	}
	
	public List<SSH> getSshError() {
		return this.sshError;
	}
	
	public void onLog(String log) {}

	public void onComplete() {}
	
	private synchronized SSH getSsh() {
		if (!isRunning)
			return null;

		if (!this.sshIterator.hasNext())
			return null;
		return this.sshIterator.next();
	}

	private class ThreadChecker implements Runnable {
		private SSH ssh;

		public void run(boolean blocking) {
			if (blocking)
				run();
			else
				new Thread(this).start();
		}

		@Override
		public void run() {
			while ((ssh = getSsh()) != null) {
				ssh.check(true);

				if (!ssh.isStopped()) {
					if (ssh.getException() != null) {
						sshError.add(ssh);
					} else if (ssh.isOn()) {
						sshOns.add(ssh);
					} else {
						sshOffs.add(ssh);
					}

					qtdChecked++;
				}
			}
			
			qtdThreadsDone++;
		}
	}
}
