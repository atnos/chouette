package fr.certu.chouette.exchange.netex.importer.converters;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;
import fr.certu.chouette.model.neptune.PTNetwork;
import org.apache.log4j.Logger;

public class PTNetworkConverter extends GenericConverter 
{    
    private static final Logger       logger = Logger.getLogger(PTNetworkConverter.class);
    private PTNetwork network = new PTNetwork();    
    private AutoPilot autoPilot;
    private VTDNav vTDNav;
    
    public PTNetworkConverter(VTDNav nav) throws XPathParseException, XPathEvalException, NavException
    {
        vTDNav = nav;
        autoPilot = new AutoPilot(nav);
        autoPilot.declareXPathNameSpace("netex","http://www.netex.org.uk/netex");
        autoPilot.selectXPath("//netex:Network");
    }
    
    public PTNetwork convert() throws XPathEvalException, NavException
    {
        int result = -1;
        
        while( (result = autoPilot.evalXPath()) != -1 )
        {                        
            network.setName(parseMandatoryElement(vTDNav, "Name"));
            network.setDescription(parseMandatoryElement(vTDNav, "Description"));                                              
        } 
        
        return network;
    }
    
}