package fr.starxpert.utils;

/**
 * Created by lepad on 02/09/16.
 */

        import com.atolcd.parapheur.model.ParapheurModel;
        import com.atolcd.parapheur.repo.ParapheurService;
        import java.io.Serializable;
        import java.util.ArrayList;
        import java.util.Date;
        import java.util.HashMap;
        import java.util.Iterator;
        import java.util.List;
        import java.util.Map;
        import java.util.Properties;
        import java.util.Map.Entry;
        import org.alfresco.model.ApplicationModel;
        import org.alfresco.model.ContentModel;
        import org.alfresco.repo.security.permissions.AccessDeniedException;
        import org.alfresco.service.cmr.dictionary.DictionaryService;
        import org.alfresco.service.cmr.dictionary.TypeDefinition;
        import org.alfresco.service.cmr.model.FileFolderService;
        import org.alfresco.service.cmr.model.FileInfo;
        import org.alfresco.service.cmr.repository.AssociationRef;
        import org.alfresco.service.cmr.repository.ChildAssociationRef;
        import org.alfresco.service.cmr.repository.NodeRef;
        import org.alfresco.service.cmr.repository.NodeService;
        import org.alfresco.service.cmr.repository.StoreRef;
        import org.alfresco.service.cmr.search.QueryParameterDefinition;
        import org.alfresco.service.cmr.search.SearchService;
        import org.alfresco.service.cmr.security.AuthenticationService;
        import org.alfresco.service.cmr.security.AuthorityService;
        import org.alfresco.service.cmr.security.AuthorityType;
        import org.alfresco.service.namespace.NamespaceService;
        import org.alfresco.service.namespace.QName;
        import org.apache.log4j.Level;
        import org.apache.log4j.Logger;
        import org.springframework.util.Assert;

public class IparapheurExtensionServiceImpl implements IparapheurExtensionService {
    private NodeService nodeService;
    private String listMetaToSearch;
    private ParapheurService parapheurService;
    private SearchService searchService;
    private Properties configuration;
    private NamespaceService namespaceService;
    private FileFolderService fileFolderService;
    private AuthenticationService authenticationService;
    private AuthorityService authorityService;
    private DictionaryService dictionaryService;
    private final Logger logger = Logger.getLogger(IparapheurExtensionServiceImpl.class);

    public IparapheurExtensionServiceImpl() {
    }

    public void createBureau(ChildAssociationRef childAssociationRef) {
        NodeRef user = childAssociationRef.getChildRef();
        ArrayList listUsers = new ArrayList();
        String userName = this.getUserName(user);
        String firstName = this.getFirstName(user);
        String lastName = this.getLastName(user);
        listUsers.add(userName);
        HashMap properties = new HashMap();
        HashMap assocs = new HashMap();
        this.logger.debug("=============================================");
        this.logger.info("création du parapheur si besoin pour l\'utilisateur " + userName);
        List parapheurs = this.parapheurService.getParapheursFromName(userName);
        if(parapheurs != null) {
            this.logger.debug("le parapheur existe déja, abandon");
        } else {
            this.logger.debug("parapheur non trouvé, création ...");
            NodeRef nodeRef = this.createParapheur(userName, lastName + " " + firstName, "bureau de " + firstName + " " + lastName);
            this.logger.debug("id du noeud du parapheur créé : " + nodeRef.getId());
            properties.put(ContentModel.PROP_NAME, userName);
            properties.put(ContentModel.PROP_TITLE, lastName + " " + firstName);
            properties.put(ContentModel.PROP_DESCRIPTION, "bureau de " + firstName + " " + lastName);
            properties.put(ParapheurModel.PROP_PROPRIETAIRES_PARAPHEUR, (Serializable)listUsers);
            properties.put(ParapheurModel.PROP_VISIBILITE_METADONNE, (Serializable)this.getMetaDatas(this.listMetaToSearch));
            this.logger.debug("on update le parapheur avec les données de l\'utilisateur et les métadonnées visibles");
            Boolean ok = Boolean.valueOf(this.updateBureau(nodeRef, properties, assocs));
            this.logger.debug("réussite ? : " + ok);
        }

    }

