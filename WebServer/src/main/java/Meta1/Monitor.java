package Meta1;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;

public class Monitor extends UnicastRemoteObject implements ControlPanel {

    private int nextPort = 7003;
    private int porto = 7002;
    private int queuePort = 0;
    ArrayList<Integer> firstBarrelPort = new ArrayList<>();
    List<ComponenteInterface> componentes = Collections.synchronizedList(new ArrayList<ComponenteInterface>());

    public Monitor() throws RemoteException {
        super();
    }

    public static void main(String[] args) {
        Monitor monitor;
        try {
            monitor = new Monitor();
            Registry r = LocateRegistry.createRegistry(monitor.porto);
            r.rebind("Monitor", monitor);
            System.out.println("Monitor esta ready.");
        } catch (RemoteException re) {
            System.out.println("Erro ao inicializar MOnitor");
        }
    }

    @Override
    public int pedirPortoQueue(ComponenteInterface newComponent) throws RemoteException {
        System.out.println("dei");
        componentes.add(newComponent);
        queuePort = nextPort;
        nextPort++;
        return queuePort;
    }

    @Override
    public ArrayList<String> informacao() throws RemoteException {
        int dead = -1;
        ArrayList<String> info = new ArrayList<>();
        info.add("Componente: Monitor no ip:127.0.0.1 e no porto " + porto);
        for (int i = 0; i < componentes.size(); i++) {
            try {
                info.add(componentes.get(i).informacaoComponente());
            } catch (RemoteException re) {
                dead = i;
                System.out.println("Morreu");
            } catch (IndexOutOfBoundsException outBounds) {
                break;
            }
        }
        if (dead != -1) {
            componentes.remove(dead);
        }
        return info;
    }

    @Override
    public int[] pedirPortoBarrel(ComponenteInterface newComponent) throws RemoteException {
        int[] primeiroPorto = new int[2];
        if (firstBarrelPort.isEmpty()) {
            primeiroPorto[0] = 0;
        } else {
            primeiroPorto[0] = firstBarrelPort.get(0);
        }
        primeiroPorto[1] = nextPort;
        firstBarrelPort.add(nextPort);
        nextPort++;
        componentes.add(newComponent);
        return primeiroPorto;
    }

    @Override
    public int pedirPortoDownloader(ComponenteInterface newComponent) throws RemoteException {
        componentes.add(newComponent);
        return queuePort;
    }

    @Override
    public int portoQueue() throws RemoteException {
        return queuePort;
    }

    @Override
    public ArrayList<Integer> portoBarrel() throws RemoteException {
        return firstBarrelPort;
    }

    @Override
    public int nBarrels() throws RemoteException {
        return firstBarrelPort.size();
    }

    @Override
    public void deadBarrel(int port) throws RemoteException {
       for (int i = 0; i < firstBarrelPort.size(); i++) {
            if(firstBarrelPort.get(i)==port){
                firstBarrelPort.remove(i);
                return;
            }
        }
    }

}


