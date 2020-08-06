package com.comxa.universo42.sshchecker.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import android.widget.TextView;

import com.comxa.universo42.sshchecker.R;

public class FileExplorerActivity extends Activity {
    public static final int RESULT_CODE_CANCELED = 2;

    private Button btnOk;
    private Button btnParent;
    private TextView txtViewSelected;
    private ListView lista;
    private List<String> filesListaName = new ArrayList<String>();
    private List<File> filesLista = new ArrayList<File>();
    private File selecionado;
    private File dirAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        this.txtViewSelected = (TextView) findViewById(R.id.txtViewSelected);
        this.btnOk = (Button) findViewById(R.id.btnOk);
        this.btnOk.setEnabled(false);
        this.btnParent = (Button) findViewById(R.id.btnParent);
        this.lista =  (ListView) findViewById(R.id.lista);
        this.lista.setOnItemClickListener(getOnItemClickLista());

        this.dirAtual = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        makeLista(this.dirAtual);
    }

    @Override
    public void onBackPressed() {
        finish(RESULT_CODE_CANCELED, new Intent());
    }

    public void onClickBtnParent(View view) {
        if (dirAtual.getParentFile() != null) {
            dirAtual = dirAtual.getParentFile();
            makeLista(dirAtual);
        }
    }

    public void onClickBtnOk(View view) {
        Intent data = new Intent();

        data.putExtra("file", selecionado.toString());

        finish(RESULT_OK, data);
    }

    public AdapterView.OnItemClickListener getOnItemClickLista() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int posicaoLinhaSelecionada, long id) {
                File fileSelecionado = filesLista.get(posicaoLinhaSelecionada);

                if(fileSelecionado.isDirectory()) {
                    dirAtual = fileSelecionado;
                    makeLista(dirAtual);
                } else {
                    selecionado = fileSelecionado;
                    txtViewSelected.setText(selecionado.getName());
                    btnOk.setEnabled(true);
                }
            }
        };
    }

    public void makeLista(File dir) {
        File[] files = dir.listFiles();

        this.filesLista.clear();
        this.filesListaName.clear();

        for (File file : files) {
            if (!file.getName().startsWith(".")) {
                if (file.isDirectory())
                    this.filesListaName.add(file.getName() + "/");
                else
                    this.filesListaName.add(file.getName());
                this.filesLista.add(file);
            }
        }

        ArrayAdapter<String> valoresLista = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, this.filesListaName);

        this.lista.setAdapter(valoresLista);
    }

    public void finish(int retCod, Intent data) {
        setResult(retCod, data);

        this.finish();
    }
}
