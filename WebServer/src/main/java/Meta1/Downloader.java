package Meta1;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

// type | url_list ; item_count | 2; item_0_name | www.uc.pt; item_1_name | www. dei . uc . pt
// type | word_list ; url | www.uc.pt ; item_count | 2 ; item_0_name | universidade ; item_1_name | estudos
class ThreadDownloader implements Runnable {

    public HashMap<String, Boolean> stopWords = new HashMap<String, Boolean>();
    private int BUFFER_SIZE = 65507;
    private final String MULTICAST_ADDRESS = "224.3.2.1";
    private final int PORT = 4321;//multicast
    private final int LENGTH_CITACAO = 12; // n de palavras da citacao
    private int portQueue;
    int id;
    Thread t;

    public ThreadDownloader(int id, HashMap<String, Boolean> stopWords, int portQueue) {
        this.id = id;
        this.stopWords = stopWords;
        this.t = new Thread(this, Integer.toString(id));
        this.portQueue = portQueue;
        System.out.println("New thread: " + id);
        t.start();
    }

    public void run() {
        MulticastSocket socket = null;
        QueueAddGet queue;
        try {
            queue = (QueueAddGet) LocateRegistry.getRegistry(portQueue).lookup("QueueURL");
        } catch (Exception e) {
            System.out.println("QueueURL indisponível");
            e.printStackTrace();
            return;
        }

        try {
            socket = new MulticastSocket(PORT);  // create socket and binding it (not only for sending)
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            while (true) {
                String url_to_be_processed = null;
                while (url_to_be_processed == null) {
                    try {
                        System.out.println("consultei queue");
                        url_to_be_processed = queue.removeQueue();
                    } catch (Exception e) {
                        System.out.println("problema no remove da queue");
                    }
                    if (url_to_be_processed == null) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            System.out.println("Interrupted");
                        }
                    }
                }

                try {
                    Document doc = Jsoup.connect(url_to_be_processed).get();
                    StringTokenizer tokens = new StringTokenizer(doc.text());
                    int countTokens = 0;
                    String citacao = "";
                    Set<String> all_words = new HashSet<>();
                    Set<String> all_urls = new HashSet<>();
                    while (tokens.hasMoreElements() && countTokens++ < 1000) {
                        String actual_word = tokens.nextToken();
                        if (countTokens < LENGTH_CITACAO) {
                            citacao = citacao + " " + actual_word;
                        }
                        String actual_word_lower = actual_word.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
                        if (!stopWords.containsKey(actual_word_lower) && actual_word_lower.length() > 2) {
                            all_words.add(actual_word_lower); // adicionamos se não for uma stop word ou se tiver menos de 3 letras
                        }
                    }
                    if (!citacao.isEmpty()) {
                        citacao += "...";
                    }
                    Elements all_links = doc.select("a[href]");
                    String url_title = "";
                    for (Element link : all_links) {
                        String actual_url = link.attr("abs:href");
                        url_title = link.text();
                        queue.addQueue(actual_url);
                        all_urls.add(actual_url);
                    }
                    //try {

                    Message mensagem = new Message(url_to_be_processed, citacao, url_title, all_words, all_urls, false);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(mensagem);
                    byte[] data = baos.toByteArray();

                    // Create a DatagramPacket object with the data, address, and port
                    int n_barrels;
                    System.out.println("packet being sent: " + url_to_be_processed);
                    DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
                    try {
                        ControlPanel cp = (ControlPanel) LocateRegistry.getRegistry(7002).lookup("Monitor");
                        n_barrels = cp.nBarrels();
                    } catch (Exception e) {
                        System.out.println("ControlPanel indisponível");
                        e.printStackTrace();
                        return;
                    }
                    
                    int counter_acks = 0;
                    boolean tudoAck = false;

// Send initial message
                    socket.send(packet);

// Set timeout to 8000 milliseconds
                    socket.setSoTimeout(8000);

                    do {
                        try {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            DatagramPacket possibleAck = new DatagramPacket(buffer, buffer.length);
                            socket.receive(possibleAck);

                            byte[] data2 = possibleAck.getData();

                            ByteArrayInputStream bais = new ByteArrayInputStream(data2);
                            ObjectInputStream ois = new ObjectInputStream(bais);
                            Message possivelMensagemAck = (Message) ois.readObject();

                            if (possivelMensagemAck.ack == true && url_to_be_processed.equals(possivelMensagemAck.url)) {
                                counter_acks++;
                                int remaining_acks = n_barrels - counter_acks;
                                System.out.println("ack recebido: " + possivelMensagemAck.url + ", faltam " + remaining_acks + " acks");
                                if (counter_acks == n_barrels) {
                                    tudoAck = true;
                                }
                            } else if (possivelMensagemAck.ack == false) {
                                System.out.println("recebi mensagem mas not an ack");
                            }
                        } catch (SocketTimeoutException e) {
                            // Resend the message
                            socket.send(packet);
                            System.out.println("ack não recebido, reenviando mensagem...");

                            // Reset the counter
                            counter_acks = 0;
                        }
                    } while (!tudoAck);

// Clear the timeout
                    socket.setSoTimeout(0);

                } catch (IOException e) {
                    System.out.println("outro erro");
                } catch (IllegalArgumentException e) {
                    System.out.println("malformed url");
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(ThreadDownloader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

}

class Downloader extends UnicastRemoteObject implements ComponenteInterface {

    public static HashMap<String, Boolean> stopWords = new HashMap<String, Boolean>();
    private int numDownloadersRunning = 2;

    public Downloader() throws RemoteException {
        super();
    }

    public static void main(String args[]) {

        ControlPanel cp = null;
        try {
            cp = (ControlPanel) LocateRegistry.getRegistry(7002).lookup("Monitor");
        } catch (Exception e) {
            System.out.println("Monitor indísponível");
            return;
        }
        int portQ = 0;
        Downloader down;
        try {
            down = new Downloader();
            portQ = cp.pedirPortoDownloader((ComponenteInterface) down);
            System.out.println("Downloader está ready.");
        } catch (RemoteException re) {
            System.out.println("Erro ao inicializar");
            return;
        }

        String fileName = "C:/Users/guega/Desktop/stopwords.txt";
        int i = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                i++;
                stopWords.put(line.trim(), true);
            }
        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
            return;
        }

        for (int d = 0; d < down.numDownloadersRunning; d++) {
            new ThreadDownloader(d, stopWords, portQ);
        }

    }

    @Override
    public void Desligar() throws RemoteException {
        System.exit(1);
    }

    @Override
    public String informacaoComponente() throws RemoteException {
        return "Componente:Downloader no ip:127.0.0.1 a correr " + numDownloadersRunning + " threads";
    }
}
