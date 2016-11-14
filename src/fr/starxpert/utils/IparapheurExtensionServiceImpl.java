package fr.starxpert.utils;

/**
 * Created by Valtchev Etienne on 02/09/16.
 */


        import java.util.ArrayList;
        import java.util.List;

        import com.atolcd.parapheur.repo.admin.UsersService;
        import org.alfresco.service.cmr.repository.ChildAssociationRef;
        import org.alfresco.service.cmr.repository.NodeRef;
        import org.alfresco.service.cmr.security.AuthenticationService;
        import org.apache.log4j.Logger;

public class IparapheurExtensionServiceImpl implements IparapheurExtensionService {

    private AuthenticationService authenticationService;
    private String urlAlfresco;
    private String listMetaToSearch;
    private String triggerModule;
    private NodeRef user;

    public void setUsersService(UsersService usersService) {
        this.usersService = usersService;
    }

    private UsersService usersService;


    private final Logger logger = Logger.getLogger(IparapheurExtensionServiceImpl.class);


    public void createBureau(ChildAssociationRef childAssociationRef) {
        //l'utilisateur system est utilisé par le ticket alfresco pour la création des utilisateurs et bureaux
        if (!usersService.isAdministrateur("system")) {
            usersService.addToAdminGroup("system");
        }
        if (triggerModule.compareTo("true") == 0) {
            logger.info("creation de bureau déclanchée");
            NodeRef user = childAssociationRef.getChildRef();
            this.user = user;
            String alfTicket = authenticationService.getCurrentTicket();
            List<String> listMeta = new ArrayList<String>();

            logger.debug("id user : " + user.getId());

            if (listMetaToSearch != null && listMetaToSearch != "") {
                listMeta = getMetaDatas(listMetaToSearch);
            }
            try {
                // création d'un nouveau thread pour ne pas faire un time out sur la transaction en cours
                Thread testConnect = new Thread(new HttpService(this.urlAlfresco, listMeta, alfTicket, user.getId()));
                testConnect.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private List<String> getMetaDatas(String concatMeta) {
        String[] tablMeta = concatMeta.split(",");
        ArrayList list = new ArrayList();

        for(int i = 0; i < tablMeta.length; ++i) {
            list.add(tablMeta[i]);
        }

        return list;
    }
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    public void setUrlAlfresco(String urlAlfresco) {
        this.urlAlfresco = urlAlfresco;
    }
    public void setListMetaToSearch(String listMetaToSearch) {
        this.listMetaToSearch = listMetaToSearch;
    }
    public void setTriggerModule(String triggerModule) {
        this.triggerModule = triggerModule;
    }

}
