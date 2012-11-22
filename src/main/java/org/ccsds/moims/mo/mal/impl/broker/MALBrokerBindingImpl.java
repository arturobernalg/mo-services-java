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
package org.ccsds.moims.mo.mal.impl.broker;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.ccsds.moims.mo.mal.*;
import org.ccsds.moims.mo.mal.broker.MALBrokerBinding;
import org.ccsds.moims.mo.mal.impl.MALContextImpl;
import org.ccsds.moims.mo.mal.impl.ServiceComponentImpl;
import org.ccsds.moims.mo.mal.structures.*;
import org.ccsds.moims.mo.mal.transport.MALEndpoint;
import org.ccsds.moims.mo.mal.transport.MALMessage;

/**
 * Implementation of MALBrokerBinding for MAL level brokers.
 */
public class MALBrokerBindingImpl extends ServiceComponentImpl implements MALBrokerBinding
{
  private final MALBrokerImpl brokerImpl;

  MALBrokerBindingImpl(final MALBrokerImpl parent,
          final MALContextImpl impl,
          final String localName,
          final String protocol,
          final Blob authenticationId,
          final QoSLevel[] expectedQos,
          final UInteger priorityLevelNumber,
          final Map qosProperties) throws MALException
  {
    super(parent,
            impl,
            localName,
            protocol,
            null,
            authenticationId,
            expectedQos,
            priorityLevelNumber,
            qosProperties,
            null);

    this.brokerImpl = parent;
    this.endpoint.startMessageDelivery();

    MALBrokerImpl.LOGGER.log(Level.INFO,
            "Creating internal MAL Broker for localName: {0} on protocol: {1} with URI: {2}", new Object[]
            {
              localName, protocol, this.localUri
            });
  }

  MALBrokerBindingImpl(final MALBrokerImpl parent,
          final MALContextImpl impl,
          final MALEndpoint endPoint,
          final Blob authenticationId,
          final QoSLevel[] expectedQos,
          final UInteger priorityLevelNumber,
          final Map qosProperties) throws MALException
  {
    super(parent,
            impl,
            endPoint,
            null,
            authenticationId,
            expectedQos,
            priorityLevelNumber,
            qosProperties,
            null);

    this.brokerImpl = parent;

    MALBrokerImpl.LOGGER.log(Level.INFO,
            "Creating internal MAL Broker for localName: {0} with URI: {1}", new Object[]
            {
              localName, this.localUri
            });
  }

  @Override
  public Blob getAuthenticationId()
  {
    return authenticationId;
  }

  @Override
  public MALMessage sendNotify(final UShort area,
          final UShort service,
          final UShort operation,
          final UOctet version,
          final URI subscriber,
          final Long transactionId,
          final IdentifierList domainId,
          final Identifier networkZone,
          final SessionType sessionType,
          final Identifier sessionName,
          final QoSLevel notifyQos,
          final Map notifyQosProps,
          final UInteger notifyPriority,
          final Identifier subscriptionId,
          final UpdateHeaderList updateHeaderList,
          final List... updateList) throws IllegalArgumentException, MALInteractionException, MALException
  {
    final Object[] body = new Object[2 + updateList.length];
    body[0] = subscriptionId;
    body[1] = updateHeaderList;
    int i = 2;
    for (Object object : updateList)
    {
      body[i++] = object;
    }

    final MALMessage msg = endpoint.createMessage(authenticationId,
            subscriber,
            new Time(new Date().getTime()),
            notifyQos,
            notifyPriority,
            domainId,
            networkZone,
            sessionType,
            sessionName,
            InteractionType.PUBSUB,
            MALPubSubOperation.NOTIFY_STAGE,
            transactionId,
            area,
            service,
            operation,
            version,
            Boolean.FALSE,
            notifyQosProps,
            body);

    endpoint.sendMessage(msg);

    return msg;
  }

  @Override
  public MALMessage sendNotify(final MALOperation op,
          final URI subscriber,
          final Long transactionId,
          final IdentifierList domainId,
          final Identifier networkZone,
          final SessionType sessionType,
          final Identifier sessionName,
          final QoSLevel notifyQos,
          final Map notifyQosProps,
          final UInteger notifyPriority,
          final Identifier subscriptionId,
          final UpdateHeaderList updateHeaderList,
          final List... updateList) throws IllegalArgumentException, MALInteractionException, MALException
  {
    final Object[] body = new Object[2 + updateList.length];
    body[0] = subscriptionId;
    body[1] = updateHeaderList;
    int i = 2;
    for (Object object : updateList)
    {
      body[i++] = object;
    }

    final MALMessage msg = endpoint.createMessage(authenticationId,
            subscriber,
            new Time(new Date().getTime()),
            notifyQos,
            notifyPriority,
            domainId,
            networkZone,
            sessionType,
            sessionName,
            transactionId,
            Boolean.FALSE,
            op,
            MALPubSubOperation.NOTIFY_STAGE,
            notifyQosProps,
            body);

    endpoint.sendMessage(msg);

    return msg;
  }

