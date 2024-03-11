package Meta1;

import java.rmi.Remote;

public interface ComponenteInterface extends Remote{
    public void Desligar() throws java.rmi.RemoteException;
    public String informacaoComponente() throws java.rmi.RemoteException;
}

