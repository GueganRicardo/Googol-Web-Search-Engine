package Meta1;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.*;

import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Barrel extends UnicastRemoteObject implements BarrelSearch, ComponenteInterface {

    int id;

    private static String MULTICAST_ADDRESS = "224.3.2.1";
    private static int portMulticast = 4321; //multicast
    private static int BUFFER_SIZE = 65507;
    private static int port = 5310; //RMI pesquisa
    public HashMap<String, Boolean> stopWords = new HashMap<String, Boolean>();
    private Map<String, Set<String>> words = Collections.synchronizedMap(new HashMap<>()); // URL
    private Map<String, Set<String>> urls = Collections.synchronizedMap(new HashMap<>()); // URL
    private Map<String, Url> infoUrl = Collections.synchronizedMap(new HashMap<>());

    public Barrel() throws RemoteException {
        super();
    }

    public static void main(String[] args) {

        ControlPanel cp;
        try {
            cp = (ControlPanel) LocateRegistry.getRegistry(7002).lookup("Monitor");
        } catch (Exception e) {
            System.out.println("Monitor indísponível");
            return;
        }
        Barrel barrel;
        try {
            barrel = new Barrel();
            int[] portos = cp.pedirPortoBarrel((ComponenteInterface) barrel);
            System.out.println(portos[0]);
            if (portos[0] != 0) {//pedir info ao outro barril
                try {
                    System.out.println("pedi ao outro barrel");
                    BarrelSearch bs = (BarrelSearch) LocateRegistry.getRegistry(portos[0]).lookup("Barrel");
                    barrel.words = bs.pedeWords();
                    barrel.infoUrl = bs.pedeInfoUrl();
                    barrel.urls = bs.pedeurls();
                } catch (Exception e) {
                    System.out.println("Barrel indisponivel");
                }
            } else {
                barrel.onStart();
            }
            
            Runtime crash = Runtime.getRuntime();
            crash.addShutdownHook(new OnCrashBarrel(barrel.words, barrel.urls, barrel.infoUrl, portos[1]));

            port = portos[1];
        } catch (RemoteException ex) {
            System.out.println("catchy");
            return;
        } catch (Exception l) {
            System.out.println("Outro Problema");
            return;
        }
        try {
            Registry r = LocateRegistry.createRegistry(port);
            r.rebind("Barrel", barrel);
            System.out.println("Barrel Search esta ready.");
        } catch (RemoteException re) {
            System.out.println("Erro ao inicializar" + re);
        }
        ///Users/samuelmachado/Desktop/sd/ProjetoSD2/stopwords.txt
        String fileName = "/Users/samuelmachado/Desktop/sd/ProjetoSD2/stopwords.txt";
        int i = 0;
        try ( BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                i++;
                barrel.stopWords.put(line.trim(), true);
            }
        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }

        barrel.principal();
    }

    public void principal() {
        System.out.println("barrel activated");
        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket(portMulticast);  // create socket and bind it
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            while (true) {
                //System.out.println("à espera");
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                //System.out.println("Received packet from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " with message:");
                byte[] data = packet.getData();
                System.out.println(data.length);
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);
                try {
                    Message mensagem = (Message) ois.readObject();
                    if (mensagem.ack == true) {
                        continue;
                    }
                    //System.out.println("no barrel chegou:");
                    System.out.println(mensagem.titulo);
                    Message ackMessage = new Message(mensagem.url, null, null, null, null, true);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(ackMessage);
                    byte[] dataAck = baos.toByteArray();

                    // Create a DatagramPacket object with the data, address, and port
                    System.out.println("sending ack of: " + mensagem.url);
                    DatagramPacket packetAck = new DatagramPacket(dataAck, dataAck.length, group, portMulticast);
                    socket.send(packetAck);

                    processMessage(mensagem);
                    //printMaps();

                } catch (ClassNotFoundException ex) {
                    System.out.println("class-not-found");
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    public void processMessage(Message mess) {
        Set<String> mess_words = mess.words;
        Set<String> mess_urls = mess.urls;
        Url url = new Url(mess.url, mess.citacao, mess.titulo);
        infoUrl.put(mess.url, url);
        for (String mess_word : mess_words) {
            synchronized (words) {
                Set<String> existingValues = words.get(mess_word);
                if (existingValues == null) {
                    Set<String> newSet = new HashSet<>();
                    newSet.add(mess.url);
                    words.put(mess_word, newSet);
                } else {
                    existingValues.add(mess.url);
                    words.put(mess_word, existingValues);
                }
            }
        }
        for (String mess_url : mess_urls) {
            synchronized (urls) {
                Set<String> existingValues = urls.get(mess_url);
                if (existingValues == null) {
                    Set<String> newSet = new HashSet<>();
                    newSet.add(mess.url);
                    urls.put(mess_url, newSet);
                } else {
                    existingValues.add(mess.url);
                    urls.put(mess_url, existingValues);
                }
            }
        }
    }

    public void printMaps() {
        // Print words map
        System.out.println("Words map:");
        int i = 1;
        for (Map.Entry<String, Set<String>> entry : words.entrySet()) {
            i = 0;
            String key = entry.getKey();
            Set<String> values = entry.getValue();
            System.out.println("- Key: " + key);
            System.out.println("  Values:");
            for (String value : values) {
                System.out.println("  - " + value);
            }
        }
        if (i == 1) {
            System.out.println("n tava cá nada");
        }

        // Print urls map
        System.out.println("Urls map:");
        for (Map.Entry<String, Set<String>> entry : urls.entrySet()) {
            String key = entry.getKey();
            Set<String> values = entry.getValue();
            System.out.println("- Key: " + key);
            System.out.println("  Values:");
            for (String value : values) {
                System.out.println("  - " + value);
            }
        }

        // Print infoUrl map
        System.out.println("InfoUrl map:");
        for (Map.Entry<String, Url> entry : infoUrl.entrySet()) {
            String key = entry.getKey();
            Url value = entry.getValue();
            System.out.println("- Key: " + key);
            System.out.println("  Value: " + value.toString());
        }
    }

    public void onStart() {

        File file = new File("words.obj");
        if (file.exists()) {
            try ( FileInputStream fis = new FileInputStream(file);  ObjectInputStream ois = new ObjectInputStream(fis)) {
                words = (Map<String, Set<String>>) ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                System.err.println("Error reading from file: " + e.getMessage());
            }
        } else {
            System.out.println("Ficheiro não encontrado");
        }
        file = new File("urls.obj");
        if (file.exists()) {
            try ( FileInputStream fis = new FileInputStream(file);  ObjectInputStream ois = new ObjectInputStream(fis)) {
                urls = (Map<String, Set<String>>) ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                System.err.println("Error reading from file: " + e.getMessage());
            }
        } else {
            System.out.println("Ficheiro não encontrado");
        }
        file = new File("infoUrl.obj");
        if (file.exists()) {
            try ( FileInputStream fis = new FileInputStream(file);  ObjectInputStream ois = new ObjectInputStream(fis)) {
                infoUrl = (Map<String, Url>) ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                System.err.println("Error reading from file: " + e.getMessage());
            }
        } else {
            System.out.println("Ficheiro não encontrado");
        }

    }

    public void onCrash() {

        System.out.println("A fazer os saves");
        
        try {
            ControlPanel cp = (ControlPanel) LocateRegistry.getRegistry(7002).lookup("Monitor");
            cp.deadBarrel(port);
        } catch (Exception e) {
            System.out.println("Monitor indísponível");
            return;
        }
        
        try ( FileOutputStream fos = new FileOutputStream("words.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(words);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }

        try ( FileOutputStream fos = new FileOutputStream("urls.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(urls);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }

        try ( FileOutputStream fos = new FileOutputStream("infoUrl.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(infoUrl);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }

        System.exit(1);
    }

    @Override
    public ArrayList<String> urlLinking(String url) throws java.rmi.RemoteException {
        return new ArrayList(urls.get(url));
    }

    @Override
    public ArrayList<Url> pesquisa(String[] palavras) throws java.rmi.RemoteException {
        System.out.println(" ---- CHEGUEI AO BARREL ----");
        Set<String> output = null;
        boolean primeira_palavra = true;
        if (palavras.length == 0) {
            return null;
        }
        for (String palavra : palavras) {
            System.out.println("palavra: " + palavra);
            if (!stopWords.containsKey(palavra.toLowerCase())) { // se for stopword ignora-se
                Set<String> urls_relacionados = words.get(palavra);
                if (urls_relacionados != null) {
                    for (String url : urls_relacionados) {
                        System.out.println("- " + url);
                    }
                }
                if (urls_relacionados == null) {
                    System.out.println("a palavra --> " + palavra + " <-- não tinha urls relacionados");
                    return null;
                }
                if (primeira_palavra) {
                    output = new HashSet<>(urls_relacionados);
                    primeira_palavra = false;
                } else {
                    output = new HashSet<>(intersect(output, urls_relacionados));
                }
            } else {
                System.out.println("palavra ignorada: " + palavra);
            }
        }
        System.out.println("resultado das intercessoes: ");
        for (String a : output) {
            System.out.println("- " + a);
        }
        ArrayList<String> urlsDesordenados = new ArrayList(output);
        Map<String, Integer> urlRelevancia = new HashMap<String, Integer>();
        for (String url : urlsDesordenados) {
            urlRelevancia.put(url, urls.get(url).size());
        }
        ArrayList<String> urlsOrdenados = new ArrayList(sortByValue(urlRelevancia));
        ArrayList<Url> objetosUrlsOrdenados = new ArrayList<Url>();
        for (String url : urlsOrdenados) {
            objetosUrlsOrdenados.add(infoUrl.get(url));
        }
        return objetosUrlsOrdenados;
    }

    public static ArrayList<String> sortByValue(Map<String, Integer> map) {
        /*for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            System.out.println(key + " => " + value);
        }*/
        ArrayList<String> sortedKeys = new ArrayList<String>();
        map.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> sortedKeys.add(entry.getKey()));
        return sortedKeys;
    }

    public static Set<String> intersect(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        return intersection;
    }

    public void printMapss() {
        // Print words map
        System.out.println("Words map:");
        int i = 1;
        for (Map.Entry<String, Set<String>> entry : words.entrySet()) {
            i = 0;
            String key = entry.getKey();
            Set<String> values = entry.getValue();
            System.out.println("- Key: " + key);
            System.out.println("  Values:");
            for (String value : values) {
                System.out.println("  - " + value);
            }
        }
        if (i == 1) {
            System.out.println("n tava cá nada");
        }

        // Print urls map
        System.out.println("Urls map:");
        for (Map.Entry<String, Set<String>> entry : urls.entrySet()) {
            String key = entry.getKey();
            Set<String> values = entry.getValue();
            System.out.println("- Key: " + key);
            System.out.println("  Values:");
            for (String value : values) {
                System.out.println("  - " + value);
            }
        }

        // Print infoUrl map
        System.out.println("InfoUrl map:");
        for (Map.Entry<String, Url> entry : infoUrl.entrySet()) {
            String key = entry.getKey();
            Url value = entry.getValue();
            System.out.println("- Key: " + key);
            System.out.println("  Value: " + value.toString());
        }
    }

    @Override
    public void Desligar() throws RemoteException {
        onCrash();
    }

    @Override
    public String informacaoComponente() throws RemoteException {
        return "Componente:Barrel no ip:127.0.0.1 e no porto " + port;
    }

    @Override
    public Map<String, Set<String>> pedeWords() throws RemoteException {
        return words;
    }

    @Override
    public Map<String, Set<String>> pedeurls() throws RemoteException {
        return urls;
    }

    @Override
    public Map<String, Url> pedeInfoUrl() throws RemoteException {
        return infoUrl;
    }

}

class OnCrashBarrel extends Thread {

    private Map<String, Set<String>> words;
    private Map<String, Set<String>> urls;
    private Map<String, Url> infoUrl;
    private int port;

    public OnCrashBarrel(Map<String, Set<String>> words, Map<String, Set<String>> urls, Map<String, Url> infoUrl, int port) {
        this.words = words;
        this.urls = urls;
        this.infoUrl = infoUrl;
        this.port=port;
    }

    @Override
    public void run() {
        
        try {
            ControlPanel cp = (ControlPanel) LocateRegistry.getRegistry(7002).lookup("Monitor");
            cp.deadBarrel(port);
        } catch (Exception e) {
            System.out.println("Monitor indísponível");
            return;
        }
        
        System.out.println("A fazer os saves");
        try ( FileOutputStream fos = new FileOutputStream("words.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(words);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }

        try ( FileOutputStream fos = new FileOutputStream("urls.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(urls);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }

        try ( FileOutputStream fos = new FileOutputStream("infoUrl.obj");  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(infoUrl);
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }
    }
}