    public NodeRef createParapheur(Map<QName, Serializable> properties) {
        String nom = (String)properties.get(ContentModel.PROP_NAME);
        String xpath = this.configuration.getProperty("spaces.company_home.childname") + "/" + this.configuration.getProperty("spaces.parapheurs.childname");

        List results;
        try {
            results = this.searchService.selectNodes(this.nodeService.getRootNode(new StoreRef(this.configuration.getProperty("spaces.store"))), xpath, (QueryParameterDefinition[])null, this.namespaceService, false);
        } catch (AccessDeniedException var10) {
            if(this.logger.isEnabledFor(Level.WARN)) {
                this.logger.warn("Erreur lors de l\'accès au répertoire de stockage des parapheurs");
            }

            return null;
        }

        if(results != null && results.size() == 1) {
            NodeRef parapheursRep = (NodeRef)results.get(0);
            FileInfo fileInfo = this.fileFolderService.create(parapheursRep, nom, ParapheurModel.TYPE_PARAPHEUR);
            NodeRef parapheur = fileInfo.getNodeRef();
            HashMap props = new HashMap();
            props.put(ContentModel.PROP_CREATOR, this.authenticationService.getCurrentUserName());
            props.put(ContentModel.PROP_CREATED, new Date());
            props.putAll(properties);
            this.nodeService.setProperties(parapheur, props);
            HashMap proprietesC = new HashMap();
            proprietesC.put(ParapheurModel.PROP_CHILD_COUNT, Integer.valueOf(0));
            proprietesC.put(ContentModel.PROP_NAME, "Dossiers à transmettre");
            proprietesC.put(ContentModel.PROP_TITLE, "Dossiers à transmettre");
            proprietesC.put(ContentModel.PROP_DESCRIPTION, "Dossiers en cours de préparation ou prêts à être émis.");
            this.nodeService.createNode(parapheur, ContentModel.ASSOC_CONTAINS, ParapheurModel.NAME_EN_PREPARATION, ParapheurModel.TYPE_CORBEILLE, proprietesC);
            proprietesC.put(ContentModel.PROP_NAME, "Dossiers à traiter");
            proprietesC.put(ContentModel.PROP_TITLE, "Dossiers à traiter");
            proprietesC.put(ContentModel.PROP_DESCRIPTION, "Dossiers à viser ou à signer.");
            this.nodeService.createNode(parapheur, ContentModel.ASSOC_CONTAINS, ParapheurModel.NAME_A_TRAITER, ParapheurModel.TYPE_CORBEILLE, proprietesC);
            proprietesC.put(ContentModel.PROP_NAME, "Dossiers en fin de circuit");
            proprietesC.put(ContentModel.PROP_TITLE, "Dossiers en fin de circuit");
            proprietesC.put(ContentModel.PROP_DESCRIPTION, "Dossiers ayant terminé leur circuit de validation.");
            proprietesC.put(ApplicationModel.PROP_ICON, "space-icon-default");
            this.nodeService.createNode(parapheur, ContentModel.ASSOC_CONTAINS, ParapheurModel.NAME_A_ARCHIVER, ParapheurModel.TYPE_CORBEILLE, proprietesC);
            proprietesC.put(ContentModel.PROP_NAME, "Dossiers retournés");
            proprietesC.put(ContentModel.PROP_TITLE, "Dossiers retournés");
            proprietesC.put(ContentModel.PROP_DESCRIPTION, "Dossiers rejetés lors du circuit de signature/visa.");
            this.nodeService.createNode(parapheur, ContentModel.ASSOC_CONTAINS, ParapheurModel.NAME_RETOURNES, ParapheurModel.TYPE_CORBEILLE, proprietesC);
            proprietesC.put(ContentModel.PROP_NAME, "Dossiers en cours");
            proprietesC.put(ContentModel.PROP_TITLE, "Dossiers en cours");
            proprietesC.put(ContentModel.PROP_DESCRIPTION, "Dossiers qui ont été émis.");
            this.nodeService.createNode(parapheur, ContentModel.ASSOC_CONTAINS, ParapheurModel.NAME_EN_COURS, ParapheurModel.TYPE_CORBEILLE_VIRTUELLE, proprietesC);
            proprietesC.put(ContentModel.PROP_NAME, "Dossiers traités");
            proprietesC.put(ContentModel.PROP_TITLE, "Dossiers traités");
            proprietesC.put(ContentModel.PROP_DESCRIPTION, "Dossiers qui ont été traités.");
            this.nodeService.createNode(parapheur, ContentModel.ASSOC_CONTAINS, ParapheurModel.NAME_TRAITES, ParapheurModel.TYPE_CORBEILLE_VIRTUELLE, proprietesC);
            proprietesC.put(ContentModel.PROP_NAME, "Dossiers à venir");
            proprietesC.put(ContentModel.PROP_TITLE, "Dossiers à venir");
            proprietesC.put(ContentModel.PROP_DESCRIPTION, "Dossiers à venir");
            this.nodeService.createNode(parapheur, ContentModel.ASSOC_CONTAINS, ParapheurModel.NAME_A_VENIR, ParapheurModel.TYPE_CORBEILLE_VIRTUELLE, proprietesC);
            proprietesC.put(ContentModel.PROP_NAME, "Dossiers récupérables");
            proprietesC.put(ContentModel.PROP_TITLE, "Dossiers récupérables");
            proprietesC.put(ContentModel.PROP_DESCRIPTION, "Dossiers sur lesquels on peut exercer un droit de remords.");
            this.nodeService.createNode(parapheur, ContentModel.ASSOC_CONTAINS, ParapheurModel.NAME_RECUPERABLES, ParapheurModel.TYPE_CORBEILLE_VIRTUELLE, proprietesC);
            proprietesC.put(ContentModel.PROP_NAME, "Dossiers à relire - annoter");
            proprietesC.put(ContentModel.PROP_TITLE, "Dossiers à relire - annoter");
            proprietesC.put(ContentModel.PROP_DESCRIPTION, "Dossiers envoyés au secrétariat pour relecture - annotation.");
            this.nodeService.createNode(parapheur, ContentModel.ASSOC_CONTAINS, ParapheurModel.NAME_SECRETARIAT, ParapheurModel.TYPE_CORBEILLE, proprietesC);
            proprietesC.put(ContentModel.PROP_NAME, "Dossiers en retard");
            proprietesC.put(ContentModel.PROP_TITLE, "Dossiers en retard");
            proprietesC.put(ContentModel.PROP_DESCRIPTION, "Dossiers en retard");
            this.nodeService.createNode(parapheur, ContentModel.ASSOC_CONTAINS, ParapheurModel.NAME_EN_RETARD, ParapheurModel.TYPE_CORBEILLE_VIRTUELLE, proprietesC);
            proprietesC.put(ContentModel.PROP_NAME, "Dossiers à imprimer");
            proprietesC.put(ContentModel.PROP_TITLE, "Dossiers à imprimer");
            proprietesC.put(ContentModel.PROP_DESCRIPTION, "Dossiers à imprimer");
            this.nodeService.createNode(parapheur, ContentModel.ASSOC_CONTAINS, ParapheurModel.NAME_A_IMPRIMER, ParapheurModel.TYPE_CORBEILLE_VIRTUELLE, proprietesC);
            proprietesC.put(ContentModel.PROP_NAME, "Dossiers en délégation");
            proprietesC.put(ContentModel.PROP_TITLE, "Dossiers en délégation");
            proprietesC.put(ContentModel.PROP_DESCRIPTION, "Dossiers ayant été délégués par d\'autres bureaux.");
            this.nodeService.createNode(parapheur, ContentModel.ASSOC_CONTAINS, ParapheurModel.NAME_DOSSIERS_DELEGUES, ParapheurModel.TYPE_CORBEILLE_VIRTUELLE, proprietesC);
            this.authorityService.createAuthority(AuthorityType.ROLE, "PHOWNER_" + parapheur.getId());
            this.authorityService.createAuthority(AuthorityType.ROLE, "PHDELEGATES_" + parapheur.getId());
            return parapheur;
        } else {
            if(this.logger.isEnabledFor(Level.WARN)) {
                this.logger.warn("Erreur lors de la récupération du répertoire de stockage des parapheurs");
            }

            return null;
        }
    }

