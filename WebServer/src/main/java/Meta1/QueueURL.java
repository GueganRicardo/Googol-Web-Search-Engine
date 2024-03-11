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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class QueueURL extends UnicastRemoteObject implements QueueAddGet , ComponenteInterface {

    private static int port = 6998;
    private List<String> filaUrls = Collections.synchronizedList(new ArrayList<String>());
    private ConcurrentHashMap<String, Integer> urlProcessados = new ConcurrentHashMap<>();

    public void addQueue(String novoUrl) throws java.rmi.RemoteException {
        Integer isProcessed = urlProcessados.putIfAbsent(novoUrl, 1);
        if (isProcessed != null && isProcessed == 1) {
            return;
        }
        try{
            filaUrls.add(novoUrl);
        } catch (Exception e){
            onCrash();
        }
        
        System.out.println("adicionei - size ficou: " + filaUrls.size());
    }

    public String removeQueue() {
        String envio = null;
        try {
            envio = filaUrls.remove(0);
        } catch (IndexOutOfBoundsException e) {
            envio = null;
        } catch (Exception e){
            onCrash();
        }
        System.out.println("removi - size ficou: " + filaUrls.size());
        return envio;
    }

    public void onStart() {

        File file = new File("filaUrls.obj");
        if (file.exists()) {
            try ( FileInputStream fis = new FileInputStream(file);  ObjectInputStream ois = new ObjectInputStream(fis)) {
                filaUrls = Collections.synchronizedList((List<String>) ois.readObject());
            } catch (ClassNotFoundException | IOException e) {
                System.err.println("Error reading from file: " + e.getMessage());
            }
        } else {
            System.out.println("Ficheiro não encontrado");
        }
        file = new File("urlProcessados.obj");
        if (file.exists()) {
            try ( FileInputStream fis = new FileInputStream(file);  ObjectInputStream ois = new ObjectInputStream(fis)) {
                urlProcessados = (ConcurrentHashMap<String, Integer>) ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                System.err.println("Error reading from file: " + e.getMessage());
            }
        } else {
            System.out.println("Ficheiro não encontrado");
        }

    }

    public void onCrash() {

        System.out.println("A fazer os saves");
        try ( FileOutputStream fos = new FileOutputStream("filaUrls.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(filaUrls);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }

        try ( FileOutputStream fos = new FileOutputStream("urlProcessados.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(urlProcessados);
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
            QueueURL qURL = new QueueURL();
            qURL.onStart();
            Runtime crash = Runtime.getRuntime();
            crash.addShutdownHook(new OnCrashqueue(qURL.filaUrls, qURL.urlProcessados));
            port  = cp.pedirPortoQueue((ComponenteInterface) qURL);
            Registry r = LocateRegistry.createRegistry(port);
            r.rebind("QueueURL", qURL);
            System.out.println("Queue está ready.");
        } catch (RemoteException re) {
            System.out.println("Erro ao inicializar" + re);
        }
    }

    public QueueURL() throws RemoteException {
        super();
    }

    
    public void Desligar() throws RemoteException {
        onCrash();
    }

    @Override
    public String informacaoComponente() throws RemoteException {
        return "Componente:QueueURL no ip:127.0.0.1 e no porto " + port;
    }

}

class OnCrashqueue extends Thread {

    private List<String> filaUrls;
    private ConcurrentHashMap<String, Integer> urlProcessados;
    
    public OnCrashqueue(List<String> filaUrls, ConcurrentHashMap<String, Integer> urlProcessados) {
        this.filaUrls = filaUrls;
        this.urlProcessados = urlProcessados;
    }

    @Override
    public void run() {
        System.out.println("A fazer os saves");
        try ( FileOutputStream fos = new FileOutputStream("filaUrls.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(filaUrls);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }

        try ( FileOutputStream fos = new FileOutputStream("urlProcessados.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(urlProcessados);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }
    }
}
