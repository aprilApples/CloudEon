FROM ranger:2.5.0

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone

ENV TZ=Asia/Shanghai  \
    FREEMARKER_GENERATOR_CLI_HOME=/opt/freemarker-generator-cli
ENV PATH=$PATH:$FREEMARKER_GENERATOR_CLI_HOME/bin

RUN apt-get update && apt-get install -y net-tools perl debianutils  netcat zip unzip lsof cron

ADD ./downloads/freemarker-generator-cli-0.2.0-SNAPSHOT-app.tar.gz /opt/
RUN mv /opt/freemarker-generator-cli-* /opt/freemarker-generator-cli

# setup jmx exporter
RUN wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.20.0/jmx_prometheus_javaagent-0.20.0.jar  \
    && mkdir -p /opt/jmx_exporter  \
    && mv jmx_prometheus_javaagent-0.20.0.jar /opt/jmx_exporter/jmx_prometheus_javaagent.jar

WORKDIR $RANGER_HOME
