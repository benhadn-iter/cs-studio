/*
* Copyright (c) 2010 Stiftung Deutsches Elektronen-Synchrotron,
* Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
*
* THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
* WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
* TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
* NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
* FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
* THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
* IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
* CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
* NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
* DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
* OR MODIFICATIONS.
* THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
* USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
* PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
* AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
*/
package org.csstudio.config.ioconfig.model.service.internal;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.csstudio.config.ioconfig.model.DocumentDBO;
import org.csstudio.config.ioconfig.model.IDocument;
import org.csstudio.config.ioconfig.model.PersistenceException;
import org.csstudio.config.ioconfig.model.hibernate.Repository;
import org.csstudio.config.ioconfig.model.service.IInternId2IONameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author hrickens
 * @author $Author: hrickens $
 * @version $Revision: 1.2 $
 * @since 27.07.2010
 */
public class InternId2IONameImplemation implements IInternId2IONameService {

    private static final Logger LOG = LoggerFactory
            .getLogger(InternId2IONameImplemation.class);

    /**
     * {@inheritDoc}
     */
    @Override
    @CheckForNull
    public String getIOName(@Nonnull final String internId) {
        String ioName = null;
        Channel4ServicesDBO loadChannelWithInternId;
        try {
            loadChannelWithInternId = Repository.loadChannelWithInternId(internId);
            if (loadChannelWithInternId!=null) {
                ioName = loadChannelWithInternId.getIoName();
            }
        } catch (final PersistenceException e) {
            LOG.warn("Can't load channel", e);
        }
        return ioName;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @CheckForNull
    public Set<IDocument> getDocuments(@Nonnull final String internId) throws PersistenceException {
        final Set<IDocument> hashSet = new HashSet<IDocument>();
        final Channel4ServicesDBO loadChannelWithInternId = Repository.loadChannelWithInternId(internId);
        if(loadChannelWithInternId!=null) {
            final Set<DocumentDBO> documents = loadChannelWithInternId.getDocuments();
            hashSet.addAll(documents);
        }
        return hashSet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CheckForNull
    public String getProcessVariables(@Nonnull final String internId) throws PersistenceException {
        final Channel4ServicesDBO loadChannelWithInternId = Repository.loadChannelWithInternId(internId);
        if(loadChannelWithInternId!=null) {
            return loadChannelWithInternId.getProcesVariable();
        }
        return null;
    }

}
