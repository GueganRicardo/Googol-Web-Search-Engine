package Meta1;

import java.rmi.Remote;
import java.util.ArrayList;

public interface ControlPanel extends Remote {

    public int pedirPortoQueue(ComponenteInterface newComponent) throws java.rmi.RemoteException;

    public int[] pedirPortoBarrel(ComponenteInterface newComponent) throws java.rmi.RemoteException;

    public int pedirPortoDownloader(ComponenteInterface newComponent) throws java.rmi.RemoteException;
    
    public int portoQueue() throws java.rmi.RemoteException;
    
    public ArrayList<Integer> portoBarrel() throws java.rmi.RemoteException;

    public ArrayList<String> informacao() throws java.rmi.RemoteException;
    
    public int nBarrels() throws java.rmi.RemoteException;
    
    public void deadBarrel(int port) throws java.rmi.RemoteException;
}

