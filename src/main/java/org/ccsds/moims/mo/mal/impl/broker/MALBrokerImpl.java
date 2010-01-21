/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ccsds.moims.mo.mal.impl.broker;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.ccsds.moims.mo.mal.impl.*;
import org.ccsds.moims.mo.mal.MALException;
import org.ccsds.moims.mo.mal.MALService;
import org.ccsds.moims.mo.mal.impl.util.MALClose;
import org.ccsds.moims.mo.mal.broker.MALBroker;
import org.ccsds.moims.mo.mal.broker.MALBrokerBinding;
import org.ccsds.moims.mo.mal.impl.util.Logging;
import org.ccsds.moims.mo.mal.structures.EntityKeyList;
import org.ccsds.moims.mo.mal.structures.IdentifierList;
import org.ccsds.moims.mo.mal.structures.MessageHeader;
import org.ccsds.moims.mo.mal.structures.QoSLevel;
import org.ccsds.moims.mo.mal.structures.Subscription;
import org.ccsds.moims.mo.mal.structures.UpdateList;
import org.ccsds.moims.mo.mal.transport.MALEndPoint;
import org.ccsds.moims.mo.mal.transport.MALMessage;

/**
 *
 * @author cooper_sf
 */
public class MALBrokerImpl extends MALClose implements MALBroker, BrokerHandler
{
  private final Map<String, MALBrokerBindingImpl> bindingMap = new TreeMap<String, MALBrokerBindingImpl>();
  private final MALBrokerBindingImpl[] bindings = new MALBrokerBindingImpl[0];
  private final BrokerHandler delegate;

  public MALBrokerImpl(MALClose parent, MALImpl impl, MALService service) throws MALException
  {
    super(parent);

    delegate = new BrokerMap(impl);
  }

  @Override
  public MALBrokerBinding[] getBindings()
  {
    return bindings;
  }

  @Override
  public synchronized void activate() throws MALException
  {
    for (int i = 0; i < bindings.length; i++)
    {
      bindings[i].activate();
    }
  }

  protected synchronized void addBinding(MALBrokerBindingImpl binding)
  {
    MALBrokerBindingImpl[] nbindings = new MALBrokerBindingImpl[bindings.length + 1];

    for (int i = 0; i < bindings.length; i++)
    {
      nbindings[i] = bindings[i];
    }

    nbindings[bindings.length] = binding;
    bindingMap.put(binding.getURI().getValue(), binding);
  }

  @Override
  public void addConsumer(MessageHeader hdr, Subscription body, MALBrokerBindingImpl binding)
  {
    delegate.addConsumer(hdr, body, binding);
  }

  @Override
  public void addProvider(MessageHeader hdr, EntityKeyList body)
  {
    delegate.addProvider(hdr, body);
  }

  @Override
  public QoSLevel getProviderQoSLevel(MessageHeader hdr)
  {
    return delegate.getProviderQoSLevel(hdr);
  }

  @Override
  public void removeConsumer(MessageHeader hdr, IdentifierList ids)
  {
    delegate.removeConsumer(hdr, ids);
  }

  @Override
  public void removeLostConsumer(MessageHeader hdr)
  {
    delegate.removeLostConsumer(hdr);
  }

  @Override
  public void removeProvider(MessageHeader hdr)
  {
    delegate.removeProvider(hdr);
  }

  @Override
  public List<BrokerMessage> createNotify(MessageHeader hdr, UpdateList updateList) throws MALException
  {
    return delegate.createNotify(hdr, updateList);
  }

  public void handlePublish(MessageHeader hdr, UpdateList updateList) throws MALException
  {
    java.util.List<BrokerMessage> msgList = delegate.createNotify(hdr, updateList);

    if (!msgList.isEmpty())
    {
      for (BrokerMessage brokerMessage : msgList)
      {
        MALEndPoint endpoint = brokerMessage.binding.getEndpoint();

        for (BrokerMessage.NotifyMessage notifyMessage : brokerMessage.msgs)
        {
          try
          {
            MALMessage msg = endpoint.createMessage(notifyMessage.header, notifyMessage.updates, new Hashtable());

            // send it out
            endpoint.sendMessage(msg);
          }
          catch (MALException ex)
          {
            // with the exception being thrown we assume that there is a problem with this consumer so remove
            //  them from the observe manager
            Logging.logMessage("ERROR: Error with notify consumer, removing from list : " + notifyMessage.header.getURIto());
            delegate.removeLostConsumer(notifyMessage.header);

            // TODO: notify local provider
          }
        }
      }
    }
  }
}