  @Override
  public MALMessage sendNotifyError(final UShort area,
          final UShort service,
          final UShort operation,
          final UOctet version,
          final URI subscriber,
          final Long transactionId,
          final IdentifierList domainId,
          final Identifier networkZone,
          final SessionType sessionType,
          final Identifier sessionName,
          final QoSLevel notifyQos,
          final Map notifyQosProps,
          final UInteger notifyPriority,
          final MALStandardError error) throws IllegalArgumentException, MALInteractionException, MALException
  {
    final MALMessage msg = endpoint.createMessage(authenticationId,
            subscriber,
            new Time(new Date().getTime()),
            notifyQos,
            notifyPriority,
            domainId,
            networkZone,
            sessionType,
            sessionName,
            InteractionType.PUBSUB,
            MALPubSubOperation.NOTIFY_STAGE,
            transactionId,
            area,
            service,
            operation,
            version,
            Boolean.TRUE,
            notifyQosProps,
            error);

    endpoint.sendMessage(msg);

    return msg;
  }

  @Override
  public MALMessage sendNotifyError(final MALOperation op,
          final URI subscriber,
          final Long transactionId,
          final IdentifierList domainId,
          final Identifier networkZone,
          final SessionType sessionType,
          final Identifier sessionName,
          final QoSLevel notifyQos,
          final Map notifyQosProps,
          final UInteger notifyPriority,
          final MALStandardError error) throws IllegalArgumentException, MALInteractionException, MALException
  {
    final MALMessage msg = endpoint.createMessage(authenticationId,
            subscriber,
            new Time(new Date().getTime()),
            notifyQos,
            notifyPriority,
            domainId,
            networkZone,
            sessionType,
            sessionName,
            transactionId,
            Boolean.TRUE,
            op,
            MALPubSubOperation.NOTIFY_STAGE,
            notifyQosProps,
            error);

    endpoint.sendMessage(msg);

    return msg;
  }

  @Override
  public MALMessage sendPublishError(final UShort area,
          final UShort service,
          final UShort operation,
          final UOctet version,
          final URI publisher,
          final Long transactionId,
          final IdentifierList domainId,
          final Identifier networkZone,
          final SessionType sessionType,
          final Identifier sessionName,
          final QoSLevel qos,
          final Map qosProps,
          final UInteger priority,
          final MALStandardError error) throws IllegalArgumentException, MALInteractionException, MALException
  {
    final MALMessage msg = endpoint.createMessage(authenticationId,
            publisher,
            new Time(new Date().getTime()),
            qos,
            priority,
            domainId,
            networkZone,
            sessionType,
            sessionName,
            InteractionType.PUBSUB,
            MALPubSubOperation.PUBLISH_STAGE,
            transactionId,
            area,
            service,
            operation,
            version,
            Boolean.TRUE,
            qosProps,
            error);

    endpoint.sendMessage(msg);

    return msg;
  }

  @Override
  public MALMessage sendPublishError(final MALOperation op,
          final URI publisher,
          final Long transactionId,
          final IdentifierList domainId,
          final Identifier networkZone,
          final SessionType sessionType,
          final Identifier sessionName,
          final QoSLevel qos,
          final Map qosProps,
          final UInteger priority,
          final MALStandardError error) throws IllegalArgumentException, MALInteractionException, MALException
  {
    final MALMessage msg = endpoint.createMessage(authenticationId,
            publisher,
            new Time(new Date().getTime()),
            qos,
            priority,
            domainId,
            networkZone,
            sessionType,
            sessionName,
            transactionId,
            Boolean.TRUE,
            op,
            MALPubSubOperation.PUBLISH_STAGE,
            qosProps,
            error);

    endpoint.sendMessage(msg);

    return msg;
  }

  /**
   * Returns the reference to the top level broker object.
   *
   * @return The parent broker.
   */
  public MALBrokerBaseImpl getBrokerImpl()
  {
    return brokerImpl;
  }
}