    public boolean updateBureau(NodeRef bureau, HashMap<QName, Serializable> propertiesMap, HashMap<QName, Serializable> assocsMap) {
        boolean habilitationsChanged = false;
        boolean habilitationsEnabled = false;
        HashMap habilitations = new HashMap();
        boolean hasAspect = this.nodeService.hasAspect(bureau, ParapheurModel.ASPECT_HABILITATIONS);
        habilitations.put(ParapheurModel.PROP_HAB_ARCHIVAGE, hasAspect?this.nodeService.getProperty(bureau, ParapheurModel.PROP_HAB_ARCHIVAGE):Boolean.valueOf(false));
        habilitations.put(ParapheurModel.PROP_HAB_SECRETARIAT, hasAspect?this.nodeService.getProperty(bureau, ParapheurModel.PROP_HAB_SECRETARIAT):Boolean.valueOf(false));
        habilitations.put(ParapheurModel.PROP_HAB_TRAITER, hasAspect?this.nodeService.getProperty(bureau, ParapheurModel.PROP_HAB_TRAITER):Boolean.valueOf(false));
        habilitations.put(ParapheurModel.PROP_HAB_TRANSMETTRE, hasAspect?this.nodeService.getProperty(bureau, ParapheurModel.PROP_HAB_TRANSMETTRE):Boolean.valueOf(false));

        Iterator i$;
        Entry entry;
        for(i$ = propertiesMap.entrySet().iterator(); i$.hasNext(); this.nodeService.setProperty(bureau, (QName)entry.getKey(), (Serializable)entry.getValue())) {
            entry = (Entry)i$.next();
            if(((QName)entry.getKey()).equals(ParapheurModel.PROP_PROPRIETAIRES_PARAPHEUR)) {
                this.updateOwnerPermission(bureau, (List)entry.getValue());
            }

            if(((QName)entry.getKey()).equals(ParapheurModel.PROP_SECRETAIRES)) {
                this.updateSecretariatPermission(bureau, (List)entry.getValue());
            }

            if(((QName)entry.getKey()).equals(ParapheurModel.ASPECT_HABILITATIONS)) {
                if(this.nodeService.hasAspect(bureau, ParapheurModel.ASPECT_HABILITATIONS)) {
                    this.nodeService.removeAspect(bureau, ParapheurModel.ASPECT_HABILITATIONS);
                }

                habilitationsEnabled = ((Boolean)entry.getValue()).booleanValue();
            }

            if(((QName)entry.getKey()).equals(ParapheurModel.PROP_HAB_ARCHIVAGE) || ((QName)entry.getKey()).equals(ParapheurModel.PROP_HAB_SECRETARIAT) || ((QName)entry.getKey()).equals(ParapheurModel.PROP_HAB_TRAITER) || ((QName)entry.getKey()).equals(ParapheurModel.PROP_HAB_TRANSMETTRE)) {
                habilitationsChanged = true;
                habilitations.put(entry.getKey(), entry.getValue());
            }
        }

        if(habilitationsChanged || habilitationsEnabled) {
            this.nodeService.addAspect(bureau, ParapheurModel.ASPECT_HABILITATIONS, habilitations);
        }

        i$ = assocsMap.entrySet().iterator();

        while(true) {
            NodeRef superieur;
            do {
                do {
                    do {
                        if(!i$.hasNext()) {
                            return true;
                        }

                        entry = (Entry)i$.next();
                    } while(!((QName)entry.getKey()).equals(ParapheurModel.ASSOC_HIERARCHIE));

                    superieur = this.getParapheurResponsable(bureau);
                    if(superieur != null) {
                        this.nodeService.removeAssociation(bureau, superieur, ParapheurModel.ASSOC_HIERARCHIE);
                    }
                } while(entry.getValue() == null);
            } while("".equals(entry.getValue()));

            for(superieur = this.getParapheurResponsable(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.toString() + "/" + entry.getValue())); superieur != null; superieur = this.getParapheurResponsable(superieur)) {
                if(superieur.equals(bureau)) {
                    return false;
                }
            }

            this.nodeService.createAssociation(bureau, new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.toString() + "/" + entry.getValue()), ParapheurModel.ASSOC_HIERARCHIE);
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

