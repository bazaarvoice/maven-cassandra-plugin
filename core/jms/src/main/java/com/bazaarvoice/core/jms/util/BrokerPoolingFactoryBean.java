package com.bazaarvoice.core.jms.util;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

/**
 * Spring factory for pooling JMS ConnectionFactory objects.  This bean
 * exists for two reasons: (1) a Spring-friendly wrapper on top of the
 * BrokerAdapter.createPoolingConnectionFactory method, and (2) a way
 * to hook Spring shutdown events to close the connection factory.
 */
public class BrokerPoolingFactoryBean implements FactoryBean, InitializingBean, DisposableBean {

    private BrokerAdapter _brokerAdapter;
    private String _brokerURL;
    private ConnectionFactory _pooledConnectionFactory;

    public Class getObjectType() {
        return (_brokerAdapter == null) ? ConnectionFactory.class : _brokerAdapter.getPoolingConnectionFactoryType();
    }

    @Required
    public void setBrokerAdapter(BrokerAdapter brokerAdapter) {
        _brokerAdapter = brokerAdapter;
    }

    @Required
    public void setBrokerURL(String brokerURL) {
        _brokerURL = brokerURL;
    }

    public void afterPropertiesSet() throws JMSException {
        _pooledConnectionFactory = _brokerAdapter.createPoolingConnectionFactory(_brokerURL);
    }

    public void destroy() throws Exception {
        _brokerAdapter.destroyPoolingConnectionFactory(_pooledConnectionFactory);
        _pooledConnectionFactory = null;
    }

    public Object getObject() {
        return _pooledConnectionFactory;
    }

    public boolean isSingleton() {
        return true;
    }
}
