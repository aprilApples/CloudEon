FROM ubuntu:22.04
SHELL ["/bin/bash", "-c"]

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone

ENV TZ=Asia/Shanghai  \
    FREEMARKER_GENERATOR_CLI_HOME=/opt/freemarker-generator-cli
ENV PATH=$PATH:$FREEMARKER_GENERATOR_CLI_HOME/bin

RUN apt-get update && apt-get install -y curl
RUN curl -L https://gitee.com/RubyMetric/chsrc/releases/download/pre/chsrc-x64-linux -o chsrc && chmod +x ./chsrc && mv ./chsrc /usr/bin/
RUN chsrc set ubuntu first

RUN apt-get update && apt-get install -y wget net-tools vim perl debianutils  netcat zip unzip lsof bc hostname cron

ADD ./downloads/freemarker-generator-cli-0.2.0-SNAPSHOT-app.tar.gz /opt/
RUN mv /opt/freemarker-generator-cli-* /opt/freemarker-generator-cli

# setup jmx exporter
COPY ./downloads/jmx_prometheus_javaagent-0.20.0.jar  /tmp/
RUN mkdir -p /opt/jmx_exporter  \
    && mv /tmp/jmx_prometheus_javaagent-0.20.0.jar /opt/jmx_exporter/jmx_prometheus_javaagent.jar

RUN apt update && \
    apt install -y openjdk-8-jdk

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH
ENV LD_LIBRARY_PATH=$JAVA_HOME/lib/amd64/jli:$JAVA_HOME/lib/amd64

RUN chsrc set ubuntu first && apt-get install -y mysql-client

# ####################################################################################################################################
# ############################################################  HADOOP  #######################################################
# ####################################################################################################################################

ENV HADOOP_HOME=/opt/hadoop \
    HADOOP_VERSION=3.3.4
ENV PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin

COPY ./downloads/hadoop-${HADOOP_VERSION}.tar.gz  /tmp/
RUN  tar -zxvf /tmp/hadoop-${HADOOP_VERSION}.tar.gz -C /opt \
    && rm -f /tmp/hadoop-*.tar.gz && mv /opt/hadoop-*  ${HADOOP_HOME}

WORKDIR $HADOOP_HOME

ENV HADOOP_CONF_DIR=${HADOOP_HOME}/etc/hadoop

# ####################################################################################################################################
# ############################################################  HIVE  #######################################################
# ####################################################################################################################################
ENV HIVE_HOME=/opt/hive
ENV HIVE_VERSION=3.1.3

ENV PATH=$PATH:$HIVE_HOME/bin

WORKDIR /tmp

COPY ./downloads/apache-hive-${HIVE_VERSION}-bin.tar.gz  /tmp/
RUN tar -zxvf /tmp/apache-hive-*.tar.gz -C /opt \
    && rm -f /tmp/apache-hive-*-bin.tar.gz && mv /opt/apache-hive-*  $HIVE_HOME


ENV MYSQL_CONN_VERSION=8.0.28
COPY ./downloads/mysql-connector-java-${MYSQL_CONN_VERSION}.jar  /tmp/
RUN  cp /tmp/mysql-connector-java-${MYSQL_CONN_VERSION}.jar $HIVE_HOME/lib/mysql-connector-java.jar  \
  && rm -rf /tmp/mysql-connector-*
# ####################################################################################################################################
# ############################################################  Ranger Plugin  #######################################################
# ####################################################################################################################################
# Install tzdata, Python, Java, python-requests
RUN apt-get update && \
    DEBIAN_FRONTEND="noninteractive" apt-get -y install tzdata vim\
    python3 python3-pip bc iputils-ping ssh pdsh xmlstarlet && \
    pip3 install apache-ranger && \
    pip3 install requests

# Set environment variables
# ENV JAVA_HOME      /usr/lib/jvm/java-${RANGER_BASE_JAVA_VERSION}-openjdk-${TARGETARCH}
ENV RANGER_DIST    /home/ranger/dist
ENV RANGER_SCRIPTS /home/ranger/scripts
ENV RANGER_HOME    /opt/ranger

# RUN update-java-alternatives --set /usr/lib/jvm/java-1.${RANGER_BASE_JAVA_VERSION}.0-openjdk-${TARGETARCH}

# setup groups, users, directories
RUN groupadd ranger && \
    useradd -g ranger -ms /bin/bash ranger && \
    useradd -g ranger -ms /bin/bash rangeradmin && \
    useradd -g ranger -ms /bin/bash rangerusersync && \
    useradd -g ranger -ms /bin/bash rangertagsync && \
    useradd -g ranger -ms /bin/bash rangerkms && \
    groupadd hadoop && \
    useradd -g hadoop -ms /bin/bash hdfs && \
    useradd -g hadoop -ms /bin/bash yarn && \
    useradd -g hadoop -ms /bin/bash hive && \
    useradd -g hadoop -ms /bin/bash hbase && \
    useradd -g hadoop -ms /bin/bash kafka && \
    groupadd knox && \
    useradd -g knox -ms /bin/bash knox && \
    mkdir -p /home/ranger/dist && \
    mkdir -p /home/ranger/scripts && \
    mkdir -p /opt/ranger

ENV HIVE_HADOOP_VERSION=3.1.1
ENV HIVE_PLUGIN_VERSION=2.5.0
ENV RANGER_DB_TYPE=mysql

COPY ./dist/version                                          /home/ranger/dist/
COPY ./dist/ranger-${HIVE_PLUGIN_VERSION}-hive-plugin.tar.gz /home/ranger/dist/

COPY ./scripts/ranger-hive-setup.sh                     /home/ranger/scripts/
COPY ./scripts/ranger-hive.sh                           /home/ranger/scripts/
COPY ./scripts/ranger-hive-plugin-install.properties    /home/ranger/scripts/
COPY ./scripts/hive-site-${RANGER_DB_TYPE}.xml          /home/ranger/scripts/hive-site.xml

RUN tar xvfz /home/ranger/dist/ranger-${HIVE_PLUGIN_VERSION}-hive-plugin.tar.gz --directory=/opt/ranger && \
    ln -s /opt/ranger/ranger-${HIVE_PLUGIN_VERSION}-hive-plugin /opt/ranger/ranger-hive-plugin && \
    rm -f /home/ranger/dist/ranger-${HIVE_PLUGIN_VERSION}-hive-plugin.tar.gz && \
    cp -f /home/ranger/scripts/ranger-hive-plugin-install.properties /opt/ranger/ranger-hive-plugin/install.properties && \
    chmod 744 ${RANGER_SCRIPTS}/ranger-hive-ranger_audit_setup.sh ${RANGER_SCRIPTS}/ranger-hive.sh

WORKDIR $HIVE_HOME

ENTRYPOINT [ "/home/ranger/scripts/ranger-hive.sh" ]
