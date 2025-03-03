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

ENV HDFS_PLUGIN_VERSION=2.5.0
ENV YARN_PLUGIN_VERSION=2.5.0

COPY ./dist/version                                          /home/ranger/dist/
COPY ./dist/ranger-${HDFS_PLUGIN_VERSION}-hdfs-plugin.tar.gz /home/ranger/dist/
COPY ./dist/ranger-${YARN_PLUGIN_VERSION}-yarn-plugin.tar.gz /home/ranger/dist/

COPY ./scripts/ranger-hadoop-setup.sh                   /home/ranger/scripts/
COPY ./scripts/ranger-hadoop.sh                         /home/ranger/scripts/
COPY ./scripts/ranger-hadoop-mkdir.sh                   /home/ranger/scripts/
COPY ./scripts/ranger-hdfs-plugin-install.properties    /home/ranger/scripts/
COPY ./scripts/ranger-yarn-plugin-install.properties    /home/ranger/scripts

RUN ln -s /opt/hadoop-${HADOOP_VERSION} /opt/hadoop && \
    tar xvfz /home/ranger/dist/ranger-${HDFS_PLUGIN_VERSION}-hdfs-plugin.tar.gz --directory=/opt/ranger && \
    ln -s /opt/ranger/ranger-${HDFS_PLUGIN_VERSION}-hdfs-plugin /opt/ranger/ranger-hdfs-plugin && \
    rm -f /home/ranger/dist/ranger-${HDFS_PLUGIN_VERSION}-hdfs-plugin.tar.gz && \
    cp -f /home/ranger/scripts/ranger-hdfs-plugin-install.properties /opt/ranger/ranger-hdfs-plugin/install.properties && \
    tar xvfz /home/ranger/dist/ranger-${YARN_PLUGIN_VERSION}-yarn-plugin.tar.gz --directory=/opt/ranger && \
    ln -s /opt/ranger/ranger-${YARN_PLUGIN_VERSION}-yarn-plugin /opt/ranger/ranger-yarn-plugin && \
    rm -f /home/ranger/dist/ranger-${YARN_PLUGIN_VERSION}-yarn-plugin.tar.gz && \
    cp -f /home/ranger/scripts/ranger-yarn-plugin-install.properties /opt/ranger/ranger-yarn-plugin/install.properties && \
    chmod 744 ${RANGER_SCRIPTS}/ranger-hadoop-setup.sh ${RANGER_SCRIPTS}/ranger-hadoop.sh ${RANGER_SCRIPTS}/ranger-hadoop-mkdir.sh && \
    chown hdfs:hadoop ${RANGER_SCRIPTS}/ranger-hadoop-mkdir.sh

ENV HADOOP_HDFS_HOME   /opt/hadoop
ENV HADOOP_MAPRED_HOME /opt/hadoop
ENV HADOOP_COMMON_HOME /opt/hadoop
ENV YARN_HOME          /opt/hadoop
ENV PATH=$PATH:/usr/java/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

ENTRYPOINT [ "/home/ranger/scripts/ranger-hadoop.sh" ]
