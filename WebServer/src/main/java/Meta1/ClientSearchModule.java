package Meta1;

import java.rmi.*;
import java.util.ArrayList;

public interface ClientSearchModule extends Remote {
    public ArrayList<Url> pesquisa (String info) throws java.rmi.RemoteException;
    public String addURL (String info) throws java.rmi.RemoteException;
    public ArrayList<String> verStatus (String info) throws java.rmi.RemoteException;
    public boolean login (String info) throws java.rmi.RemoteException;
    public boolean register (String info) throws java.rmi.RemoteException;
    public ArrayList<String> urlAponta (String info) throws java.rmi.RemoteException;
}

