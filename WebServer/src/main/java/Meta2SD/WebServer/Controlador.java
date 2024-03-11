package Meta2SD.WebServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import Meta1.ClientSearchModule;
import Meta1.Url;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

@Controller
public class Controlador {

    @GetMapping("/")
    public String inicio() {
        return "homePage";
    }
    
    
     @GetMapping("/ResultadosAponta")
    public String apontaURL(HttpSession sessao, Model model) {
        ClientSearchModule CSM = null;
        ArrayList<String> resultado = null;
        try {
            CSM = (ClientSearchModule) LocateRegistry.getRegistry(7001).lookup("SearchModule");
        } catch (Exception e) {
            System.out.println("O servidor encontra-se indisponível");
            return "PaginaErro";
        }
        try {
            resultado = CSM.urlAponta("1 " + sessao.getAttribute("contexto"));
        } catch (Exception ex) {
            return "PaginaErro";
        }
        model.addAttribute("contexto", resultado);
        return "URLaponta";
    }

    @PostMapping("/")
    public String infoPesquisa(HttpSession sessao, @ModelAttribute GenericString cenas) {
        String palavras = cenas.getGenericString();
        sessao.setAttribute("contexto", palavras);
        return ("redirect:/Pesquisa");
    }
    
    
    @GetMapping("/apontaURL")
    public String paginaPesquisaURL() {
        return "searchURLaponta";
    }
    
     @PostMapping("/apontaURL")
    public String dumpInfoapontaURL(HttpSession sessao, @ModelAttribute GenericString cenas) {
        String palavras = cenas.getGenericString();
        sessao.setAttribute("contexto", palavras);
        return ("redirect:/ResultadosAponta");
    }
    
    
    

    @GetMapping("/Pesquisa")
    public String pesquisa(HttpSession sessao, Model model) {
        ClientSearchModule CSM = null;
        ArrayList<Url> resultado = null;
        try {
            CSM = (ClientSearchModule) LocateRegistry.getRegistry(7001).lookup("SearchModule");
        } catch (Exception e) {
            System.out.println("O servidor encontra-se indisponível");
            return "PaginaErro";
        }
        try {
            resultado = CSM.pesquisa("1 " + sessao.getAttribute("contexto"));
        } catch (Exception ex) {
            return "PaginaErro";
        }
        model.addAttribute("contexto", resultado);

        return "Pesquisa10"; // Return the desired view name
    }
  

    @GetMapping("/IndexURL")
    public String indexador() {
        return "indexarURL";
    }

    @PostMapping("/IndexURL")
    public String fazerindex(HttpSession sessao, @ModelAttribute GenericString cenas) {
        String palavras = cenas.getGenericString();
        System.out.println(palavras);

        ClientSearchModule CSM = null;
        try {
            CSM = (ClientSearchModule) LocateRegistry.getRegistry(7001).lookup("SearchModule");
        } catch (Exception e) {
            System.out.println("O servidor encontra-se indisponível");
            return "PaginaErro";
        }
        try {
            CSM.addURL(palavras);
            System.out.println("URL adicionado");
        } catch (Exception ex) {
            System.out.println("Aconteceu um erro ao indexar");
        }

        System.out.println("URL adicionado");
        return ("redirect:/");
    }

    @GetMapping("/sysDetails")
    public String detalhesSistema() {
        // Return the correct template name
        return "SystemInfo";
    }

    @PostMapping("/hackernewstopstories")
    private String hackerNewsTopStories(@ModelAttribute GenericString cenas) {

        String search = cenas.getGenericString();

        String topStoriesEndpoint = "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";

        RestTemplate restTemplate = new RestTemplate();
        List hackerNewsNewTopStories = restTemplate.getForObject(topStoriesEndpoint, List.class);

        assert hackerNewsNewTopStories != null;

        int stories = 0;
        for (int i = 0; i < hackerNewsNewTopStories.size(); i++) {
            Integer storyId = (Integer) hackerNewsNewTopStories.get(i);

            String storyItemDetailsEndpoint = String.format("https://hacker-news.firebaseio.com/v0/item/%s.json?print=pretty", storyId);
            HackerNewsItemRecord hackerNewsItemRecord = restTemplate.getForObject(storyItemDetailsEndpoint, HackerNewsItemRecord.class);

            if (hackerNewsItemRecord == null || hackerNewsItemRecord.url() == null || hackerNewsItemRecord.text() == null) {
                continue;
            }

            if (search != null) {

                List<String> searchTermsList = List.of(search.toLowerCase().split(" "));
                if (searchTermsList.stream().anyMatch(hackerNewsItemRecord.text().toLowerCase()::contains)) {
                    ClientSearchModule CSM = null;
                    try {
                        CSM = (ClientSearchModule) LocateRegistry.getRegistry(7001).lookup("SearchModule");
                    } catch (Exception e) {
                        System.out.println("O servidor encontra-se indisponível");
                        return "PaginaErro";
                    }
                    try {
                        CSM.addURL(hackerNewsItemRecord.url());
                        System.out.println("URL adicionado");
                    } catch (Exception ex) {
                        System.out.println("Aconteceu um erro ao indexar");
                    }
                }
                stories++;
                if (stories == 10) {
                    break;
                }

            }
        }
        return ("redirect:/");
    }
    
    
    
    
    @PostMapping("/tuasStories")
    private String tuasStories(@ModelAttribute GenericString cenas) {
        
        String search = cenas.getGenericString();

        String StoriesEndpoint = String.format("https://hacker-news.firebaseio.com/v0/user/%s.json?print=pretty", search);

        RestTemplate restTemplate = new RestTemplate();
        
        HackerNewsUserRecord oUser=null;
        
        try{
            oUser = restTemplate.getForObject(StoriesEndpoint, HackerNewsUserRecord.class);
        }catch(Exception ex){
            return "PaginaErro";
        }
       if(oUser==null){
           return ("redirect:/");
       }

        List stories = oUser.submitted();

        for (int i = 0; i < stories.size(); i++) { // Iterate only through 50 of them...
            Integer storyId = (Integer) stories.get(i);

            String storyItemDetailsEndpoint = String.format("https://hacker-news.firebaseio.com/v0/item/%s.json?print=pretty", storyId);
            HackerNewsItemRecord hackerNewsItemRecord = restTemplate.getForObject(storyItemDetailsEndpoint, HackerNewsItemRecord.class);

            if (hackerNewsItemRecord == null || hackerNewsItemRecord.url() == null) {
                continue;
            }

            ClientSearchModule CSM = null;
            try {
                CSM = (ClientSearchModule) LocateRegistry.getRegistry(7001).lookup("SearchModule");
            } catch (Exception e) {
                System.out.println("O servidor encontra-se indisponível");
                return "PaginaErro";
            }
            try {
                CSM.addURL(hackerNewsItemRecord.url());
                System.out.println("URL adicionado");
            } catch (Exception ex) {
                System.out.println("Aconteceu um erro ao indexar");
            }

        }
        return ("redirect:/");
    }
    
}
