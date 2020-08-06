package com.comxa.universo42.sshchecker.view;

import android.content.ClipData;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.comxa.universo42.sshchecker.R;

import java.util.ArrayList;

abstract public class TabFragment extends android.support.v4.app.Fragment {
    private ListView lista;
    private ArrayList<String> sshs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_fragment_sshs, container, false);

        LinearLayout ll = (LinearLayout) view.findViewById(R.id.linearLayoutTab);

        lista = new ListView(getContext());
        lista.setOnItemClickListener(getOnItemClickLista());

        ll.addView(lista);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstance) {
        super.onActivityCreated(savedInstance);
        sshs = getSshs();

        lista.setAdapter(new ArrayAdapter<String>(getContext(), R.layout.my_simple_list_item_1, sshs));
    }

    abstract public ArrayList<String> getSshs();

    public SshListActivity getSshActivity() {
        return (SshListActivity) getActivity();
    }

    public AdapterView.OnItemClickListener getOnItemClickLista() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int posicaoLinhaSelecionada, long id) {
                setClipBoardStr(sshs.get(posicaoLinhaSelecionada));
                Toast.makeText(getActivity(), getString(R.string.msgCopiado), Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void setClipBoardStr(String str) {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);

            clipboard.setText(str);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);

            clipboard.setPrimaryClip(ClipData.newPlainText("simple text", str));
        }
    }


    public static class TabSshs extends TabFragment{
        @Override
        public ArrayList<String> getSshs() {
            return getSshActivity().getSshs();
        }
    }

    public static class TabSshOns extends TabFragment {
        @Override
        public ArrayList<String> getSshs() {
            return getSshActivity().getSshOns();
        }
    }

    public static class TabSshOffs extends TabFragment {
        @Override
        public ArrayList<String> getSshs() {
            return getSshActivity().getSshOffs();
        }
    }

    public static class TabSshError extends TabFragment {
        @Override
        public ArrayList<String> getSshs() {
            return getSshActivity().getSshError();
        }
    }

    public static class TabSshEmpty extends TabFragment {
        @Override
        public ArrayList<String> getSshs() {
            return new ArrayList<String>();
        }
    }
}
