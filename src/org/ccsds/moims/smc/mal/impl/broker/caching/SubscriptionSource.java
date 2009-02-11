package org.ccsds.moims.smc.mal.impl.broker.caching;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import org.ccsds.moims.smc.mal.api.structures.MALDomainIdentifier;
import org.ccsds.moims.smc.mal.api.structures.MALIdentifier;
import org.ccsds.moims.smc.mal.api.structures.MALIdentifierList;
import org.ccsds.moims.smc.mal.api.structures.MALMessageHeader;
import org.ccsds.moims.smc.mal.api.structures.MALOctet;
import org.ccsds.moims.smc.mal.api.structures.MALSessionType;
import org.ccsds.moims.smc.mal.api.structures.MALSubscription;
import org.ccsds.moims.smc.mal.api.structures.MALUpdate;
import org.ccsds.moims.smc.mal.api.structures.MALUpdateList;
import org.ccsds.moims.smc.mal.api.structures.MALUpdateType;
import org.ccsds.moims.smc.mal.impl.broker.MALBrokerMessage;
import org.ccsds.moims.smc.mal.impl.broker.MALSubscriptionKey;

/**
 * A SubscriptionSource is keyed on Area, Service and Operation,
 * it contains one to many ConsumerDetails.
 */
class SubscriptionSource
{
  private final MALIdentifier transactionId;
  private final MALDomainIdentifier domain;
  private final MALIdentifier networkZone;
  private final MALSessionType session;
  private final MALIdentifier area;
  private final MALIdentifier service;
  private final MALIdentifier operation;
  private final MALOctet version;
  private final String sig;
  private final Map<MALSubscriptionKey, PublishedEntry> published = new TreeMap<MALSubscriptionKey, PublishedEntry>();
  private final Map<String, ConsumerDetails> details = new TreeMap<String, ConsumerDetails>();

  public SubscriptionSource(MALMessageHeader hdr)
  {
    super();
    this.transactionId = hdr.getTransactionId();
    this.domain = hdr.getDomain();
    this.networkZone = hdr.getNetworkZone();
    this.session = hdr.getSession();
    this.area = hdr.getArea();
    this.service = hdr.getService();
    this.operation = hdr.getOperation();
    this.version = hdr.getVersion();
    this.sig = CachingBrokerHandler.makeSig(hdr);
  }

//    public boolean notActive()
//    {
//      return required.isEmpty();
//    }
  public void report()
  {
    Set values = details.entrySet();
    Iterator it = values.iterator();
    System.out.println("  START Source ( " + sig + " )");
    System.out.println("  Required: " + String.valueOf(published.size()));
    while (it.hasNext())
    {
      ((ConsumerDetails) ((Entry) it.next()).getValue()).report();
    }
    System.out.println("  END Source ( " + sig + " )");
  }

  public void addSubscription(String consumer, MALSubscription subscription)
  {
    ConsumerDetails det = getDetails(consumer);
    det.addSubscription(published, subscription);
  }

  public void populateNotifyList(MALMessageHeader srcHdr, List<MALBrokerMessage> lst, MALUpdateList updateList)
  {
    int length = updateList.size();
    for (int i = 0; i < length; ++i)
    {
      MALUpdate update = (MALUpdate) updateList.get(i);
      MALSubscriptionKey key = new MALSubscriptionKey(update.getKey());
      boolean onlyForAll = update.getUpdateType().equals(MALUpdateType.UPDATE);

      PublishedEntry publishedEntry = published.get(key);
      if (null == publishedEntry)
      {
        publishedEntry = populatePublishedMap(key);
      }

      Vector<SubscriptionDetails> subsList = null;
      if (onlyForAll)
      {
        subsList = publishedEntry.onAll;
      }
      else
      {
        subsList = publishedEntry.onChange;
      }

      for (SubscriptionDetails subscriptionDetails : subsList)
      {
        subscriptionDetails.populateNotify(update);
      }
    }

    for (ConsumerDetails entry : details.values())
    {
      entry.getNotifyMessage(srcHdr, transactionId, lst);
    }
  }

//  public void populateNotifyListx(MALMessageHeader srcHdr, List<MALBrokerMessage> lst, MALUpdateList updateList)
//  {
//    Map<Integer, Map.Entry<SubscriptionDetails, Vector<MALUpdate>>> subsList = new TreeMap<Integer, Map.Entry<SubscriptionDetails, Vector<MALUpdate>>>();
//
//    int length = updateList.size();
//    for (int i = 0; i < length; ++i)
//    {
//      MALUpdate update = (MALUpdate) updateList.get(i);
//      MALSubscriptionKey key = new MALSubscriptionKey(update.getKey());
//      Vector<SubscriptionDetails> clients = published.get(key);
//
//      if (null == clients)
//      {
//        clients = populatePublishedMap(key);
//      }
//
//      for (SubscriptionDetails subscriptionDetails : clients)
//      {
//        if (subscriptionDetails.requiresUpdate(key, update))
//        {
//          Map.Entry<SubscriptionDetails, Vector<MALUpdate>> subsUpdates = subsList.get(subscriptionDetails.hashCode());
//          if (null == subsUpdates)
//          {
//            subsUpdates = new TreeMap.SimpleEntry<SubscriptionDetails, Vector<MALUpdate>>(subscriptionDetails, new Vector<MALUpdate>());
//            subsList.put(subscriptionDetails.hashCode(), subsUpdates);
//          }
//          subsUpdates.getValue().add(update);
//        }
//      }
//    }
//
//    Map<Integer, MALBrokerMessage> localLst = new TreeMap<Integer, MALBrokerMessage>();
//    for (Map.Entry<SubscriptionDetails, Vector<MALUpdate>> entry : subsList.values())
//    {
//      entry.getKey().populateNotify(srcHdr, transactionId, localLst, entry.getValue());
//    }
//
//    // zip through list and insert our details
//    if (!localLst.isEmpty())
//    {
//      lst.addAll(localLst.values());
//    }
//  }
  public void removeSubscriptions(String consumer, MALIdentifierList subscriptions)
  {
    ConsumerDetails det = getDetails(consumer);
    // when a consumer deregisters we need to remove the deregistered Subscriptions from the published map
    det.removeSubscriptions(subscriptions, published);
    if (det.notActive())
    {
      details.remove(consumer);
    }
  }

  public void removeAllSubscriptions(String consumer)
  {
    ConsumerDetails det = getDetails(consumer);
    // when a copnsumer deregisters we need to remove the deregistered Subscriptions from the published map
    det.removeAllSubscriptions(published);
    if (det.notActive())
    {
      details.remove(consumer);
    }
  }

//    protected void updateIds()
//    {
//      required.clear();
//
//      java.util.Set<Map.Entry<String, ConsumerDetails>> values = details.entrySet();
//      for (Map.Entry<String, ConsumerDetails> entry : values)
//      {
//        entry.getValue().appendIds(required);
//      }
//    }
  private PublishedEntry populatePublishedMap(MALSubscriptionKey key)
  {
    PublishedEntry publishedEntry = new PublishedEntry();

    Set<Map.Entry<String, ConsumerDetails>> values = details.entrySet();

    for (Map.Entry<String, ConsumerDetails> entry : values)
    {
      entry.getValue().appendSubscriptions(publishedEntry, key);
    }

    published.put(key, publishedEntry);

    return publishedEntry;
  }

  private ConsumerDetails getDetails(String consumer)
  {
    ConsumerDetails retVal = (ConsumerDetails) details.get(consumer);
    if (null == retVal)
    {
      retVal = new ConsumerDetails(consumer);
      details.put(consumer, retVal);
    }
    return retVal;
  }
}
