package Meta1;

import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public interface BarrelSearch extends Remote {

    public ArrayList<Url> pesquisa(String[] palavras) throws java.rmi.RemoteException;

    public ArrayList<String> urlLinking(String url) throws java.rmi.RemoteException;

    public Map<String, Set<String>> pedeWords() throws java.rmi.RemoteException;
    
    public Map<String, Set<String>> pedeurls() throws java.rmi.RemoteException;
    
    public Map<String, Url> pedeInfoUrl() throws java.rmi.RemoteException;

}