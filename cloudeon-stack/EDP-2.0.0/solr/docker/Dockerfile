/       FROM registry.cn-guangzhou.aliyuncs.com/bigdata200/hadoop:3.3.4

WORKDIR /tmp

# 新增solr组件
ENV SOLR_HOME=/opt/solr
ENV PATH=$PATH:$SOLR_HOME/bin

COPY solr-8.11.4.tgz /tmp/
RUN tar -zxvf /tmp/solr-8.11.4.tgz -C /opt \
    && rm -f /tmp/solr.8.11.4.tar && mv /opt/*solr*  $SOLR_HOME

WORKDIR $SOLR_HOME