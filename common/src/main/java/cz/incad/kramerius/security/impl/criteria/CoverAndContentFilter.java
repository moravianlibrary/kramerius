package cz.incad.kramerius.security.impl.criteria;

import cz.incad.kramerius.FedoraNamespaceContext;
import cz.incad.kramerius.ObjectModelsPath;
import cz.incad.kramerius.security.EvaluatingResult;
import cz.incad.kramerius.security.RightCriterium;
import cz.incad.kramerius.security.RightCriteriumException;
import cz.incad.kramerius.security.RightCriteriumPriorityHint;
import cz.incad.kramerius.security.SecuredActions;
import cz.incad.kramerius.security.SpecialObjects;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * CoverAndContentFilter
 *
 *  Page types FrontCover, TableOfContents, FrontJacket, TitlePage and jacket
 *  are copyright free (uncommercial and library usage)
 *
 * @author Martin Rumanek
 */
public class CoverAndContentFilter extends AbstractCriterium implements RightCriterium {

    Logger LOGGER = java.util.logging.Logger.getLogger(CoverAndContentFilter.class.getName());
    private static XPathExpression modsTypeExpr = null;
    private static final List<String> allowedPageTypes = Arrays.asList(
            "frontcover", "tableofcontents", "frontjacket", "titlepage", "jacket"
    );
    private static final Map<String, List<String>> forbiddenTypeForModels = new HashMap<String, List<String>>() {{
        put("titlepage", Collections.singletonList("periodical"));
    }};

    @Override
    public EvaluatingResult evalute() throws RightCriteriumException {
        try {
            String uuid = getEvaluateContext().getRequestedPid();
            if (isSpecial(uuid))
                return EvaluatingResult.TRUE;
            else if (isPage(uuid))
                return pageCanBeShown(uuid);
            else
                return EvaluatingResult.NOT_APPLICABLE;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return EvaluatingResult.NOT_APPLICABLE;
        }
    }

    private EvaluatingResult pageCanBeShown(String uuid) throws IOException {
        String pageType = getPageType(uuid);
        List<String> rootModels = getRootModels(uuid);
        if (pageTypeIsAllowed(pageType) && typeIsAllowedForRoot(pageType, rootModels))
            return EvaluatingResult.TRUE;
        else
            return EvaluatingResult.NOT_APPLICABLE;
    }

    private String getPageType(String uuid) throws IOException {
        Document mods = getEvaluateContext().getFedoraAccess().getBiblioMods(uuid);
        try {
            if (modsTypeExpr == null) initModsTypeExpr();
            return modsTypeExpr.evaluate(mods).toLowerCase();
        } catch (XPathExpressionException e) {
            throw new IOException(e);
        }
    }

    private List<String> getRootModels(String uuid) throws IOException {
        ObjectModelsPath[] modelsPaths = getEvaluateContext().getSolrAccess().getPathOfModels(uuid);
        return Arrays.stream(modelsPaths).map(ObjectModelsPath::getRoot).collect(Collectors.toList());
    }

    private boolean pageTypeIsAllowed(String type) {
        return allowedPageTypes.contains(type);
    }

    private boolean typeIsAllowedForRoot(String type, List<String> rootModels) {
        // true if there is no forbidden root models for given page type
        if (forbiddenTypeForModels.containsKey(type)) {
            return Collections.disjoint(rootModels, forbiddenTypeForModels.get(type));
        } else return true;
    }

    private boolean isSpecial(String uuid) {
        return uuid.equals(SpecialObjects.REPOSITORY.getPid());
    }

    private boolean isPage(String uuid) throws IOException {
        return "page".equals(getEvaluateContext().getFedoraAccess().getKrameriusModelName(uuid));
    }

    private void initModsTypeExpr() throws IOException {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(new FedoraNamespaceContext());
            modsTypeExpr = xpath.compile("/mods:modsCollection/mods:mods/mods:part/@type");
        } catch (XPathExpressionException e) {
            throw new IOException(e);
        }
    }

    @Override
    public RightCriteriumPriorityHint getPriorityHint() {
        return RightCriteriumPriorityHint.NORMAL;
    }

    @Override
    public boolean isParamsNecessary() {
        return false;
    }

    @Override
    public SecuredActions[] getApplicableActions() {
        return new SecuredActions[]{SecuredActions.READ};
    }
}
