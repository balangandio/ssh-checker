package com.comxa.universo42.sshchecker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.comxa.universo42.sshchecker.modelo.*;
import com.comxa.universo42.sshchecker.view.FileExplorerActivity;
import com.comxa.universo42.sshchecker.view.SshListActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements ServiceConnection {
    public static final int FILE_EXPLORER_REQUEST_CODE = 1;
    public static final int DEFAULT_QTD_THREADS = 10;
    public static final String FILES_DIR = "SSHchecker";
    public static final String FILE_ON = "sshOns.txt";
    public static final String FILE_OFF = "sshOffs.txt";
    public static final String FILE_ERROR = "sshError.txt";
    public static final String FILE_CONFIG = "config.conf";

    private Button btnCheck;
    private Button btnSshs;
    private Button btnArq;
    private Button btnColar;
    private Button btnDelimitador;
    private Button btnQtdThreads;
    private Button btnFixedParams;

    private ConfigLoader config;
    private String delimitador = SSHloader.DEFAULT_DELIMITER;
    private int qtdThreads = DEFAULT_QTD_THREADS;
    private File fileSelecionado;
    private List<SSH> sshs = new ArrayList<SSH>();
    private SSHloader loader;
    private String fixedUser;
    private String fixedPass;
    private int fixedPort;

    private boolean needUnbind;

    private ServiceControl serviceControl;
    private SSHchecker checker;
    private ThreadChecker threadChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCheck = (Button) findViewById(R.id.btnCheck);
        btnCheck.setEnabled(false);
        btnCheck.setBackgroundResource(R.drawable.btn_checked);
        btnSshs = (Button) findViewById(R.id.btnSshs);
        btnSshs.setEnabled(false);
        btnArq = (Button) findViewById(R.id.btnArq);
        btnColar = (Button) findViewById(R.id.btnColar);
        btnFixedParams = (Button) findViewById(R.id.btnFixedParams);
        btnDelimitador = (Button) findViewById(R.id.btnDelimitador);
        refreshBtnDelimitador();
        btnQtdThreads = (Button) findViewById(R.id.btnThreads);
        refreshBtnThreads();

        config = new ConfigLoader(getFilesDir().getAbsolutePath() + "/" + FILE_CONFIG);
        loadConfig();
        refreshBtnThreads();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveConfig();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, CheckerService.class), this, 0);
        needUnbind = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService();
        stopThreadChecker();
    }

    public void onClickBtnArq(View view) {
        Intent intent = new Intent(this, FileExplorerActivity.class);

        startActivityForResult(intent, FILE_EXPLORER_REQUEST_CODE);
    }

    public void onClickBtnColar(View view) {
        String data = getClipBoardStr();

        if (data == null)
            return;

        loader = new SSHloader(data);
        loadSshs();
    }

    public void onClickBtnFixedParams(View view) {
        showFixedParamsInput();
    }

    public void onClickBtnDelimitador(View view) {
        showDelimitadorInput();
    }

    public void onClickBtnThreads(View view) {
        showThreadsInput();
    }

    public void onClickBtnSshs(View view) {
        Intent intent = new Intent(this, SshListActivity.class);

        if (checker == null) {
            intent.putExtra(SshListActivity.INTENT_SSHS, serializarSshs(sshs));
        } else {
            if (checker.getSshOns().size() > 0)
                intent.putExtra(SshListActivity.INTENT_SSH_ONS, serializarSshs(checker.getSshOns()));
            if (checker.getSshOffs().size() > 0)
                intent.putExtra(SshListActivity.INTENT_SSH_OFFS, serializarSshs(checker.getSshOffs()));
            if (checker.getSshError().size() > 0)
                intent.putExtra(SshListActivity.INTENT_SSH_ERROR, serializarSshs(checker.getSshError()));
        }

        startActivity(intent);
    }

    public void onClickBtnCheck(View view) {
        bindService(new Intent(this, CheckerService.class), this, BIND_AUTO_CREATE);
        btnSshs.setText(getString(R.string.btnSshs));
    }

    public void setBtnCheckStop() {
        btnCheck.setEnabled(true);
        btnCheck.setText(getString(R.string.btnCheckStop));
        btnCheck.setBackgroundResource(R.drawable.btn_stop);
        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checker != null) {
                    btnCheck.setEnabled(false);
                    stopThreadChecker();
                    checker.stop();
                    stopService();
                    btnCheck.setText(getString(R.string.btnCheckStopped));
                }
            }
        });
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        needUnbind = true;
        deleteInput();

        CheckerService.Controller controller = (CheckerService.Controller) service;
        serviceControl = controller.getControl();

        //Checker já iniciado - usuário reabriu activity
        if (serviceControl.getChecker() != null) {
            checker = serviceControl.getChecker();

            btnSshs.setEnabled(true);

            //Checker se encontra completo completo
            if (checker.isComplete()) {
                onCheckerDone();
                return;
            }

            //Checker se encontra rodando

        }else{
            //Criar checker - usuário uniciou um check
            serviceControl.setChecker(sshs, qtdThreads);
            checker = serviceControl.getChecker();
            startService();

            checker.check(false);
        }

        startThreadChecker();
        setBtnCheckStop();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        stopThreadChecker();
        if (checker != null && checker.isRunning())
            checker.stop();
        serviceControl = null;
        checker = null;
        stopService(new Intent(this, CheckerService.class));
        btnCheck.setText(getString(R.string.btnCheckStopped));
        showMsg(getString(R.string.msgServiceDisconnet));
    }

    private void unbindService() {
        if (needUnbind) {
            unbindService(this);
            needUnbind = false;
        }
    }

    private void startService() {
        Intent i = new Intent(this, CheckerService.class);
        startService(i);
    }

    private void stopService() {
        unbindService();

        serviceControl = null;

        Intent i = new Intent(this, CheckerService.class);
        stopService(i);
    }

    private void startThreadChecker() {
        threadChecker = new ThreadChecker();
        threadChecker.start();
    }

    private void stopThreadChecker() {
        if (threadChecker != null) {
            threadChecker.stop();
            threadChecker = null;
        }
    }

    private void onCheckerDone() {
        btnCheck.setEnabled(false);

        if (checker != null && checker.isComplete()) {
            btnCheck.setText(getString(R.string.btnCheckChecked));
            btnCheck.setBackgroundResource(R.drawable.btn_checked);
        } else {
            btnCheck.setText(getString(R.string.btnCheckStopped));
        }

        saveSshs();

        if (serviceControl != null)
            stopService();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_EXPLORER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                fileSelecionado = new File(data.getStringExtra("file"));

                loader = new SSHloader(fileSelecionado);
                loadSshs();
            }
        }
    }

    private void loadSshs() {
        loader.setDelimiter(delimitador);

        try {
            if (fixedUser != null && fixedPass != null)
                loader.loadFixed(fixedUser, fixedPass, fixedPort);
            else
                loader.load();
            sshs.addAll(loader.getSSHs());
            refleshBtnSshs();

            if (sshs.size() > 0) {
                btnCheck.setEnabled(true);
                btnSshs.setEnabled(true);
            }
        } catch (IOException e) {
            showMsg(e.getMessage());
        }
    }

    private void saveSshs() {
        if (checker == null)
            return;

        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + FILES_DIR;

        try {
            File fileDir = new File(dir);
            if (!fileDir.exists()) {
                if (!new File(dir).mkdir())
                    throw new IOException(getString(R.string.msgErroAoSalvarDir));
            }

            dir += "/";

            SSHloader saver = new SSHloader(new File(dir + FILE_ON));
            saver.setSshs(checker.getSshOns());
            saver.save();
            saver = new SSHloader(new File(dir + FILE_OFF));
            saver.setSshs(checker.getSshOffs());
            saver.save();
            saver = new SSHloader(new File(dir + FILE_ERROR));
            saver.setSshs(checker.getSshError());
            saver.save();

            showMsg(getString(R.string.msgSucessoAoSalvar));
        } catch(IOException e) {
            showMsg(getString(R.string.msgErroAoSalvar) + " " + e.getMessage());
        }
    }

    private void saveConfig() {
        config.setQtdThreads(qtdThreads);

        try {
            config.save();
        } catch(IOException e) {
            showMsg(e.getMessage());
        }
    }

    private void loadConfig() {
        try {
            config.load();

            qtdThreads = config.getQtdThreads();
        } catch(IOException e) {
            showMsg(e.getMessage());
        }
    }

    private ArrayList<String> serializarSshs(List<SSH> sshs) {
        ArrayList<String> serial = new ArrayList<String>(sshs.size());

        for (SSH ssh : sshs) {
            serial.add(ssh.toString());
        }

        return serial;
    }

    private void deleteInput() {
        btnColar.setVisibility(View.GONE);
        btnArq.setVisibility(View.GONE);
        btnDelimitador.setVisibility(View.GONE);
        btnQtdThreads.setVisibility(View.GONE);
        btnFixedParams.setVisibility(View.GONE);
    }

    private String getClipBoardStr() {
        String data = null;

        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            data = String.valueOf(clipboard.getText());
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            if (!clipboard.hasPrimaryClip()) {
                showMsg(getString(R.string.strClipBoardEmpty));
                return null;
            }

            if (!clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) && !clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                showMsg(getString(R.string.strClipBoardNonText));
                return null;
            }

            android.content.ClipData clip = clipboard.getPrimaryClip();

            data = String.valueOf(clip.getItemAt(0).getText());
        }

        return data;
    }

    private void refreshBtnThreads() {
        btnQtdThreads.setText(getString(R.string.btnThreads) + " ( " +  qtdThreads + " )");
    }

    private void refreshBtnDelimitador() {
        btnDelimitador.setText(getString(R.string.btnDelimitador) + " ( " +  delimitador + " )");
    }

    private void refleshBtnSshs() {
        btnSshs.setText(getString(R.string.btnSshs) + " ( " + sshs.size() + " )");
    }

    private void showFixedParamsInput() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final CheckBox box = new CheckBox(this);
        box.setText(getString(R.string.strCheckBoxFixed));
        box.setChecked(fixedUser != null && fixedPass != null);
        layout.addView(box);

        final EditText user = new EditText(this);
        user.setHint(getString(R.string.strUser));
        if (fixedUser != null)
            user.setText(fixedUser);
        else
            user.setEnabled(false);
        layout.addView(user);

        final EditText pass = new EditText(this);
        pass.setHint(getString(R.string.strPass));
        if (fixedPass != null)
            pass.setText(fixedPass);
        else
            pass.setEnabled(false);
        layout.addView(pass);

        final EditText port = new EditText(this);
        port.setHint(getString(R.string.strPort));
        if (fixedPort != 0)
            port.setText(String.valueOf(fixedPort));
        else
            port.setEnabled(false);
        layout.addView(port);

        box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user.setEnabled(!user.isEnabled());
                pass.setEnabled(!pass.isEnabled());
                port.setEnabled(!port.isEnabled());
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.strFixedDialogTitle));
        builder.setView(layout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (box.isChecked()) {
                    String strUser = user.getText().toString();
                    String strPass = pass.getText().toString();
                    String strPort = port.getText().toString();

                    if (!strUser.isEmpty() && !strPass.isEmpty() && !strPort.isEmpty()) {
                        try {
                            fixedPort = Integer.parseInt(strPort);
                            fixedUser = strUser;
                            fixedPass = strPass;

                            for (SSH ssh : sshs) {
                                ssh.setUser(strUser);
                                ssh.setPass(strPass);
                                ssh.setPort(fixedPort);
                            }
                        } catch(NumberFormatException e) {
                            showMsg(getString(R.string.msgPortInvalido));
                        }
                    }
                }else{
                    fixedUser = null;
                    fixedPass = null;
                }

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void showDelimitadorInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.titleDelimiterDialog));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(delimitador);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                delimitador = input.getText().toString();
                refreshBtnDelimitador();
            }
        });

        builder.show();
    }

    private void showThreadsInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.titleThreadsDialog));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(qtdThreads));
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    qtdThreads = Integer.parseInt(input.getText().toString());
                    refreshBtnThreads();
                } catch(NumberFormatException e) {
                    showMsg(getString(R.string.msgQtdThreadsNaoNumerica));
                }
            }
        });

        builder.show();
    }

    private void showMsg(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }


    private class ThreadChecker implements Runnable {
        private boolean running;

        public void start() {
            running = true;
            new Thread(this).start();
        }

        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            while (running && checker.isRunning() && !checker.isComplete()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
            }

            if (running) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onCheckerDone();
                    }
                });
            }
        }
    }
}
