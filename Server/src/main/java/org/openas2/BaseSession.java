package org.openas2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openas2.cert.CertificateFactory;
import org.openas2.lib.message.AS2Standards;
import org.openas2.partner.PartnershipFactory;
import org.openas2.processor.Processor;
import org.openas2.processor.ProcessorModule;
import org.openas2.processor.receiver.DirectoryPollingModule;
import org.openas2.util.Properties;
import org.openas2.util.XMLUtil;
import org.w3c.dom.Node;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import java.util.HashMap;
import java.util.Map;


public abstract class BaseSession implements Session {
    private Map<String, Component> components = new HashMap<String, Component>();
    private String baseDirectory;

    protected static final Log LOGGER = LogFactory.getLog(XMLSession.class.getSimpleName());

    private Map<String, Map<String, Object>> polledDirectories = new HashMap<String, Map<String, Object>>();

    /**
     * Creates a <code>BaseSession</code> object, then calls the <code>init()</code> method.
     *
     * @throws OpenAS2Exception - - Houston we have a problem
     * @see #init()
     */
    public BaseSession() throws OpenAS2Exception {
        init();
    }

    @Override
    public void start() throws OpenAS2Exception {
        getProcessor().startActiveModules();
    }

    @Override
    public void stop() throws Exception {
        for (Component component : components.values()) {
            component.destroy();
        }
    }

    public CertificateFactory getCertificateFactory() throws ComponentNotFoundException {
        return (CertificateFactory) getComponent(CertificateFactory.COMPID_CERTIFICATE_FACTORY);
    }

    public Map<String, Map<String, Object>> getPolledDirectories() {
        return polledDirectories;
    }

    public void setPolledDirectories(Map<String, Map<String, Object>> polledDirectories) {
        this.polledDirectories = polledDirectories;
    }

    /**
     * Registers a component to a specified ID.
     *
     * @param componentID registers the component to this ID
     * @param comp        component to register
     * @see Component
     */
    public void setComponent(String componentID, Component comp) {
        Map<String, Component> objects = getComponents();
        objects.put(componentID, comp);
    }

    public Component getComponent(String componentID) throws ComponentNotFoundException {
        Map<String, Component> comps = getComponents();
        Component comp = comps.get(componentID);

        if (comp == null) {
            throw new ComponentNotFoundException(componentID);
        }

        return comp;
    }

    public Map<String, Component> getComponents() {
        return components;
    }

    public PartnershipFactory getPartnershipFactory() throws ComponentNotFoundException {
        return (PartnershipFactory) getComponent(PartnershipFactory.COMPID_PARTNERSHIP_FACTORY);
    }

    public Processor getProcessor() throws ComponentNotFoundException {
        return (Processor) getComponent(Processor.COMPID_PROCESSOR);
    }

    /**
     * This method is called by the <code>BaseSession</code> constructor to set up any global
     * configuration settings.
     *
     * @throws OpenAS2Exception If an error occurs while initializing systems
     */
    protected void init() throws OpenAS2Exception {
        initJavaMail();
    }

    /**
     * Adds a group of content handlers to the Mailcap <code>CommandMap</code>. These handlers are
     * used by the JavaMail API to encode and decode information of specific mime types.
     *
     * @throws OpenAS2Exception If an error occurs while initializing mime types
     */
    private void initJavaMail() throws OpenAS2Exception {
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap(AS2Standards.DISPOSITION_TYPE + ";; x-java-content-handler=org.openas2.lib.util.javamail.DispositionDataContentHandler");
        CommandMap.setDefaultCommandMap(mc);
    }

    private void checkPollerModuleConfig(String pollerDir) throws OpenAS2Exception {
        if (polledDirectories.containsKey(pollerDir)) {
            Map<String, Object> meta = polledDirectories.get(pollerDir);
            throw new OpenAS2Exception("Directory already being polled from config in " + meta.get("configSource") + " for the " + meta.get("partnershipName") + " partnership: " + pollerDir);
        }
    }

    private void trackPollerModule(String pollerDir, String partnershipName, String configSource, ProcessorModule pollerInstance) {
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("partnershipName", partnershipName);
        meta.put("configSource", configSource);
        meta.put("pollerInstance", pollerInstance);
        polledDirectories.put(pollerDir, meta);
    }

    public void destroyPartnershipPollers() {
        for (Map.Entry<String, Map<String, Object>> entry : polledDirectories.entrySet()) {
            Map<String, Object> meta = entry.getValue();
            ProcessorModule poller = (ProcessorModule) meta.get("pollerInstance");
            try {
                poller.destroy();
            } catch (Exception e) {
                // something went wrong stoppint it - report and keep going
                LOGGER.error("Failed to stop a partnership poller for directory " + entry.getKey() + ": " + meta, e);
            }
            
        }
    }

    public void loadPartnershipPoller(Node moduleNode, String partnershipName, String configSource) throws OpenAS2Exception {
        ProcessorModule procmod = (ProcessorModule) XMLUtil.getComponent(moduleNode, this);
        String pollerDir = procmod.getParameters().get(DirectoryPollingModule.PARAM_OUTBOX_DIRECTORY);
        try {
            checkPollerModuleConfig(pollerDir);
        } catch (OpenAS2Exception oae) {
            try {
                procmod.destroy();
            } catch (Exception e) {
                throw new OpenAS2Exception("Failed to destroy a partnershipthat has config errors", e);
            }
            throw new OpenAS2Exception("Partnership cannot be loaded because there is a configuration error: " + partnershipName, oae);
        }
        Processor proc = (Processor)getComponent(Processor.COMPID_PROCESSOR);
        proc.getModules().add(procmod);
        trackPollerModule(pollerDir, partnershipName, configSource, procmod);
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    void setBaseDirectory(String dir) {
        baseDirectory = dir;
        Properties.setProperty(Properties.APP_BASE_DIR_PROP, baseDirectory);
    }

}
