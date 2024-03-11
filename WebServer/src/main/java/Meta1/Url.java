package Meta1;

import java.io.Serializable;
import java.util.Objects;

public class Url implements Serializable {

    public String url;
    public String citacao;
    public String titulo;

    public Url(String url, String citacao, String titulo) {
        this.url = url;
        this.citacao = citacao;
        this.titulo = titulo;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.url);
        hash = 97 * hash + Objects.hashCode(this.citacao);
        hash = 97 * hash + Objects.hashCode(this.titulo);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Url other = (Url) obj;
        if (!Objects.equals(this.url, other.url)) {
            return false;
        }
        if (!Objects.equals(this.citacao, other.citacao)) {
            return false;
        }
        return Objects.equals(this.titulo, other.titulo);
    }

    @Override
    public String toString() {
        if (citacao.equals("")) {
            return "URL: " + url + "\nTítulo: " + titulo + "\n";
        } else {
            return "URL: " + url + "\nTítulo: " + titulo + "\nCitação: " + citacao + "\n";
        }
    }

}