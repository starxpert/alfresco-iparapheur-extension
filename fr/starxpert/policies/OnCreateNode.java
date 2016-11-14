package fr.starxpert.policies;

/**
 * Created by lepad on 02/09/16.
 */

    import fr.starxpert.utils.IparapheurExtensionService;
    import org.alfresco.model.ContentModel;
    import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
    import org.alfresco.repo.policy.JavaBehaviour;
    import org.alfresco.repo.policy.PolicyComponent;
    import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
    import org.alfresco.service.cmr.repository.ChildAssociationRef;
    import org.alfresco.service.namespace.QName;

    public class OnCreateNode implements OnCreateNodePolicy {
        private PolicyComponent policyComponent;
        private IparapheurExtensionService iparapheurExtensionService;

        public OnCreateNode() {
        }

        public void init() {
            this.policyComponent.bindClassBehaviour(QName.createQName("http://www.alfresco.org", "onCreateNode"), ContentModel.TYPE_PERSON, new JavaBehaviour(this, "onCreateNode", NotificationFrequency.TRANSACTION_COMMIT));
        }

        public void onCreateNode(ChildAssociationRef arg0) {
            this.iparapheurExtensionService.createBureau(arg0);
        }

        public void setPolicyComponent(PolicyComponent policyComponent) {
            this.policyComponent = policyComponent;
        }

        public void setIparapheurExtensionService(IparapheurExtensionService iparapheurExtensionService) {
            this.iparapheurExtensionService = iparapheurExtensionService;
        }
    }

