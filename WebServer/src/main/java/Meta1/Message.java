package Meta1;

import java.io.Serializable;
import java.util.Set;

public class Message implements Serializable {

    public String url;
    public String citacao;
    public String titulo;
    public Set<String> words;
    public Set<String> urls;
    public boolean ack;
    

    public Message(String url, String citacao, String titulo, Set<String> words, Set<String> urls, boolean ack) {
        this.url = url;
        this.citacao = citacao;
        this.titulo = titulo;
        this.words = words;
        this.urls = urls;
        this.ack = ack;
    }
    
    
    @Override
    public String toString() {
    if (ack == true) 
        return "mens de ack do " + url;
    if (citacao.equals("")) {
        return "URL: " + url + "\nTítulo: " + titulo + "\n";
    } else {
        return "URL: " + url + "\nTítulo: " + titulo + "\nCitação: " + citacao + "\n";
    }
}

    
}