    public NodeRef getParapheurResponsable(NodeRef parapheur) {
        Assert.isTrue(this.isParapheur(parapheur), "NodeRef doit être un ph:parapheur");
        List hierarchie = this.nodeService.getTargetAssocs(parapheur, ParapheurModel.ASSOC_HIERARCHIE);
        return hierarchie.size() == 1?((AssociationRef)hierarchie.get(0)).getTargetRef():null;
    }

    public boolean isParapheur(NodeRef nodeRef) {
        return this.isOfType(nodeRef, ParapheurModel.TYPE_PARAPHEUR);
    }

    private boolean isOfType(NodeRef nodeRef, QName type) {
        Assert.notNull(nodeRef, "Node Ref is mandatory");
        Assert.notNull(this.nodeService, "NodeService is mandatory");
        Assert.isTrue(this.nodeService.exists(nodeRef), "Node Ref must exist in the repository");
        Assert.notNull(type, "Type is mandatory");
        QName type2 = this.nodeService.getType(nodeRef);
        TypeDefinition typeDef = this.dictionaryService.getType(type2);
        if(typeDef == null) {
            if(this.logger.isEnabledFor(Level.WARN)) {
                this.logger.warn("Found invalid object in database: id = " + nodeRef + ", type = " + type2);
            }

            return false;
        } else {
            return this.dictionaryService.isSubClass(type2, type);
        }
    }

