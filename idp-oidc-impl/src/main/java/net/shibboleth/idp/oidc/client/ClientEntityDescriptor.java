package net.shibboleth.idp.oidc.client;

import net.shibboleth.utilities.java.support.collection.LockableClassToInstanceMultiMap;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.joda.time.DateTime;
import org.opensaml.core.xml.Namespace;
import org.opensaml.core.xml.NamespaceManager;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSBooleanValue;
import org.opensaml.core.xml.util.AttributeMap;
import org.opensaml.core.xml.util.IDIndex;
import org.opensaml.saml.saml2.metadata.AdditionalMetadataLocation;
import org.opensaml.saml.saml2.metadata.AffiliationDescriptor;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml.saml2.metadata.AuthnAuthorityDescriptor;
import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.Organization;
import org.opensaml.saml.saml2.metadata.PDPDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.xmlsec.signature.Signature;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Adapts OIDC protocol service metadata onto SAML metadata.
 */
public class ClientEntityDescriptor implements EntityDescriptor {
    private String clientId;

    @Nonnull
    private LockableClassToInstanceMultiMap<Object> objectMetadata;

    /**
     * Instantiates a new client entity descriptor.
     *
     * @param clientIdentifier the client identifier
     */
    public ClientEntityDescriptor(@Nonnull final String clientIdentifier) {
        this.clientId = Constraint.isNotNull(clientIdentifier, "Client cannot be null");
        this.objectMetadata = new LockableClassToInstanceMultiMap(true);
    }

    @Override
    public String getEntityID() {
        return clientId;
    }

    @Override
    public void setEntityID(final String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getID() {
        return clientId;
    }

    @Override
    public void setID(final String newID) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Extensions getExtensions() {
        return null;
    }

    @Override
    public void setExtensions(final Extensions extensions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<RoleDescriptor> getRoleDescriptors() {
        return Collections.emptyList();
    }

    @Override
    public List<RoleDescriptor> getRoleDescriptors(final QName typeOrName) {
        return Collections.emptyList();
    }

    @Override
    public List<RoleDescriptor> getRoleDescriptors(final QName typeOrName, final String supportedProtocol) {
        return Collections.emptyList();
    }

    @Override
    public IDPSSODescriptor getIDPSSODescriptor(final String supportedProtocol) {
        return null;
    }

    @Override
    public SPSSODescriptor getSPSSODescriptor(final String supportedProtocol) {
        return null;
    }

    @Override
    public AuthnAuthorityDescriptor getAuthnAuthorityDescriptor(final String supportedProtocol) {
        return null;
    }

    @Override
    public AttributeAuthorityDescriptor getAttributeAuthorityDescriptor(final String supportedProtocol) {
        return null;
    }

    @Override
    public PDPDescriptor getPDPDescriptor(final String supportedProtocol) {
        return null;
    }

    @Override
    public AffiliationDescriptor getAffiliationDescriptor() {
        return null;
    }

    @Override
    public void setAffiliationDescriptor(final AffiliationDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Organization getOrganization() {
        return null;
    }

    @Override
    public void setOrganization(final Organization organization) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ContactPerson> getContactPersons() {
        return Collections.emptyList();
    }

    @Override
    public List<AdditionalMetadataLocation> getAdditionalMetadataLocations() {
        return Collections.emptyList();
    }

    @Override
    @Nonnull
    public AttributeMap getUnknownAttributes() {
        return null;
    }

    @Override
    public Long getCacheDuration() {
        return null;
    }

    @Override
    public void setCacheDuration(final Long duration) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nullable
    public String getSignatureReferenceID() {
        return null;
    }

    @Override
    public boolean isSigned() {
        return false;
    }

    @Override
    @Nullable
    public Signature getSignature() {
        return null;
    }

    @Override
    public void setSignature(@Nullable final Signature newSignature) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public DateTime getValidUntil() {
        return DateTime.now().plusDays(1);
    }

    @Override
    public void setValidUntil(final DateTime validUntil) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void detach() {
    }

    @Override
    @Nullable
    public Element getDOM() {
        return null;
    }

    @Override
    @Nonnull
    public QName getElementQName() {
        return new QName("http://openid.net/connect", "oidc");
    }

    @Override
    @Nonnull
    public IDIndex getIDIndex() {
        return null;
    }

    @Override
    @Nonnull
    public NamespaceManager getNamespaceManager() {
        return null;
    }

    @Override
    @Nonnull
    public Set<Namespace> getNamespaces() {
        return Collections.emptySet();
    }

    @Override
    @Nullable
    public String getNoNamespaceSchemaLocation() {
        return null;
    }

    @Override
    @Nullable
    public List<XMLObject> getOrderedChildren() {
        return Collections.emptyList();
    }

    @Override
    @Nullable
    public XMLObject getParent() {
        return null;
    }

    @Override
    @Nullable
    public String getSchemaLocation() {
        return null;
    }

    @Override
    @Nullable
    public QName getSchemaType() {
        return null;
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public boolean hasParent() {
        return false;
    }

    @Override
    public void releaseChildrenDOM(final boolean propagateRelease) {
    }

    @Override
    public void releaseDOM() {
    }

    @Override
    public void releaseParentDOM(final boolean propagateRelease) {
    }

    @Override
    @Nullable
    public XMLObject resolveID(@Nonnull final String id) {
        return null;
    }

    @Override
    @Nullable
    public XMLObject resolveIDFromRoot(@Nonnull final String id) {
        return null;
    }

    @Override
    public void setDOM(@Nullable final Element dom) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNoNamespaceSchemaLocation(@Nullable final String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setParent(@Nullable final XMLObject parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSchemaLocation(@Nullable final String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nullable
    public Boolean isNil() {
        return Boolean.valueOf(false);
    }

    @Override
    @Nullable
    public XSBooleanValue isNilXSBoolean() {
        return null;
    }

    @Override
    public void setNil(@Nullable final Boolean newNil) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNil(@Nullable final XSBooleanValue newNil) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public LockableClassToInstanceMultiMap<Object> getObjectMetadata() {
        return this.objectMetadata;
    }
}

