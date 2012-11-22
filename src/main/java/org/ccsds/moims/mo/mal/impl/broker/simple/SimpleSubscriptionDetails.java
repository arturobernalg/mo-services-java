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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.impl.broker.BrokerMessage.NotifyMessage;
import org.ccsds.moims.mo.mal.impl.broker.MALBrokerImpl;
import org.ccsds.moims.mo.mal.impl.broker.SubscriptionKey;
import org.ccsds.moims.mo.mal.impl.broker.UpdateKey;
import org.ccsds.moims.mo.mal.structures.*;
import org.ccsds.moims.mo.mal.transport.MALMessageHeader;
import org.ccsds.moims.mo.mal.transport.MALPublishBody;

/**
 * A SimpleSubscriptionDetails is keyed on subscription Id
 */
class SimpleSubscriptionDetails
{
  private final String subscriptionId;
  private Set<SubscriptionKey> required = new TreeSet<SubscriptionKey>();
  private Set<SubscriptionKey> onAll = new TreeSet<SubscriptionKey>();
  private Set<SubscriptionKey> onChange = new TreeSet<SubscriptionKey>();

  SimpleSubscriptionDetails(final String subscriptionId)
  {
    this.subscriptionId = subscriptionId;
  }

  void report()
  {
    MALBrokerImpl.LOGGER.log(Level.FINE, "    START Subscription ( {0} )", subscriptionId);
    MALBrokerImpl.LOGGER.log(Level.FINE, "     Required: {0}", required.size());
    for (SubscriptionKey key : required)
    {
      MALBrokerImpl.LOGGER.log(Level.FINE, "            : Rqd : {0}", key);
    }
    for (SubscriptionKey key : onAll)
    {
      MALBrokerImpl.LOGGER.log(Level.FINE, "            : All : {0}", key);
    }
    for (SubscriptionKey key : onChange)
    {
      MALBrokerImpl.LOGGER.log(Level.FINE, "            : Chg : {0}", key);
    }
    MALBrokerImpl.LOGGER.log(Level.FINE, "    END Subscription ( {0} )", subscriptionId);
  }

  boolean notActive()
  {
    return required.isEmpty();
  }

  void setIds(final MALMessageHeader srcHdr, final EntityRequestList lst)
  {
    required.clear();
    onAll.clear();
    onChange.clear();
    for (EntityRequest rqst : lst)
    {
      final EntityKeyList keyList = rqst.getEntityKeys();
      final boolean bOnChange = rqst.getOnlyOnChange();

      for (EntityKey id : keyList)
      {
        final SubscriptionKey key = new SubscriptionKey(srcHdr, rqst, id);
        required.add(key);
        if (bOnChange)
        {
          onChange.add(key);
        }
        else
        {
          onAll.add(key);
        }
      }
    }
  }

  NotifyMessage populateNotifyList(final MALMessageHeader srcHdr,
          final String srcDomainId,
          final UpdateHeaderList updateHeaderList,
          final MALPublishBody publishBody) throws MALException
  {
    MALBrokerImpl.LOGGER.fine("INFO: Checking SimSubDetails");

    final UpdateHeaderList notifyHeaders = new UpdateHeaderList();

    final List[] updateLists = publishBody.getUpdateLists((List[]) null);
    final Object[] notifyLists = new Object[updateLists.length + 2];
    for (int i = 2; i < notifyLists.length; i++)
    {
      notifyLists[i] = (List) ((Element) updateLists[i - 2]).createElement();
    }

    for (int i = 0; i < updateHeaderList.size(); ++i)
    {
      populateNotifyList(srcHdr, srcDomainId, updateHeaderList.get(i), updateLists, i, notifyHeaders, notifyLists);
    }

    NotifyMessage retVal = null;
    if (!notifyHeaders.isEmpty())
    {
      retVal = new NotifyMessage();
      notifyLists[0] = new Identifier(subscriptionId);
      notifyLists[1] = notifyHeaders;
      retVal.updates = notifyLists;
    }

    return retVal;
  }

  private void populateNotifyList(final MALMessageHeader srcHdr,
          final String srcDomainId,
          final UpdateHeader updateHeader,
          final List[] updateLists,
          final int index,
          final UpdateHeaderList notifyHeaders,
          final Object[] notifyLists) throws MALException
  {
    final UpdateKey key = new UpdateKey(srcHdr, srcDomainId, updateHeader.getKey());
    MALBrokerImpl.LOGGER.log(Level.FINE, "Checking {0}", key);
    boolean updateRequired = matchedUpdate(key, onAll);

    if (!updateRequired && (updateHeader.getUpdateType().getOrdinal() != UpdateType._UPDATE_INDEX))
    {
      updateRequired = matchedUpdate(key, onChange);
    }

    if (updateRequired)
    {
      // add update for this consumer/subscription
      notifyHeaders.add(updateHeader);

      for (int i = 2; i < notifyLists.length; i++)
      {
        ((List) notifyLists[i]).add(updateLists[i - 2].get(index));
      }
    }
  }

  private static boolean matchedUpdate(final UpdateKey key, final Set<SubscriptionKey> searchSet)
  {
    boolean matched = false;
    for (SubscriptionKey subscriptionKey : searchSet)
    {
      MALBrokerImpl.LOGGER.log(Level.FINE, "Checking {0} against {1}", new Object[]
              {
                key, subscriptionKey
              });
      if (subscriptionKey.matches(key))
      {
        MALBrokerImpl.LOGGER.fine("    : Matched");
        matched = true;
        break;
      }
      MALBrokerImpl.LOGGER.fine("    : No match");
    }
    return matched;
  }

  void appendIds(final Set<SubscriptionKey> subSet)
  {
    subSet.addAll(required);
  }
}