    private String getUserName(NodeRef user) {
        Serializable userName = this.nodeService.getProperty(user, ContentModel.PROP_USERNAME);
        return userName != null && userName != ""?userName.toString():"";
    }

    private String getFirstName(NodeRef user) {
        Serializable firstName = this.nodeService.getProperty(user, ContentModel.PROP_FIRSTNAME);
        return firstName != null && firstName != ""?firstName.toString():"";
    }

    private String getLastName(NodeRef user) {
        Serializable lastName = this.nodeService.getProperty(user, ContentModel.PROP_LASTNAME);
        return lastName != null && lastName != ""?lastName.toString():"";
    }

    public NodeRef createParapheur(String name, String title, String description) {
        HashMap properties = new HashMap();
        properties.put(ContentModel.PROP_NAME, name);
        properties.put(ContentModel.PROP_TITLE, title);
        properties.put(ContentModel.PROP_DESCRIPTION, description);
        properties.put(ParapheurModel.PROP_SECRETAIRES, new ArrayList());
        properties.put(ParapheurModel.PROP_PROPRIETAIRES_PARAPHEUR, new ArrayList());
        return this.createParapheur(properties);
    }

    public void updateOwnerPermission(NodeRef bureau, List<String> newOwners) {
        ArrayList previousOwners = (ArrayList)this.nodeService.getProperty(bureau, ParapheurModel.PROP_PROPRIETAIRES_PARAPHEUR);
        this.updateAuthorityChildren(bureau, previousOwners, newOwners, "PHOWNER_");
    }

    private void updateAuthorityChildren(NodeRef bureau, List<String> previousChildren, List<String> newChildren, String shortName) {
        String roleName = "ROLE_" + shortName;
        if(!this.authorityService.authorityExists(roleName + bureau.getId())) {
            this.authorityService.createAuthority(AuthorityType.ROLE, shortName + bureau.getId());
        }

        ArrayList childrenToAdd;
        if(newChildren != null) {
            childrenToAdd = new ArrayList(newChildren);
        } else {
            childrenToAdd = new ArrayList();
        }

        if(previousChildren != null) {
            childrenToAdd.removeAll(previousChildren);
        }

        ArrayList childrenToRemove;
        if(previousChildren != null) {
            childrenToRemove = new ArrayList(previousChildren);
        } else {
            childrenToRemove = new ArrayList();
        }

        if(newChildren != null) {
            childrenToRemove.removeAll(newChildren);
        }

        Iterator i$;
        String childToAdd;
        if(!childrenToRemove.isEmpty()) {
            i$ = childrenToRemove.iterator();

            while(i$.hasNext()) {
                childToAdd = (String)i$.next();
                if(!childToAdd.isEmpty()) {
                    this.authorityService.removeAuthority(roleName + bureau.getId(), this.authorityService.getName(AuthorityType.USER, childToAdd));
                }
            }
        }

        if(!childrenToAdd.isEmpty()) {
            i$ = childrenToAdd.iterator();

            while(i$.hasNext()) {
                childToAdd = (String)i$.next();
                if(!childToAdd.isEmpty()) {
                    this.authorityService.addAuthority(roleName + bureau.getId(), this.authorityService.getName(AuthorityType.USER, childToAdd));
                }
            }
        }

    }

    public void updateSecretariatPermission(NodeRef bureau, List<String> newSecretariat) {
        ArrayList previousSecretariat = (ArrayList)this.nodeService.getProperty(bureau, ParapheurModel.PROP_SECRETAIRES);
        this.updateAuthorityChildren(bureau, previousSecretariat, newSecretariat, "PHSECRETARIAT_");
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setListMetaToSearch(String listMetaToSearch) {
        this.listMetaToSearch = listMetaToSearch;
    }

    public void setParapheurService(ParapheurService parapheurService) {
        this.parapheurService = parapheurService;
    }

    public void setConfiguration(Properties properties) {
        this.configuration = properties;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public void setDictionaryService(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }
}
