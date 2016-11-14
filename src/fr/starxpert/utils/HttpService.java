package fr.starxpert.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Valtchev Etienne on 07/11/16.
 */
public class HttpService extends Thread{

    private String url;
    private String alfTicket;
    private List<String> listMetaToSearch;
    private final Logger logger = Logger.getLogger(HttpService.class);
    private String idUser;

    public HttpService(String url, List<String> listMetaToSearch, String alfTicket, String idUser){
        this.url = url;
        this.idUser = idUser;
        this.alfTicket = alfTicket;
        this.listMetaToSearch = listMetaToSearch;
    }

    private void createBureau(String userJson, String metaDataJson){
        String userName;
        String firstName;
        String lastName;
        JSONObject obj;
        try {
            obj = new JSONObject(userJson);
            userName = obj.getString("username");
            if (obj.has("firstName"))
                firstName = obj.getString("firstName");
            else
                firstName = userName;
            if (obj.has("lastName"))
                lastName = obj.getString("lastName");
            else
                lastName = userName;
            logger.debug("création du bureau pour l'utilisateur"+userName);
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost message = new HttpPost(this.url + "/service/parapheur/bureaux?alf_ticket="+this.alfTicket);
            logger.debug("requete : "+this.url + "/service/parapheur/bureaux?alf_ticket="+this.alfTicket);
            logger.debug("{\"name\":\"" + userName + "\",\"title\":\"" + firstName + "\",\"description\":\"" + lastName + "\",\"proprietaires\":["+userJson+"],"+metaDataJson+"}");
            StringEntity requestEntity = new StringEntity(
                    "{\"name\":\"" + userName + "\",\"title\":\"" + firstName + "\",\"description\":\"" + lastName + "\",\"proprietaires\":["+userJson+"],"+metaDataJson+"}",
                    "application/json",
                    "UTF-8");
            message.setEntity(requestEntity);
            HttpResponse response = httpClient.execute(message);
            logger.info("appel bureau : "+response.getStatusLine().toString());
            HttpEntity entity = response.getEntity();

            if (response.getStatusLine().toString().contains("200")){
                logger.debug("création du bureau réussie");
            }else {
                logger.debug(EntityUtils.toString(entity));
            }
            EntityUtils.consume(entity);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private String getUserJson(){

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet message = new HttpGet(this.url + "/service/parapheur/utilisateurs/"+idUser+"?alf_ticket="+this.alfTicket);
        String result = null;
        try {
            logger.debug("récupération des données de l'utilisateur");
            HttpResponse response = httpClient.execute(message);
            logger.info("appel utilisateurs : "+response.getStatusLine().toString());

            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity);
            logger.debug(result);
            EntityUtils.consume(entity);
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public void run(){
        try {
            //attente de 5 secondes car les infos utilisateurs ne sont parfois pas encore disponibles à l'appel du webscript utilisateurs
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String userJson = getUserJson();
        String metadataJson = createJsonMetadata();
        createBureau(userJson, metadataJson);
    }

    public String createJsonMetadata(){
        JSONArray objListMeta = new JSONArray();

        for (String metaData : this.listMetaToSearch){
            objListMeta.put(metaData);
        }
        String view = "\"metadatas-visibility\":"+objListMeta.toString();
        return view;
    }
}
