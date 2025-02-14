FROM registry.cn-guangzhou.aliyuncs.com/bigdata200/hadoop:3.3.4

ENV FLINK_HOME=/opt/flink
ENV FLINK_VERSION=1.15.4
ENV PATH=$PATH:$FLINK_HOME/bin

WORKDIR /tmp


# 新增seatunnel组件
ENV SEATUNNEL_HOME=/opt/seatunnel
ENV PATH=$PATH:$SEATUNNEL_HOME

RUN tar -xvf /home/software/apache-seatunnel-2.3.5.tar -C /opt \
    && mv /opt/*seatunnel*  $SEATUNNEL_HOME

RUN wget https://mirrors.huaweicloud.com/apache/flink/flink-${FLINK_VERSION}/flink-${FLINK_VERSION}-bin-scala_2.12.tgz \
    && tar -zxvf flink-*.tgz -C /opt \
    && rm -f flink-*.tgz && mv /opt/flink-*  ${FLINK_HOME}

# flink 增加jar包
COPY /home/software/flink/commons-cli-1.4.jar ${FLINK_HOME}/lib

ENV SPARK_HOME=/opt/spark
ENV SPARK_VERSION=3.2.3
ENV PATH=$PATH:$SPARK_HOME/bin

RUN wget https://mirrors.huaweicloud.com/apache/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop3.2.tgz \
    && tar -zxvf spark-*.tgz -C /opt \
    && rm -f spark-*.tgz && mv /opt/spark-*  ${SPARK_HOME}

# support iceberg
RUN wget https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark-runtime-3.2_2.12/1.2.1/iceberg-spark-runtime-3.2_2.12-1.2.1.jar \
    && mv iceberg-spark-runtime*.jar ${SPARK_HOME}/jars/


ENV HIVE_HOME=/opt/hive
ENV HIVE_VERSION=3.1.3
ENV PATH=$PATH:$HIVE_HOME/bin

WORKDIR /tmp

RUN wget https://mirrors.huaweicloud.com/apache/hive/hive-${HIVE_VERSION}/apache-hive-${HIVE_VERSION}-bin.tar.gz \
    && tar -zxvf apache-hive-*.tar.gz -C /opt \
    && rm -f apache-hive-*-bin.tar.gz && mv /opt/apache-hive-*  $HIVE_HOME


ENV MYSQL_CONN_VERSION=8.0.20
ENV MYSQL_CONN_URL=https://downloads.mysql.com/archives/get/p/3/file/mysql-connector-java-${MYSQL_CONN_VERSION}.tar.gz
RUN  curl -fSL $MYSQL_CONN_URL -o /tmp/mysql-connector-java.tar.gz     && tar -xzvf /tmp/mysql-connector-java.tar.gz -C /tmp/   \
  && cp /tmp/mysql-connector-java-${MYSQL_CONN_VERSION}/mysql-connector-java-${MYSQL_CONN_VERSION}.jar $HIVE_HOME/lib/mysql-connector-java.jar  \
  && rm -rf /tmp/mysql-connector-*

ENV DOLPHINSCHEDULER_HOME=/opt/dolphinscheduler
ENV DOLPHINSCHEDULER_VERSION=3.2.2
ENV PATH=$PATH:$DOLPHINSCHEDULER_HOME/bin


#RUN wget https://mirrors.huaweicloud.com/apache/dolphinscheduler/${DOLPHINSCHEDULER_VERSION}/apache-dolphinscheduler-${DOLPHINSCHEDULER_VERSION}-bin.tar.gz \
#    && tar -zxvf apache-dolphinscheduler-*.tar.gz -C /opt \
#    && rm -f apache-dolphinscheduler-*.tar.gz && mv /opt/*dolphinscheduler*  $DOLPHINSCHEDULER_HOME
RUN tar -zxvf /home/software/dolphinscheduler/apache-dolphinscheduler-dev-SNAPSHOT-bin.tar.gz -C /opt \
    && mv /opt/*dolphinscheduler*  $DOLPHINSCHEDULER_HOME


RUN wget https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.28/mysql-connector-java-8.0.28.jar \
    && cp mysql-connector-java-8.0.28.jar $DOLPHINSCHEDULER_HOME/alert-server/libs/mysql-connector-java-8.0.28.jar \
    && cp mysql-connector-java-8.0.28.jar $DOLPHINSCHEDULER_HOME/api-server/libs/mysql-connector-java-8.0.28.jar \
    && cp mysql-connector-java-8.0.28.jar $DOLPHINSCHEDULER_HOME/master-server/libs/mysql-connector-java-8.0.28.jar \
    && cp mysql-connector-java-8.0.28.jar $DOLPHINSCHEDULER_HOME/tools/libs/mysql-connector-java-8.0.28.jar \
    && cp mysql-connector-java-8.0.28.jar $DOLPHINSCHEDULER_HOME/worker-server/libs/mysql-connector-java-8.0.28.jar \
    && rm -f mysql-connector-java-8.0.28.jar

RUN apt-get install -y mysql-client

# 更新包列表并安装必要的工具
RUN apt-get update && \
    apt-get install -y software-properties-common && \
    add-apt-repository ppa:deadsnakes/ppa && \
    apt-get update && \
    apt-get install -y python3.11 python3.11-dev python3.11-venv python3-pip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 设置默认的 Python 版本
RUN update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.11 1 && \
    update-alternatives --install /usr/bin/python python /usr/bin/python3.11 1

# 设置 PYTHON_HOME
ENV PYTHON_HOME=/usr/bin/python
ENV PYTHON_LAUNCHER=/usr/bin/python

# 设置 python 和 pip 命令指向 Python 3.11
RUN ln -sf /usr/bin/python3.11 /usr/bin/python && \
    rm -f /usr/bin/pip && \
    ln -s /usr/bin/pip3 /usr/bin/pip

# 验证 Python 和 pip 版本
RUN python --version && pip --version

RUN chsrc set python first

RUN \cp -f /opt/hive/lib/mysql-connector-java.jar $DOLPHINSCHEDULER_HOME/libs/

RUN apt-get update && apt-get install -y sudo

WORKDIR $DOLPHINSCHEDULER_HOME

