package Meta1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SearchModule extends UnicastRemoteObject implements ClientSearchModule {

    private ConcurrentHashMap<String, PesquisaUser> rankPesquisas = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> utilizadores = new ConcurrentHashMap<>();
    CopyOnWriteArrayList<PesquisaUser> ranking = new CopyOnWriteArrayList<>();
    private static int port = 7001;
    private int portQueue;
    private int nextBarrel = 0;
    ArrayList<Integer> portBarrels;

    public SearchModule() throws RemoteException {
        super();
    }

    @Override
    public ArrayList<Url> pesquisa(String info) throws java.rmi.RemoteException {
        info = info.substring(1);
        String pesquisa = info.replaceAll("[^ a-zA-Z0-9]", "").toLowerCase().trim();
        try {
            rankPesquisas.putIfAbsent(pesquisa, new PesquisaUser(pesquisa, 0));
            PesquisaUser pesqui = rankPesquisas.get(pesquisa);
            pesqui.setNumvezes(pesqui.getNumvezes() + 1);
            if (pesqui.getNumvezes() == 1) {
                ranking.add(pesqui);
            }
        } catch (Exception e) {
            onCrash();
        }
        String[] palavras = pesquisa.split(" +");
        for (int i = 0; i < palavras.length; i++) {
            System.out.println(i);
            System.out.println(palavras[i]);
        }

        //pedir os barrels existentes
        try {
            ControlPanel cp = (ControlPanel) LocateRegistry.getRegistry(7002).lookup("Monitor");
            portBarrels = cp.portoBarrel();
        } catch (Exception e) {
            System.out.println("Monitor indísponível");
            onCrash();
        }
        if (nextBarrel == portBarrels.size()) {
            nextBarrel = 0;
        }

        System.out.println("consultei o barrel nº " + nextBarrel);
        try {
            BarrelSearch bs = (BarrelSearch) LocateRegistry.getRegistry(portBarrels.get(nextBarrel)).lookup("Barrel");
            nextBarrel++;
            return bs.pesquisa(palavras);
        } catch (Exception e) {
            System.out.println("Barrel indisponivel");
        }

        return null;
    }

    @Override
    public String addURL(String info) throws java.rmi.RemoteException {
        if (!isUrlWellFormed(info)) {
            return "URL inválido";
        }
        try {
            QueueAddGet queueAG = (QueueAddGet) LocateRegistry.getRegistry(portQueue).lookup("QueueURL");
            queueAG.addQueue(info);
        } catch (Exception e) {
            return "Queue URL indisponível";
        }
        return "URL adicionado com sucesso";
    }

    @Override
    public ArrayList<String> verStatus(String info) throws java.rmi.RemoteException {
        ArrayList<String> top10 = new ArrayList<>();
        int tam = ranking.size();
        Collections.sort(ranking);
        top10.add("Top 10:");
        try {
            for (int i = tam - 1; i > tam - 11; i--) {
                top10.add(ranking.get(i).getPesquisa());
                if (i == 0) {
                    break;
                }
            }
        } catch (IndexOutOfBoundsException ob) {

        } catch (Exception e) {
            onCrash();
        }
        top10.add("Componentes:");
        top10.add("Componente:SearchModule no ip:127.0.0.1 e no porto " + port);
        try {
            ControlPanel painelControlo = (ControlPanel) LocateRegistry.getRegistry(7002).lookup("Monitor");
            top10.addAll(painelControlo.informacao());
        } catch (Exception e) {
            return top10;
        }
        return top10;
    }

    public boolean isUrlWellFormed(String urlString) {
        try {
            new URL(urlString);
            System.out.println(urlString + " was a valid link");
            return true;
        } catch (MalformedURLException e) {
            System.out.println(urlString + " was a malformed link - not inserted in the queue");
            return false;
        }
    }

    @Override
    public boolean login(String info) throws RemoteException {
        info = info.substring(1);
        String pesquisa = info.trim();
        String[] credenciais = pesquisa.split(" +");
        if (credenciais.length < 2) {
            return false;
        }
        String pass = utilizadores.get(credenciais[0]);
        if (pass == null) {
            return false;
        }
        return pass.equals(credenciais[1]);
    }

    @Override
    public boolean register(String info) throws RemoteException {
        info = info.substring(1);
        String pesquisa = info.trim();
        String[] credenciais = pesquisa.split(" +");
        if (credenciais.length < 2) {
            return false;
        }
        String pass = utilizadores.get(credenciais[0]);
        if (pass != null) {
            return false;
        }
        utilizadores.put(credenciais[0], credenciais[1]);
        return true;
    }

    @Override
    public ArrayList<String> urlAponta(String info) throws RemoteException {//fix
        info = info.substring(1);
        String pesquisa = info.trim();
        String[] url = pesquisa.split(" +");
        
        try {
            ControlPanel cp = (ControlPanel) LocateRegistry.getRegistry(7002).lookup("Monitor");
            portBarrels = cp.portoBarrel();
        } catch (Exception e) {
            System.out.println("Monitor indísponível");
            onCrash();
        }
        if (nextBarrel == portBarrels.size()) {
            nextBarrel = 0;
        }

        System.out.println("consultei o barrel nº " + nextBarrel);
        try {
            BarrelSearch bs = (BarrelSearch) LocateRegistry.getRegistry(portBarrels.get(nextBarrel)).lookup("Barrel");
            nextBarrel++;
            return bs.urlLinking(url[0]);
        } catch (Exception e) {
            System.out.println("Barrel indisponivel");
        }

        return null;
    }

    public void onStart() {

        File file = new File("rankPesquisas.obj");
        if (file.exists()) {
            try ( FileInputStream fis = new FileInputStream(file);  ObjectInputStream ois = new ObjectInputStream(fis)) {
                rankPesquisas = (ConcurrentHashMap<String, PesquisaUser>) ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                System.err.println("Error reading from file: " + e.getMessage());
            }
        } else {
            System.out.println("Ficheiro não encontrado");
        }
        file = new File("utilizadores.obj");
        if (file.exists()) {
            try ( FileInputStream fis = new FileInputStream(file);  ObjectInputStream ois = new ObjectInputStream(fis)) {
                utilizadores = (ConcurrentHashMap<String, String>) ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                System.err.println("Error reading from file: " + e.getMessage());
            }
        } else {
            System.out.println("Ficheiro não encontrado");
        }
        file = new File("ranking.obj");
        if (file.exists()) {
            try ( FileInputStream fis = new FileInputStream(file);  ObjectInputStream ois = new ObjectInputStream(fis)) {
                ranking = (CopyOnWriteArrayList<PesquisaUser>) ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                System.err.println("Error reading from file: " + e.getMessage());
            }
        } else {
            System.out.println("Ficheiro não encontrado");
        }

    }

    public void onCrash() {

        System.out.println("A fazer os saves");
        try ( FileOutputStream fos = new FileOutputStream("rankPesquisas.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(rankPesquisas);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }

        try ( FileOutputStream fos = new FileOutputStream("utilizadores.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(utilizadores);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }

        try ( FileOutputStream fos = new FileOutputStream("ranking.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(ranking);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }
        System.exit(1);
    }

    public static void main(String[] args) {

        ControlPanel cp = null;
        try {
            cp = (ControlPanel) LocateRegistry.getRegistry(7002).lookup("Monitor");
        } catch (Exception e) {
            System.out.println("Monitor indísponível");
            return;
        }

        try {
            SearchModule sM = new SearchModule();
            Runtime crash = Runtime.getRuntime();
            sM.onStart();
            crash.addShutdownHook(new OnCrash(sM.rankPesquisas, sM.utilizadores, sM.ranking));
            sM.portQueue = cp.portoQueue();
            Registry r = LocateRegistry.createRegistry(port);
            r.rebind("SearchModule", sM);
            System.out.println("Search Module esta ready.");
        } catch (RemoteException re) {
            System.out.println("Erro ao inicializar");
            return;
        } catch (Exception e) {
            System.out.println("Erro ao inicializar");
            return;
        }
    }

}

class OnCrash extends Thread {

    private ConcurrentHashMap<String, PesquisaUser> rankPesquisas;
    private ConcurrentHashMap<String, String> utilizadores;
    CopyOnWriteArrayList<PesquisaUser> ranking;

    public OnCrash(ConcurrentHashMap<String, PesquisaUser> rankPesquisas,
            ConcurrentHashMap<String, String> utilizadores,
            CopyOnWriteArrayList<PesquisaUser> ranking) {
        this.rankPesquisas = rankPesquisas;
        this.utilizadores = utilizadores;
        this.ranking = ranking;
    }

    @Override
    public void run() {
        System.out.println("A fazer os saves");
        try ( FileOutputStream fos = new FileOutputStream("rankPesquisas.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(rankPesquisas);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }

        try ( FileOutputStream fos = new FileOutputStream("utilizadores.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(utilizadores);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }

        try ( FileOutputStream fos = new FileOutputStream("ranking.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(ranking);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }
    }
}
