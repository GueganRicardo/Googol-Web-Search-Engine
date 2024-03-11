package Meta1;

import java.io.Serializable;

public class PesquisaUser implements Comparable<PesquisaUser>, Serializable {

    private String pesquisa;
    private int numvezes;

    public PesquisaUser(String pesquisa, int numvezes) {
        this.pesquisa = pesquisa;
        this.numvezes = numvezes;
    }

    public String getPesquisa() {
        return pesquisa;
    }

    public void setPesquisa(String pesquisa) {
        this.pesquisa = pesquisa;
    }

    public int getNumvezes() {
        return numvezes;
    }

    public void setNumvezes(int numvezes) {
        this.numvezes = numvezes;
    }

    @Override
    public int compareTo(PesquisaUser o) {
        return Integer.compare(this.numvezes, o.getNumvezes());
    }

}

