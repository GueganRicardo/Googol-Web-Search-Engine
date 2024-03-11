package Meta1;

import java.rmi.*;

public interface QueueAddGet extends Remote {
    public void addQueue(String url) throws java.rmi.RemoteException;
    public String removeQueue() throws java.rmi.RemoteException;
}

