package v1;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

/*
Main function for initializing the server client and site.
 */


public class TwitterRunner {

    public static void main(String[] args) throws IOException
    {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Please enter total number of processes: ");

        int numProcess = scanner.nextInt();

        System.out.print("Please Enter ID: ");

        int id = scanner.nextInt();


        ObjectMapper mapper = new ObjectMapper();
        File from = new File("config.txt");
        TypeReference<HashMap<Integer,String>> typeRef
                = new TypeReference<HashMap<Integer,String>>() {};

        HashMap<Integer,String> ips = mapper.readValue(from, typeRef);

        File f = new File("site.json");
        Site site;
        if(f.exists() && !f.isDirectory()) {
            System.out.println("site.json file already exists, recovering data!");
            ObjectMapper siteMapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            site = siteMapper.readValue(f, Site.class);
        }else{
            site = new Site(numProcess, id);
        }

        startServer(id, ips, site);
        startClientGUI(id, ips, site);
    }

    public static void startServer(int id, HashMap<Integer,String> ips, Site s)
    {
        (new Thread() {
            @Override
            public void run() 
            {
 
                new TwitterServer(id, ips, s).run();
            }
        }).start();
    }
    public static void startClientGUI(int id, HashMap<Integer,String> ips, Site s)
    {
        (new Thread() {
            @Override
            public void run() 
            {
                try {
                    new TwitterClientGUI(id, ips, s).run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}