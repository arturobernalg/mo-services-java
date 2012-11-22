/* ----------------------------------------------------------------------------
 * (C) 2010      European Space Agency
 *               European Space Operations Centre
 *               Darmstadt Germany
 * ----------------------------------------------------------------------------
 * System       : CCSDS MO MAL Implementation
 * Author       : Sam Cooper
 *
 * ----------------------------------------------------------------------------
 */
package org.ccsds.moims.mo.mal.impl.broker.simple;

import java.util.*;
import java.util.logging.Level;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.impl.broker.BrokerMessage;
import org.ccsds.moims.mo.mal.impl.broker.BrokerMessage.NotifyMessage;
import org.ccsds.moims.mo.mal.impl.broker.MALBrokerBindingImpl;
import org.ccsds.moims.mo.mal.impl.broker.MALBrokerImpl;
import org.ccsds.moims.mo.mal.impl.broker.SubscriptionKey;
import org.ccsds.moims.mo.mal.impl.broker.SubscriptionSource;
import org.ccsds.moims.mo.mal.impl.util.StructureHelper;
import org.ccsds.moims.mo.mal.structures.Identifier;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.Subscription;
import org.ccsds.moims.mo.mal.structures.UpdateHeaderList;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;
import org.ccsds.moims.mo.mal.transport.MALPublishBody;

/**
 * A SimpleSubscriptionSource represents a single consumer indexed by URI.
 */
class SimpleSubscriptionSource extends SubscriptionSource
{
  private final String signature;
  private final Set<SubscriptionKey> required = new TreeSet<SubscriptionKey>();
  private final MALBrokerBindingImpl binding;
  private final Map<String, SimpleSubscriptionDetails> details = new TreeMap<String, SimpleSubscriptionDetails>();

  public SimpleSubscriptionSource(final MALMessageHeader hdr, final MALBrokerBindingImpl binding)
  {
    super(hdr, hdr.getURIFrom(), binding);
    this.signature = hdr.getURIFrom().getValue();
    this.binding = binding;
  }

  @Override
  public boolean active()
  {
    return !required.isEmpty();
  }

  @Override
  public void report()
  {
    MALBrokerImpl.LOGGER.log(Level.FINE, "  START Consumer ( {0} )", signature);
    MALBrokerImpl.LOGGER.log(Level.FINE, "   Required: {0}", required.size());
    for (Map.Entry<String, SimpleSubscriptionDetails> entry : details.entrySet())
    {
      entry.getValue().report();
    }
    MALBrokerImpl.LOGGER.log(Level.FINE, "  END Consumer ( {0} )", signature);
  }

  @Override
  public String getSignature()
  {
    return signature;
  }

  @Override
  public void addSubscription(final MALMessageHeader srcHdr, final Subscription subscription)
  {
    final String subId = subscription.getSubscriptionId().getValue();
    SimpleSubscriptionDetails sub = details.get(subId);
    if (null == sub)
    {
      sub = new SimpleSubscriptionDetails(subId);
      details.put(subId, sub);
    }
    sub.setIds(srcHdr, subscription.getEntities());

    updateIds();
  }

  @Override
  public void populateNotifyList(final MALMessageHeader srcHdr,
          final List<BrokerMessage> lst,
          final UpdateHeaderList updateHeaderList,
          final MALPublishBody publishBody) throws MALException
  {
    MALBrokerImpl.LOGGER.log(Level.FINE, "Checking SimComSource : {0}", signature);

    final String srcDomainId = StructureHelper.domainToString(srcHdr.getDomain());
    final BrokerMessage bmsg = new BrokerMessage(binding);

    for (Map.Entry<String, SimpleSubscriptionDetails> ent : details.entrySet())
    {
      final NotifyMessage subUpdate =
              ent.getValue().populateNotifyList(srcHdr, srcDomainId, updateHeaderList, publishBody);
      if (null != subUpdate)
      {
        bmsg.msgs.add(subUpdate);
      }
    }

    if (!bmsg.msgs.isEmpty())
    {
      for (NotifyMessage msg : bmsg.msgs)
      {
        // update the details in the header
        msg.details = msgDetails;
        msg.transId = transactionId;
        msg.domain = srcHdr.getDomain();
        msg.networkZone = srcHdr.getNetworkZone();
        msg.area = srcHdr.getServiceArea();
        msg.service = srcHdr.getService();
        msg.operation = srcHdr.getOperation();
        msg.version = srcHdr.getServiceVersion();
      }

      lst.add(bmsg);
    }
  }

  @Override
  public void removeSubscriptions(final IdentifierList subscriptions)
  {
    for (Identifier sub : subscriptions)
    {
      details.remove(sub.getValue());
    }

    updateIds();
  }

  @Override
  public void removeAllSubscriptions()
  {
    details.clear();
    required.clear();
  }

  private void updateIds()
  {
    required.clear();

    for (Map.Entry<String, SimpleSubscriptionDetails> entry : details.entrySet())
    {
      entry.getValue().appendIds(required);
    }
  }
}
