package Meta1;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        System.out.println("             __                     __     \n" +
                            " _    _____ / /______  __ _  ___   / /____ \n" +
                            "| |/|/ / -_) / __/ _ \\/  ' \\/ -_) / __/ _ \\\n" +
                            "|__,__/\\__/_/\\__/\\___/_/_/_/\\__/  \\__/\\___/\n" +
                            "                                           ");
        System.out.println(" .88888.                                      dP \n" +
                           "d8'   `88                                     88 \n" +
                           "88        .d8888b. .d8888b. .d8888b. .d8888b. 88 \n" +
                           "88   YP88 88'  `88 88'  `88 88'  `88 88'  `88 88 \n" +
                           "Y8.   .88 88.  .88 88.  .88 88.  .88 88.  .88 88 \n" +
                           " `88888'  `88888P' `88888P' `8888P88 `88888P' dP \n" +
                           "                                 .88             \n" +
                           "                             d8888P              ");
        Scanner stdin = new Scanner(System.in);
        String resposta;
        String user = "";
        ArrayList<Url> resposta_urls;
        ArrayList<String> resposta_p;
        ClientSearchModule CSM = null;
        boolean loged = false;

        try {
            CSM = (ClientSearchModule) LocateRegistry.getRegistry(7001).lookup("SearchModule");
        } catch (Exception e) {
            System.out.println("O servidor encontra-se indisponível");
            return;
        }
        while (true) {
            System.out.println("/------------------------------------------------\\");
            System.out.println("|Fazer uma pesquisa: 1 palavra1 palavra2 palavra3|");
            System.out.println("|Adicionar um URL: 2 URL                         |");
            System.out.println("|Ver estado do Sistema: 3                        |");
            if (!loged) {
                System.out.println("|Efetuar Login: 4 username pass                  |");
                System.out.println("|Efetuar Registo: 5 username pass                |");
            }
            if (loged) {
                System.out.println("|Ligações a um URL: 4 ULR                          |");
                System.out.println("|Efetuar Logout: 5                                 |");
            }
            System.out.println("\\------------------------------------------------/");
            System.out.print(" => ");
            String linha = stdin.nextLine();
            {
                try {
                    String[] parametros = linha.split(" ");
                    if (parametros[0].equals("1")) {//pesquisa
                        resposta_urls = CSM.pesquisa(linha);
                        if (resposta_urls == null) {
                            System.out.println("Não foram obtidos resultados");
                        } else {
                            int nUrls = resposta_urls.size();
                            System.out.println("\nObtidos " + nUrls + " resultados.\n");
                            int countUrls = 0;
                            for (Url u : resposta_urls) {
                                System.out.println(countUrls + 1 + " " + u);
                                if (countUrls % 10 == 9) {
                                    Scanner scanner = new Scanner(System.in);
                                    System.out.println("Enter para ver mais. \"q\" para sair");
                                    String input = scanner.nextLine();
                                    if (input.equals("q")) {
                                        break;
                                    }
                                }
                                countUrls++;
                            }
                        }
                    } else if (parametros[0].equals("2")) {//add URL
                        resposta = CSM.addURL(parametros[1]);
                        System.out.println("Resultado: " + resposta);
                    } else if (parametros[0].equals("3")) {//ver info sistema
                        resposta_p = CSM.verStatus(linha);
                        for (int i = 0; i < resposta_p.size(); i++) {
                            System.out.println(resposta_p.get(i));
                        }
                    } else if (parametros[0].equals("4") && !loged) {// efetuar login
                        loged = CSM.login(linha);
                        if (!loged) System.out.println("Não foi possível efetuar login.");
                        else {
                            user = linha.substring(1);
                            user = user.trim();
                            user = user.split(" +")[0];
                            System.out.println("Bem-vindo de volta " + user + "!");
                        }
                    } else if (parametros[0].equals("5") && !loged) {// efetuar registo
                        loged = CSM.register(linha);
                        if (!loged) System.out.println("Não foi possível efetuar o registo.");
                        else {
                            user = linha.substring(1);
                            user = user.trim();
                            user = user.split(" +")[0];
                            System.out.println("Bem-vindo " + user + "!");
                        }
                    } else if (parametros[0].equals("4") && loged) {// pesquisa do url
                        resposta_p = CSM.urlAponta(linha);
                        if (resposta_p == null) {
                            System.out.println("Nenhum URL aponta para este URL");
                            continue;
                        }
                        for (int i = 0; i < resposta_p.size(); i++) {
                            System.out.println(i + ": " + resposta_p.get(i));
                        }
                    }else if (parametros[0].equals("5") && loged) {// efetuar registo
                        loged = false;
                        System.out.println("Até breve " + user);
                    } else {
                        System.out.println("comando não suportado");
                    }

                } catch (RemoteException e) {
                    System.out.println("O servidor encontra-se indisponível");
                } catch (Exception ex) {
                    System.out.println("Aconteceu um problema?");
                }
            }
            System.out.println("");
        }
    }

}
